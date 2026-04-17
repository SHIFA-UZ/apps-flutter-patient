package com.shifa.repo

import com.shifa.domain.DocumentAccessGrant
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentAccessGrantRepository : JpaRepository<DocumentAccessGrant, Long> {

    fun existsByDocument_IdAndDoctor_Id(documentId: Long, doctorId: Long): Boolean
}
