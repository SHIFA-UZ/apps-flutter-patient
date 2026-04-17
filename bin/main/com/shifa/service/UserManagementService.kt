package com.shifa.service

import com.shifa.domain.AdminLevel
import com.shifa.domain.AdminProfile
import com.shifa.domain.Role
import com.shifa.domain.User
import com.shifa.repo.AdminProfileRepository
import com.shifa.repo.DoctorProfileRepository
import com.shifa.repo.PatientProfileRepository
import com.shifa.repo.UserRepository
import com.shifa.repo.UserSessionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.*

@Service
class UserManagementService(
    private val userRepository: UserRepository,
    private val adminProfileRepository: AdminProfileRepository,
    private val doctorProfileRepository: DoctorProfileRepository,
    private val patientProfileRepository: PatientProfileRepository,
    private val userSessionRepository: UserSessionRepository,
    private val passwordEncoder: PasswordEncoder
) {
    
    /**
     * Create a new admin user (role ADMIN) with an admin profile.
     * Only creates admin users; email must be unique.
     */
    @Transactional
    fun createAdminUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        adminLevel: AdminLevel = AdminLevel.ADMIN
    ): User {
        if (userRepository.findByEmail(email).isPresent) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered")
        }
        val user = User(
            email = email.trim().lowercase(),
            passwordHash = passwordEncoder.encode(password),
            role = Role.ADMIN,
            enabled = true
        )
        val savedUser = userRepository.save(user)
        val profile = AdminProfile(
            user = savedUser,
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            adminLevel = adminLevel
        )
        adminProfileRepository.save(profile)
        return savedUser
    }

    /**
     * Get all users with pagination and filtering
     */
    fun listUsers(
        role: Role? = null,
        enabled: Boolean? = null,
        pageable: Pageable
    ): Page<User> {
        return when {
            role != null && enabled != null -> userRepository.findByRoleAndEnabled(role, enabled, pageable)
            role != null -> userRepository.findByRole(role, pageable)
            enabled != null -> userRepository.findByEnabled(enabled, pageable)
            else -> userRepository.findAll(pageable)
        }
    }
    
    /**
     * Get user by ID
     */
    fun getUserById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("User not found: $userId") }
    }
    
    /**
     * Activate/deactivate user
     */
    @Transactional
    fun setUserEnabled(userId: Long, enabled: Boolean): User {
        val user = getUserById(userId)
        user.enabled = enabled
        return userRepository.save(user)
    }
    
    /**
     * Reset user password (generates temporary password)
     */
    @Transactional
    fun resetPassword(userId: Long): String {
        val user = getUserById(userId)
        // Generate temporary password (8-12 chars, alphanumeric)
        val tempPassword = generateTemporaryPassword()
        user.passwordHash = passwordEncoder.encode(tempPassword)
        userRepository.save(user)
        return tempPassword
    }
    
    /**
     * Force logout - revoke all active sessions
     */
    @Transactional
    fun forceLogout(userId: Long, revokedBy: User? = null) {
        val user = getUserById(userId)
        val now = OffsetDateTime.now()
        userSessionRepository.revokeAllUserSessions(userId, now)
    }
    
    /**
     * Unlock user account (clear lockout)
     */
    @Transactional
    fun unlockUser(userId: Long): User {
        val user = getUserById(userId)
        user.lockedUntil = null
        user.failedLoginAttempts = 0
        return userRepository.save(user)
    }
    
    /**
     * Update last login timestamp
     */
    @Transactional
    fun updateLastLogin(userId: Long) {
        val user = getUserById(userId)
        user.lastLoginAt = OffsetDateTime.now()
        user.failedLoginAttempts = 0 // Reset on successful login
        userRepository.save(user)
    }
    
    /**
     * Increment failed login attempts
     */
    @Transactional
    fun incrementFailedLoginAttempts(userId: Long, maxAttempts: Int = 5, lockoutMinutes: Int = 30) {
        val user = getUserById(userId)
        user.failedLoginAttempts = (user.failedLoginAttempts ?: 0) + 1
        
        if (user.failedLoginAttempts >= maxAttempts) {
            user.lockedUntil = OffsetDateTime.now().plusMinutes(lockoutMinutes.toLong())
        }
        
        userRepository.save(user)
    }
    
    /**
     * Get user's active sessions
     */
    fun getActiveSessions(userId: Long): List<com.shifa.domain.UserSession> {
        return userSessionRepository.findActiveSessionsByUserId(userId, OffsetDateTime.now())
    }
    
    /**
     * Get user's session history
     */
    fun getSessionHistory(userId: Long): List<com.shifa.domain.UserSession> {
        return userSessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
    }
    
    /**
     * Generate temporary password
     */
    private fun generateTemporaryPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..12)
            .map { chars.random() }
            .joinToString("")
    }
    
    /**
     * Get user profile info (doctor or patient)
     */
    fun getUserProfileInfo(userId: Long): Map<String, Any> {
        val user = getUserById(userId)
        val info = mutableMapOf<String, Any>(
            "userId" to user.id,
            "email" to (user.email ?: ""),
            "phone" to (user.phone ?: ""),
            "role" to user.role.name,
            "enabled" to user.enabled,
            "lastLoginAt" to (user.lastLoginAt?.toString() ?: ""),
            "failedLoginAttempts" to (user.failedLoginAttempts ?: 0),
            "lockedUntil" to (user.lockedUntil?.toString() ?: "")
        )
        
        when (user.role) {
            Role.DOCTOR -> {
                doctorProfileRepository.findByUserId(userId).ifPresent { doctor ->
                    info["doctorId"] = doctor.id
                    info["firstName"] = doctor.firstName
                    info["lastName"] = doctor.lastName
                    info["clinic"] = doctor.clinic ?: ""
                }
            }
            Role.ADMIN -> {
                adminProfileRepository.findByUserId(userId).ifPresent { admin ->
                    info["firstName"] = admin.firstName
                    info["lastName"] = admin.lastName
                    info["adminLevel"] = admin.adminLevel.name
                }
            }
            Role.PATIENT -> {
                // Patient profiles don't have user_id link, need to find by phone/email
                // This is a limitation - consider adding user_id to patient_profiles
            }
            else -> {}
        }
        
        return info
    }
}
