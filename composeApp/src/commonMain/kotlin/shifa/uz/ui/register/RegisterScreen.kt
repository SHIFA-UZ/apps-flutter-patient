package shifa.uz.ui.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.accountAlreadyExists
import shifa.composeapp.generated.resources.accountInformation
import shifa.composeapp.generated.resources.address
import shifa.composeapp.generated.resources.cancel
import shifa.composeapp.generated.resources.codeExpiresIn
import shifa.composeapp.generated.resources.confirmPassword
import shifa.composeapp.generated.resources.continueWithEmail
import shifa.composeapp.generated.resources.countryOfResidence
import shifa.composeapp.generated.resources.createAccount
import shifa.composeapp.generated.resources.createPatientAccount
import shifa.composeapp.generated.resources.dateOfBirth
import shifa.composeapp.generated.resources.email
import shifa.composeapp.generated.resources.emailOptional
import shifa.composeapp.generated.resources.enterEmailCode
import shifa.composeapp.generated.resources.enterSmsCode
import shifa.composeapp.generated.resources.existingDoctorMessage
import shifa.composeapp.generated.resources.existingDoctorTitle
import shifa.composeapp.generated.resources.existingPatientMessage
import shifa.composeapp.generated.resources.hidePassword
import shifa.composeapp.generated.resources.ic_alert_circle_16
import shifa.composeapp.generated.resources.ic_calendar_20
import shifa.composeapp.generated.resources.ic_check_12
import shifa.composeapp.generated.resources.ic_check_circle_16
import shifa.composeapp.generated.resources.ic_eye_20
import shifa.composeapp.generated.resources.ic_eye_off_20
import shifa.composeapp.generated.resources.ic_upload_22
import shifa.composeapp.generated.resources.img_login_hero
import shifa.composeapp.generated.resources.img_logo
import shifa.composeapp.generated.resources.invalidEmail
import shifa.composeapp.generated.resources.name
import shifa.composeapp.generated.resources.next
import shifa.composeapp.generated.resources.ok
import shifa.composeapp.generated.resources.otpSentToEmail
import shifa.composeapp.generated.resources.otpSentToPhone
import shifa.composeapp.generated.resources.password
import shifa.composeapp.generated.resources.phoneNumber
import shifa.composeapp.generated.resources.profileImage
import shifa.composeapp.generated.resources.resendCode
import shifa.composeapp.generated.resources.resendCodeIn
import shifa.composeapp.generated.resources.searchCountry
import shifa.composeapp.generated.resources.selectDate
import shifa.composeapp.generated.resources.showPassword
import shifa.composeapp.generated.resources.smsFailedUseEmailMessage
import shifa.composeapp.generated.resources.smsUnavailableTitle
import shifa.composeapp.generated.resources.surname
import shifa.composeapp.generated.resources.upload
import shifa.composeapp.generated.resources.verifyAndCreate
import shifa.uz.data.model.OtpChannel
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.widgets.ShifaBanner
import shifa.uz.ui.widgets.ShifaBannerType
import shifa.uz.ui.widgets.ShifaDropdown
import shifa.uz.ui.widgets.ShifaInputField
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPager
import shifa.uz.ui.widgets.ShifaPopUp
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSearchField
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.ui.widgets.ShifaTertiaryButton
import shifa.uz.ui.widgets.ShifaTray
import shifa.uz.utils.Country
import shifa.uz.utils.countries
import shifa.uz.utils.formatDate
import shifa.uz.utils.getCurrentInstant
import shifa.uz.utils.rememberImagePickerLauncher

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onClose: () -> Unit,
    viewModel: RegisterViewModel = viewModel { RegisterViewModel() },
) {
    val state by viewModel.state.collectAsState()
    val registered by rememberUpdatedState(onRegistered)
    LaunchedEffect(viewModel) { viewModel.registered.collect { registered() } }
    val onBack = {
        if (!viewModel.back()) {
            viewModel.reset()
            onClose()
        }
    }
    BackHandler(onBack = onBack)
    val pickPhoto = rememberImagePickerLauncher(viewModel::onPhotoPicked)
    when (state.step) {
        RegisterStep.Account ->
            CreateAccountContent(
                state = state,
                onFirstNameChange = viewModel::onFirstNameChange,
                onLastNameChange = viewModel::onLastNameChange,
                onPhoneChange = viewModel::onPhoneChange,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
                onBack = onBack,
                onNext = viewModel::next,
            )
        RegisterStep.Info ->
            AccountInfoContent(
                state = state,
                onBirthDateChange = viewModel::onBirthDateChange,
                onCountryChange = viewModel::onCountryChange,
                onAddressChange = viewModel::onAddressChange,
                onBack = onBack,
                onNext = viewModel::next,
            )
        RegisterStep.Photo ->
            ProfileImageContent(
                state,
                onUpload = pickPhoto,
                onBack = onBack,
                onNext = viewModel::next,
            )
        RegisterStep.Otp ->
            VerifyCodeContent(
                state = state,
                onOtpChange = viewModel::onOtpChange,
                onResend = viewModel::resend,
                onBack = onBack,
                onVerify = viewModel::next,
            )
    }
    RegisterDialogs(
        dialog = state.dialog,
        fallbackEmail = state.fallbackEmailInput,
        fallbackEmailError = state.fallbackEmailInputError,
        onDismiss = viewModel::dismissDialog,
        onCreatePatientAccount = viewModel::onCreatePatientAccount,
        onFallbackEmailChange = viewModel::onFallbackEmailChange,
        onContinueWithEmail = viewModel::onContinueWithEmail,
    )
}

