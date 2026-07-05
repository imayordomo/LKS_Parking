package com.lksnext.ParkingIMayordomo.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import com.lksnext.ParkingIMayordomo.MainDispatcherRule
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
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
    
    @MockK
    lateinit var context: Context
    @MockK
    lateinit var prefs: SharedPreferences
    @MockK
    lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repository = mockk(relaxed = true)
        viewModel = NotificationsViewModel(repository)
        
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } just Runs
        
        // Default: don't skip confirm
        every { prefs.getBoolean("dont_ask_delete_again", false) } returns false
        
        viewModel.init(context)
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
    fun `requestDeleteNotification shows confirm state when skip is false`() = runTest {
        viewModel.requestDeleteNotification("id1")
        assertEquals(DeleteConfirmState.Single("id1"), viewModel.deleteConfirmState.value)
    }

    @Test
    fun `requestDeleteNotification skips confirm when skip is true`() = runTest {
        every { prefs.getBoolean("dont_ask_delete_again", false) } returns true
        // Re-init to pickup change if it was cached, though current impl doesn't cache it
        viewModel.init(context)
        
        viewModel.requestDeleteNotification("id1")
        coVerify { repository.deleteNotification("id1") }
        assertNull(viewModel.deleteConfirmState.value)
    }

    @Test
    fun `confirmDelete with single notification calls repository and saves pref`() = runTest {
        viewModel.requestDeleteNotification("id1")
        viewModel.confirmDelete(dontAskAgain = true)
        
        coVerify { repository.deleteNotification("id1") }
        verify { editor.putBoolean("dont_ask_delete_again", true) }
        assertNull(viewModel.deleteConfirmState.value)
    }

    @Test
    fun `requestDeleteAll shows confirm state`() = runTest {
        viewModel.requestDeleteAll()
        assertEquals(DeleteConfirmState.All, viewModel.deleteConfirmState.value)
    }

    @Test
    fun `requestDeleteAll skips confirm when skip is true`() = runTest {
        every { prefs.getBoolean("dont_ask_delete_again", false) } returns true
        viewModel.init(context)
        
        viewModel.requestDeleteAll()
        coVerify { repository.deleteAllNotifications() }
        assertNull(viewModel.deleteConfirmState.value)
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
}
