package shifa.uz.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.accountTitle
import shifa.composeapp.generated.resources.cancel
import shifa.composeapp.generated.resources.changePassword
import shifa.composeapp.generated.resources.delete
import shifa.composeapp.generated.resources.deleteAccount
import shifa.composeapp.generated.resources.deleteAccountWarning
import shifa.composeapp.generated.resources.editProfile
import shifa.composeapp.generated.resources.ic_camera_8
import shifa.composeapp.generated.resources.ic_check_circle_16
import shifa.composeapp.generated.resources.ic_clock_16
import shifa.composeapp.generated.resources.ic_globe_24
import shifa.composeapp.generated.resources.ic_log_out_24
import shifa.composeapp.generated.resources.ic_refresh_cw_24
import shifa.composeapp.generated.resources.ic_shield_24
import shifa.composeapp.generated.resources.ic_trash_24
import shifa.composeapp.generated.resources.language
import shifa.composeapp.generated.resources.languageChanged
import shifa.composeapp.generated.resources.logOut
import shifa.composeapp.generated.resources.noName
import shifa.composeapp.generated.resources.privacy
import shifa.composeapp.generated.resources.profile
import shifa.composeapp.generated.resources.profileUpdatedSuccessfully
import shifa.composeapp.generated.resources.selectLanguage
import shifa.composeapp.generated.resources.settingsTitle
import shifa.composeapp.generated.resources.signOutConfirm
import shifa.composeapp.generated.resources.uploadPhoto
import shifa.composeapp.generated.resources.uploading
import shifa.uz.data.model.PatientProfile
import shifa.uz.data.storage.AppLanguage
import shifa.uz.ui.debug.DebugScreen
import shifa.uz.ui.navigation.ScopedViewModels
import shifa.uz.ui.register.Banners
import shifa.uz.ui.register.MessagePopUp
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.ui.widgets.CardTitle
import shifa.uz.ui.widgets.ShifaAvatar
import shifa.uz.ui.widgets.ShifaBanner
import shifa.uz.ui.widgets.ShifaBannerType
import shifa.uz.ui.widgets.ShifaPageElement
import shifa.uz.ui.widgets.ShifaPageElementType
import shifa.uz.ui.widgets.ShifaPopUp
import shifa.uz.ui.widgets.ShifaPrimaryButton
import shifa.uz.ui.widgets.ShifaSecondaryButton
import shifa.uz.ui.widgets.ShifaSection
import shifa.uz.ui.widgets.ShifaSectionType
import shifa.uz.ui.widgets.ShifaSettingsItem
import shifa.uz.ui.widgets.ShifaSubHeader
import shifa.uz.utils.isDebugBuild
import shifa.uz.utils.rememberImagePickerLauncher
import shifa.uz.utils.rememberInAppBrowser

private enum class ProfilePage { Edit, ChangePassword, Debug }

private const val PRIVACY_URL = "https://shifa.uz/#privacy"

