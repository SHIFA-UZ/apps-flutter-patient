package com.shifa.domain

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity @Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true) var email: String? = null,
    @Column(unique = true) var phone: String? = null,
    @Column(unique = true) var username: String? = null,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) var role: Role = Role.DOCTOR,

    @Column(nullable = false) var enabled: Boolean = true,
    
    @Column(name = "force_password_reset", nullable = false)
    var forcePasswordReset: Boolean = false,
    
    @Column(name = "last_login_at")
    var lastLoginAt: OffsetDateTime? = null,
    
    @Column(name = "failed_login_attempts", nullable = false)
    var failedLoginAttempts: Int = 0,
    
    @Column(name = "locked_until")
    var lockedUntil: OffsetDateTime? = null
) {
    fun isLocked(): Boolean = lockedUntil != null && lockedUntil!!.isAfter(OffsetDateTime.now())
}
