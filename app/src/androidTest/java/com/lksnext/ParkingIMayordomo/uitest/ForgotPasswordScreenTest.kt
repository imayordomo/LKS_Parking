package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.lksnext.ParkingIMayordomo.ui.pages.ForgotPassword
import com.lksnext.ParkingIMayordomo.ui.viewmodel.ForgotPasswordViewModel
import com.lksnext.ParkingIMayordomo.utils.TestTags
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class ForgotPasswordScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createViewModel(
        email: String = "",
        isEmailSent: Boolean = false,
        isLoading: Boolean = false,
        errorResId: Int? = null
    ): ForgotPasswordViewModel {
        val vm = mockk<ForgotPasswordViewModel>(relaxed = true)
        every { vm.email } returns MutableStateFlow(email)
        every { vm.isEmailSent } returns MutableStateFlow(isEmailSent)
        every { vm.isLoading } returns MutableStateFlow(isLoading)
        every { vm.errorResId } returns MutableStateFlow(errorResId)
        return vm
    }

    @Test
    fun forgotPasswordForm_isDisplayed() {
        composeTestRule.setContent {
            ForgotPassword(viewModel = createViewModel(), onBackToLogin = { })
        }

        composeTestRule.onNodeWithTag(TestTags.FORGOT_PASSWORD_EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.FORGOT_PASSWORD_SEND_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.FORGOT_PASSWORD_BACK_BUTTON).assertIsDisplayed()
    }

    @Test
    fun emailField_acceptsInput() {
        composeTestRule.setContent {
            ForgotPassword(viewModel = createViewModel(), onBackToLogin = { })
        }

        composeTestRule.onNodeWithTag(TestTags.FORGOT_PASSWORD_EMAIL_FIELD).performTextInput("test@lksnext.com")
    }

    @Test
    fun sendButton_callsSendResetEmail() {
        val vm = createViewModel()
        composeTestRule.setContent {
            ForgotPassword(viewModel = vm, onBackToLogin = { })
        }

        composeTestRule.onNodeWithTag(TestTags.FORGOT_PASSWORD_SEND_BUTTON).performClick()
    }

    @Test
    fun backButton_navigatesBack() {
        var backCalled = false
        composeTestRule.setContent {
            ForgotPassword(
                viewModel = createViewModel(),
                onBackToLogin = { backCalled = true }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.FORGOT_PASSWORD_BACK_BUTTON).performClick()
        assert(backCalled) { "onBackToLogin should be called" }
    }

    @Test
    fun loadingState_disablesForm() {
        composeTestRule.setContent {
            ForgotPassword(viewModel = createViewModel(isLoading = true), onBackToLogin = { })
        }

        composeTestRule.onNodeWithTag(TestTags.FORGOT_PASSWORD_SEND_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.FORGOT_PASSWORD_BACK_BUTTON).assertIsDisplayed()
    }

    @Test
    fun emailSent_hidesFormShowsSuccess() {
        composeTestRule.setContent {
            ForgotPassword(viewModel = createViewModel(isEmailSent = true), onBackToLogin = { })
        }

        composeTestRule.onNodeWithTag(TestTags.FORGOT_PASSWORD_EMAIL_FIELD).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.FORGOT_PASSWORD_SEND_BUTTON).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.FORGOT_PASSWORD_BACK_BUTTON).assertIsDisplayed()
    }
}
