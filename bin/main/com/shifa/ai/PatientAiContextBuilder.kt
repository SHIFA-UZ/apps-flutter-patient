package com.shifa.ai

import com.shifa.repo.PatientDocumentRepository
import com.shifa.repo.PatientProfileRepository
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Period

@Component
class PatientAiContextBuilder(
    private val patientRepo: PatientProfileRepository,
    private val documentRepo: PatientDocumentRepository
) {

    fun build(patientId: Long): PatientAiContext {
        val patient = patientRepo.findById(patientId)
            .orElseThrow { IllegalArgumentException("Patient not found") }

        val age = patient.birthDate?.let {
            Period.between(it, LocalDate.now()).years
        }

        val documentSummaries = documentRepo
            .listForPatient(patientId)
            .take(5)
            .map { doc ->
                DocumentSummary(
                    title = doc.title,
                    date = doc.date
                )
            }

        return PatientAiContext(
            patientId = patient.id!!,
            age = age,
            language = patient.language, // nullable → OK
            documentSummaries = documentSummaries,
            appointmentSummaries = emptyList() // 👈 intentionally empty (for now)
        )
    }
}
