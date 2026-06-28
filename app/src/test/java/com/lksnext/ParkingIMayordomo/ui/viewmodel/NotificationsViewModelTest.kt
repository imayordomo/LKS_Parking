package com.lksnext.ParkingIMayordomo.ui.viewmodel

import com.lksnext.ParkingIMayordomo.MainDispatcherRule
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
    fun `deleteNotification should call repository`() = runTest {
        viewModel.deleteNotification("id1")
        coVerify { repository.deleteNotification("id1") }
    }
}
