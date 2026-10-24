package shifa.uz.ui.doctors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ktor.http.decodeURLPart
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.address
import shifa.composeapp.generated.resources.certificate
import shifa.composeapp.generated.resources.consultationInPerson
import shifa.composeapp.generated.resources.consultationServiceFreeBadge
import shifa.composeapp.generated.resources.createBooking
import shifa.composeapp.generated.resources.doctorProfile
import shifa.composeapp.generated.resources.email
import shifa.composeapp.generated.resources.noBiographyAvailable
import shifa.composeapp.generated.resources.noCertificatesAvailable
import shifa.composeapp.generated.resources.noContactInformationAvailable
import shifa.composeapp.generated.resources.noReviews
import shifa.composeapp.generated.resources.noServicesAvailable
import shifa.composeapp.generated.resources.patient
import shifa.composeapp.generated.resources.phoneNumber
import shifa.composeapp.generated.resources.priceNotSet
import shifa.composeapp.generated.resources.retry
import shifa.uz.data.model.Doctor
import shifa.uz.data.model.PatientDocument
import shifa.uz.ui.bookings.InfoSection
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.widgets.ShifaCardSkeleton
import shifa.uz.ui.widgets.ShifaCarousel
import shifa.uz.ui.widgets.ShifaCertificateCard
import shifa.uz.ui.widgets.ShifaChip
import shifa.uz.ui.widgets.ShifaDoctorServiceCard
import shifa.uz.ui.widgets.ShifaListSkeleton
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaReviewCard
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.ui.widgets.ShifaSection
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.utils.formatDay
import shifa.uz.utils.getCurrentInstant

/**
 * 07 §4.3 (DOC-02), Figma 1655:16298. [doctor] is the tapped card's copy, shown until D2 loads.
 * ponytail: no service sheet, open-in-maps or ⋮ menu yet.
 */
@Composable
fun DoctorProfileScreen(
    doctor: Doctor,
    onBack: () -> Unit,
    onCertificate: (PatientDocument) -> Unit,
    onBook: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: DoctorProfileViewModel = viewModel { DoctorProfileViewModel(doctor) },
) {
    val state by viewModel.state.collectAsState()
    val signedOut by rememberUpdatedState(onSignedOut)
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { signedOut() } }
    val uri = LocalUriHandler.current
    DoctorProfileContent(
        state,
        now = getCurrentInstant(),
        onBack = onBack,
        onTab = viewModel::selectTab,
        onRetry = viewModel::load,
        onCertificate = onCertificate,
        onOpen = { runCatching { uri.openUri(it) } },
        onBook = onBook,
    )
}

