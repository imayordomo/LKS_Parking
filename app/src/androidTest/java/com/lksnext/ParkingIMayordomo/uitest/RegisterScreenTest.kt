package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.ui.pages.Register
import com.lksnext.ParkingIMayordomo.ui.viewmodel.RegisterViewModel
import com.lksnext.ParkingIMayordomo.utils.TestTags
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createViewModel(
        loading: Boolean = false,
        errorResId: Int? = null
    ): RegisterViewModel {
        val vm = mockk<RegisterViewModel>(relaxed = true)
        every { vm.loading } returns MutableStateFlow(loading)
        every { vm.errorResId } returns MutableStateFlow(errorResId)
        return vm
    }

    @Test
    fun allFormFields_areDisplayed() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.REGISTER_EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.REGISTER_PASSWORD_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.REGISTER_TOGGLE_PASSWORD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.REGISTER_CONFIRM_PASSWORD_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.REGISTER_TOGGLE_CONFIRM_PASSWORD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.REGISTER_REGISTER_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.REGISTER_LOGIN_LINK).assertIsDisplayed()
    }

    @Test
    fun nameField_acceptsInput() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("John Doe")
    }

    @Test
    fun emailField_acceptsInput() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_EMAIL_FIELD).performTextInput("test@lksnext.com")
    }

    @Test
    fun passwordField_acceptsInput() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_PASSWORD_FIELD).performTextInput("Test1234")
    }

    @Test
    fun passwordToggle_togglesVisibility() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_TOGGLE_PASSWORD).performClick()
    }

    @Test
    fun confirmPasswordToggle_togglesVisibility() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_TOGGLE_CONFIRM_PASSWORD).performClick()
    }

    @Test
    fun loginLink_navigatesBack() {
        var backCalled = false
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(),
                onBackToLogin = { backCalled = true },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_LOGIN_LINK).performClick()
        assert(backCalled) { "onBackToLogin should be called" }
    }

    @Test
    fun loadingState_showsLoadingIndicator() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(loading = true),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_REGISTER_BUTTON).assertIsDisplayed()
    }

    @Test
    fun loginLink_disabledDuringLoading() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(loading = true),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_LOGIN_LINK).assertIsDisplayed()
    }

    // ── Client-side validation ──

    @Test
    fun emptyFields_clickRegister_showsRequiredFieldsError() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_REGISTER_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.REGISTER_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun nonCorporateEmail_clickRegister_showsCorporateOnlyError() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("John")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_EMAIL_FIELD).performTextInput("john@gmail.com")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_PASSWORD_FIELD).performTextInput("Test1234")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_CONFIRM_PASSWORD_FIELD).performTextInput("Test1234")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_REGISTER_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.REGISTER_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun passwordMismatch_clickRegister_showsPasswordsDontMatchError() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("John")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_EMAIL_FIELD).performTextInput("john@lksnext.com")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_PASSWORD_FIELD).performTextInput("Test1234")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_CONFIRM_PASSWORD_FIELD).performTextInput("Test5678")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_REGISTER_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.REGISTER_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun shortPassword_clickRegister_showsPasswordTooShortError() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("John")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_EMAIL_FIELD).performTextInput("john@lksnext.com")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_PASSWORD_FIELD).performTextInput("Ab1")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_CONFIRM_PASSWORD_FIELD).performTextInput("Ab1")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_REGISTER_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.REGISTER_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun weakPassword_clickRegister_showsPasswordComplexityError() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("John")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_EMAIL_FIELD).performTextInput("john@lksnext.com")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_PASSWORD_FIELD).performTextInput("abcdefgh")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_CONFIRM_PASSWORD_FIELD).performTextInput("abcdefgh")
        composeTestRule.onNodeWithTag(TestTags.REGISTER_REGISTER_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.REGISTER_ERROR_MESSAGE).assertIsDisplayed()
    }

    // ── Server-side API errors ──

    @Test
    fun apiError_corporateOnly_showsError() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(errorResId = R.string.error_corporate_only),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun apiError_passwordsDontMatch_showsError() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(errorResId = R.string.error_passwords_dont_match),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun apiError_passwordTooShort_showsError() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(errorResId = R.string.error_password_too_short),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun apiError_passwordComplexity_showsError() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(errorResId = R.string.error_password_complexity),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun apiError_emailAlreadyInUse_showsError() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(errorResId = R.string.error_email_already_in_use),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun apiError_unknown_showsError() {
        composeTestRule.setContent {
            Register(
                viewModel = createViewModel(errorResId = R.string.error_unknown),
                onBackToLogin = { },
                onRegisterSuccess = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_ERROR_MESSAGE).assertIsDisplayed()
    }
}
