package shifa.uz.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.address
import shifa.composeapp.generated.resources.birthDate
import shifa.composeapp.generated.resources.discardChanges
import shifa.composeapp.generated.resources.editProfile
import shifa.composeapp.generated.resources.firstName
import shifa.composeapp.generated.resources.gender
import shifa.composeapp.generated.resources.generalInformation
import shifa.composeapp.generated.resources.ic_calendar_20
import shifa.composeapp.generated.resources.ic_camera_16
import shifa.composeapp.generated.resources.ic_map_pin_22
import shifa.composeapp.generated.resources.language
import shifa.composeapp.generated.resources.lastName
import shifa.composeapp.generated.resources.locationAndLanguage
import shifa.composeapp.generated.resources.phoneNumber
import shifa.composeapp.generated.resources.saveChanges
import shifa.composeapp.generated.resources.selectDate
import shifa.composeapp.generated.resources.selectOnMap
import shifa.composeapp.generated.resources.streetAddress
import shifa.composeapp.generated.resources.uploadPhoto
import shifa.uz.data.model.PatientProfile
import shifa.uz.data.storage.AppLanguage
import shifa.uz.ui.navigation.ComingSoon
import shifa.uz.ui.register.Banners
import shifa.uz.ui.register.BirthDatePicker
import shifa.uz.ui.register.Field
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaShadow
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.widgets.ShifaActionButton
import shifa.uz.ui.widgets.ShifaAvatar
import shifa.uz.ui.widgets.ShifaDropdown
import shifa.uz.ui.widgets.ShifaInputField
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.ui.widgets.ShifaSection
import shifa.uz.ui.widgets.ShifaSectionType
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.utils.formatDate
import shifa.uz.utils.rememberImagePickerLauncher

/** 14 §3.2. The photo saves on its own ([onPhotoChanged]); the rest on Save ([onSaved]). */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditProfileScreen(
    initial: PatientProfile?,
    onBack: () -> Unit,
    onPhotoChanged: (PatientProfile) -> Unit,
    onSaved: (PatientProfile) -> Unit,
    viewModel: EditProfileViewModel = viewModel { EditProfileViewModel(initial) },
) {
    val state by viewModel.state.collectAsState()
    val photoChanged by rememberUpdatedState(onPhotoChanged)
    val saved by rememberUpdatedState(onSaved)
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                is EditProfileEvent.PhotoChanged -> photoChanged(it.profile)
                is EditProfileEvent.Saved -> saved(it.profile)
            }
        }
    }
    val pickPhoto = rememberImagePickerLauncher(viewModel::uploadPhoto)
    var pickingDate by remember { mutableStateOf(false) }
    var onMap by rememberSaveable { mutableStateOf(false) }
    EditProfileContent(
        state,
        onBack = onBack,
        onChangePhoto = pickPhoto,
        onFirstNameChange = viewModel::onFirstNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onPhoneChange = viewModel::onPhoneChange,
        onPickDate = { pickingDate = true },
        onGenderChange = viewModel::onGenderChange,
        onAddressChange = viewModel::onAddressChange,
        onLanguageChange = viewModel::onLanguageChange,
        onStreetAddressChange = viewModel::onStreetAddressChange,
        onSelectOnMap = { onMap = true },
        onSave = viewModel::save,
    )
    if (pickingDate) {
        BirthDatePicker(state.birthDate, onPick = viewModel::onBirthDateChange, onDismiss = { pickingDate = false })
    }
    // ponytail: PROF-04 map picker is a placeholder; the street address is typed meanwhile.
    if (onMap) {
        BackHandler { onMap = false }
        ComingSoon(stringResource(Res.string.selectOnMap), onBack = { onMap = false })
    }
}

