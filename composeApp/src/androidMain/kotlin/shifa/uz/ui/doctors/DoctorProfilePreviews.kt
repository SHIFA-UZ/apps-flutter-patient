package shifa.uz.ui.doctors

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.Instant
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.errorNetworkConnection
import shifa.uz.data.model.DoctorPrice
import shifa.uz.data.model.DoctorService
import shifa.uz.data.model.Review
import shifa.uz.ui.bookings.previewDoctor
import shifa.uz.ui.widgets.PreviewTheme

private val now = Instant.parse("2026-09-27T09:00:00Z")

private val loaded =
    DoctorProfileUiState(
        previewDoctor.copy(
            serviceItems =
                listOf(
                    DoctorService(
                        title = "Consultation",
                        description = "A first visit to talk the issue through and plan what's next.",
                        isFreeConsultation = true,
                    ),
                    DoctorService(title = "Implant", prices = listOf(DoctorPrice(30000000, "UZS"))),
                    DoctorService(title = "Cleaning"),
                ),
        ),
        isLoaded = true,
    )

@Composable
private fun Profile(state: DoctorProfileUiState) =
    PreviewTheme {
        DoctorProfileContent(state, now)
    }

// Figma 1655:16298.
@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun DoctorProfileAboutPreview() = Profile(loaded)

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun DoctorProfileServicesPreview() = Profile(loaded.copy(tab = DoctorTab.Services))

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun DoctorProfileClinicPreview() = Profile(loaded.copy(tab = DoctorTab.Clinic))

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun DoctorProfileErrorPreview() =
    Profile(DoctorProfileUiState(previewDoctor, error = Res.string.errorNetworkConnection))

// Figma 1655:16298.
@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun DoctorProfileReviewsPreview() =
    Profile(
        loaded.copy(
            tab = DoctorTab.Reviews,
            reviews =
                listOf(
                    Review(
                        "1",
                        "Sarah Karimova",
                        rating = 5,
                        comment = "Very attentive, explained everything.",
                        createdAt = "2026-09-19",
                    ),
                    Review("2", "Bekzod Aliev", rating = 3, createdAt = "2026-09-02"),
                ),
        ),
    )
