package com.shifa.repo

import com.shifa.domain.DocumentAccessRequest
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentAccessRequestRepository : JpaRepository<DocumentAccessRequest, Long> {

    fun existsByDocument_IdAndRequestingDoctor_IdAndStatus(
        documentId: Long,
        requestingDoctorId: Long,
        status: DocumentAccessRequest.Status
    ): Boolean

    fun findByDocument_IdAndRequestingDoctor_IdAndStatus(
        documentId: Long,
        requestingDoctorId: Long,
        status: DocumentAccessRequest.Status
    ): DocumentAccessRequest?
}
