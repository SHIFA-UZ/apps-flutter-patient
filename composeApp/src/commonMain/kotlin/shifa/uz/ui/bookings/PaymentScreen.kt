package shifa.uz.ui.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.bookAgain
import shifa.composeapp.generated.resources.completePayment
import shifa.composeapp.generated.resources.ic_alert_circle_16
import shifa.composeapp.generated.resources.payment
import shifa.composeapp.generated.resources.paymentSlotHeld
import shifa.uz.data.model.Appointment
import shifa.uz.ui.register.Banners
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.widgets.ShifaBanner
import shifa.uz.ui.widgets.ShifaBannerType
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.utils.PaymentWebView
import kotlin.time.Duration

/**
 * Figma 1825:17388: the provider's checkout over the booking page, with the held slot's countdown. PAID closes it
 * ([onDone], then the visit's details, 1825:17774); a failed or expired payment says so and offers Book again.
 * Figma's title "Pay via webview" is a placeholder, so it says Payment.
 */
@Composable
fun PaymentScreen(
    appointment: Appointment,
    onBack: () -> Unit,
    onDone: (Appointment) -> Unit,
    onSignedOut: () -> Unit,
    viewModel: PaymentViewModel = viewModel { PaymentViewModel(appointment) },
) {
    val state by viewModel.state.collectAsState()
    val done by rememberUpdatedState(onDone)
    val signedOut by rememberUpdatedState(onSignedOut)
    // A paid booking goes on by itself; a failed one waits for Book again.
    LaunchedEffect(viewModel) { viewModel.done.collect { if (it.isPaid && !it.isPaymentDead) done(it) } }
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { signedOut() } }
    PaymentContent(state, onBack, onCheck = viewModel::check, onBookAgain = { onDone(state.appointment) })
}

@Composable
internal fun PaymentContent(
    state: PaymentUiState,
    onBack: () -> Unit = {},
    onCheck: () -> Unit = {},
    onBookAgain: () -> Unit = {},
) {
    val failure = state.failure
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding()) {
        ShifaPageElement(ShifaPageElementType.Header) {
            ShifaSubHeader(stringResource(Res.string.payment), onBack = onBack)
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(ShifaColors.Bg.Canvas), contentAlignment = Alignment.Center) {
            if (failure == null) {
                PaymentWebView(state.checkoutUrl, Modifier.fillMaxSize())
            } else {
                ShifaBanner(
                    stringResource(failure),
                    Res.drawable.ic_alert_circle_16,
                    ShifaBannerType.Error,
                    Modifier.padding(ShifaSpacing.Inset.Md),
                )
            }
        }
        ShifaPageElement(ShifaPageElementType.ActionTray, Modifier.navigationBarsPadding()) {
            if (failure == null) {
                Banners(null, state.notice)
                state.holdLeft?.takeIf { it > Duration.ZERO }?.let {
                    Text(
                        stringResource(Res.string.paymentSlotHeld, countdown(it)),
                        Modifier.fillMaxWidth(),
                        style = ShifaTextStyles.caption(),
                        color = ShifaColors.Text.Tertiary,
                        textAlign = TextAlign.Center,
                    )
                }
                ShifaPrimaryButton(
                    stringResource(Res.string.completePayment),
                    onCheck,
                    Modifier.fillMaxWidth(),
                    loading = state.isChecking,
                )
            } else {
                ShifaPrimaryButton(stringResource(Res.string.bookAgain), onBookAgain, Modifier.fillMaxWidth())
            }
        }
    }
}

/** m:ss. */
internal fun countdown(left: Duration) =
    left.inWholeSeconds.let { "${it / 60}:${(it % 60).toString().padStart(2, '0')}" }
