package shifa.uz.data.bookings

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonElement
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.FAILED_TO_LOAD_APPOINTMENTS
import shifa.uz.data.api.apiCall
import shifa.uz.data.api.httpClient
import shifa.uz.data.api.items
import shifa.uz.data.model.Appointment

class BookingsRepository(
    private val client: HttpClient = httpClient,
) {
    /** B2, every status. A body that isn't an array is no appointments (15 §5.6). */
    suspend fun getAppointments(): Result<List<Appointment>> =
        apiCall<JsonElement>(FAILED_TO_LOAD_APPOINTMENTS) { client.get(ApiConfig.Endpoints.APPOINTMENTS) }
            .map { it.items<Appointment>() }
}
