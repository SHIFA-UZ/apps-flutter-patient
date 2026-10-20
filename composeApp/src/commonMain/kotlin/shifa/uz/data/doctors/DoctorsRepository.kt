package shifa.uz.data.doctors

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.FAILED_TO_LOAD_DOCTOR
import shifa.uz.data.api.FAILED_TO_LOAD_REVIEWS
import shifa.uz.data.api.FAILED_TO_SEARCH_DOCTORS
import shifa.uz.data.api.FAILED_TO_SUBMIT_REVIEW
import shifa.uz.data.api.apiCall
import shifa.uz.data.api.httpClient
import shifa.uz.data.api.items
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.data.model.DoctorLocation
import shifa.uz.data.model.Review
import shifa.uz.data.model.ReviewRequest

/** D1 query keys, sent only when set (15 D1). */
data class DoctorsQuery(
    val search: String? = null,
    val consultationType: String? = null, // online, in_person, both
    val availableWithin: String? = null, // today, tomorrow, week
    val sortBy: String? = null, // rating, distance, reviews, availability
    val latitude: Double? = null, // Near me
    val longitude: Double? = null,
    val radiusKm: Double? = null,
)

class DoctorsRepository(
    private val client: HttpClient = httpClient,
) {
    /** D2. */
    suspend fun getDoctor(id: String): Result<Doctor> =
        apiCall(FAILED_TO_LOAD_DOCTOR) { client.get(ApiConfig.Endpoints.doctor(id)) }

    /** D4. A body that isn't an array is no reviews. */
    suspend fun getReviews(id: String): Result<List<Review>> =
        apiCall<JsonElement>(FAILED_TO_LOAD_REVIEWS) { client.get(ApiConfig.Endpoints.doctorReviews(id)) }
            .map { it.items<Review>() }

    /** D5, with the patient's token though it's under /public (15 §6.2); its 401 doesn't log out. */
    suspend fun submitReview(
        id: String,
        request: ReviewRequest,
    ): Result<Unit> =
        apiCall<String>(FAILED_TO_SUBMIT_REVIEW) {
            client.post(ApiConfig.Endpoints.doctorReviews(id)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }.map {}

    /** D3. `id <= 0` are legacy placeholders; an error is no locations and never blocks booking (15 D3). */
    suspend fun getLocations(id: String): List<DoctorLocation> =
        apiCall<JsonElement>(FAILED_TO_LOAD_DOCTOR) { client.get(ApiConfig.Endpoints.doctorLocations(id)) }
            .map { json -> json.items<DoctorLocation>().filter { it.id > 0 } }
            .getOrDefault(emptyList())

    /**
     * D1, every page: `{doctors: [...], totalCount}`, or a bare array (one page).
     * ponytail: every page on each search, up to [MAX_PAGES]; load on scroll once the catalog outgrows that.
     */
    suspend fun searchDoctors(query: DoctorsQuery = DoctorsQuery()): Result<List<Doctor>> {
        val all = mutableListOf<Doctor>()
        for (page in 1..MAX_PAGES) {
            val json =
                apiCall<JsonElement>(FAILED_TO_SEARCH_DOCTORS) {
                    client.get(ApiConfig.Endpoints.DOCTORS) {
                        parameter("search", query.search?.trim()?.ifEmpty { null })
                        parameter("consultationType", query.consultationType)
                        parameter("availableWithin", query.availableWithin)
                        parameter("sortBy", query.sortBy)
                        parameter("latitude", query.latitude)
                        parameter("longitude", query.longitude)
                        parameter("radiusKm", query.radiusKm)
                        parameter("includeNextAvailable", true)
                        parameter("page", page.takeIf { it > 1 })
                        parameter("pageSize", PAGE_SIZE)
                    }
                }.getOrElse { return Result.failure(it) }
            val items = json.items<Doctor>("doctors")
            all += items
            val total = (json as? JsonObject)?.get("totalCount")?.jsonPrimitive?.intOrNull
            if (total == null || items.isEmpty() || all.size >= total) break
        }
        return Result.success(all)
    }

    private companion object {
        const val PAGE_SIZE = 50 // the backend's cap
        const val MAX_PAGES = 20
    }
}

/**
 * Figma "My Doctors": the doctors of the latest bookings, one D2 each, newest first (15 D2). One that fails is
 * left out; the first error is returned only when all fail.
 */
suspend fun DoctorsRepository.doctorsOf(
    appointments: List<Appointment>,
    limit: Int = Int.MAX_VALUE,
): Result<List<Doctor>> =
    coroutineScope {
        val ids =
            appointments.sortedByDescending { it.start }
                .mapNotNull { it.doctorId?.ifBlank { null } }
                .distinct()
                .take(limit)
        val results = ids.map { async { getDoctor(it) } }.awaitAll()
        val found = results.mapNotNull { it.getOrNull() }
        if (ids.isNotEmpty() && found.isEmpty()) {
            Result.failure(
                results.first().exceptionOrNull()!!,
            )
        } else {
            Result.success(found)
        }
    }
