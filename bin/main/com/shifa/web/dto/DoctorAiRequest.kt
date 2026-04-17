package com.shifa.web.dto

import com.shifa.ai.OutputLanguage
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class DoctorAiRequest(
    val patientId: Long?,
    val consultationId: Long? = null,
    @get:NotBlank(message = "Question must not be empty")
    @get:Size(max = 4000, message = "Question must not exceed 4000 characters")
    val question: String,
    val language: OutputLanguage
)
