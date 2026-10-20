package shifa.uz.data.bookings

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonElement
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.FAILED_TO_BOOK_APPOINTMENT
import shifa.uz.data.api.FAILED_TO_CANCEL_APPOINTMENT
import shifa.uz.data.api.FAILED_TO_LOAD_APPOINTMENT
import shifa.uz.data.api.FAILED_TO_LOAD_APPOINTMENTS
import shifa.uz.data.api.FAILED_TO_LOAD_PAYMENT_METHODS
import shifa.uz.data.api.FAILED_TO_LOAD_SLOTS
import shifa.uz.data.api.FAILED_TO_SUBMIT_SIGNATURE
import shifa.uz.data.api.apiCall
import shifa.uz.data.api.httpClient
import shifa.uz.data.api.items
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.AvailableSlot
import shifa.uz.data.model.BookingRequest
import shifa.uz.data.model.PaymentMethod
import shifa.uz.data.model.Review
import shifa.uz.data.model.SignatureRequest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class BookingsRepository(
    private val client: HttpClient = httpClient,
) {
    /** B2, every status. A body that isn't an array is no appointments (15 §5.6). */
    suspend fun getAppointments(): Result<List<Appointment>> =
        apiCall<JsonElement>(FAILED_TO_LOAD_APPOINTMENTS) { client.get(ApiConfig.Endpoints.APPOINTMENTS) }
            .map { it.items<Appointment>() }

    /** B4: one appointment, with `signatureRequested` and `alreadySigned` (B2 has neither). */
    suspend fun getAppointment(id: String): Result<Appointment> =
        apiCall(FAILED_TO_LOAD_APPOINTMENT) { client.get(ApiConfig.Endpoints.appointment(id)) }

    /** B6: signs the appointment summary with a PNG [signature]. The body is ignored. */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun submitSignature(
        id: String,
        signature: ByteArray,
    ): Result<Unit> =
        apiCall<String>(FAILED_TO_SUBMIT_SIGNATURE) {
            client.post(ApiConfig.Endpoints.appointmentSignature(id)) {
                contentType(ContentType.Application.Json)
                setBody(SignatureRequest(Base64.encode(signature)))
            }
        }.map {}

    /** B7: my review of the visit. Any error, 404 included, is no review (15 B7). */
    suspend fun getReview(id: String): Review? =
        apiCall<Review>(FAILED_TO_LOAD_APPOINTMENT) { client.get(ApiConfig.Endpoints.appointmentReview(id)) }
            .getOrNull()
            ?.takeIf { it.rating > 0 }

    /** B5, a hard delete. The body is ignored. */
    suspend fun cancel(id: String): Result<Unit> =
        apiCall<String>(FAILED_TO_CANCEL_APPOINTMENT) { client.delete(ApiConfig.Endpoints.appointment(id)) }.map {}

    /** B1, one local [day]; [locationId] only for in-person. A body that isn't an array is no slots. */
    suspend fun getSlots(
        doctorId: String,
        day: LocalDate,
        locationId: Int?,
    ): Result<List<AvailableSlot>> =
        apiCall<JsonElement>(FAILED_TO_LOAD_SLOTS) {
            client.get(ApiConfig.Endpoints.availableSlots(doctorId)) {
                parameter("day", day.toString())
                parameter("locationId", locationId)
            }
        }.map { it.items<AvailableSlot>() }

    /** MOBILE_BOOKING_PAYMENT §2.1: the providers for a pay-now [serviceId]; empty for free and pay later. */
    suspend fun getPaymentMethods(
        serviceId: String,
        locationId: Int?,
    ): Result<List<PaymentMethod>> =
        apiCall<JsonElement>(FAILED_TO_LOAD_PAYMENT_METHODS) {
            client.get(ApiConfig.Endpoints.PAYMENT_METHODS) {
                parameter("serviceId", serviceId)
                parameter("locationId", locationId)
            }
        }.map { it.items<PaymentMethod>() }

    /** B3. */
    suspend fun book(request: BookingRequest): Result<Appointment> =
        apiCall(FAILED_TO_BOOK_APPOINTMENT) {
            client.post(ApiConfig.Endpoints.APPOINTMENTS) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
}