/** Figma edit profile (`1703:17376`). */
@Composable
internal fun EditProfileContent(
    state: EditProfileUiState,
    onBack: () -> Unit,
    onChangePhoto: () -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPickDate: () -> Unit,
    onGenderChange: (Gender) -> Unit,
    onAddressChange: (String) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onStreetAddressChange: (String) -> Unit,
    onSelectOnMap: () -> Unit,
    onSave: () -> Unit,
) {
    fun shown(error: StringResource?) = error.takeIf { state.showErrors }
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding().imePadding()) {
        ShifaPageElement(ShifaPageElementType.Header) {
            ShifaSubHeader(stringResource(Res.string.editProfile), onBack = onBack)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ShifaPageElement(ShifaPageElementType.Body) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Banners(state.error, state.notice)
                    if (state.isUploading) UploadingBanner()
                    Box {
                        ShifaAvatar(
                            state.photoUrl,
                            ShifaDimens.ProfileAvatarLarge,
                            placeholder = ShifaColors.Bg.Disabled,
                        )
                        ShifaActionButton(
                            onChangePhoto,
                            Modifier.align(Alignment.BottomEnd),
                            icon = Res.drawable.ic_camera_16,
                            contentDescription = stringResource(Res.string.uploadPhoto),
                            enabled = !state.isUploading,
                        )
                    }
                    Text(state.fullName, style = ShifaTextStyles.profileName(), color = ShifaColors.Text.Primary)
                    ShifaSection(
                        stringResource(Res.string.generalInformation),
                        type = ShifaSectionType.CardTransparent,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm)) {
                            Field(state.firstName, onFirstNameChange, Res.string.firstName, shown(state.firstNameError))
                            Field(state.lastName, onLastNameChange, Res.string.lastName, shown(state.lastNameError))
                            // ponytail: one field with the dial code; Figma's flag and code boxes need a country picker here.
                            Field(
                                state.phone,
                                onPhoneChange,
                                Res.string.phoneNumber,
                                shown(state.phoneError),
                                KeyboardType.Phone,
                            )
                            DateField(state.birthDate, onPickDate)
                            GenderDropdown(state.gender, onGenderChange)
                            Field(state.address, onAddressChange, Res.string.address, null)
                        }
                    }
                    ShifaSection(
                        stringResource(Res.string.locationAndLanguage),
                        type = ShifaSectionType.CardTransparent,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm)) {
                            LanguageDropdown(state.language, onLanguageChange)
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .dropShadow(ShifaRadius.ControlLg, ShifaShadow.Section)
                                    .background(ShifaColors.Alpha.White90, ShifaRadius.ControlLg)
                                    .padding(ShifaSpacing.Inset.Sm),
                                verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
                            ) {
                                Field(
                                    state.streetAddress,
                                    onStreetAddressChange,
                                    Res.string.streetAddress,
                                    null,
                                    imeAction = ImeAction.Done,
                                )
                                ShifaSecondaryButton(
                                    stringResource(Res.string.selectOnMap),
                                    onSelectOnMap,
                                    Modifier.fillMaxWidth(),
                                    icon = Res.drawable.ic_map_pin_22,
                                )
                            }
                        }
                    }
                }
            }
        }
        ShifaPageElement(ShifaPageElementType.ActionTray, Modifier.navigationBarsPadding()) {
            ShifaPrimaryButton(
                stringResource(Res.string.saveChanges),
                onSave,
                Modifier.fillMaxWidth(),
                loading = state.isSaving,
            )
            ShifaSecondaryButton(
                stringResource(Res.string.discardChanges),
                onBack,
                Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
            )
        }
    }
}

@Composable
private fun DateField(
    date: LocalDate?,
    onPick: () -> Unit,
) {
    Box {
        ShifaInputField(
            value = date?.let(::formatDate).orEmpty(),
            onValueChange = {},
            label = stringResource(Res.string.birthDate),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(painterResource(Res.drawable.ic_calendar_20), null, tint = ShifaColors.Icon.Secondary)
            },
        )
        // Opens the picker instead of the keyboard.
        val selectDate = stringResource(Res.string.selectDate)
        Box(Modifier.matchParentSize().clickable(role = Role.Button, onClickLabel = selectDate, onClick = onPick))
    }
}

@Composable
private fun GenderDropdown(
    selected: Gender?,
    onSelect: (Gender) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val labels = Gender.entries.map { stringResource(it.label) }
    ShifaDropdown(
        label = stringResource(Res.string.gender),
        options = labels,
        selected = selected?.let { labels[it.ordinal] },
        onSelect = { onSelect(Gender.entries[labels.indexOf(it)]) },
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    )
}

// Figma shows the code in a text field; a dropdown of the four app languages can't hold a bad value.
@Composable
private fun LanguageDropdown(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ShifaDropdown(
        label = stringResource(Res.string.language),
        options = AppLanguage.entries.map { it.displayName },
        selected = selected.displayName,
        onSelect = { name -> AppLanguage.entries.firstOrNull { it.displayName == name }?.let(onSelect) },
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    )
}
