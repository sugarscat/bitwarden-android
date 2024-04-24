package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorDataModel
import com.x8bit.bitwarden.data.auth.datasource.network.util.availableAuthMethods
import com.x8bit.bitwarden.data.auth.datasource.network.util.preferredAuthMethod
import com.x8bit.bitwarden.data.auth.datasource.network.util.twoFactorDisplayEmail
import com.x8bit.bitwarden.data.auth.datasource.network.util.twoFactorDuoAuthUrl
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.ResendEmailResult
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.DuoCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.data.auth.util.YubiKeyResult
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.baseWebVaultUrlOrDefault
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util.button
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util.imageRes
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util.isDuo
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util.shouldUseNfc
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the Two-Factor Login screen.
 */
@HiltViewModel
@Suppress("TooManyFunctions")
class TwoFactorLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val environmentRepository: EnvironmentRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<TwoFactorLoginState, TwoFactorLoginEvent, TwoFactorLoginAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: TwoFactorLoginState(
            authMethod = authRepository.twoFactorResponse.preferredAuthMethod,
            availableAuthMethods = authRepository.twoFactorResponse.availableAuthMethods,
            codeInput = "",
            displayEmail = authRepository.twoFactorResponse.twoFactorDisplayEmail,
            dialogState = null,
            isContinueButtonEnabled = authRepository.twoFactorResponse.preferredAuthMethod.isDuo,
            isRememberMeEnabled = false,
            captchaToken = null,
            email = TwoFactorLoginArgs(savedStateHandle).emailAddress,
            password = TwoFactorLoginArgs(savedStateHandle).password,
        ),
) {

    private val recover2faUri: Uri
        get() {
            val baseUrl = environmentRepository
                .environment
                .environmentUrlData
                .baseWebVaultUrlOrDefault
            return "$baseUrl/#/recover-2fa".toUri()
        }

    init {
        // As state updates, write to saved state handle.
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)

        // Automatically attempt to login again if a captcha token is received.
        authRepository
            .captchaTokenResultFlow
            .map { TwoFactorLoginAction.Internal.ReceiveCaptchaToken(tokenResult = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        // Process the Duo result when it is received.
        authRepository
            .duoTokenResultFlow
            .map { TwoFactorLoginAction.Internal.ReceiveDuoResult(duoResult = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        // Fill in the verification code input field when a Yubi Key code is received.
        authRepository
            .yubiKeyResultFlow
            .map { TwoFactorLoginAction.Internal.ReceiveYubiKeyResult(yubiKeyResult = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: TwoFactorLoginAction) {
        when (action) {
            TwoFactorLoginAction.CloseButtonClick -> handleCloseButtonClicked()
            is TwoFactorLoginAction.CodeInputChanged -> handleCodeInputChanged(action)
            TwoFactorLoginAction.ContinueButtonClick -> handleContinueButtonClick()
            TwoFactorLoginAction.DialogDismiss -> handleDialogDismiss()
            is TwoFactorLoginAction.RememberMeToggle -> handleRememberMeToggle(action)
            TwoFactorLoginAction.ResendEmailClick -> handleResendEmailClick()
            is TwoFactorLoginAction.SelectAuthMethod -> handleSelectAuthMethod(action)
            is TwoFactorLoginAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleInternalAction(action: TwoFactorLoginAction.Internal) {
        when (action) {
            is TwoFactorLoginAction.Internal.ReceiveLoginResult -> handleReceiveLoginResult(action)
            is TwoFactorLoginAction.Internal.ReceiveCaptchaToken -> {
                handleCaptchaTokenReceived(action)
            }

            is TwoFactorLoginAction.Internal.ReceiveDuoResult -> {
                handleReceiveDuoResult(action)
            }

            is TwoFactorLoginAction.Internal.ReceiveYubiKeyResult -> {
                handleReceiveYubiKeyResult(action)
            }

            is TwoFactorLoginAction.Internal.ReceiveResendEmailResult -> {
                handleReceiveResendEmailResult(action)
            }
        }
    }

    private fun handleCaptchaTokenReceived(
        action: TwoFactorLoginAction.Internal.ReceiveCaptchaToken,
    ) {
        when (val tokenResult = action.tokenResult) {
            CaptchaCallbackTokenResult.MissingToken -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = R.string.log_in_denied.asText(),
                            message = R.string.captcha_failed.asText(),
                        ),
                    )
                }
            }

            is CaptchaCallbackTokenResult.Success -> {
                mutableStateFlow.update {
                    it.copy(captchaToken = tokenResult.token)
                }
                initiateLogin()
            }
        }
    }

    /**
     * Update the state with the new text and enable or disable the continue button.
     */
    private fun handleCodeInputChanged(action: TwoFactorLoginAction.CodeInputChanged) {
        mutableStateFlow.update {
            it.copy(
                codeInput = action.input,
                isContinueButtonEnabled = action.input.length >= 6,
            )
        }
    }

    /**
     * Navigates to the Duo webpage if appropriate, else processes the login.
     */
    private fun handleContinueButtonClick() {
        if (state.authMethod.isDuo) {
            val authUrl = authRepository.twoFactorResponse.twoFactorDuoAuthUrl
            // The url should not be empty unless the environment is somehow not supported.
            sendEvent(
                event = authUrl
                    ?.let {
                        TwoFactorLoginEvent.NavigateToDuo(
                            uri = Uri.parse(it),
                        )
                    }
                    ?: TwoFactorLoginEvent.ShowToast(
                        message = R.string.generic_error_message.asText(),
                    ),
            )
        } else {
            initiateLogin()
        }
    }

    /**
     * Dismiss the view.
     */
    private fun handleCloseButtonClicked() {
        sendEvent(TwoFactorLoginEvent.NavigateBack)
    }

    /**
     * Dismiss the dialog.
     */
    private fun handleDialogDismiss() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    /**
     * Handle the login result.
     */
    private fun handleReceiveLoginResult(action: TwoFactorLoginAction.Internal.ReceiveLoginResult) {
        // Dismiss the loading overlay.
        mutableStateFlow.update { it.copy(dialogState = null) }

        when (val loginResult = action.loginResult) {
            // Launch the captcha flow if necessary.
            is LoginResult.CaptchaRequired -> {
                sendEvent(
                    event = TwoFactorLoginEvent.NavigateToCaptcha(
                        uri = generateUriForCaptcha(captchaId = loginResult.captchaId),
                    ),
                )
            }

            // NO-OP: This error shouldn't be possible at this stage.
            is LoginResult.TwoFactorRequired -> Unit

            // Display any error with the same invalid verification code message.
            is LoginResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.invalid_verification_code.asText(),
                        ),
                    )
                }
            }

            // NO-OP: Let the auth flow handle navigation after this.
            is LoginResult.Success -> Unit
        }
    }

    /**
     * Handles the Duo callback result.
     */
    private fun handleReceiveDuoResult(
        action: TwoFactorLoginAction.Internal.ReceiveDuoResult,
    ) {
        when (val result = action.duoResult) {
            is DuoCallbackTokenResult.MissingToken -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DuoCallbackTokenResult.Success -> {
                mutableStateFlow.update {
                    it.copy(
                        codeInput = result.token,
                    )
                }
                initiateLogin()
            }
        }
    }

    /**
     * Handle the Yubi Key result.
     */
    private fun handleReceiveYubiKeyResult(
        action: TwoFactorLoginAction.Internal.ReceiveYubiKeyResult,
    ) {
        mutableStateFlow.update {
            it.copy(
                codeInput = action.yubiKeyResult.token,
                isContinueButtonEnabled = true,
            )
        }
    }

    /**
     * Handle the resend email result.
     */
    private fun handleReceiveResendEmailResult(
        action: TwoFactorLoginAction.Internal.ReceiveResendEmailResult,
    ) {
        // Dismiss the loading overlay.
        mutableStateFlow.update { it.copy(dialogState = null) }

        when (action.resendEmailResult) {
            // Display a dialog for an error result.
            is ResendEmailResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.verification_email_not_sent.asText(),
                        ),
                    )
                }
            }

            // Display a toast for a successful result.
            ResendEmailResult.Success -> {
                if (action.isUserInitiated) {
                    sendEvent(
                        TwoFactorLoginEvent.ShowToast(
                            message = R.string.verification_email_sent.asText(),
                        ),
                    )
                }
            }
        }
    }

    /**
     * Update the state with the new toggle value.
     */
    private fun handleRememberMeToggle(action: TwoFactorLoginAction.RememberMeToggle) {
        mutableStateFlow.update {
            it.copy(
                isRememberMeEnabled = action.isChecked,
            )
        }
    }

    /**
     * Resend the verification code email.
     */
    private fun handleResendEmailClick() {
        // Ensure that the user is in fact verifying with email.
        if (state.authMethod != TwoFactorAuthMethod.EMAIL) {
            return
        }

        // Show the loading overlay.
        mutableStateFlow.update {
            it.copy(
                dialogState = TwoFactorLoginState.DialogState.Loading(
                    message = R.string.submitting.asText(),
                ),
            )
        }

        // Resend the email notification.
        viewModelScope.launch {
            val result = authRepository.resendVerificationCodeEmail()
            sendAction(
                TwoFactorLoginAction.Internal.ReceiveResendEmailResult(
                    resendEmailResult = result,
                    isUserInitiated = true,
                ),
            )
        }
    }

    /**
     * Update the state with the auth method or opens the url for the recovery code.
     */
    private fun handleSelectAuthMethod(action: TwoFactorLoginAction.SelectAuthMethod) {
        when (action.authMethod) {
            TwoFactorAuthMethod.RECOVERY_CODE -> {
                sendEvent(TwoFactorLoginEvent.NavigateToRecoveryCode(recover2faUri))
            }

            TwoFactorAuthMethod.EMAIL -> {
                if (state.authMethod != TwoFactorAuthMethod.EMAIL) {
                    viewModelScope.launch {
                        val result = authRepository.resendVerificationCodeEmail()
                        sendAction(
                            TwoFactorLoginAction.Internal.ReceiveResendEmailResult(
                                resendEmailResult = result,
                                isUserInitiated = false,
                            ),
                        )
                    }
                }
                mutableStateFlow.update { it.copy(authMethod = action.authMethod) }
            }

            TwoFactorAuthMethod.AUTHENTICATOR_APP,
            TwoFactorAuthMethod.DUO,
            TwoFactorAuthMethod.YUBI_KEY,
            TwoFactorAuthMethod.U2F,
            TwoFactorAuthMethod.REMEMBER,
            TwoFactorAuthMethod.DUO_ORGANIZATION,
            TwoFactorAuthMethod.WEB_AUTH,
            -> {
                mutableStateFlow.update { it.copy(authMethod = action.authMethod) }
            }
        }
    }

    /**
     * Verify the input and attempt to authenticate with the code.
     */
    private fun initiateLogin() {
        mutableStateFlow.update {
            it.copy(
                dialogState = TwoFactorLoginState.DialogState.Loading(
                    message = R.string.logging_in.asText(),
                ),
            )
        }

        // If the user is manually entering a code, remove any white spaces, just in case.
        val code = when (state.authMethod) {
            TwoFactorAuthMethod.AUTHENTICATOR_APP,
            TwoFactorAuthMethod.EMAIL,
            -> state.codeInput.replace(" ", "")

            TwoFactorAuthMethod.DUO,
            TwoFactorAuthMethod.DUO_ORGANIZATION,
            TwoFactorAuthMethod.YUBI_KEY,
            TwoFactorAuthMethod.U2F,
            TwoFactorAuthMethod.REMEMBER,
            TwoFactorAuthMethod.WEB_AUTH,
            TwoFactorAuthMethod.RECOVERY_CODE,
            -> state.codeInput
        }

        viewModelScope.launch {
            val result = authRepository.login(
                email = state.email,
                password = state.password,
                twoFactorData = TwoFactorDataModel(
                    code = code,
                    method = state.authMethod.value.toString(),
                    remember = state.isRememberMeEnabled,
                ),
                captchaToken = state.captchaToken,
            )
            sendAction(
                TwoFactorLoginAction.Internal.ReceiveLoginResult(
                    loginResult = result,
                ),
            )
        }
    }
}