/** Figma create-account (`1631:10131`), plus an optional email (05 §3.3). "Continue with Google" is hidden. */
@Composable
internal fun CreateAccountContent(
    state: RegisterUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    fun shown(error: StringResource?) = error.takeIf { state.showErrors }
    RegisterPage(
        stringResource(Res.string.createAccount),
        onBack,
        stringResource(Res.string.next),
        onNext,
        state.isLoading,
        0,
    ) {
        SubLogo()
        Banners(state.error)
        Field(state.firstName, onFirstNameChange, Res.string.name, shown(state.firstNameError))
        Field(state.lastName, onLastNameChange, Res.string.surname, shown(state.lastNameError))
        Field(state.phone, onPhoneChange, Res.string.phoneNumber, shown(state.phoneError), KeyboardType.Phone)
        Field(state.email, onEmailChange, Res.string.emailOptional, shown(state.emailError), KeyboardType.Email)
        val passwordTransformation =
            if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
        val eye: @Composable () -> Unit = { PasswordEye(state.isPasswordVisible, onTogglePasswordVisibility) }
        Field(
            state.password,
            onPasswordChange,
            Res.string.password,
            shown(state.passwordError),
            KeyboardType.Password,
            passwordTransformation,
            eye,
        )
        if (state.password.isNotEmpty()) PasswordChecklist(state.password)
        Field(
            state.confirmPassword,
            onConfirmPasswordChange,
            Res.string.confirmPassword,
            shown(state.confirmPasswordError),
            KeyboardType.Password,
            passwordTransformation,
            eye,
            ImeAction.Done,
            onNext,
        )
    }
}

/** Figma account-information (`1631:10296`). Every field is optional. "Continue with Google" is hidden. */
@Composable
internal fun AccountInfoContent(
    state: RegisterUiState,
    onBirthDateChange: (LocalDate) -> Unit,
    onCountryChange: (Country) -> Unit,
    onAddressChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    var pickingDate by remember { mutableStateOf(false) }
    var pickingCountry by remember { mutableStateOf(false) }
    RegisterPage(
        stringResource(Res.string.accountInformation),
        onBack,
        stringResource(Res.string.next),
        onNext,
        state.isLoading,
        1,
    ) {
        SubLogo()
        Banners(state.error)
        Box {
            ShifaInputField(
                value = state.birthDate?.let(::formatDate).orEmpty(),
                onValueChange = {},
                label = stringResource(Res.string.dateOfBirth),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(painterResource(Res.drawable.ic_calendar_20), null, tint = ShifaColors.Icon.Secondary)
                },
            )
            // Opens the picker instead of the keyboard.
            val selectDate = stringResource(Res.string.selectDate)
            Box(
                Modifier.matchParentSize().clickable(
                    role = Role.Button,
                    onClickLabel = selectDate,
                ) { pickingDate = true },
            )
        }
        // ~250 countries don't fit the inline menu: the closed dropdown opens a searchable tray.
        ShifaDropdown(
            label = stringResource(Res.string.countryOfResidence),
            options = emptyList(),
            selected = state.country?.name,
            onSelect = {},
            expanded = false,
            onExpandedChange = { pickingCountry = true },
            modifier = Modifier.fillMaxWidth(),
        )
        Field(state.address, onAddressChange, Res.string.address, null, imeAction = ImeAction.Done, onDone = onNext)
    }
    if (pickingDate) {
        BirthDatePicker(state.birthDate, onPick = onBirthDateChange, onDismiss = { pickingDate = false })
    }
    if (pickingCountry) {
        CountryPicker(state.country, onPick = onCountryChange, onDismiss = { pickingCountry = false })
    }
}

