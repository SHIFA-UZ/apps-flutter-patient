package com.shifa.repo

import com.shifa.domain.Role
import com.shifa.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun findByPhone(phone: String): Optional<User>
    fun findByUsername(username: String): Optional<User>
    fun findByRole(role: Role, pageable: Pageable): Page<User>
    fun findByEnabled(enabled: Boolean, pageable: Pageable): Page<User>
    fun findByRoleAndEnabled(role: Role, enabled: Boolean, pageable: Pageable): Page<User>
    fun countByRole(role: Role): Long
    fun countByRoleAndEnabled(role: Role, enabled: Boolean): Long
    
    // Search users by query string (email or phone) for specific role, excluding a user ID
    @org.springframework.data.jpa.repository.Query(
        "SELECT u FROM User u WHERE u.id != :excludeUserId AND u.role = :role AND " +
        "(LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :query, '%')))"
    )
    fun searchByRoleAndQuery(
        excludeUserId: Long,
        role: Role,
        query: String
    ): List<User>
}
