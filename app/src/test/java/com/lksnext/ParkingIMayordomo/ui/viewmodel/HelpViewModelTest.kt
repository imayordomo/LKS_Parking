package com.lksnext.ParkingIMayordomo.ui.viewmodel

import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test

class HelpViewModelTest {
    @Test
    fun `HelpViewModel can be instantiated`() {
        val repository = mockk<ParkingRepository>()
        val viewModel = HelpViewModel(repository)
        assertNotNull(viewModel)
    }
}
