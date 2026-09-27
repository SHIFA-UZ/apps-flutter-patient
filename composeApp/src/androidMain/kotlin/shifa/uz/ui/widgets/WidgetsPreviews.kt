package shifa.uz.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.PreviewContextConfigurationEffect
import org.jetbrains.compose.resources.painterResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.ic_eye_20
import shifa.composeapp.generated.resources.ic_plus_16
import shifa.composeapp.generated.resources.ic_plus_22
import shifa.composeapp.generated.resources.ic_plus_24
import shifa.uz.ui.navigation.ShifaNavBar
import shifa.uz.ui.navigation.ShifaTab
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaTheme

// Also rendered by the screenshot tests in androidUnitTest. Robolectric doesn't start the
// Compose resources ContentProvider, so inspection mode is forced to let the effect set the context.
@Composable
internal fun PreviewTheme(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalInspectionMode provides true) {
        PreviewContextConfigurationEffect()
        ShifaTheme(content)
    }

@Composable
internal fun Gallery(
    background: Color = ShifaColors.Bg.Surface,
    content: @Composable ColumnScope.() -> Unit,
) = PreviewTheme {
    Column(
        Modifier.width(402.dp).background(background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Preview
@Composable
internal fun ButtonsPreview() =
    Gallery {
        val wide = Modifier.fillMaxWidth()
        ShifaPrimaryButton("Click Me", {}, wide, icon = Res.drawable.ic_plus_22)
        ShifaPrimaryButton("Click Me", {}, wide, enabled = false)
        ShifaPrimaryButton("Click Me", {}, wide, destructive = true)
        ShifaPrimaryButton("Click Me", {}, wide, loading = true)
        ShifaSecondaryButton("Click Me", {}, wide, icon = Res.drawable.ic_plus_22)
        ShifaSecondaryButton("Click Me", {}, wide, enabled = false)
        ShifaSecondaryButton("Click Me", {}, wide, destructive = true)
        ShifaSecondaryButton("Click Me", {}, wide, loading = true)
        ShifaTertiaryButton("Click Me", {}, icon = Res.drawable.ic_plus_22)
        ShifaTertiaryButton("Click Me", {}, icon = Res.drawable.ic_plus_22, enabled = false)
        ShifaTertiaryButton("Click Me", {}, icon = Res.drawable.ic_plus_22, destructive = true)
        listOf(
            Triple(false, true, false),
            Triple(true, true, false),
            Triple(false, false, false),
            Triple(false, true, true),
        ).forEach { (secondary, enabled, destructive) ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ShifaActionButton(
                    {},
                    text = "Click Me",
                    secondary = secondary,
                    enabled = enabled,
                    destructive = destructive,
                )
                ShifaActionButton({
                }, icon = Res.drawable.ic_plus_16, secondary = secondary, enabled = enabled, destructive = destructive)
                ShifaActionButton(
                    {},
                    text = "Click Me",
                    icon = Res.drawable.ic_plus_16,
                    secondary = secondary,
                    enabled = enabled,
                    destructive = destructive,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(false, true).forEach { secondary ->
                ShifaIconButton(Res.drawable.ic_plus_24, null, {}, secondary = secondary)
                ShifaIconButton(Res.drawable.ic_plus_24, null, {}, secondary = secondary, enabled = false)
                ShifaIconButton(Res.drawable.ic_plus_24, null, {}, secondary = secondary, destructive = true)
            }
        }
    }

@Preview
@Composable
internal fun InputFieldPreview() =
    Gallery {
        ShifaInputField("", {}, "label", supportingText = "Suggestive Text")
        ShifaInputField("Input", {}, "label", supportingText = "Suggestive Text")
        ShifaInputField("Input", {}, "label", supportingText = "Suggestive Text", isError = true)
        ShifaInputField("", {}, "label", supportingText = "Suggestive Text", enabled = false)
        ShifaInputField("Input", {}, "label", trailingIcon = {
            Icon(painterResource(Res.drawable.ic_eye_20), null, tint = ShifaColors.Icon.Secondary)
        })
    }

@Preview
@Composable
internal fun TagPreview() =
    Gallery {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ShifaTagType.entries.forEach { ShifaTag("tag text", type = it) }
        }
    }

@Preview
@Composable
internal fun CardsPreview() =
    Gallery {
        val name = "Azamat Rakhimov"
        val clinic = "Dentist - Sog‘lom Hayot Klinikasi"
        val video = "Dentist - Video Consultation"
        ShifaUpcomingAppointmentCard(name, clinic, "10:00, Today")
        ShifaUpcomingAppointmentCard(name, video, "Now", video = true, joinText = "Join")
        ShifaAppointmentStatus.entries.forEach {
            ShifaPastAppointmentCard(name, clinic, "10:00, 15 Aug", status = it)
            ShifaPastAppointmentCard(name, video, "10:00, 15 Aug", status = it, video = true)
        }
        ShifaDoctorCard(
            name,
            clinic,
            rating = "5.0(760)",
            availability = "Appointments from tomorrow",
            tags = {
                ShifaTag("Clinic", type = ShifaTagType.Primary)
                ShifaTag("Online", type = ShifaTagType.Secondary)
                ShifaTag("Nearby", type = ShifaTagType.Tertiary)
            },
        )
        ShifaDoctorCard(
            name,
            clinic,
            type = ShifaDoctorCardType.Compact,
            rating = "5.0(760)",
            availability = "Appointments from tomorrow",
        )
        ShifaDoctorCard(
            name,
            "Dentist",
            type = ShifaDoctorCardType.In,
            tags = { ShifaTag("Appointments tomorrow", type = ShifaTagType.Primary) },
        )
        ShifaDocumentCard("abc.pdf", "Added by Dr Azamat Rakhimov", "15.07.2026", onClick = {})
        ShifaCertificateCard("Certificate name", "15.07.2026", onClick = {})
        ShifaDoctorServiceCard("Setting Page", "300000.00 UZS", onClick = {})
        ShifaCheckInCard("Date", "Time", false, {}, "", {}, "Enter ‘task’", "Submit Check In", {})
        ShifaCheckInCard("Date", "Time", true, {}, "", {}, "Enter ‘task’", "Submit Check In", {})
        ShifaTaskCard("Running", "Description text", "Due today", type = ShifaTaskType.Exercise, tag = "Exercise")
        ShifaTaskCard(
            "Running",
            "Description text",
            "Started 17.08.2026",
            type = ShifaTaskType.Medicine,
            tag = "Medicine",
            completed = 1,
            total = 13,
        )
        ShifaSettingsItem("Setting Page", {})
    }

@Preview
@Composable
internal fun PageHeaderPreview() =
    Gallery {
        ShifaMainHeader("Home", home = true, onNotifications = {})
        ShifaMainHeader("Page Name", onNotifications = {})
        ShifaSubHeader("Page Name", onBack = {}, onCheck = {}, onMore = {})
    }

@Preview
@Composable
internal fun NavBarPreview() =
    Gallery(background = ShifaColors.Bg.Canvas) {
        ShifaTab.entries.forEach { ShifaNavBar(selected = it, onSelect = {}) }
    }
