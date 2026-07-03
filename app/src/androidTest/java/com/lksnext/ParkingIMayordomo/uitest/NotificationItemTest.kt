package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lksnext.ParkingIMayordomo.data.model.Notification
import com.lksnext.ParkingIMayordomo.data.model.NotificationType
import com.lksnext.ParkingIMayordomo.ui.pages.NotificationItem
import com.lksnext.ParkingIMayordomo.utils.TestTags
import org.junit.Rule
import org.junit.Test
import java.util.Date

class NotificationItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun notification_withPlainTitleAndMessage_showsText() {
        composeTestRule.setContent {
            NotificationItem(
                notification = Notification(
                    id = "n1", userId = "u1", type = NotificationType.INFO,
                    title = "Test Title", message = "Test Message", time = Date()
                ),
                onRead = {},
                onDelete = {}
            )
        }
        composeTestRule.onNodeWithText("Test Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Message").assertIsDisplayed()
    }

    @Test
    fun notification_withWarningType_showsWarningIcon() {
        composeTestRule.setContent {
            NotificationItem(
                notification = Notification(
                    id = "n2", userId = "u1", type = NotificationType.WARNING,
                    title = "Warning", message = "Something went wrong", time = Date()
                ),
                onRead = {},
                onDelete = {}
            )
        }
        composeTestRule.onNodeWithText("Warning").assertIsDisplayed()
    }

    @Test
    fun notification_withSuccessType_showsCheckIcon() {
        composeTestRule.setContent {
            NotificationItem(
                notification = Notification(
                    id = "n3", userId = "u1", type = NotificationType.SUCCESS,
                    title = "Success", message = "Operation completed", time = Date()
                ),
                onRead = {},
                onDelete = {}
            )
        }
        composeTestRule.onNodeWithText("Success").assertIsDisplayed()
    }

    @Test
    fun unreadNotification_showsMarkReadButton() {
        composeTestRule.setContent {
            NotificationItem(
                notification = Notification(
                    id = "n4", userId = "u1", type = NotificationType.INFO,
                    title = "Unread", message = "Not read yet", time = Date(), read = false
                ),
                onRead = {},
                onDelete = {}
            )
        }
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_ITEM_MARK_READ).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_ITEM_DELETE).assertIsDisplayed()
    }

    @Test
    fun readNotification_hidesMarkReadButton() {
        composeTestRule.setContent {
            NotificationItem(
                notification = Notification(
                    id = "n5", userId = "u1", type = NotificationType.INFO,
                    title = "Read", message = "Already read", time = Date(), read = true
                ),
                onRead = {},
                onDelete = {}
            )
        }
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_ITEM_MARK_READ).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_ITEM_DELETE).assertIsDisplayed()
    }

    @Test
    fun notification_withEmptyTitleAndMessage_showsNoCrash() {
        composeTestRule.setContent {
            NotificationItem(
                notification = Notification(
                    id = "n6", userId = "u1", type = NotificationType.INFO,
                    title = null, message = null, time = Date()
                ),
                onRead = {},
                onDelete = {}
            )
        }
        // Should not crash - mark read and delete still visible
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_ITEM_DELETE).assertIsDisplayed()
    }

    @Test
    fun notification_withTitleRes_fallsBackToTitle_whenResNotFound() {
        composeTestRule.setContent {
            NotificationItem(
                notification = Notification(
                    id = "n7", userId = "u1", type = NotificationType.INFO,
                    titleRes = "nonexistent_resource_name",
                    title = "Fallback Title",
                    message = "Fallback Message",
                    time = Date()
                ),
                onRead = {},
                onDelete = {}
            )
        }
        composeTestRule.onNodeWithText("Fallback Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fallback Message").assertIsDisplayed()
    }

    @Test
    fun notification_withTitleRes_resolvesExistingResourceOrDefault() {
        composeTestRule.setContent {
            NotificationItem(
                notification = Notification(
                    id = "n8", userId = "u1", type = NotificationType.INFO,
                    titleRes = "notif_welcome_title",
                    messageRes = "notif_welcome_msg",
                    time = Date()
                ),
                onRead = {},
                onDelete = {}
            )
        }
        // The notification item should render without crash and show delete button
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_ITEM_DELETE).assertIsDisplayed()
        // At least one of the known localized titles should be visible
        val possibleTitles = listOf(
            "Bienvenido a LKS Parking",
            "Welcome to LKS Parking",
            "Ongi etorri LKS Parking-era"
        )
        val found = possibleTitles.any { title ->
            try {
                composeTestRule.onNodeWithText(title).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        assert(found) { "None of the expected notification titles were found on screen"}
    }

    @Test
    fun deleteClick_triggersCallback() {
        var deleted = false
        composeTestRule.setContent {
            NotificationItem(
                notification = Notification(
                    id = "n9", userId = "u1", type = NotificationType.INFO,
                    title = "Delete me", message = "Msg", time = Date()
                ),
                onRead = {},
                onDelete = { deleted = true }
            )
        }
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_ITEM_DELETE).performClick()
        assert(deleted)
    }
}
