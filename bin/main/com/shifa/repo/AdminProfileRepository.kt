package com.shifa.repo

import com.shifa.domain.AdminProfile
import com.shifa.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AdminProfileRepository : JpaRepository<AdminProfile, Long> {
    fun findByUser(user: User): Optional<AdminProfile>
    fun findByUserId(userId: Long): Optional<AdminProfile>
}
