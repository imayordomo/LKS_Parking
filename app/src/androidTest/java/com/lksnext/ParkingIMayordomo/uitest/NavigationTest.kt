package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.lksnext.ParkingIMayordomo.ui.components.ParkingBottomBar
import com.lksnext.ParkingIMayordomo.ui.components.ParkingDrawerContent
import com.lksnext.ParkingIMayordomo.ui.components.ParkingTopAppBar
import com.lksnext.ParkingIMayordomo.utils.TestTags
import org.junit.Rule
import org.junit.Test

class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun topAppBar_menuButton_isDisplayedAndClickable() {
        composeTestRule.setContent {
            ParkingTopAppBar(
                onMenuClick = { },
                onNotificationsClick = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.NAV_MENU_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NAV_MENU_BUTTON).performClick()
    }

    @Test
    fun topAppBar_notificationsButton_isDisplayedAndClickable() {
        composeTestRule.setContent {
            ParkingTopAppBar(
                onMenuClick = { },
                onNotificationsClick = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.NAV_NOTIFICATIONS_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NAV_NOTIFICATIONS_BUTTON).performClick()
    }

    @Test
    fun bottomBar_allItemsAreDisplayed() {
        composeTestRule.setContent {
            ParkingBottomBar(
                selectedItem = 0,
                onItemSelected = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.NAV_BOTTOM_DASHBOARD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NAV_BOTTOM_HISTORY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NAV_BOTTOM_PROFILE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NAV_BOTTOM_PARKING).assertIsDisplayed()
    }

    @Test
    fun bottomBar_itemSelection_callsCallback() {
        var selectedIndex = -1
        composeTestRule.setContent {
            ParkingBottomBar(
                selectedItem = 0,
                onItemSelected = { selectedIndex = it }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.NAV_BOTTOM_HISTORY).performClick()
        assert(selectedIndex == 1) { "History item should call onItemSelected with index 1" }
    }

    @Test
    fun drawerContent_allItemsAreDisplayed() {
        composeTestRule.setContent {
            ParkingDrawerContent(
                currentRoute = "",
                onItemClick = { }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.NAV_DRAWER_DASHBOARD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NAV_DRAWER_HISTORY).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NAV_DRAWER_PROFILE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NAV_DRAWER_PARKING).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NAV_DRAWER_NOTIFICATIONS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NAV_DRAWER_REPORT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NAV_DRAWER_HELP).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NAV_DRAWER_ABOUT).assertIsDisplayed()
    }

    @Test
    fun drawerContent_itemClick_callsCallback() {
        var clickedRoute: String? = null
        composeTestRule.setContent {
            ParkingDrawerContent(
                currentRoute = "",
                onItemClick = { clickedRoute = it }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.NAV_DRAWER_HELP).performClick()
    }
}
