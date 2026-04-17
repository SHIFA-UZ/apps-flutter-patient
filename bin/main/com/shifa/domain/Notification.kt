package com.shifa.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "notifications")
class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "patient_id")
    val patient: PatientProfile? = null,

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    val doctor: DoctorProfile? = null,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val message: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: Type,

    @Column(name = "appointment_id")
    val appointmentId: Long? = null,

    @Column(name = "document_access_request_id")
    val documentAccessRequestId: Long? = null,

    @Column(name = "task_id")
    val taskId: Long? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "read_at")
    var readAt: Instant? = null
) {
    enum class Type {
        APPOINTMENT_CANCELLED,
        APPOINTMENT_CHANGED,
        APPOINTMENT_REMINDER,
        APPOINTMENT_ASSIGNED,
        SIGNATURE_REQUESTED,
        TASK_REMINDER,
        TASK_COMPLETED,
        DOCUMENT_ACCESS_REQUEST,
        DOCUMENT_ACCESS_APPROVED,
        DOCUMENT_ACCESS_REJECTED,
        TASK_ASSIGNED,
        TASK_CANCELLED,
        GENERAL
    }

    fun isRead(): Boolean = readAt != null
}
