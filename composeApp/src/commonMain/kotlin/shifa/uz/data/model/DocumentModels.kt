package shifa.uz.data.model

import kotlinx.serialization.Serializable

/** F1/F2 item (15 §5.7). Ids may come as numbers; the lenient JSON reads them into strings. */
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
    val creatorLabel: String? = null, // Doctor, Patient, Unknown
    val category: String? = null,
) {
    val displayDate get() = date?.ifBlank { null } ?: createdAt

    val file get() = fileUrl?.ifBlank { null } ?: url?.ifBlank { null }

    /**
     * F3 only deletes these. 11 expects `creatorLabel` "Patient", but the backend sends the uploader's full name,
     * so the patient's own uploads carry [patientName] (P1 `fullName`).
     *
     * ponytail: a doctor with the patient's exact name looks like theirs; F3 then fails with an error. Needs a backend
     * flag.
     */
    fun isUploadedBy(patientName: String?) =
        creatorLabel == "Patient" || (!patientName.isNullOrBlank() && creatorLabel?.trim() == patientName.trim())
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