/** Figma profile-image (`1631:10449`). The placeholder is the login hero art, as in Figma. */
@Composable
internal fun ProfileImageContent(
    state: RegisterUiState,
    onUpload: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    RegisterPage(
        stringResource(Res.string.profileImage),
        onBack,
        stringResource(Res.string.next),
        onNext,
        state.isLoading,
        2,
    ) {
        Banners(state.error)
        Column(
            verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val bytes = state.photo?.fileBytes
            val photo = remember(bytes) { bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }
            val avatar = Modifier.size(ShifaDimens.RegisterAvatar).clip(ShifaRadius.Full)
            if (photo != null) {
                Image(photo, null, avatar, contentScale = ContentScale.Crop)
            } else {
                Image(painterResource(Res.drawable.img_login_hero), null, avatar, contentScale = ContentScale.Crop)
            }
            ShifaSecondaryButton(
                stringResource(Res.string.upload),
                onUpload,
                Modifier.fillMaxWidth(),
                icon = Res.drawable.ic_upload_22,
            )
        }
    }
}

/** 05 §3.4 and §3.6, no Figma frame: built from the kit like the other steps. */
@Composable
internal fun VerifyCodeContent(
    state: RegisterUiState,
    onOtpChange: (String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    onVerify: () -> Unit,
) {
    val email = state.otpChannel == OtpChannel.Email
    val title = stringResource(Res.string.verifyAndCreate)
    RegisterPage(title, onBack, title, onVerify, state.isLoading, page = null) {
        SubLogo()
        Banners(state.error, state.notice)
        CodeEntry(
            message =
                stringResource(
                    if (email) Res.string.otpSentToEmail else Res.string.otpSentToPhone,
                    state.otpDestination,
                ),
            email = email,
            otp = state.otp,
            onOtpChange = onOtpChange,
            resendSeconds = state.resendSeconds,
            canResend = state.canResend,
            onResend = onResend,
            onDone = onVerify,
        )
    }
}

/** The code screen body, shared with forgot password (05 §3.8), which adds [expiresInSeconds]. */
@Composable
internal fun CodeEntry(
    message: String,
    email: Boolean,
    otp: String,
    onOtpChange: (String) -> Unit,
    resendSeconds: Int,
    canResend: Boolean,
    onResend: () -> Unit,
    onDone: () -> Unit,
    expiresInSeconds: Int? = null,
) {
    Text(
        message,
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = ShifaColors.Text.Primary,
        textAlign = TextAlign.Center,
    )
    expiresInSeconds?.let {
        Text(
            stringResource(Res.string.codeExpiresIn, formatCountdown(it)),
            style = ShifaTextStyles.caption(),
            color = if (it <= 60) ShifaColors.Text.Error else ShifaColors.Text.Tertiary,
        )
    }
    Field(
        otp,
        onOtpChange,
        if (email) Res.string.enterEmailCode else Res.string.enterSmsCode,
        null,
        KeyboardType.NumberPassword,
        imeAction = ImeAction.Done,
        onDone = onDone,
    )
    ShifaTertiaryButton(
        if (resendSeconds > 0) {
            stringResource(Res.string.resendCodeIn, formatCountdown(resendSeconds))
        } else {
            stringResource(Res.string.resendCode)
        },
        onResend,
        enabled = canResend,
    )
}

// 05 §7: m:ss.
internal fun formatCountdown(seconds: Int) = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

// Header, a scrolling body centred in the free space, then the pager and the action (Figma page-elements).
@Composable
internal fun RegisterPage(
    title: String,
    onBack: () -> Unit,
    action: String,
    onAction: () -> Unit,
    isLoading: Boolean,
    page: Int?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding().imePadding()) {
        ShifaPageElement(ShifaPageElementType.Header) { ShifaSubHeader(title, onBack = onBack) }
        BoxWithConstraints(Modifier.weight(1f)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
                verticalArrangement = Arrangement.Center,
            ) {
                ShifaPageElement(ShifaPageElementType.Body) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = content,
                    )
                }
            }
        }
        ShifaPageElement(ShifaPageElementType.ActionTray) {
            page?.let { ShifaPager(count = 3, current = it, modifier = Modifier.align(Alignment.CenterHorizontally)) }
            ShifaPrimaryButton(action, onAction, Modifier.fillMaxWidth(), loading = isLoading)
        }
        Spacer(Modifier.height(ShifaDimens.LoginFooterHeight))
    }
}

