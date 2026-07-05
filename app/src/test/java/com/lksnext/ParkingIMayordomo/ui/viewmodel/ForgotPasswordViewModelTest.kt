package com.lksnext.ParkingIMayordomo.ui.viewmodel

import com.lksnext.ParkingIMayordomo.MainDispatcherRule
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: ParkingRepository
    private lateinit var viewModel: ForgotPasswordViewModel

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        viewModel = ForgotPasswordViewModel(repository)
    }

    @Test
    fun `onEmailChange should update email and clear error`() {
        viewModel.onEmailChange("test@lksnext.com")
        assertEquals("test@lksnext.com", viewModel.email.value)
        assertNull(viewModel.errorResId.value)
    }

    @Test
    fun `sendResetEmail with empty email should set error`() {
        viewModel.onEmailChange("")
        viewModel.sendResetEmail()
        assertEquals(R.string.error_required_fields, viewModel.errorResId.value)
    }

    @Test
    fun `sendResetEmail with non-corporate email should set error`() {
        viewModel.onEmailChange("test@gmail.com")
        viewModel.sendResetEmail()
        assertEquals(R.string.error_corporate_only, viewModel.errorResId.value)
    }

    @Test
    fun `sendResetEmail with valid corporate email should call repository`() = runTest {
        val email = "test@lksnext.com"
        viewModel.onEmailChange(email)
        
        viewModel.sendResetEmail()

        coVerify { repository.resetPassword(email) }
        assertTrue(viewModel.isEmailSent.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `sendResetEmail with special authorized email should call repository`() = runTest {
        val email = "imayordomo004@ikasle.ehu.eus"
        viewModel.onEmailChange(email)
        
        viewModel.sendResetEmail()

        coVerify { repository.resetPassword(email) }
        assertTrue(viewModel.isEmailSent.value)
    }

    @Test
    fun `sendResetEmail failure with unknown error should set default error`() = runTest {
        val email = "test@lksnext.com"
        viewModel.onEmailChange(email)
        coEvery { repository.resetPassword(any()) } throws Exception("some_error")

        viewModel.sendResetEmail()

        assertEquals(R.string.error_unknown, viewModel.errorResId.value)
        assertFalse(viewModel.isEmailSent.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `sendResetEmail failure with corporate only error should set specific error`() = runTest {
        val email = "test@lksnext.com"
        viewModel.onEmailChange(email)
        coEvery { repository.resetPassword(any()) } throws Exception("error_corporate_only")

        viewModel.sendResetEmail()

        assertEquals(R.string.error_corporate_only, viewModel.errorResId.value)
    }

    @Test
    fun `resetState should clear state`() {
        viewModel.onEmailChange("test@lksnext.com")
        viewModel.sendResetEmail()
        
        viewModel.resetState()
        
        assertFalse(viewModel.isEmailSent.value)
        assertNull(viewModel.errorResId.value)
    }
}
