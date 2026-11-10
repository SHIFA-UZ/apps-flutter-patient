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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.bookAgain
import shifa.composeapp.generated.resources.cancelBooking
import shifa.composeapp.generated.resources.completePayment
import shifa.composeapp.generated.resources.continuePayment
import shifa.composeapp.generated.resources.ic_alert_circle_16
import shifa.composeapp.generated.resources.leavePaymentMessage
import shifa.composeapp.generated.resources.payment
import shifa.composeapp.generated.resources.paymentSlotHeld
import shifa.uz.data.analytics.Analytics
import shifa.uz.data.analytics.AnalyticsEvent
import shifa.uz.data.analytics.AnalyticsScreen
import shifa.uz.data.analytics.paymentFailureReason
import shifa.uz.data.model.Appointment
import shifa.uz.ui.register.Banners
import shifa.uz.ui.register.MessagePopUp
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.widgets.ShifaBanner
import shifa.uz.ui.widgets.ShifaBannerType
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.ui.widgets.TrackScreen
import shifa.uz.utils.PaymentWebView
import kotlin.time.Duration

/**
 * Figma 1825:17388: the provider's checkout over the booking page, with the held slot's countdown. PAID closes it
 * ([onDone], then the visit's details, 1825:17774); a failed or expired payment says so and offers Book again.
 * Figma's title "Pay via webview" is a placeholder, so it says Payment.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PaymentScreen(
    appointment: Appointment,
    onBack: () -> Unit,
    onDone: (Appointment) -> Unit,
    onSignedOut: () -> Unit,
    // Leaves without giving the booking up: after the provider's success page, when the backend's PAID is still due.
    onKeep: () -> Unit = onBack,
    // [onBack] releases the held slot, so Back asks first.
    confirmLeave: Boolean = true,
    viewModel: PaymentViewModel = viewModel { PaymentViewModel(appointment) },
) {
    val state by viewModel.state.collectAsState()
    TrackScreen(AnalyticsScreen.Payment, "provider" to appointment.payment?.provider?.lowercase())
    val done by rememberUpdatedState(onDone)
    val signedOut by rememberUpdatedState(onSignedOut)
    // A paid booking goes on by itself; a failed one waits for Book again.
    LaunchedEffect(viewModel) { viewModel.done.collect { if (it.isPaid && !it.isPaymentDead) done(it) } }
    LaunchedEffect(viewModel) { viewModel.signedOut.collect { signedOut() } }
    var asking by remember { mutableStateOf(false) }
    val leave = {
        when {
            state.failure != null -> onBack()
            state.providerSaidPaid -> onKeep()
            confirmLeave -> asking = true
            else -> onBack()
        }
    }
    BackHandler(onBack = leave) // the system Back too, like the header's
    if (asking) {
        Dialog({ asking = false }) {
            MessagePopUp(
                stringResource(Res.string.cancelBooking),
                stringResource(Res.string.leavePaymentMessage),
                { asking = false },
            ) {
                ShifaPrimaryButton(
                    stringResource(Res.string.continuePayment),
                    { asking = false },
                    Modifier.fillMaxWidth(),
                )
                ShifaSecondaryButton(
                    stringResource(Res.string.cancelBooking),
                    {
                        asking = false
                        onBack()
                    },
                    Modifier.fillMaxWidth(),
                )
            }
        }
    }
    PaymentContent(
        state,
        leave,
        onCheck = { viewModel.check() },
        onBookAgain = {
            Analytics.track(AnalyticsEvent.PaymentBookAgainTapped(state.appointment.paymentFailureReason()))
            onDone(state.appointment)
        },
        onNavigate = { url ->
            // The provider's redirects (Flutter's checkout screen): the backend's pages aren't shown.
            when {
                CHECKOUT_SUCCESS in url -> viewModel.onRedirect("success")
                CHECKOUT_CANCEL in url -> {
                    viewModel.onRedirect("cancel")
                    onBack()
                }
                else -> return@PaymentContent false
            }
            true
        },
        onError = { Analytics.track(AnalyticsEvent.WebviewError(appointment.payment?.provider?.lowercase(), it)) },
    )
}

private const val CHECKOUT_SUCCESS = "/payments/checkout/success"
private const val CHECKOUT_CANCEL = "/payments/checkout/cancel"

@Composable
internal fun PaymentContent(
    state: PaymentUiState,
    onBack: () -> Unit = {},
    onCheck: () -> Unit = {},
    onBookAgain: () -> Unit = {},
    onNavigate: (String) -> Boolean = { false },
    onError: (Int) -> Unit = {},
) {
    val failure = state.failure
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding()) {
        ShifaPageElement(ShifaPageElementType.Header) {
            ShifaSubHeader(stringResource(Res.string.payment), onBack = onBack)
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(ShifaColors.Bg.Canvas), contentAlignment = Alignment.Center) {
            if (failure == null) {
                PaymentWebView(state.checkoutUrl, Modifier.fillMaxSize(), onNavigate, onError)
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