// Figma "shifa sub logo" is a glass tile; with no backdrop to blur it's drawn as a surface tile with a faint brand border.
@Composable
internal fun SubLogo() {
    Box(
        Modifier
            .padding(vertical = ShifaSpacing.Inset.Md)
            .size(ShifaDimens.LoginHeroTile)
            .clip(ShifaRadius.ControlLg)
            .background(ShifaColors.Bg.Surface)
            .border(ShifaSize.S2, ShifaColors.Alpha.Teal15, ShifaRadius.ControlLg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(Res.drawable.img_logo),
            null,
            Modifier.size(ShifaDimens.LoginLogo),
            tint = ShifaColors.Icon.Brand,
        )
    }
}

@Composable
internal fun Banners(
    error: StringResource?,
    notice: StringResource? = null,
) {
    error?.let { ShifaBanner(stringResource(it), Res.drawable.ic_alert_circle_16, ShifaBannerType.Error) }
    notice?.let { ShifaBanner(stringResource(it), Res.drawable.ic_check_circle_16, ShifaBannerType.Success) }
}

@Composable
internal fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: StringResource,
    error: StringResource?,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
) = ShifaInputField(
    value = value,
    onValueChange = onValueChange,
    label = stringResource(label),
    modifier = Modifier.fillMaxWidth(),
    supportingText = error?.let { stringResource(it) },
    isError = error != null,
    clearable = value.isNotEmpty(),
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
    keyboardActions = KeyboardActions(onDone = { onDone() }),
    visualTransformation = visualTransformation,
    trailingIcon = trailingIcon,
)

@Composable
internal fun PasswordEye(
    visible: Boolean,
    onToggle: () -> Unit,
) = Icon(
    painterResource(if (visible) Res.drawable.ic_eye_off_20 else Res.drawable.ic_eye_20),
    stringResource(if (visible) Res.string.hidePassword else Res.string.showPassword),
    Modifier.clip(ShifaRadius.Full).clickable(onClick = onToggle),
    tint = ShifaColors.Icon.Secondary,
)

// 05 §3.3: shown once the password is non-empty. No Figma component.
@Composable
internal fun PasswordChecklist(password: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = ShifaSpacing.Inset.Sm)) {
        PasswordRule.entries.forEach { rule ->
            val passes = rule.passes(password)
            Row(
                horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(Res.drawable.ic_check_12),
                    null,
                    tint = if (passes) ShifaColors.Icon.Brand else ShifaColors.Icon.Disabled,
                )
                Text(
                    stringResource(rule.label),
                    style = ShifaTextStyles.caption(),
                    color = if (passes) ShifaColors.Text.Primary else ShifaColors.Text.Tertiary,
                )
            }
        }
    }
}

@Composable
internal fun BirthDatePicker(
    selected: LocalDate?,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { getCurrentInstant().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    DayPicker(selected, LocalDate(1900, 1, 1)..today, selected ?: today.minus(25, DateTimeUnit.YEAR), onPick, onDismiss)
}

/** Material date picker over [range]; [shown] is the month it opens on. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DayPicker(
    selected: LocalDate?,
    range: ClosedRange<LocalDate>,
    shown: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val millis = range.start.toEpochMillis()..range.endInclusive.toEpochMillis()
    val picker =
        rememberDatePickerState(
            initialSelectedDateMillis = selected?.toEpochMillis(),
            initialDisplayedMonthMillis = shown.toEpochMillis(),
            yearRange = range.start.year..range.endInclusive.year,
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in millis
                },
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            ShifaTertiaryButton(stringResource(Res.string.ok), {
                picker.selectedDateMillis?.let {
                    onPick(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date)
                }
                onDismiss()
            })
        },
        dismissButton = { ShifaTertiaryButton(stringResource(Res.string.cancel), onDismiss) },
    ) {
        DatePicker(picker)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryPicker(
    selected: Country?,
    onPick: (Country) -> Unit,
    onDismiss: () -> Unit,
) {
    val countries = remember { countries() }
    var query by remember { mutableStateOf("") }
    // As the ShifaTray KDoc says: the tray draws the sheet.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RectangleShape,
        containerColor = Color.Transparent,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        CountryTray(countries, selected, query, { query = it }, onPick = {
            onPick(it)
            onDismiss()
        }, onDismiss)
    }
}

/** No Figma frame: the kit tray with a search field and dropdown-style options. */
@Composable
internal fun CountryTray(
    countries: List<Country>,
    selected: Country?,
    query: String,
    onQueryChange: (String) -> Unit,
    onPick: (Country) -> Unit,
    onClose: () -> Unit,
) = ShifaTray(stringResource(Res.string.countryOfResidence), onClose, Modifier.statusBarsPadding()) {
    ShifaSearchField(query, onQueryChange, stringResource(Res.string.searchCountry), Modifier.fillMaxWidth())
    val q = query.trim()
    val shown = countries.filter { it.name.contains(q, ignoreCase = true) || it.apiName.contains(q, ignoreCase = true) }
    val style = MaterialTheme.typography.bodyLarge
    // Full height, so the tray doesn't jump while the results shrink.
    LazyColumn(Modifier.weight(1f)) {
        items(shown, key = { it.code }) { country ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(country == selected, role = Role.RadioButton) { onPick(country) }
                    .padding(horizontal = ShifaSpacing.Inset.Sm, vertical = ShifaSpacing.Inset.Xs),
                horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (country == selected) {
                    Icon(painterResource(Res.drawable.ic_check_12), null, tint = ShifaColors.Icon.Brand)
                }
                Text(country.name, style = style, color = ShifaColors.Text.Primary)
            }
        }
    }
    Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)))
}