/** 14 §3.1: the menu, its dialogs, and the pages it opens. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogOut: () -> Unit,
    onAccountDeleted: () -> Unit,
    onProfileChanged: (PatientProfile) -> Unit,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() },
) {
    val state by viewModel.state.collectAsState()
    val logOut by rememberUpdatedState(onLogOut)
    val changed by rememberUpdatedState(onProfileChanged)
    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                ProfileEvent.SignedOut -> logOut()
                ProfileEvent.Changed -> viewModel.state.value.profile?.let(changed)
            }
        }
    }
    val pickPhoto = rememberImagePickerLauncher(viewModel::uploadPhoto)
    val openUrl = rememberInAppBrowser()
    var page by rememberSaveable { mutableStateOf<ProfilePage?>(null) }
    ProfileContent(
        state,
        onBack = onBack,
        onRefresh = viewModel::load,
        onChangePhoto = pickPhoto,
        onEdit = { page = ProfilePage.Edit },
        onLanguage = { viewModel.showDialog(ProfileDialog.Language) },
        onChangePassword = { page = ProfilePage.ChangePassword },
        onPrivacy = { openUrl(PRIVACY_URL) },
        onDebug = { page = ProfilePage.Debug }.takeIf { isDebugBuild },
        onLogOut = { viewModel.showDialog(ProfileDialog.LogOut) },
        onDeleteAccount = { viewModel.showDialog(ProfileDialog.DeleteAccount) },
    )
    state.dialog?.let {
        Dialog(viewModel::dismissDialog) {
            ProfileDialogContent(
                it,
                state.language,
                state.isRequestingDeletion,
                onDismiss = viewModel::dismissDialog,
                onSelectLanguage = viewModel::selectLanguage,
                onLogOut = {
                    viewModel.dismissDialog()
                    onLogOut()
                },
                onDelete = viewModel::requestDeletion,
            )
        }
    }
    page?.let {
        val close = { page = null }
        BackHandler(onBack = close)
        key(it) {
            ScopedViewModels {
                when (it) {
                    ProfilePage.Edit ->
                        EditProfileScreen(
                            state.profile,
                            onBack = close,
                            onPhotoChanged = { updated -> viewModel.onProfileChanged(updated) },
                            onSaved = { updated ->
                                viewModel.onProfileChanged(updated, Res.string.profileUpdatedSuccessfully)
                                close()
                            },
                        )
                    ProfilePage.ChangePassword ->
                        ChangePasswordScreen(onBack = close, onChanged = {
                            viewModel.onPasswordChanged()
                            close()
                        })
                    ProfilePage.Debug -> DebugScreen(onBack = close)
                }
            }
        }
    }
    state.deletion?.let {
        BackHandler(onBack = viewModel::closeDeletion)
        key(it) {
            ScopedViewModels {
                DeleteAccountScreen(it, state.email, onBack = viewModel::closeDeletion, onDeleted = onAccountDeleted)
            }
        }
    }
}

/** Figma profile-settings (`1703:16707`). No version footer: Figma has none. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileContent(
    state: ProfileUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onChangePhoto: () -> Unit,
    onEdit: () -> Unit,
    onLanguage: () -> Unit,
    onChangePassword: () -> Unit,
    onPrivacy: () -> Unit,
    onLogOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDebug: (() -> Unit)? = null, // debug builds only
) {
    Column(Modifier.fillMaxSize().background(ShifaColors.Bg.Surface).statusBarsPadding()) {
        ShifaPageElement(ShifaPageElementType.Header) {
            ShifaSubHeader(stringResource(Res.string.profile), onBack = onBack)
        }
        PullToRefreshBox(state.isLoading, onRefresh, Modifier.weight(1f).background(ShifaColors.Bg.Canvas)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
                ShifaPageElement(ShifaPageElementType.SubBody) {
                    Column(verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm)) {
                        Banners(state.error)
                        if (state.isUploading) UploadingBanner()
                        state.notice?.let {
                            val text = stringResource(it)
                            ShifaBanner(
                                if (it == Res.string.languageChanged) "$text ${state.language.displayName}" else text,
                                Res.drawable.ic_check_circle_16,
                                ShifaBannerType.Success,
                            )
                        }
                        ProfileCard(state.profile, onChangePhoto)
                        ShifaPrimaryButton(
                            stringResource(Res.string.editProfile),
                            onEdit,
                            Modifier.fillMaxWidth(),
                            enabled = state.profile != null,
                        )
                        ShifaSection(
                            stringResource(Res.string.settingsTitle),
                            type = ShifaSectionType.CardTransparent,
                        ) {
                            Column {
                                ShifaSettingsItem(
                                    stringResource(Res.string.language),
                                    onLanguage,
                                    icon = Res.drawable.ic_globe_24,
                                )
                                ShifaSettingsItem(
                                    stringResource(Res.string.changePassword),
                                    onChangePassword,
                                    icon = Res.drawable.ic_refresh_cw_24,
                                )
                                ShifaSettingsItem(
                                    stringResource(Res.string.privacy),
                                    onPrivacy,
                                    icon = Res.drawable.ic_shield_24,
                                )
                                onDebug?.let { ShifaSettingsItem("Debug menu", it) }
                            }
                        }
                        ShifaSection(stringResource(Res.string.accountTitle), type = ShifaSectionType.CardTransparent) {
                            Column {
                                ShifaSettingsItem(
                                    stringResource(Res.string.logOut),
                                    onLogOut,
                                    icon = Res.drawable.ic_log_out_24,
                                )
                                ShifaSettingsItem(
                                    stringResource(Res.string.deleteAccount),
                                    onDeleteAccount,
                                    icon = Res.drawable.ic_trash_24,
                                    destructive = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun UploadingBanner() =
    ShifaBanner(
        stringResource(Res.string.uploading),
        Res.drawable.ic_clock_16,
        ShifaBannerType.Success,
    )

// Figma doctor-card as the profile header: avatar with a camera badge, name, phone, email.
@Composable
private fun ProfileCard(
    profile: PatientProfile?,
    onChangePhoto: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(ShifaRadius.ControlMd)
            .background(ShifaColors.Bg.Surface)
            .padding(ShifaSpacing.Inset.Md),
        horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.clickable(
                onClickLabel = stringResource(Res.string.uploadPhoto),
                role = Role.Button,
                onClick = onChangePhoto,
            ),
        ) {
            ShifaAvatar(profile?.photoUrl, ShifaDimens.ProfileAvatar, placeholder = ShifaColors.Bg.Disabled)
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .clip(ShifaRadius.Full)
                    .background(ShifaColors.Bg.Brand)
                    .padding(ShifaSpacing.Gap.Xs),
            ) {
                Icon(painterResource(Res.drawable.ic_camera_8), null, tint = ShifaColors.Icon.Inverse)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Xs)) {
            if (profile != null) {
                Text(
                    profile.fullName.trim().ifEmpty { stringResource(Res.string.noName) },
                    style = ShifaTextStyles.profileName(),
                    color = ShifaColors.Text.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            profile?.phone?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = ShifaColors.Text.Primary)
            }
            profile?.email?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = ShifaColors.Text.Primary)
            }
        }
    }
}

/** 14 §3.1.1-3.1.3; no Figma, so the pop-up widget. */
@Composable
internal fun ProfileDialogContent(
    dialog: ProfileDialog,
    language: AppLanguage,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onLogOut: () -> Unit,
    onDelete: () -> Unit,
) = when (dialog) {
    ProfileDialog.Language ->
        ShifaPopUp(stringResource(Res.string.selectLanguage), onDismiss) {
            // Display names aren't translated (14 §3.1.1).
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach {
                    LanguageRow(it.displayName, it == language) { onSelectLanguage(it) }
                }
            }
            ShifaSecondaryButton(stringResource(Res.string.cancel), onDismiss, Modifier.fillMaxWidth())
        }
    ProfileDialog.LogOut ->
        MessagePopUp(stringResource(Res.string.logOut), stringResource(Res.string.signOutConfirm), onDismiss) {
            ShifaPrimaryButton(stringResource(Res.string.logOut), onLogOut, Modifier.fillMaxWidth(), destructive = true)
            ShifaSecondaryButton(stringResource(Res.string.cancel), onDismiss, Modifier.fillMaxWidth())
        }
    ProfileDialog.DeleteAccount ->
        MessagePopUp(
            stringResource(Res.string.deleteAccount),
            stringResource(Res.string.deleteAccountWarning),
            onDismiss,
        ) {
            ShifaPrimaryButton(
                stringResource(Res.string.delete),
                onDelete,
                Modifier.fillMaxWidth(),
                destructive = true,
                loading = isDeleting,
            )
            ShifaSecondaryButton(
                stringResource(Res.string.cancel),
                onDismiss,
                Modifier.fillMaxWidth(),
                enabled = !isDeleting,
            )
        }
}

@Composable
private fun LanguageRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = ShifaSpacing.Inset.Sm),
        horizontalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected,
            onClick = null,
            colors =
                RadioButtonDefaults.colors(
                    selectedColor = ShifaColors.Interactive.Primary,
                    unselectedColor = ShifaColors.Icon.Secondary,
                ),
        )
        CardTitle(name)
    }
}
