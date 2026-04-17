package com.shifa.repo

import com.shifa.domain.ConsultationNote
import org.springframework.data.jpa.repository.JpaRepository

interface ConsultationNoteRepository : JpaRepository<ConsultationNote, Long> {
    fun findByDoctorIdAndPatientIdOrderByCreatedAtDesc(
        doctorId: Long,
        patientId: Long
    ): List<ConsultationNote>

    fun findByAppointmentIdOrderByCreatedAtAsc(appointmentId: Long): List<ConsultationNote>
}