// The Material date picker counts in UTC midnights.
internal fun LocalDate.toEpochMillis() = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

@Composable
private fun RegisterDialogs(
    dialog: RegisterDialog?,
    fallbackEmail: String,
    fallbackEmailError: Boolean,
    onDismiss: () -> Unit,
    onCreatePatientAccount: () -> Unit,
    onFallbackEmailChange: (String) -> Unit,
    onContinueWithEmail: () -> Unit,
) {
    if (dialog == null) return
    // Only the buttons close the email one (05 §2.4 step 6).
    val dismissible = dialog != RegisterDialog.EmailFallback
    Dialog(onDismiss, DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible)) {
        RegisterDialogContent(
            dialog,
            fallbackEmail,
            fallbackEmailError,
            onDismiss,
            onCreatePatientAccount,
            onFallbackEmailChange,
            onContinueWithEmail,
        )
    }
}

@Composable
internal fun RegisterDialogContent(
    dialog: RegisterDialog,
    fallbackEmail: String,
    fallbackEmailError: Boolean,
    onDismiss: () -> Unit,
    onCreatePatientAccount: () -> Unit,
    onFallbackEmailChange: (String) -> Unit,
    onContinueWithEmail: () -> Unit,
) = when (dialog) {
    RegisterDialog.ExistingPatient ->
        MessagePopUp(
            stringResource(Res.string.accountAlreadyExists),
            stringResource(Res.string.existingPatientMessage),
            onDismiss,
        ) {
            ShifaPrimaryButton(stringResource(Res.string.ok), onDismiss, Modifier.fillMaxWidth())
        }
    is RegisterDialog.ExistingDoctor ->
        MessagePopUp(
            stringResource(Res.string.existingDoctorTitle),
            stringResource(Res.string.existingDoctorMessage, dialog.name),
            onDismiss,
        ) {
            ShifaPrimaryButton(
                stringResource(Res.string.createPatientAccount),
                onCreatePatientAccount,
                Modifier.fillMaxWidth(),
            )
            ShifaSecondaryButton(stringResource(Res.string.cancel), onDismiss, Modifier.fillMaxWidth())
        }
    RegisterDialog.EmailFallback ->
        MessagePopUp(
            stringResource(Res.string.smsUnavailableTitle),
            stringResource(Res.string.smsFailedUseEmailMessage),
            onDismiss,
        ) {
            Field(
                fallbackEmail,
                onFallbackEmailChange,
                Res.string.email,
                Res.string.invalidEmail.takeIf { fallbackEmailError },
                KeyboardType.Email,
                imeAction = ImeAction.Done,
                onDone = onContinueWithEmail,
            )
            ShifaPrimaryButton(
                stringResource(Res.string.continueWithEmail),
                onContinueWithEmail,
                Modifier.fillMaxWidth(),
            )
            ShifaSecondaryButton(stringResource(Res.string.cancel), onDismiss, Modifier.fillMaxWidth())
        }
}

@Composable
internal fun MessagePopUp(
    title: String,
    message: String,
    onClose: () -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
) = ShifaPopUp(title, onClose) {
    Text(message, style = MaterialTheme.typography.bodyMedium, color = ShifaColors.Text.Primary)
    actions()
}