@Composable
internal fun DoctorProfileContent(
    state: DoctorProfileUiState,
    now: Instant,
    onBack: () -> Unit = {},
    onTab: (DoctorTab) -> Unit = {},
    onRetry: () -> Unit = {},
    onCertificate: (PatientDocument) -> Unit = {},
    onOpen: (String) -> Unit = {},
    onBook: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding()) {
        ShifaPageElement(ShifaPageElementType.Header) {
            ShifaSubHeader(stringResource(Res.string.doctorProfile), onBack = onBack)
        }
        ShifaPageElement(ShifaPageElementType.FlexHeader) { DoctorHeaderCard(state.doctor, now) }
        ShifaPageElement(ShifaPageElementType.ActionTray) {
            ShifaCarousel {
                DoctorTab.entries.forEach { ShifaChip(stringResource(it.label), state.tab == it, { onTab(it) }) }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ShifaColors.Bg.Canvas)
                .verticalScroll(rememberScrollState())
                .padding(ShifaSpacing.Inset.Md),
            verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val error = state.error
            when {
                state.isLoaded -> TabContent(state, onCertificate, onOpen)
                error != null -> {
                    Empty(error)
                    ShifaSecondaryButton(stringResource(Res.string.retry), onRetry)
                }
                else -> ShifaListSkeleton(count = 2, shadow = false) { ShifaCardSkeleton(lines = 4, shadow = false) }
            }
        }
        ShifaPageElement(ShifaPageElementType.ActionTray, Modifier.navigationBarsPadding()) {
            ShifaPrimaryButton(stringResource(Res.string.createBooking), onBook, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ColumnScope.TabContent(
    state: DoctorProfileUiState,
    onCertificate: (PatientDocument) -> Unit,
    onOpen: (String) -> Unit,
) {
    val doctor = state.doctor
    when (state.tab) {
        DoctorTab.Reviews -> {
            val reviews = state.reviews
            val error = state.reviewsError
            when {
                reviews == null -> ShifaListSkeleton()
                error != null -> Empty(error)
                reviews.isEmpty() -> Empty(Res.string.noReviews)
                else ->
                    reviews.forEach {
                        ShifaReviewCard(
                            it.patientName?.ifBlank { null } ?: stringResource(Res.string.patient),
                            it.rating,
                            date = it.createdAt?.let(::formatDay),
                            comment = it.comment,
                            avatarUrl = it.patientPhotoUrl,
                        )
                    }
            }
        }
        DoctorTab.About -> InfoSection(null, doctor.about ?: stringResource(Res.string.noBiographyAvailable))
        DoctorTab.Services ->
            when {
                doctor.serviceItems.isNotEmpty() ->
                    doctor.serviceItems.forEach {
                        val price =
                            when {
                                it.isFreeConsultation -> stringResource(Res.string.consultationServiceFreeBadge)
                                it.prices.isEmpty() -> stringResource(Res.string.priceNotSet)
                                else -> it.prices.first().text
                            }
                        ShifaDoctorServiceCard(it.title, price)
                    }
                doctor.services.isNotEmpty() -> doctor.services.forEach { InfoSection(null, it) }
                else -> Empty(Res.string.noServicesAvailable)
            }
        DoctorTab.Certificates ->
            if (doctor.certificates.isEmpty()) {
                Empty(Res.string.noCertificatesAvailable)
            } else {
                val title = stringResource(Res.string.certificate)
                doctor.certificates.forEach { url ->
                    // 07: the file label is the decoded last path segment.
                    val label =
                        url.substringBefore('?').trimEnd('/').substringAfterLast('/')
                            .let { runCatching { it.decodeURLPart() }.getOrDefault(it) }
                            .ifEmpty { url }
                    ShifaCertificateCard(title, label, onClick = {
                        onCertificate(PatientDocument(title = label, fileUrl = url))
                    })
                }
            }
        DoctorTab.Clinic -> {
            val links =
                listOfNotNull(
                    doctor.fullAddress?.let { Contact(Res.string.address, it) },
                    doctor.clinic?.ifBlank { null }?.let { Contact(Res.string.consultationInPerson, it) },
                    doctor.phone?.ifBlank { null }?.let {
                        Contact(Res.string.phoneNumber, it, "tel:${it.filterNot(Char::isWhitespace)}")
                    },
                    doctor.email?.ifBlank { null }?.let { Contact(Res.string.email, it, "mailto:${it.trim()}") },
                    doctor.telegram?.ifBlank { null }?.let { Contact(null, it, social(it, "https://t.me/"), TELEGRAM) },
                    doctor.instagram?.ifBlank { null }?.let {
                        Contact(null, it, social(it, "https://instagram.com/"), INSTAGRAM)
                    },
                )
            if (links.isEmpty()) Empty(Res.string.noContactInformationAvailable)
            links.forEach { ContactSection(it, onOpen) }
        }
    }
}

// Brand names, not translated.
private const val TELEGRAM = "Telegram"
private const val INSTAGRAM = "Instagram"

private class Contact(
    val title: StringResource?,
    val text: String,
    val uri: String? = null,
    val brand: String? = null,
)

// 07 §4.3: a URL as-is, "@x" or "x" → base + x.
private fun social(
    value: String,
    base: String,
): String {
    val v = value.trim()
    return if (v.startsWith("http://") || v.startsWith("https://")) v else base + v.removePrefix("@")
}

@Composable
private fun ContactSection(
    contact: Contact,
    onOpen: (String) -> Unit,
) = ShifaSection(
    contact.brand ?: contact.title?.let { stringResource(it) },
    contact.uri?.let { uri -> Modifier.clip(ShifaRadius.ControlLg).clickable { onOpen(uri) } } ?: Modifier,
) {
    Text(
        contact.text,
        Modifier.fillMaxWidth(),
        style = ShifaTextStyles.chatMessage(),
        color = if (contact.uri != null) ShifaColors.Text.Link else ShifaColors.Text.Primary,
    )
}

@Composable
private fun Empty(text: StringResource) =
    Text(
        stringResource(text),
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = ShifaColors.Text.Tertiary,
        textAlign = TextAlign.Center,
    )
