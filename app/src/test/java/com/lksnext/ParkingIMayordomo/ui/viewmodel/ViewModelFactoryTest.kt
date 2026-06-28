package com.lksnext.ParkingIMayordomo.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ViewModelFactoryTest {

    private lateinit var repository: ParkingRepository
    private lateinit var factory: ViewModelFactory

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        factory = ViewModelFactory(repository)
    }

    @Test
    fun `create should return correct ViewModel classes`() {
        val viewModels = listOf(
            DashboardViewModel::class.java,
            NewReservationViewModel::class.java,
            EditReservationViewModel::class.java,
            LoginViewModel::class.java,
            RegisterViewModel::class.java,
            HistoryViewModel::class.java,
            ProfileViewModel::class.java,
            NotificationsViewModel::class.java,
            ViewParkingViewModel::class.java,
            ForgotPasswordViewModel::class.java,
            ReportViewModel::class.java,
            LandingViewModel::class.java,
            HelpViewModel::class.java
        )

        for (modelClass in viewModels) {
            val viewModel = factory.create(modelClass)
            assertNotNull("Failed to create ${modelClass.simpleName}", viewModel)
            assertTrue(modelClass.isInstance(viewModel))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `create should throw exception for unknown ViewModel class`() {
        class UnknownViewModel : ViewModel()
        factory.create(UnknownViewModel::class.java)
    }
}
