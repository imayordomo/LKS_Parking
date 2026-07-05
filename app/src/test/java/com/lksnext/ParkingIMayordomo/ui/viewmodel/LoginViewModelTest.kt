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
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ParkingRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        viewModel = LoginViewModel(repository)
    }

    @Test
    fun `login with empty email should set error`() = runTest {
        viewModel.login("", "password", onSuccess = {})
        assertEquals(R.string.error_required_fields, viewModel.errorResId.value)
    }

    @Test
    fun `login with blank email should set error`() = runTest {
        viewModel.login("   ", "password", onSuccess = {})
        assertEquals(R.string.error_required_fields, viewModel.errorResId.value)
    }

    @Test
    fun `login with empty password should set error`() = runTest {
        viewModel.login("test@lksnext.com", "", onSuccess = {})
        assertEquals(R.string.error_required_fields, viewModel.errorResId.value)
    }

    @Test
    fun `login success should call onSuccess and clear errors`() = runTest {
        val email = "test@example.com"
        val password = "password"
        val onSuccess = mockk<() -> Unit>(relaxed = true)

        viewModel.login(email, password, onSuccess)

        coVerify { repository.login(email, password) }
        verify { onSuccess() }
        assertNull(viewModel.errorResId.value)
    }

    @Test
    fun `login failure with corporate only error should set correct resource`() = runTest {
        coEvery { repository.login(any(), any()) } throws Exception("error_corporate_only")
        viewModel.login("a@b.com", "p", {})
        assertEquals(R.string.error_corporate_only, viewModel.errorResId.value)
    }

    @Test
    fun `login failure with invalid email format should set correct resource`() = runTest {
        coEvery { repository.login(any(), any()) } throws Exception("error_invalid_email_format")
        viewModel.login("a@b.com", "p", {})
        assertEquals(R.string.error_invalid_email_format, viewModel.errorResId.value)
    }

    @Test
    fun `login failure with invalid credentials should set correct resource`() = runTest {
        coEvery { repository.login(any(), any()) } throws Exception("error_invalid_credentials")
        viewModel.login("a@b.com", "p", {})
        assertEquals(R.string.error_invalid_credentials, viewModel.errorResId.value)
    }

    @Test
    fun `login failure with unknown error should set default error resource`() = runTest {
        coEvery { repository.login(any(), any()) } throws Exception("some_random_error")
        viewModel.login("a@b.com", "p", {})
        assertEquals(R.string.error_unknown, viewModel.errorResId.value)
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        viewModel.login("", "", onSuccess = {})
        assertNotNull(viewModel.errorResId.value)

        viewModel.clearError()
        assertNull(viewModel.errorResId.value)
    }

    @Test
    fun `loading state should transition to true then false`() = runTest {
        coEvery { repository.login(any(), any()) } coAnswers {
            delay(100)
        }

        viewModel.loading.test {
            assertEquals(false, awaitItem()) // Initial
            viewModel.login("test@lksnext.com", "pass", {})
            assertEquals(true, awaitItem()) // Starts loading
            assertEquals(false, awaitItem()) // Ends loading
        }
    }
}
