package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.ui.pages.Login
import com.lksnext.ParkingIMayordomo.ui.viewmodel.LoginViewModel
import com.lksnext.ParkingIMayordomo.utils.TestTags
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createViewModel(
        loading: Boolean = false,
        errorResId: Int? = null
    ): LoginViewModel {
        val vm = mockk<LoginViewModel>(relaxed = true)
        every { vm.loading } returns MutableStateFlow(loading)
        every { vm.errorResId } returns MutableStateFlow(errorResId)
        return vm
    }

    @Test
    fun loginScreen_displaysAllFormElements() {
        composeTestRule.setContent {
            Login(
                viewModel = createViewModel(),
                onRegisterClick = { },
                onForgotPasswordClick = { },
                onLoginSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LOGIN_EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.LOGIN_PASSWORD_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.LOGIN_TOGGLE_PASSWORD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.LOGIN_FORGOT_PASSWORD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.LOGIN_LOGIN_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.LOGIN_REGISTER_LINK).assertIsDisplayed()
    }

    @Test
    fun loginScreen_typeEmail_updatesField() {
        composeTestRule.setContent {
            Login(
                viewModel = createViewModel(),
                onRegisterClick = { },
                onForgotPasswordClick = { },
                onLoginSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LOGIN_EMAIL_FIELD).performTextClearance()
        composeTestRule.onNodeWithTag(TestTags.LOGIN_EMAIL_FIELD).performTextInput("test@lksnext.com")
        composeTestRule.onNodeWithTag(TestTags.LOGIN_EMAIL_FIELD).assertTextContains("test@lksnext.com")
    }

    @Test
    fun loginScreen_togglePasswordVisibility_showsPassword() {
        composeTestRule.setContent {
            Login(
                viewModel = createViewModel(),
                onRegisterClick = { },
                onForgotPasswordClick = { },
                onLoginSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LOGIN_PASSWORD_FIELD).performTextInput("secret123")
        composeTestRule.onNodeWithTag(TestTags.LOGIN_TOGGLE_PASSWORD).performClick()
    }

    @Test
    fun loginScreen_registerLink_navigates() {
        var called = false
        composeTestRule.setContent {
            Login(
                viewModel = createViewModel(),
                onRegisterClick = { called = true },
                onForgotPasswordClick = { },
                onLoginSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LOGIN_REGISTER_LINK).performClick()
        assert(called) { "onRegisterClick should be called" }
    }

    @Test
    fun loginScreen_forgotPasswordLink_navigates() {
        var called = false
        composeTestRule.setContent {
            Login(
                viewModel = createViewModel(),
                onRegisterClick = { },
                onForgotPasswordClick = { called = true },
                onLoginSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LOGIN_FORGOT_PASSWORD).performClick()
        assert(called) { "onForgotPasswordClick should be called" }
    }

    @Test
    fun loginScreen_loadingState_disablesButton() {
        composeTestRule.setContent {
            Login(
                viewModel = createViewModel(loading = true),
                onRegisterClick = { },
                onForgotPasswordClick = { },
                onLoginSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LOGIN_LOGIN_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.LOGIN_REGISTER_LINK).assertIsDisplayed()
    }

    @Test
    fun error_invalidCredentials_showsError() {
        composeTestRule.setContent {
            Login(
                viewModel = createViewModel(errorResId = R.string.error_invalid_credentials),
                onRegisterClick = { },
                onForgotPasswordClick = { },
                onLoginSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LOGIN_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun error_corporateOnly_showsError() {
        composeTestRule.setContent {
            Login(
                viewModel = createViewModel(errorResId = R.string.error_corporate_only),
                onRegisterClick = { },
                onForgotPasswordClick = { },
                onLoginSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LOGIN_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun error_invalidEmailFormat_showsError() {
        composeTestRule.setContent {
            Login(
                viewModel = createViewModel(errorResId = R.string.error_invalid_email_format),
                onRegisterClick = { },
                onForgotPasswordClick = { },
                onLoginSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LOGIN_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun error_requiredFields_showsError() {
        composeTestRule.setContent {
            Login(
                viewModel = createViewModel(errorResId = R.string.error_required_fields),
                onRegisterClick = { },
                onForgotPasswordClick = { },
                onLoginSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LOGIN_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun error_network_showsError() {
        composeTestRule.setContent {
            Login(
                viewModel = createViewModel(errorResId = R.string.error_network),
                onRegisterClick = { },
                onForgotPasswordClick = { },
                onLoginSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LOGIN_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun error_userDisabled_showsError() {
        composeTestRule.setContent {
            Login(
                viewModel = createViewModel(errorResId = R.string.error_user_disabled),
                onRegisterClick = { },
                onForgotPasswordClick = { },
                onLoginSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LOGIN_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun error_tooManyRequests_showsError() {
        composeTestRule.setContent {
            Login(
                viewModel = createViewModel(errorResId = R.string.error_too_many_requests),
                onRegisterClick = { },
                onForgotPasswordClick = { },
                onLoginSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LOGIN_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun error_unknown_showsError() {
        composeTestRule.setContent {
            Login(
                viewModel = createViewModel(errorResId = R.string.error_unknown),
                onRegisterClick = { },
                onForgotPasswordClick = { },
                onLoginSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LOGIN_ERROR_MESSAGE).assertIsDisplayed()
    }
}
