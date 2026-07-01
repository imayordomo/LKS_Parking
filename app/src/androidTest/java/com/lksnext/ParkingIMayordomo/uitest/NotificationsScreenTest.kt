package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.lksnext.ParkingIMayordomo.data.model.Notification
import com.lksnext.ParkingIMayordomo.data.model.NotificationType
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.ui.pages.Notifications
import com.lksnext.ParkingIMayordomo.ui.viewmodel.NotificationsViewModel
import com.lksnext.ParkingIMayordomo.utils.TestTags
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import java.util.Date

class NotificationsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createRepository(
        notifications: List<Notification> = emptyList()
    ): ParkingRepository {
        val repo = mockk<ParkingRepository>(relaxed = true)
        every { repo.notifications } returns MutableStateFlow(notifications)
        return repo
    }

    @Test
    fun emptyNotifications_showsNoItems() {
        val vm = NotificationsViewModel(createRepository())
        composeTestRule.setContent {
            Notifications(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_MARK_ALL_READ).assertIsNotDisplayed()
    }

    @Test
    fun withUnreadNotifications_showsMarkAllRead() {
        val notifications = listOf(
            Notification(id = "n1", userId = "u1", type = NotificationType.INFO, title = "title1", message = "msg1", time = Date())
        )
        val vm = NotificationsViewModel(createRepository(notifications))
        composeTestRule.setContent {
            Notifications(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_MARK_ALL_READ).assertIsDisplayed()
    }

    @Test
    fun markAllRead_click() {
        val notifications = listOf(
            Notification(id = "n1", userId = "u1", type = NotificationType.INFO, title = "title1", message = "msg1", time = Date())
        )
        val vm = NotificationsViewModel(createRepository(notifications))
        composeTestRule.setContent {
            Notifications(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_MARK_ALL_READ).performClick()
    }

    @Test
    fun notificationItem_showsMarkReadAndDelete() {
        val notifications = listOf(
            Notification(id = "n1", userId = "u1", type = NotificationType.INFO, title = "title1", message = "msg1", time = Date())
        )
        val vm = NotificationsViewModel(createRepository(notifications))
        composeTestRule.setContent {
            Notifications(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_ITEM_MARK_READ).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_ITEM_DELETE).assertIsDisplayed()
    }

    @Test
    fun markRead_click() {
        val notifications = listOf(
            Notification(id = "n1", userId = "u1", type = NotificationType.INFO, title = "title1", message = "msg1", time = Date())
        )
        val vm = NotificationsViewModel(createRepository(notifications))
        composeTestRule.setContent {
            Notifications(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_ITEM_MARK_READ).performClick()
    }

    @Test
    fun deleteNotification_click() {
        val notifications = listOf(
            Notification(id = "n1", userId = "u1", type = NotificationType.INFO, title = "title1", message = "msg1", time = Date())
        )
        val vm = NotificationsViewModel(createRepository(notifications))
        composeTestRule.setContent {
            Notifications(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_ITEM_DELETE).performClick()
    }

    @Test
    fun readNotification_doesNotShowMarkRead() {
        val notifications = listOf(
            Notification(id = "n1", userId = "u1", type = NotificationType.INFO, title = "title1", message = "msg1", time = Date(), read = true)
        )
        val vm = NotificationsViewModel(createRepository(notifications))
        composeTestRule.setContent {
            Notifications(viewModel = vm, onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_ITEM_MARK_READ).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.NOTIFICATIONS_ITEM_DELETE).assertIsDisplayed()
    }
}
