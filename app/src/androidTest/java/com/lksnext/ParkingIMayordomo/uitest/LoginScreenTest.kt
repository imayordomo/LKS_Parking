package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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

    private fun createViewModel(): LoginViewModel {
        val vm = mockk<LoginViewModel>(relaxed = true)
        every { vm.loading } returns MutableStateFlow(false)
        every { vm.errorResId } returns MutableStateFlow<Int?>(null)
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
}
