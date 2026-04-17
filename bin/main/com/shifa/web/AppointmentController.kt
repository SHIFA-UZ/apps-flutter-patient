// src/main/kotlin/com/shifa/web/AppointmentController.kt
package com.shifa.web

import com.shifa.domain.Appointment
import com.shifa.domain.Notification
import com.shifa.repo.AppointmentRepository
import com.shifa.repo.ConsultationNoteRepository
import com.shifa.repo.NotificationRepository
import com.shifa.security.DoctorPrincipal
import com.shifa.service.FcmService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.ZoneId
import java.time.*

@RestController
@RequestMapping("/api/appointments")
class AppointmentController(
    private val appts: AppointmentRepository,
    private val notifications: NotificationRepository,
    private val consultationNoteRepo: ConsultationNoteRepository,
    private val fcmService: FcmService
) {

    // -------------------- Doctor: get single appointment (for polling signature status) --------------------

    data class AppointmentDto(
        val id: Long,
        val patientId: Long?,
        val patientName: String?,
        val doctorId: Long?,
        val startAt: String,
        val endAt: String,
        val location: String,
        val status: String,
        val signatureRequested: Boolean,
        val patientSignedAt: String?,
        val patientSignatureImageBase64: String?
    )

    @GetMapping("/{appointmentId}")
    fun getById(
        @AuthenticationPrincipal principal: DoctorPrincipal,
        @PathVariable appointmentId: Long
    ): AppointmentDto {
        val doctor = principal.profile
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val appointment = appts.findById(appointmentId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found: $appointmentId")
            }
        if (appointment.doctor.id != doctor.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Appointment does not belong to this doctor")
        }
        return AppointmentDto(
            id = appointment.id,
            patientId = appointment.patient?.id,
            patientName = appointment.patient?.fullName,
            doctorId = appointment.doctor.id,
            startAt = appointment.startAt.toString(),
            endAt = appointment.endAt.toString(),
            location = appointment.location,
            status = appointment.status.name,
            signatureRequested = appointment.signatureRequested,
            patientSignedAt = appointment.patientSignedAt?.toString(),
            patientSignatureImageBase64 = appointment.patientSignatureImage
        )
    }

    // -------------------- Doctor: request patient signature --------------------

    /** Consultation notes for this appointment (e.g. saved Shifa AI drafts). Shown in appointment documentation with "From Shifa AI" badge when source == AI_DRAFT. */
    data class ConsultationNoteDto(
        val id: Long,
        val body: String?,
        val subjective: String?,
        val assessment: String?,
        val plan: String?,
        val source: String,
        val createdAt: String
    )

    @GetMapping("/{appointmentId}/consultation-notes")
    fun getConsultationNotes(
        @AuthenticationPrincipal principal: DoctorPrincipal,
        @PathVariable appointmentId: Long
    ): List<ConsultationNoteDto> {
        val doctor = principal.profile
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val appointment = appts.findById(appointmentId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found: $appointmentId")
            }
        if (appointment.doctor.id != doctor.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Appointment does not belong to this doctor")
        }
        val notes = consultationNoteRepo.findByAppointmentIdOrderByCreatedAtAsc(appointmentId)
        return notes.map { n ->
            ConsultationNoteDto(
                id = n.id!!,
                body = n.body,
                subjective = n.subjective,
                assessment = n.assessment,
                plan = n.plan,
                source = n.source,
                createdAt = n.createdAt.toString()
            )
        }
    }

    @PutMapping("/{appointmentId}/request-signature")
    fun requestSignature(
        @AuthenticationPrincipal principal: DoctorPrincipal,
        @PathVariable appointmentId: Long
    ) {
        val doctor = principal.profile
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val appointment = appts.findById(appointmentId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found: $appointmentId")
            }
        if (appointment.doctor.id != doctor.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Appointment does not belong to this doctor")
        }
        appointment.signatureRequested = true
        appts.save(appointment)

        val doctorName = "${doctor.firstName ?: ""} ${doctor.lastName ?: ""}".trim().ifEmpty { "Doctor" }
        val notif = Notification(
            patient = appointment.patient,
            title = "Signature Requested",
            message = "Dr. $doctorName is requesting your signature for the appointment summary.",
            type = Notification.Type.SIGNATURE_REQUESTED,
            appointmentId = appointment.id
        )
        val savedNotif = notifications.save(notif)
        appointment.patient.fcmToken?.let { fcmService.sendPatientNotification(it, savedNotif) }
    }

    /** startAt: ISO 8601 UTC. */
    data class ChangeSlotReq(
        val startAt: String,
        val slotMinutes: Int
    )

    @DeleteMapping("/{appointmentId}")
    fun cancel(
        @AuthenticationPrincipal principal: DoctorPrincipal,
        @PathVariable appointmentId: Long
    ) {
        val doctor = principal.profile
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)

        val appointment = appts.findById(appointmentId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found: $appointmentId")
            }

        // Verify the appointment belongs to this doctor
        if (appointment.doctor.id != doctor.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Appointment does not belong to this doctor")
        }

        // Past appointments cannot be cancelled
        if (appointment.startAt.isBefore(Instant.now())) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel a past appointment")
        }

        // Cancel the appointment
        appointment.status = Appointment.Status.CANCELLED
        appts.save(appointment)

        val zone = ZoneId.of(doctor.timeZone)
        val startLdt = appointment.startAt.atZone(zone).toLocalDateTime()
        val monthName = startLdt.month.name.lowercase().replaceFirstChar { it.uppercase() }
        val dateStr = "${startLdt.dayOfMonth} $monthName ${startLdt.year}"
        
        val notif = com.shifa.domain.Notification(
            patient = appointment.patient,
            title = "Appointment Cancelled",
            message = "Doctor has cancelled your appointment on $dateStr. Please make another appointment.",
            type = com.shifa.domain.Notification.Type.APPOINTMENT_CANCELLED,
            appointmentId = appointment.id
        )
        val savedNotif = notifications.save(notif)
        appointment.patient.fcmToken?.let { fcmService.sendPatientNotification(it, savedNotif) }
    }

    @PutMapping("/{appointmentId}/complete")
    fun complete(
        @AuthenticationPrincipal principal: DoctorPrincipal,
        @PathVariable appointmentId: Long
    ) {
        val doctor = principal.profile
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)

        val appointment = appts.findById(appointmentId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found: $appointmentId")
            }

        // Verify the appointment belongs to this doctor
        if (appointment.doctor.id != doctor.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Appointment does not belong to this doctor")
        }

        // Mark appointment as completed
        appointment.status = Appointment.Status.COMPLETED
        appts.save(appointment)
    }

    @PutMapping("/{appointmentId}/change")
    fun changeSlot(
        @AuthenticationPrincipal principal: DoctorPrincipal,
        @PathVariable appointmentId: Long,
        @RequestBody req: ChangeSlotReq
    ) {
        val doctor = principal.profile
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)

        val appointment = appts.findById(appointmentId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found: $appointmentId")
            }

        // Verify the appointment belongs to this doctor
        if (appointment.doctor.id != doctor.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Appointment does not belong to this doctor")
        }

        // Past appointments cannot be changed
        if (appointment.startAt.isBefore(Instant.now())) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change a past appointment")
        }

        val newStartAt = Instant.parse(req.startAt)
        // Cannot move appointment to a past date or time
        if (newStartAt.isBefore(Instant.now())) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move appointment to a past date or time")
        }
        val newEndAt = newStartAt.plusSeconds(req.slotMinutes * 60L)
        val zone = ZoneId.of(doctor.timeZone)

        // Check for overlaps for the doctor (excluding the current appointment)
        val overlapping = appts.findOverlapping(doctor.id!!, newStartAt, newEndAt)
            .filter { it.id != appointmentId }
        
        if (overlapping.isNotEmpty()) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Overlapping appointment exists")
        }

        // Check for overlapping appointments for the patient (prevent double booking)
        val patientOverlapping = appts.findOverlappingForPatient(
            appointment.patient?.id ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"),
            newStartAt,
            newEndAt
        ).filter { it.id != appointmentId }

        if (patientOverlapping.isNotEmpty()) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Patient already has an appointment scheduled at this date and time. Please choose a different time slot."
            )
        }

        val oldStartLdt = appointment.startAt.atZone(zone).toLocalDateTime()
        val newStartLdt = newStartAt.atZone(zone).toLocalDateTime()
        appointment.startAt = newStartAt
        appointment.endAt = newEndAt
        appts.save(appointment)

        val oldMonthName = oldStartLdt.month.name.lowercase().replaceFirstChar { it.uppercase() }
        val newMonthName = newStartLdt.month.name.lowercase().replaceFirstChar { it.uppercase() }
        val oldDateStr = "${oldStartLdt.dayOfMonth} $oldMonthName ${oldStartLdt.year}"
        val newDateStr = "${newStartLdt.dayOfMonth} $newMonthName ${newStartLdt.year}"
        
        val notif = com.shifa.domain.Notification(
            patient = appointment.patient,
            title = "Appointment Changed",
            message = "Doctor changed your appointment from $oldDateStr to $newDateStr. If you are okay with this appointment, that's good, otherwise reschedule yourself.",
            type = com.shifa.domain.Notification.Type.APPOINTMENT_CHANGED,
            appointmentId = appointment.id
        )
        val savedNotif = notifications.save(notif)
        appointment.patient.fcmToken?.let { fcmService.sendPatientNotification(it, savedNotif) }
    }
}
