package shifa.uz.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.changePassword
import shifa.composeapp.generated.resources.confirmPassword
import shifa.composeapp.generated.resources.currentPassword
import shifa.composeapp.generated.resources.newPassword
import shifa.composeapp.generated.resources.save
import shifa.uz.ui.register.Banners
import shifa.uz.ui.register.Field
import shifa.uz.ui.register.PasswordChecklist
import shifa.uz.ui.register.PasswordEye
import shifa.uz.ui.register.RegisterPage

/** 14 §3.4 (no Figma): the registration form layout. */
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    onChanged: () -> Unit,
    viewModel: ChangePasswordViewModel = viewModel { ChangePasswordViewModel() },
) {
    val state by viewModel.state.collectAsState()
    val changed by rememberUpdatedState(onChanged)
    LaunchedEffect(viewModel) { viewModel.changed.collect { changed() } }
    ChangePasswordContent(
        state,
        onBack = onBack,
        onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onSave = viewModel::save,
    )
}

@Composable
internal fun ChangePasswordContent(
    state: ChangePasswordUiState,
    onBack: () -> Unit,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSave: () -> Unit,
) {
    fun shown(error: StringResource?) = error.takeIf { state.showErrors }
    val transformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
    val eye: @Composable () -> Unit = { PasswordEye(state.isPasswordVisible, onTogglePasswordVisibility) }
    RegisterPage(
        stringResource(Res.string.changePassword),
        onBack,
        stringResource(Res.string.save),
        onSave,
        state.isSaving,
        null,
    ) {
        Banners(state.error)
        Field(
            state.currentPassword,
            onCurrentPasswordChange,
            Res.string.currentPassword,
            shown(state.currentPasswordError),
            KeyboardType.Password,
            transformation,
            eye,
        )
        Field(
            state.newPassword,
            onNewPasswordChange,
            Res.string.newPassword,
            shown(state.newPasswordError),
            KeyboardType.Password,
            transformation,
            eye,
        )
        Field(
            state.confirmPassword,
            onConfirmPasswordChange,
            Res.string.confirmPassword,
            shown(state.confirmPasswordError),
            KeyboardType.Password,
            transformation,
            eye,
            ImeAction.Done,
            onSave,
        )
        PasswordChecklist(state.newPassword)
    }
}
