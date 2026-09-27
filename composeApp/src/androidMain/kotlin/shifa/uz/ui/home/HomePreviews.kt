package shifa.uz.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.Instant
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.errorNetworkConnection
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.Doctor
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.ui.widgets.PreviewTheme

private val now = Instant.parse("2026-09-27T09:00:00Z")

private val clinic = "Sog'lom Hayot Klinikasi"

private val loaded =
    HomeUiState(
        firstName = "Sheroziy",
        unreadNotifications = 3,
        upcoming =
            listOf(
                Appointment(
                    doctorName = "Aziz Karimov",
                    doctorProfession = "Dentist",
                    startAt = "2026-09-27T08:58:00Z",
                    endAt = "2026-09-27T09:28:00Z",
                    location = "Video Consultation",
                ),
                Appointment(
                    doctorName = "Malika Rahimova",
                    doctorProfession = "Cardiologist",
                    doctorClinic = clinic,
                    startAt = "2026-09-28T05:00:00Z",
                ),
            ),
        tasks =
            listOf(
                RemoteCareTask(
                    taskName = "Running",
                    description = "30 minutes, easy pace",
                    category = "EXERCISE",
                    startDate = "2026-09-20",
                ),
                RemoteCareTask(
                    taskName = "Blood pressure",
                    description = "Morning and evening",
                    category = "VITAL",
                    startDate = "2026-09-29",
                ),
            ),
        doctors =
            listOf(
                Doctor(
                    firstName = "Aziz",
                    lastName = "Karimov",
                    profession = "Dentist",
                    clinic = clinic,
                    averageRating = 5.0,
                    reviewCount = 760,
                    certificates = listOf("c"),
                    nextAvailableStartAt = "2026-09-28T05:00:00Z",
                ),
                Doctor(firstName = "Malika", lastName = "Rahimova", profession = "Cardiologist", clinic = clinic),
            ),
    )

@Composable
private fun Home(state: HomeUiState) = PreviewTheme { HomeContent(state, now) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun HomeLoadedPreview() = Home(loaded)

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun HomeEmptyPreview() =
    Home(HomeUiState(firstName = "Sheroziy", upcoming = emptyList(), tasks = emptyList(), doctors = emptyList()))

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun HomeLoadingPreview() = Home(HomeUiState(isLoading = true))

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun HomeErrorPreview() =
    Home(
        loaded.copy(upcoming = null, doctors = null, error = Res.string.errorNetworkConnection),
    )
