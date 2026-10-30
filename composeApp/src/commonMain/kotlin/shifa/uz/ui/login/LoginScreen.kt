package shifa.uz.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.accountLockedTryAgainIn
import shifa.composeapp.generated.resources.createAccount
import shifa.composeapp.generated.resources.emailOrPhone
import shifa.composeapp.generated.resources.emailOrPhoneRequired
import shifa.composeapp.generated.resources.forgotPassword
import shifa.composeapp.generated.resources.hidePassword
import shifa.composeapp.generated.resources.ic_alert_circle_16
import shifa.composeapp.generated.resources.ic_check_circle_16
import shifa.composeapp.generated.resources.ic_clock_16
import shifa.composeapp.generated.resources.ic_eye_20
import shifa.composeapp.generated.resources.ic_eye_off_20
import shifa.composeapp.generated.resources.img_login_hero
import shifa.composeapp.generated.resources.img_logo
import shifa.composeapp.generated.resources.loginAttemptsRemaining
import shifa.composeapp.generated.resources.password
import shifa.composeapp.generated.resources.passwordRequired
import shifa.composeapp.generated.resources.showPassword
import shifa.composeapp.generated.resources.signIn
import shifa.uz.data.debug.Backend
import shifa.uz.data.debug.BackendChoice
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.widgets.ShifaBanner
import shifa.uz.ui.widgets.ShifaBannerType
import shifa.uz.ui.widgets.ShifaInputField
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaTertiaryButton
import shifa.uz.utils.isDebugBuild

@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    notice: StringResource? = null,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
) {
    val state by viewModel.state.collectAsState()
    val signedIn by rememberUpdatedState(onSignedIn)
    LaunchedEffect(viewModel) { viewModel.signedIn.collect { signedIn() } }
    if (state.isLocked) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(30_000) // 05 §2.2: count the minutes down, unlock when it expires
                viewModel.refreshLockout()
            }
        }
    }
    // Debug builds: the backend can be switched before signing in (a tester has no production account).
    val backend by BackendChoice.current.collectAsState()
    LoginScreenContent(
        state = state,
        onIdentifierChange = viewModel::onIdentifierChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onSignIn = viewModel::signIn,
        onForgotPassword = onForgotPassword,
        onCreateAccount = onCreateAccount,
        notice = notice,
        debugBackend = backend.takeIf { isDebugBuild },
        onDebugBackend = BackendChoice::set,
    )
}

/** Figma get-started (`1614:12247`). "Continue with Google" is hidden until the backend supports it. */
@Composable
internal fun LoginScreenContent(
    state: LoginUiState,
    onIdentifierChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSignIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    notice: StringResource? = null,
    debugBackend: Backend? = null, // debug builds: the backend in use, switchable here
    onDebugBackend: (Backend) -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding().imePadding()) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
            verticalArrangement = Arrangement.Bottom,
        ) {
            ShifaPageElement(ShifaPageElementType.Body) {
                Column(verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm)) {
                    Hero()
                    // After account deletion (14 §3.5).
                    notice?.let {
                        ShifaBanner(
                            stringResource(it),
                            Res.drawable.ic_check_circle_16,
                            ShifaBannerType.Success,
                        )
                    }
                    if (state.isLocked) {
                        ShifaBanner(
                            stringResource(
                                Res.string.accountLockedTryAgainIn,
                                state.loginLockedUntilMinutes.toString(),
                            ),
                            Res.drawable.ic_clock_16,
                            ShifaBannerType.Error,
                        )
                    } else {
                        state.error?.let {
                            ShifaBanner(stringResource(it), Res.drawable.ic_alert_circle_16, ShifaBannerType.Error)
                        }
                        if (state.showAttemptsBanner) {
                            ShifaBanner(
                                stringResource(
                                    Res.string.loginAttemptsRemaining,
                                    state.loginAttemptsRemaining.toString(),
                                ),
                                Res.drawable.ic_alert_circle_16,
                                ShifaBannerType.Warning,
                            )
                        }
                    }
                    ShifaInputField(
                        value = state.identifier,
                        onValueChange = onIdentifierChange,
                        label = stringResource(Res.string.emailOrPhone),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText =
                            if (state.identifierError) {
                                stringResource(
                                    Res.string.emailOrPhoneRequired,
                                )
                            } else {
                                null
                            },
                        isError = state.identifierError,
                        clearable = state.identifier.isNotEmpty(),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                    )
                    ShifaInputField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        label = stringResource(Res.string.password),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = if (state.passwordError) stringResource(Res.string.passwordRequired) else null,
                        isError = state.passwordError,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions = KeyboardActions(onDone = { onSignIn() }),
                        visualTransformation =
                            if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Icon(
                                painterResource(
                                    if (state.isPasswordVisible) Res.drawable.ic_eye_off_20 else Res.drawable.ic_eye_20,
                                ),
                                stringResource(
                                    if (state.isPasswordVisible) Res.string.hidePassword else Res.string.showPassword,
                                ),
                                Modifier.clip(ShifaRadius.Full).clickable(onClick = onTogglePasswordVisibility),
                                tint = ShifaColors.Icon.Secondary,
                            )
                        },
                    )
                    ShifaTertiaryButton(
                        stringResource(Res.string.forgotPassword),
                        onForgotPassword,
                        Modifier.align(Alignment.End),
                    )
                }
            }
            ShifaPageElement(ShifaPageElementType.ActionTray) {
                ShifaPrimaryButton(
                    stringResource(Res.string.signIn),
                    onSignIn,
                    Modifier.fillMaxWidth(),
                    enabled = state.isSignInEnabled,
                    loading = state.isLoading,
                )
                ShifaTertiaryButton(stringResource(Res.string.createAccount), onCreateAccount, Modifier.fillMaxWidth())
                debugBackend?.let { current ->
                    // ponytail: English only, debug builds only; App re-keys on the choice.
                    val other = if (current == Backend.Production) Backend.Development else Backend.Production
                    ShifaTertiaryButton(
                        "Backend: ${current.name} · switch to ${other.name}",
                        { onDebugBackend(other) },
                        Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(ShifaDimens.LoginFooterHeight))
        }
    }
}

@Composable
private fun Hero() {
    Box(
        Modifier.fillMaxWidth().height(ShifaDimens.LoginHeroHeight).clip(ShifaRadius.ControlLg),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(Res.drawable.img_login_hero),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .size(ShifaDimens.LoginHeroTile)
                .clip(ShifaRadius.ControlLg)
                .background(ShifaColors.Raw.GlassTile)
                .border(ShifaSize.S2, ShifaColors.Raw.GlassTileBorder, ShifaRadius.ControlLg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(Res.drawable.img_logo),
                contentDescription = null,
                modifier = Modifier.size(ShifaDimens.LoginLogo),
                tint = ShifaColors.Icon.Inverse,
            )
        }
    }
}
