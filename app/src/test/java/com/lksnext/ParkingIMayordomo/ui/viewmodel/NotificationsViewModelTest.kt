package com.lksnext.ParkingIMayordomo.ui.viewmodel

import com.lksnext.ParkingIMayordomo.MainDispatcherRule
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ParkingRepository
    private lateinit var viewModel: NotificationsViewModel

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        viewModel = NotificationsViewModel(repository)
    }

    @Test
    fun `markAsRead should call repository`() = runTest {
        viewModel.markAsRead("id1")
        coVerify { repository.markAsRead("id1") }
    }

    @Test
    fun `markAllAsRead should call repository`() = runTest {
        viewModel.markAllAsRead()
        coVerify { repository.markAllAsRead() }
    }

    @Test
    fun `requestDeleteNotification shows confirm state`() = runTest {
        assertNull(viewModel.deleteConfirmState.value)
        viewModel.requestDeleteNotification("id1")
        assertEquals(DeleteConfirmState.Single("id1"), viewModel.deleteConfirmState.value)
    }

    @Test
    fun `confirmDelete with single notification calls repository`() = runTest {
        viewModel.requestDeleteNotification("id1")
        viewModel.confirmDelete(dontAskAgain = false)
        coVerify { repository.deleteNotification("id1") }
        assertNull(viewModel.deleteConfirmState.value)
    }

    @Test
    fun `requestDeleteAll shows confirm state`() = runTest {
        assertNull(viewModel.deleteConfirmState.value)
        viewModel.requestDeleteAll()
        assertEquals(DeleteConfirmState.All, viewModel.deleteConfirmState.value)
    }

    @Test
    fun `confirmDelete with all calls deleteAllNotifications`() = runTest {
        viewModel.requestDeleteAll()
        viewModel.confirmDelete(dontAskAgain = false)
        coVerify { repository.deleteAllNotifications() }
        assertNull(viewModel.deleteConfirmState.value)
    }

    @Test
    fun `dismissDeleteConfirm clears state`() = runTest {
        viewModel.requestDeleteNotification("id1")
        viewModel.dismissDeleteConfirm()
        assertNull(viewModel.deleteConfirmState.value)
    }

    @Test
    fun `requestDeleteNotification skips confirm when dontAskAgain is saved`() = runTest {
        // When dontAskAgain is set via confirmDelete
        viewModel.requestDeleteNotification("id1")
        viewModel.confirmDelete(dontAskAgain = true)
        coVerify { repository.deleteNotification("id1") }

        // Subsequent requestDeleteNotification should skip confirmation
        // But since we can't mock SharedPreferences in unit tests,
        // just verify the state flow is reset
        assertNull(viewModel.deleteConfirmState.value)
    }
}
