package com.lksnext.ParkingIMayordomo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository

class ViewModelFactory(private val repository: ParkingRepository) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repository) as T
            modelClass.isAssignableFrom(NewReservationViewModel::class.java) -> NewReservationViewModel(repository) as T
            modelClass.isAssignableFrom(EditReservationViewModel::class.java) -> EditReservationViewModel(repository) as T
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(repository) as T
            modelClass.isAssignableFrom(RegisterViewModel::class.java) -> RegisterViewModel(repository) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> HistoryViewModel(repository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(repository) as T
            modelClass.isAssignableFrom(NotificationsViewModel::class.java) -> NotificationsViewModel(repository) as T
            modelClass.isAssignableFrom(ViewParkingViewModel::class.java) -> ViewParkingViewModel(repository) as T
            modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java) -> ForgotPasswordViewModel() as T
            modelClass.isAssignableFrom(ReportViewModel::class.java) -> ReportViewModel(repository) as T
            modelClass.isAssignableFrom(LandingViewModel::class.java) -> LandingViewModel(repository) as T
            modelClass.isAssignableFrom(HelpViewModel::class.java) -> HelpViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    // Mantener compatibilidad con versiones que no usan CreationExtras
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return create(modelClass, CreationExtras.Empty)
    }
}
