package com.shifa.service

import com.shifa.ai.PatientAiContext
import com.shifa.ai.RedFlagEngine
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.shifa.ai.MedicalPromptBuilder
import com.shifa.ai.OutputLanguage
import com.shifa.config.OpenAiProperties
import com.shifa.domain.DoctorProfile
import com.shifa.web.AiStreamException
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class OpenAiResponsesService(
    private val props: OpenAiProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = jacksonObjectMapper()
    private val rateLimiter = SimpleRateLimiter(props.maxRequestsPerMinute)

    private val client = OkHttpClient.Builder()
		.connectTimeout(Duration.ofSeconds(10))
		.writeTimeout(Duration.ofSeconds(10))
		.readTimeout(Duration.ZERO) // 🔴 REQUIRED for SSE (infinite stream)
		.build()


    // Verify OpenAI key injection once at startup (no key material logged)
    @PostConstruct
    fun debugOpenAiKey() {
        log.info(
            "OpenAI key configured: present={}, length={}",
            props.apiKey.isNotBlank(),
            props.apiKey.length
        )
    }

    /**
     * SSE streaming doctor assistant.
     * Deltas are appended and emitted exactly as received from OpenAI; no token-level spacing logic.
     */
    fun streamDoctorAssistant(
        doctor: DoctorProfile,
        patientContext: PatientAiContext?,
        question: String,
        language: OutputLanguage
    ): Flow<String> = flow {
		
		val combinedInput = buildString {
    if (patientContext != null) {
        append(patientContext.toString())
        append(" ")
    }
    append(question)
}


		val redFlagResult = RedFlagEngine.analyze(combinedInput)
		if (redFlagResult.hasEmergency) {
			throw AiStreamException(
				code = "SAFETY_BLOCK",
				message = "This may represent a medical emergency. I cannot provide medical advice for this situation. Please seek immediate professional medical care or contact emergency services."
			)
		}

		if (!rateLimiter.tryAcquire()) {
			throw AiStreamException(
				code = "RATE_LIMIT",
				message = "AI rate limit exceeded. Please try again later."
			)
		}
		

// 🧠 Optional patient-aware context (read-only, abstracted)
// 🔐 Build system messages (hard guardrails first)
val systemMessages = mutableListOf<Map<String, String>>(
    mapOf(
        "role" to "system",
        "content" to MedicalPromptBuilder.systemPrompt(doctor, language)
    )
)

// 🧠 Optional patient-aware context (read-only, abstracted)
patientContext?.let { ctx ->
    systemMessages += mapOf(
        "role" to "system",
        "content" to MedicalPromptBuilder.patientContextPrompt(ctx)
    )
}

// 📦 Final payload for Chat Completions API
val payload = mapper.writeValueAsString(
    mapOf(
        "model" to props.model,
        "stream" to true,
        "messages" to systemMessages + listOf(
            mapOf(
                "role" to "user",
                "content" to question
            )
        )
    )
)

val request = Request.Builder()
    .url("https://api.openai.com/v1/chat/completions")
    .addHeader("Authorization", "Bearer ${props.apiKey}")
    .addHeader("OpenAI-Project", props.projectId) // ✅ REQUIRED FOR sk-proj keys
    .addHeader("Content-Type", "application/json")
    .addHeader("Accept", "text/event-stream")
    .post(payload.toRequestBody("application/json".toMediaType()))
    .build()


        client.newCall(request).execute().use { response ->

            log.info("OpenAI SSE status={}", response.code)

            if (!response.isSuccessful) {
                throw AiStreamException(
                    code = "AI_UNAVAILABLE",
                    message = "AI is temporarily unavailable. Please try again later."
                )
            }

            val source = response.body?.source() ?: return@use
            val buffer = StringBuilder()

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue
                if (!line.startsWith("data:")) continue

                val data = line.removePrefix("data:").trim()
                if (data.isBlank() || data == "[DONE]") continue

                try {
                    val json = mapper.readTree(data)
                    val delta = json.path("choices")
                        .path(0)
                        .path("delta")
                        .path("content")
                        .asText(null)

                    if (delta != null && delta.isNotEmpty()) {
                        buffer.append(delta)
                        emit(delta)
                    }
                } catch (e: Exception) {
                    log.debug("Skipping SSE frame: {}", e.message)
                }
            }
        }
    }
}
