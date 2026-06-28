package com.lksnext.ParkingIMayordomo.ui.viewmodel

import app.cash.turbine.test
import com.lksnext.ParkingIMayordomo.MainDispatcherRule
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ParkingRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        viewModel = RegisterViewModel(repository)
    }

    @Test
    fun `register success should call onSuccess and update state`() = runTest {
        val onSuccess = mockk<() -> Unit>(relaxed = true)
        
        viewModel.register("Test", "test@lksnext.com", "pass123", onSuccess)

        coVerify { repository.register("Test", "test@lksnext.com", "pass123") }
        verify { onSuccess() }
        assertFalse(viewModel.loading.value)
        assertNull(viewModel.errorResId.value)
    }

    @Test
    fun `register failure should set error and stop loading`() = runTest {
        coEvery { repository.register(any(), any(), any()) } throws Exception("error_email_already_in_use")

        viewModel.register("Name", "email", "pass", {})

        assertEquals(R.string.error_email_already_in_use, viewModel.errorResId.value)
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `register with various errors should map correctly`() = runTest {
        val errors = listOf(
            "error_corporate_only" to R.string.error_corporate_only,
            "error_passwords_dont_match" to R.string.error_passwords_dont_match,
            "error_password_too_short" to R.string.error_password_too_short,
            "error_password_complexity" to R.string.error_password_complexity,
            "error_invalid_email_format" to R.string.error_invalid_email_format,
            "unknown_msg" to R.string.error_unknown
        )

        for ((msg, expectedRes) in errors) {
            coEvery { repository.register(any(), any(), any()) } throws Exception(msg)
            viewModel.register("n", "e", "p", {})
            assertEquals("Failed for $msg", expectedRes, viewModel.errorResId.value)
        }
    }

    @Test
    fun `loading state should transition correctly`() = runTest {
        coEvery { repository.register(any(), any(), any()) } coAnswers { delay(50) }

        viewModel.loading.test {
            assertEquals(false, awaitItem())
            viewModel.register("n", "e", "p", {})
            assertEquals(true, awaitItem())
            assertEquals(false, awaitItem())
        }
    }
}
