package shifa.uz.data.doctors

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.FAILED_TO_LOAD_DOCTOR
import shifa.uz.data.api.apiCall
import shifa.uz.data.api.httpClient
import shifa.uz.data.model.Doctor

class DoctorsRepository(
    private val client: HttpClient = httpClient,
) {
    /** D2. */
    suspend fun getDoctor(id: String): Result<Doctor> =
        apiCall(FAILED_TO_LOAD_DOCTOR) { client.get(ApiConfig.Endpoints.doctor(id)) }
}
