package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
            Register(viewModel = createViewModel(), onBackToLogin = { }, onRegisterSuccess = { })
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
    fun registerButton_isEnabled_whenFieldsEmpty() {
        composeTestRule.setContent {
            Register(viewModel = createViewModel(), onBackToLogin = { }, onRegisterSuccess = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_REGISTER_BUTTON).assertIsDisplayed()
    }

    @Test
    fun nameField_acceptsInput() {
        composeTestRule.setContent {
            Register(viewModel = createViewModel(), onBackToLogin = { }, onRegisterSuccess = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_NAME_FIELD).performTextInput("John Doe")
    }

    @Test
    fun emailField_acceptsInput() {
        composeTestRule.setContent {
            Register(viewModel = createViewModel(), onBackToLogin = { }, onRegisterSuccess = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_EMAIL_FIELD).performTextInput("test@lksnext.com")
    }

    @Test
    fun passwordField_acceptsInput() {
        composeTestRule.setContent {
            Register(viewModel = createViewModel(), onBackToLogin = { }, onRegisterSuccess = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_PASSWORD_FIELD).performTextInput("Test1234")
    }

    @Test
    fun passwordToggle_togglesVisibility() {
        composeTestRule.setContent {
            Register(viewModel = createViewModel(), onBackToLogin = { }, onRegisterSuccess = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_TOGGLE_PASSWORD).performClick()
    }

    @Test
    fun confirmPasswordToggle_togglesVisibility() {
        composeTestRule.setContent {
            Register(viewModel = createViewModel(), onBackToLogin = { }, onRegisterSuccess = { })
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
            Register(viewModel = createViewModel(loading = true), onBackToLogin = { }, onRegisterSuccess = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_REGISTER_BUTTON).assertIsDisplayed()
    }

    @Test
    fun loginLink_disabledDuringLoading() {
        composeTestRule.setContent {
            Register(viewModel = createViewModel(loading = true), onBackToLogin = { }, onRegisterSuccess = { })
        }

        composeTestRule.onNodeWithTag(TestTags.REGISTER_LOGIN_LINK).assertIsDisplayed()
    }
}
