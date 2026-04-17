package com.shifa.web

import com.shifa.domain.*
import com.shifa.repo.*
import com.shifa.security.JwtService
import com.shifa.util.PhoneNormalizer
import java.time.ZoneOffset
import com.shifa.security.PasswordPolicy
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val users: UserRepository,
    private val invites: InvitationKeyRepository,
    private val doctors: DoctorProfileRepository,
    private val patients: com.shifa.repo.PatientProfileRepository,
    private val userSessions: UserSessionRepository,
    private val encoder: PasswordEncoder,
    private val jwt: JwtService,
    private val userActivityService: com.shifa.service.UserActivityService,
    private val userManagementService: com.shifa.service.UserManagementService,
    private val userRoles: com.shifa.repo.UserRoleRepository,
    private val firebaseAuthService: com.shifa.service.FirebaseAuthService
) {
    private val log = LoggerFactory.getLogger(AuthController::class.java)
    // ---------- VerifyKey ----------
    data class VerifyKeyRequest(@field:NotBlank val key: String)
    data class VerifyKeyResponse(val valid: Boolean)

    /**
     * VerifyKeyScreen calls this to check if the one-time key is valid & not consumed.
     * UI: show "Next" if valid, else show error.
     */
    @PostMapping("/verify-key")
    fun verifyKey(@RequestBody @Valid req: VerifyKeyRequest) =
        VerifyKeyResponse(invites.findByKeyCode(req.key.trim())?.consumed == false)

    // ---------- Check existing patient (for doctor registration UX) ----------
    data class CheckExistingPatientRequest(
        @field:NotBlank val firstName: String,
        @field:NotBlank val lastName: String,
        @field:NotBlank val phone: String
    )
    data class CheckExistingPatientResponse(
        val found: Boolean,
        val fullName: String? = null,
        val photoUrl: String? = null,
        val email: String? = null
    )

    /**
     * Doctor app calls this after user enters first name, last name, phone.
     * If a user exists with this phone (e.g. existing patient account), returns found=true and their details
     * so the UI can show "There is already a patient... we are creating a doctor account" and hide extra fields.
     */
    @PostMapping("/check-existing-patient")
    fun checkExistingPatient(@RequestBody @Valid req: CheckExistingPatientRequest): CheckExistingPatientResponse {
        val phoneTrimmed = req.phone.trim()
        val phoneNorm = PhoneNormalizer.normalize(phoneTrimmed)
        val existingUser = users.findByPhone(phoneTrimmed).orElse(null)
            ?: phoneNorm?.let { users.findByPhone(it).orElse(null) }
            ?: return CheckExistingPatientResponse(found = false)
        val patientProfile = patients.findByUserId(existingUser.id).orElse(null)
        val fullName = patientProfile?.fullName?.takeIf { it.isNotBlank() }
            ?: "${req.firstName.trim()} ${req.lastName.trim()}".trim()
        return CheckExistingPatientResponse(
            found = true,
            fullName = fullName,
            photoUrl = patientProfile?.photoUrl?.takeIf { it.isNotBlank() },
            email = existingUser.email?.takeIf { it.isNotBlank() }
        )
    }

    // ---------- Verify Firebase ID Token (Doctor Phone OTP) ----------
    /**
     * Doctor app sends Firebase ID token after phone OTP sign-in.
     * Backend verifies token, resolves user by phone, checks DOCTOR role and enabled; returns JWT.
     */
    @PostMapping("/verify")
    fun verifyFirebaseToken(@RequestHeader("Authorization") authorization: String?): TokenResponse {
        val bearer = authorization?.takeIf { it.startsWith("Bearer ") }?.substring(7)?.trim()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Bearer token")
        if (!firebaseAuthService.isConfigured()) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Firebase verification not configured")
        }
        val decodedToken = firebaseAuthService.verifyIdToken(bearer)
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token") }
        val uid = decodedToken.uid
        val phone = firebaseAuthService.getPhoneNumberByUid(uid)
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access restricted to doctors.")
        val user = users.findByPhone(phone).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access restricted to doctors.")
        if (user.role != Role.DOCTOR) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access restricted to doctors.")
        }
        if (!user.enabled) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Your account has been blocked.")
        }
        val principal = user.email ?: user.phone!!
        val tokenResult = jwt.generate(user.id, principal, Role.DOCTOR.name)
        userSessions.save(
            UserSession(
                user = user,
                tokenJti = tokenResult.jti,
                expiresAt = OffsetDateTime.ofInstant(tokenResult.expiresAt, ZoneOffset.UTC)
            )
        )
        userManagementService.updateLastLogin(user.id)
        return TokenResponse(token = tokenResult.token, forcePasswordReset = user.forcePasswordReset)
    }

    // ---------- Register ----------
    data class RegisterRequest(
        @field:NotBlank val firstName: String,
        @field:NotBlank val lastName: String,
        @field:NotBlank val phone: String,
        @field:Email val email: String?,
        @field:NotBlank val password: String,
        @field:NotBlank val key: String
    )
    data class TokenResponse(
        val token: String,
        val forcePasswordReset: Boolean = false // ✅ NEW
    )

    /**
     * Creates a DOCTOR user (or adds DOCTOR role to an existing user, e.g. patient creating doctor account).
     * Saves DoctorProfile, consumes the invitation key, and returns JWT.
     */
    @PostMapping("/register")
    fun register(@RequestBody @Valid r: RegisterRequest): TokenResponse {
        // SECURITY (NEW): Enforce strong password before hashing; never log password.
        PasswordPolicy.validate(r.password)?.let { msg ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, msg)
        }
        val inv = invites.findByKeyCode(r.key.trim())
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid key")
        if (inv.consumed) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Key already used")
        }

        val phoneTrimmed = r.phone.trim()
        val emailTrimmed = r.email?.trim()
        val existingUser = users.findByPhone(phoneTrimmed).orElse(null)
            ?: emailTrimmed?.let { users.findByEmail(it).orElse(null) }

        val user = if (existingUser != null) {
            // Existing account (e.g. patient): add DOCTOR role and doctor profile; copy patient avatar if present
            if (!existingUser.enabled) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled.")
            }
            if (!userRoles.existsByUserIdAndRole(existingUser.id, Role.DOCTOR)) {
                userRoles.save(
                    com.shifa.domain.UserRole(user = existingUser, role = Role.DOCTOR)
                )
            }
            val patientProfile = patients.findByUserId(existingUser.id).orElse(null)
            val patientPhotoUrl = patientProfile?.photoUrl?.takeIf { it.isNotBlank() }
            if (doctors.findByUserId(existingUser.id).isEmpty) {
                doctors.save(
                    DoctorProfile(
                        user = existingUser,
                        firstName = r.firstName.trim(),
                        lastName = r.lastName.trim(),
                        avatarUrl = patientPhotoUrl
                    )
                )
            } else {
                // Already has doctor profile: ensure avatar is set from patient if doctor has none
                val existingDoctor = doctors.findByUserId(existingUser.id).orElse(null)
                if (existingDoctor != null && (existingDoctor.avatarUrl.isNullOrBlank()) && patientPhotoUrl != null) {
                    existingDoctor.avatarUrl = patientPhotoUrl
                    doctors.save(existingDoctor)
                }
            }
            existingUser.role = Role.DOCTOR
            existingUser.passwordHash = encoder.encode(r.password)
            users.save(existingUser)
            log.info("Added DOCTOR role and profile to existing user ${existingUser.id}")
            existingUser
        } else {
            // New user: create user, DOCTOR role, and doctor profile
            users.save(
                User(
                    email = emailTrimmed,
                    phone = phoneTrimmed,
                    passwordHash = encoder.encode(r.password),
                    role = Role.DOCTOR
                )
            ).also { newUser ->
                userRoles.save(
                    com.shifa.domain.UserRole(user = newUser, role = Role.DOCTOR)
                )
                doctors.save(
                    DoctorProfile(
                        user = newUser,
                        firstName = r.firstName.trim(),
                        lastName = r.lastName.trim()
                    )
                )
            }
        }

        // Consume the invitation key
        inv.consumed = true
        inv.consumedAt = OffsetDateTime.now()
        inv.consumedByUserId = user.id
        invites.save(inv)

        val principal = user.email ?: user.phone!!
        val tokenResult = jwt.generate(user.id, principal, Role.DOCTOR.name)
        userSessions.save(
            UserSession(
                user = user,
                tokenJti = tokenResult.jti,
                expiresAt = OffsetDateTime.ofInstant(tokenResult.expiresAt, ZoneOffset.UTC)
            )
        )
        return TokenResponse(
            token = tokenResult.token,
            forcePasswordReset = user.forcePasswordReset
        )
    }

    // ---------- Login ----------
    data class LoginRequest(@field:NotBlank val username: String, @field:NotBlank val password: String)

    /**
     * Accepts email OR phone as 'username'. Returns JWT on success.
     * 
     * App-based role enforcement:
     * - Query param ?app=doctor or header X-App: doctor → requires DOCTOR role
     * - Query param ?app=patient or header X-App: patient → requires PATIENT role (auto-adds if missing for existing users)
     * - If app param/header not provided → uses user's primary role (backward compatibility)
     */
	
	@PostMapping("/login", produces = ["application/json"])
	fun login(
        @RequestBody @Valid r: LoginRequest,
        @RequestParam(required = false) app: String?,
        request: jakarta.servlet.http.HttpServletRequest
    ): TokenResponse {
		// Validate request
		if (r.username.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required")
		}
		if (r.password.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required")
		}

		val user = try {
			val foundUser = users.findByUsername(r.username).orElseGet {
				users.findByEmail(r.username).orElseGet {
					users.findByPhone(r.username)
					.orElseThrow {
						// SECURITY (NEW): Log suspicious activity (failed login - unknown identifier) without revealing if user exists
						val clientIp = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim() ?: request.remoteAddr
						log.warn("Failed login attempt (unknown identifier) from ip={} path={}", clientIp, request.servletPath)
						ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
					}
				}
			}
			foundUser
		} catch (e: ResponseStatusException) {
			throw e
		} catch (e: Exception) {
			log.error("Login unexpected exception: ${e.message}", e)
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Login error: ${e.message}")
		}

		// Check if account is disabled
		if (!user.enabled) {
			userActivityService.logActivity(
				user = user,
				activityType = "LOGIN",
				success = false,
				failureReason = "Account disabled",
				request = request
			)
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is disabled")
		}

		// Check if account is locked
		if (user.isLocked()) {
			userActivityService.logActivity(
				user = user,
				activityType = "LOGIN",
				success = false,
				failureReason = "Account locked",
				request = request
			)
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is locked")
		}

		val passwordMatches = encoder.matches(r.password, user.passwordHash)
		
		if (!passwordMatches) {
			userActivityService.logActivity(
				user = user,
				activityType = "LOGIN",
				success = false,
				failureReason = "Invalid password",
				request = request
			)
			userManagementService.incrementFailedLoginAttempts(user.id)
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials - password mismatch")
		}

		// Log successful login
		userActivityService.logActivity(
			user = user,
			activityType = "LOGIN",
			success = true,
			request = request
		)
		
		// Update last login
		userManagementService.updateLastLogin(user.id)

        // App-based role enforcement (multi-role support)
        val appType = app?.lowercase() ?: request.getHeader("X-App")?.lowercase()
        val requiredRole = when (appType) {
            "doctor" -> Role.DOCTOR
            "patient" -> Role.PATIENT
            "admin" -> Role.ADMIN
            else -> null // No app specified, use primary role (backward compatibility)
        }

		if (requiredRole != null) {
			// Check if user has the required role
			val hasRole = userRoles.existsByUserIdAndRole(user.id, requiredRole)
			
			if (!hasRole) {
				if (requiredRole == Role.PATIENT) {
					// Auto-add PATIENT role for doctor logging into patient app
					// This allows a doctor to use the same credentials in patient app
					log.info("Auto-adding PATIENT role for user ${user.id} (doctor logging into patient app)")
					
					// Add PATIENT role to user_roles
					userRoles.save(
						com.shifa.domain.UserRole(
							user = user,
							role = Role.PATIENT
						)
					)
					
					// Ensure patient profile exists and is linked to this user
					// First check if user already has a patient profile (by user_id)
					val patientProfileByUserId = patients.findByUserId(user.id).orElse(null)
					
					val patientProfile = if (patientProfileByUserId != null) {
						// User already has a patient profile linked
						patientProfileByUserId
					} else {
						// Use normalized phone first so we match regardless of format and respect one-patient-per-phone
						val phoneNorm = user.phone?.let { PhoneNormalizer.normalize(it) }
						if (phoneNorm != null) {
							val existingByPhoneNorm = patients.findByPhoneNormalized(phoneNorm).orElse(null)
							if (existingByPhoneNorm != null) {
								if (existingByPhoneNorm.user == null) {
									existingByPhoneNorm.user = user
									patients.save(existingByPhoneNorm)
									log.info("Linked existing patient profile ${existingByPhoneNorm.id} to user ${user.id} (by normalized phone)")
									existingByPhoneNorm
								} else {
									// Same normalized phone already linked to another user
									throw ResponseStatusException(
										HttpStatus.CONFLICT,
										"Patient with this phone number already exists and is linked to another account."
									)
								}
							} else {
								null // fall through to exact phone/email or create
							}
						} else {
							null
						} ?: run {
						// Fallback: exact phone/email (e.g. legacy profiles without phone_normalized)
						val existingByPhone = user.phone?.let { patients.findByPhone(it).orElse(null) }
						val existingByEmail = user.email?.let { patients.findByEmail(it).orElse(null) }
						val existingUnlinked = existingByPhone ?: existingByEmail
						
						if (existingUnlinked != null && existingUnlinked.user == null) {
							// Link existing patient profile to user
							existingUnlinked.user = user
							patients.save(existingUnlinked)
							log.info("Linked existing patient profile ${existingUnlinked.id} to user ${user.id}")
							existingUnlinked
						} else {
							// Create new patient profile from doctor profile or user data; set phoneNormalized so uniqueness applies
							val doctorProfile = doctors.findByUserId(user.id).orElse(null)
							val newPatientProfile = com.shifa.domain.PatientProfile(
								fullName = doctorProfile?.let { "${it.firstName} ${it.lastName}".trim() }
									?: (user.email ?: user.phone ?: "Patient"),
								phone = user.phone,
								phoneNormalized = phoneNorm ?: user.phone?.let { PhoneNormalizer.normalize(it) },
								email = user.email,
								address = doctorProfile?.address,
								birthDate = doctorProfile?.dob,
								language = null,
								documents = mutableListOf()
							)
							newPatientProfile.user = user
							patients.save(newPatientProfile)
							log.info("Created patient profile for user ${user.id}")
							newPatientProfile
						}
						}
					}
				} else {
					// Doctor/Admin app requires the role - reject if missing
					userActivityService.logActivity(
						user = user,
						activityType = "LOGIN",
						success = false,
						failureReason = "Missing required role: ${requiredRole.name}",
						request = request
					)
					throw ResponseStatusException(
						HttpStatus.FORBIDDEN,
						"Access denied: This app requires ${requiredRole.name} role"
					)
				}
			}
		}

		// Determine which role to use for token (use required role if specified, else primary role)
		val tokenRole = requiredRole ?: user.role
		
		val principal = user.username ?: user.email ?: user.phone!!
		val tokenResult = jwt.generate(user.id, principal, tokenRole.name)
		userSessions.save(
			UserSession(
				user = user,
				tokenJti = tokenResult.jti,
				expiresAt = OffsetDateTime.ofInstant(tokenResult.expiresAt, ZoneOffset.UTC)
			)
		)
		return TokenResponse(
			token = tokenResult.token,
			forcePasswordReset = user.forcePasswordReset
		)
	}

    // ---------- Register Patient ----------
    data class RegisterPatientRequest(
        @field:NotBlank val firstName: String,
        @field:NotBlank val lastName: String,
        @field:NotBlank val phone: String,
        @field:Email val email: String?,
        @field:NotBlank val password: String,
        val birthDate: String? = null,
        val gender: String? = null,
        val address: String? = null,
        val language: String? = null
    )

    /**
     * Creates a PATIENT user, saves PatientProfile, and returns JWT.
     */
    @PostMapping("/register-patient")
    fun registerPatient(@RequestBody @Valid r: RegisterPatientRequest): TokenResponse {
        // SECURITY (NEW): Enforce strong password before hashing; never log password.
        PasswordPolicy.validate(r.password)?.let { msg ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, msg)
        }
        // Basic uniqueness checks (email/phone); use normalized phone for patient-profile uniqueness
        val phoneNormalized = PhoneNormalizer.normalize(r.phone)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid phone number")
        r.email?.let {
            if (users.findByEmail(it).isPresent) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered")
            }
        }
        if (users.findByPhone(phoneNormalized).isPresent || users.findByPhone(r.phone.trim()).isPresent) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Phone already registered")
        }
        if (patients.findByPhoneNormalized(phoneNormalized).isPresent) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Patient with this phone number already exists.")
        }

        val user = users.save(
            User(
                email = r.email?.trim(),
                phone = phoneNormalized,
                passwordHash = encoder.encode(r.password),
                role = Role.PATIENT
            )
        )

        // Add PATIENT role to user_roles (multi-role support)
        userRoles.save(
            com.shifa.domain.UserRole(
                user = user,
                role = Role.PATIENT
            )
        )

        // Create patient profile and link to user so doctor app shows "Account already available"
        val patientProfile = com.shifa.domain.PatientProfile(
            fullName = "${r.firstName.trim()} ${r.lastName.trim()}".trim(),
            phone = phoneNormalized,
            phoneNormalized = phoneNormalized,
            email = r.email?.trim(),
            address = r.address?.trim(),
            birthDate = r.birthDate?.let { java.time.LocalDate.parse(it) },
            language = r.language?.trim(),
            documents = mutableListOf()
        )
        patientProfile.user = user
        patients.save(patientProfile)

        val principal = user.email ?: user.phone!!
        val tokenResult = jwt.generate(user.id, principal, user.role.name)
        userSessions.save(
            UserSession(
                user = user,
                tokenJti = tokenResult.jti,
                expiresAt = OffsetDateTime.ofInstant(tokenResult.expiresAt, ZoneOffset.UTC)
            )
        )
        return TokenResponse(
            token = tokenResult.token,
            forcePasswordReset = user.forcePasswordReset
        )
    }

    // ---------- Change/Reset Password ----------
    // For normal change: requires currentPassword + newPassword.
    // For forced reset (forcePasswordReset=true): only newPassword needed.
    data class ChangePasswordRequest(
        val currentPassword: String? = null,
        @field:NotBlank val newPassword: String
    )

    // ---------- Forgot Password (Phone OTP) ----------
    data class ForgotPasswordResetRequest(
        @field:NotBlank val idToken: String,
        @field:NotBlank val newPassword: String
    )

    /**
     * Reset password using Firebase ID token (proves recent OTP verification).
     * Backend verifies token, finds user by phone, updates password, returns JWT for auto-login.
     */
    @PostMapping("/forgot-password-reset")
    fun forgotPasswordReset(@RequestBody @Valid r: ForgotPasswordResetRequest): TokenResponse {
        PasswordPolicy.validate(r.newPassword)?.let { msg ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, msg)
        }
        if (!firebaseAuthService.isConfigured()) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Firebase verification not configured")
        }
        val decodedToken = firebaseAuthService.verifyIdToken(r.idToken)
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token") }
        val phone = firebaseAuthService.getPhoneNumberByUid(decodedToken.uid)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number not found")
        val user = users.findByPhone(phone).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        if (user.role != Role.DOCTOR) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access restricted to doctors.")
        }
        user.passwordHash = encoder.encode(r.newPassword)
        user.forcePasswordReset = false
        users.save(user)
        val principal = user.email ?: user.phone!!
        val tokenResult = jwt.generate(user.id, principal, Role.DOCTOR.name)
        userSessions.save(
            UserSession(
                user = user,
                tokenJti = tokenResult.jti,
                expiresAt = OffsetDateTime.ofInstant(tokenResult.expiresAt, ZoneOffset.UTC)
            )
        )
        return TokenResponse(token = tokenResult.token, forcePasswordReset = false)
    }

    @PostMapping("/reset-password")
    fun resetPassword(
        @RequestBody @Valid r: ChangePasswordRequest,
        @AuthenticationPrincipal principal: UserDetails
    ): Map<String, String> {
        // SECURITY (NEW): Enforce strong password for new password; never log password.
        PasswordPolicy.validate(r.newPassword)?.let { msg ->
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, msg)
        }
        val userId = principal.username.toLong()
        val user = users.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        if (!user.forcePasswordReset) {
            if (r.currentPassword.isNullOrBlank()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is required")
            }
            if (!encoder.matches(r.currentPassword, user.passwordHash)) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect")
            }
        }

        user.passwordHash = encoder.encode(r.newPassword)
        user.forcePasswordReset = false
        users.save(user)

        return mapOf("message" to "Password updated successfully")
    }

}