/**
 * Models state of the Two-Factor Login screen.
 */
@Parcelize
data class TwoFactorLoginState(
    val authMethod: TwoFactorAuthMethod,
    val availableAuthMethods: List<TwoFactorAuthMethod>,
    val codeInput: String,
    val dialogState: DialogState?,
    val displayEmail: String,
    val isContinueButtonEnabled: Boolean,
    val isRememberMeEnabled: Boolean,
    // Internal
    val captchaToken: String?,
    val email: String,
    val password: String?,
) : Parcelable {

    /**
     * The text to display for the button given the [authMethod].
     */
    val buttonText: Text get() = authMethod.button

    /**
     * Indicates if the screen should be listening for NFC events from the operating system.
     */
    val shouldListenForNfc: Boolean get() = authMethod.shouldUseNfc

    /**
     * Indicates whether the code input should be displayed.
     */
    val shouldShowCodeInput: Boolean get() = !authMethod.isDuo

    /**
     * The image to display for the given the [authMethod].
     */
    @get:DrawableRes
    val imageRes: Int? get() = authMethod.imageRes

    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {
        /**
         * Represents an error dialog with the given [message] and optional [title]. It no title
         * is specified a default will be provided.
         */
        @Parcelize
        data class Error(
            val title: Text? = null,
            val message: Text,
        ) : DialogState()

        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events for the Two-Factor Login screen.
 */
sealed class TwoFactorLoginEvent {
    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : TwoFactorLoginEvent()

    /**
     * Navigates to the captcha verification screen.
     */
    data class NavigateToCaptcha(val uri: Uri) : TwoFactorLoginEvent()

    /**
     * Navigates to the Duo 2-factor authentication screen.
     */
    data class NavigateToDuo(val uri: Uri) : TwoFactorLoginEvent()

    /**
     * Navigates to the recovery code help page.
     *
     * @param uri The recovery uri.
     */
    data class NavigateToRecoveryCode(val uri: Uri) : TwoFactorLoginEvent()

    /**
     * Shows a toast with the given [message].
     */
    data class ShowToast(
        val message: Text,
    ) : TwoFactorLoginEvent()
}

/**
 * Models actions for the Two-Factor Login screen.
 */
sealed class TwoFactorLoginAction {
    /**
     * Indicates that the top-bar close button was clicked.
     */
    data object CloseButtonClick : TwoFactorLoginAction()

    /**
     * Indicates that the input on the verification code field changed.
     */
    data class CodeInputChanged(
        val input: String,
    ) : TwoFactorLoginAction()

    /**
     * Indicates that the Continue button was clicked.
     */
    data object ContinueButtonClick : TwoFactorLoginAction()

    /**
     * Indicates that the dialog has been dismissed.
     */
    data object DialogDismiss : TwoFactorLoginAction()

    /**
     * Indicates that the Remember Me switch  toggled.
     */
    data class RememberMeToggle(
        val isChecked: Boolean,
    ) : TwoFactorLoginAction()

    /**
     * Indicates that the Resend Email button was clicked.
     */
    data object ResendEmailClick : TwoFactorLoginAction()

    /**
     * Indicates an auth method was selected from the menu dropdown.
     */
    data class SelectAuthMethod(
        val authMethod: TwoFactorAuthMethod,
    ) : TwoFactorLoginAction()

    /**
     * Models actions that the [TwoFactorLoginViewModel] itself might send.
     */
    sealed class Internal : TwoFactorLoginAction() {
        /**
         * Indicates a captcha callback token has been received.
         */
        data class ReceiveCaptchaToken(
            val tokenResult: CaptchaCallbackTokenResult,
        ) : Internal()

        /**
         * Indicates that a Dup callback token has been received.
         */
        data class ReceiveDuoResult(
            val duoResult: DuoCallbackTokenResult,
        ) : Internal()

        /**
         * Indicates a Yubi Key result has been received.
         */
        data class ReceiveYubiKeyResult(
            val yubiKeyResult: YubiKeyResult,
        ) : Internal()

        /**
         * Indicates a login result has been received.
         */
        data class ReceiveLoginResult(
            val loginResult: LoginResult,
        ) : Internal()

        /**
         * Indicates a resend email result has been received.
         */
        data class ReceiveResendEmailResult(
            val resendEmailResult: ResendEmailResult,
            val isUserInitiated: Boolean,
        ) : Internal()
    }
}
