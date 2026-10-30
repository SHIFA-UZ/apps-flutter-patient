package shifa.uz.data.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * F1/F2 item (15 §5.7). Ids may come as numbers; the lenient JSON reads them into strings. [appointmentId]: the visit
 * the document came from; the backend's PatientDocumentDto doesn't send it yet (backend-improvements §15).
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PatientDocument(
    val id: String = "",
    val title: String = "",
    val date: String? = null,
    val createdAt: String? = null,
    val fileUrl: String? = null,
    val url: String? = null,
    val type: String? = null, // a MIME type or extension hint
    val doctorName: String? = null,
    val creatorLabel: String? = null, // the uploader's name (11 said Doctor / Patient / Unknown)
    val uploadedByPatient: Boolean? = null, // backend Phase 1: who uploaded it, next to creatorLabel
    val creatorType: String? = null, // PATIENT, DOCTOR, UNKNOWN
    val category: String? = null,
    @JsonNames("appointment_id") val appointmentId: String? = null,
) {
    val displayDate get() = date?.ifBlank { null } ?: createdAt

    val file get() = fileUrl?.ifBlank { null } ?: url?.ifBlank { null }

    /**
     * F3 only deletes these. The backend's `uploadedByPatient` (or `creatorType`) decides; an older backend sends
     * neither, only the uploader's full name in `creatorLabel`, which is then compared with [patientName] (P1
     * `fullName`).
     */
    fun isUploadedBy(patientName: String?) =
        uploadedByPatient
            ?: creatorType?.let { it.equals("PATIENT", ignoreCase = true) }
            ?: (
                creatorLabel == "Patient" ||
                    (!patientName.isNullOrBlank() && creatorLabel?.trim() == patientName.trim())
            )
}

/**
 * 11: how to show a file: `"pdf"`, an image extension, or null when unsupported.
 * The bytes decide first; [type] and the URL's extension are only hints.
 */
fun documentExtension(
    bytes: ByteArray,
    type: String?,
    url: String?,
): String? {
    fun at(
        offset: Int,
        vararg magic: Int,
    ) = bytes.size >= offset + magic.size && magic.indices.all { bytes[offset + it] == magic[it].toByte() }
    when {
        at(0, 0x25, 0x50, 0x44, 0x46) -> return "pdf" // %PDF
        at(0, 0xFF, 0xD8, 0xFF) -> return "jpg"
        at(0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> return "png"
        at(0, 0x52, 0x49, 0x46, 0x46) && at(8, 0x57, 0x45, 0x42, 0x50) -> return "webp" // RIFF…WEBP
    }
    val urlExtension = url?.substringBefore('?')?.substringAfterLast('/')?.substringAfterLast('.', "")
    return listOfNotNull(type, urlExtension).firstNotNullOfOrNull {
        val hint = it.trim().lowercase()
        when {
            "pdf" in hint -> "pdf"
            hint in listOf("jpg", "jpeg", "png", "webp") -> hint
            hint.startsWith("image/") -> hint.removePrefix("image/").ifBlank { null }
            else -> null
        }
    }
}
