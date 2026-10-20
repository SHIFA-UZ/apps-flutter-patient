package shifa.uz.ui.bookings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.Instant
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.thankYouForYourRating
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.data.model.Review
import shifa.uz.ui.widgets.PreviewTheme

private val now = Instant.parse("2026-09-27T09:00:00Z")

private val clinic = "Sog'lom Hayot Klinikasi"

internal val previewDoctor =
    Doctor(
        id = "d1",
        firstName = "Azamat",
        lastName = "Rakhimov",
        profession = "Dentist",
        clinic = clinic,
        averageRating = 5.0,
        reviewCount = 760,
        certificates = listOf("https://cdn.shifa.uz/certificates/implantology%20course.pdf"),
        nextAvailableStartAt = "2026-09-28T05:00:00Z",
        supportsOnline = true,
        phone = "+998 90 123 45 67",
        email = "azamat@shifa.uz",
        telegram = "@azamat",
        locationStreetAddress = "Amir Temur 1",
        locationCity = "Tashkent",
        biography =
            "Lorem ipsum dolor sit amet consectetur. In orci id arcu viverra elementum et nam ipsum duis. " +
                "Turpis quis lobortis morbi id quis ut eget sed. Consequat felis consectetur eget suspendisse " +
                "tincidunt et et eu cursus.",
    )

private val past =
    listOf(
        Appointment(
            id = "1",
            doctorId = "d1",
            doctorName = "Azamat Rakhimov",
            doctorProfession = "Dentist",
            doctorClinic = clinic,
            startAt = "2026-09-20T07:00:00Z",
        ),
        Appointment(
            id = "2",
            doctorName = "Malika Rahimova",
            doctorProfession = "Cardiologist",
            location = "Video Consultation",
            startAt = "2026-09-10T10:30:00Z",
            status = "REQUESTED",
        ),
        Appointment(
            id = "3",
            doctorName = "Aziz Karimov",
            doctorProfession = "Therapist",
            doctorClinic = clinic,
            startAt = "2026-09-01T05:00:00Z",
            status = "CANCELLED",
        ),
    )

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun BookingsLoadedPreview() =
    PreviewTheme {
        BookingsContent(
            BookingsUiState(
                recommended = listOf(previewDoctor, previewDoctor.copy(id = "d2", firstName = "Malika")),
                myDoctors =
                    listOf(
                        previewDoctor,
                        previewDoctor.copy(id = "d2", firstName = "Malika", supportsOnline = false),
                    ),
                past = past,
            ),
            now,
        )
    }

// Any chip but All: only the doctors, as full cards with tags (Figma 1653:12376).
@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun BookingsFilteredPreview() =
    PreviewTheme {
        BookingsContent(
            BookingsUiState(
                filter = DoctorFilter.Remote,
                recommended =
                    listOf(
                        previewDoctor,
                        previewDoctor.copy(id = "d2", firstName = "Malika", clinic = null, locationCity = null),
                    ),
                past = past,
            ),
            now,
        )
    }

private val listState =
    BookingsUiState(
        recommended = listOf(previewDoctor, previewDoctor.copy(id = "d2", firstName = "Malika")),
        myDoctors = listOf(previewDoctor, previewDoctor.copy(id = "d2", firstName = "Malika")),
        past = past,
    )

// Figma 1655:15670: the last visit, none for Malika.
@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun BookingsMyDoctorsPreview() = PreviewTheme { BookingsListContent(BookingsList.MyDoctors, listState, now) }

// Figma 1655:15892.
@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun BookingsPastPreview() = PreviewTheme { BookingsListContent(BookingsList.Past, listState, now) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun BookingsRecommendedPreview() =
    PreviewTheme {
        BookingsListContent(BookingsList.Recommended, listState, now)
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun BookingsLoadingPreview() = PreviewTheme { BookingsContent(BookingsUiState(isLoading = true), now) }

internal val previewAppointment =
    Appointment(
        id = "1",
        doctorId = "d1",
        doctorName = "Azamat Rakhimov",
        doctorClinic = clinic,
        startAt = "2026-09-28T07:00:00Z",
        endAt = "2026-09-28T07:30:00Z",
        location = "SEBAMED",
        reason = "check up",
    )

private val soon = previewAppointment

// Figma 1662:17723: less than 24 h away.
@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun AppointmentDetailsSoonPreview() =
    PreviewTheme { AppointmentDetailsContent(AppointmentDetailsUiState(soon, now, previewDoctor)) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun AppointmentDetailsCancellablePreview() =
    PreviewTheme {
        AppointmentDetailsContent(
            AppointmentDetailsUiState(soon.copy(startAt = "2026-10-05T07:00:00Z", endAt = null, reason = null), now),
        )
    }

private val pastVisit =
    previewAppointment.copy(startAt = "2026-09-15T05:00:00Z", endAt = "2026-09-15T05:30:00Z", status = "COMPLETED")

// Figma 1661:17567: B7 found no review, so Leave Review shows.
@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun AppointmentDetailsPastPreview() =
    PreviewTheme {
        AppointmentDetailsContent(AppointmentDetailsUiState(pastVisit, now, previewDoctor, reviewChecked = true))
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun AppointmentDetailsReviewedPreview() =
    PreviewTheme {
        AppointmentDetailsContent(
            AppointmentDetailsUiState(
                pastVisit,
                now,
                previewDoctor,
                detailsOpen = false,
                reviewChecked = true,
                review = Review(rating = 4, comment = "Very attentive, explained everything."),
                notice = Res.string.thankYouForYourRating,
            ),
        )
    }

// Figma 1891:20212: rated, so Submit is on.
@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ReviewTrayPreview() =
    PreviewTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            ReviewTray(AppointmentDetailsUiState(pastVisit, now, previewDoctor, reviewChecked = true, rating = 3))
        }
    }

// Figma 1825:17774: paid, after the payment page closed.
@Preview(widthDp = 393, heightDp = 1000)
@Composable
internal fun AppointmentDetailsPaidPreview() =
    PreviewTheme {
        AppointmentDetailsContent(
            AppointmentDetailsUiState(
                soon.copy(
                    startAt = "2026-10-05T07:00:00Z",
                    endAt = "2026-10-05T07:30:00Z",
                    serviceTitle = "Check up",
                    paymentStatus = Appointment.PAID,
                    paymentBadge = Appointment.PAID,
                ),
                now,
                previewDoctor,
            ),
        )
    }

// Figma 1825:17960: paid at the clinic, under 24 h.
@Preview(widthDp = 393, heightDp = 1000)
@Composable
internal fun AppointmentDetailsPayInClinicPreview() =
    PreviewTheme {
        AppointmentDetailsContent(
            AppointmentDetailsUiState(
                soon.copy(
                    serviceTitle = "Check up",
                    paymentStatus = Appointment.PAY_IN_CLINIC,
                    paymentBadge = Appointment.PAY_IN_CLINIC,
                    paymentAmountMinor = 300_000,
                    paymentCurrency = "UZS",
                ),
                now,
                previewDoctor,
            ),
        )
    }
