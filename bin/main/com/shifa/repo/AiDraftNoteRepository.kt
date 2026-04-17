package com.shifa.repo

import com.shifa.domain.AiDraftNote
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface AiDraftNoteRepository : JpaRepository<AiDraftNote, UUID> {
    fun findByDoctorIdAndStatusOrderByCreatedAtDesc(
        doctorId: Long,
        status: AiDraftNote.Status
    ): List<AiDraftNote>

    fun findByStatusAndCreatedAtBefore(status: AiDraftNote.Status, before: Instant): List<AiDraftNote>
}
