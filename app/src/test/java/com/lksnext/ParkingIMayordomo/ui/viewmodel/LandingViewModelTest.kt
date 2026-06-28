package com.lksnext.ParkingIMayordomo.ui.viewmodel

import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test

class LandingViewModelTest {
    @Test
    fun `LandingViewModel can be instantiated`() {
        val repository = mockk<ParkingRepository>()
        val viewModel = LandingViewModel(repository)
        assertNotNull(viewModel)
    }
}
