package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
}
