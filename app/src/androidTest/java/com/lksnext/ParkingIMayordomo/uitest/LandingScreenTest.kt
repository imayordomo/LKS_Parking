package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.lksnext.ParkingIMayordomo.ui.pages.Landing
import com.lksnext.ParkingIMayordomo.utils.TestTags
import org.junit.Rule
import org.junit.Test

class LandingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun landingScreen_displaysLoginAndRegisterButtons() {
        composeTestRule.setContent {
            Landing(
                onLoginClick = { },
                onRegisterClick = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LANDING_LOGIN_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.LANDING_REGISTER_BUTTON).assertIsDisplayed()
    }

    @Test
    fun loginButton_navigatesToLogin() {
        var called = false
        composeTestRule.setContent {
            Landing(
                onLoginClick = { called = true },
                onRegisterClick = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LANDING_LOGIN_BUTTON).performClick()
        assert(called) { "onLoginClick should be called" }
    }

    @Test
    fun registerButton_navigatesToRegister() {
        var called = false
        composeTestRule.setContent {
            Landing(
                onLoginClick = { },
                onRegisterClick = { called = true }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.LANDING_REGISTER_BUTTON).performClick()
        assert(called) { "onRegisterClick should be called" }
    }
}
