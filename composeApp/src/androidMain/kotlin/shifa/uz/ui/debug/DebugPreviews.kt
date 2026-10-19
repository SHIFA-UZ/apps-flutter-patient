package shifa.uz.ui.debug

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import shifa.uz.ui.widgets.PreviewTheme

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun DebugPreview() =
    PreviewTheme {
        DebugContent(
            DebugUiState(
                demo = true,
                appointmentId = "9101",
                taskId = "9401",
                documentId = "9701",
                notice = "Sent: tap it in the notification shade.",
            ),
        )
    }
