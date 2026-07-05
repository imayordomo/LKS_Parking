package com.lksnext.ParkingIMayordomo.uitest

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.test.platform.app.InstrumentationRegistry
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.Reservation
import com.lksnext.ParkingIMayordomo.data.model.User
import com.lksnext.ParkingIMayordomo.data.model.Vehicle
import com.lksnext.ParkingIMayordomo.data.model.VehicleType
import com.lksnext.ParkingIMayordomo.data.repository.ParkingRepository
import com.lksnext.ParkingIMayordomo.ui.pages.Profile
import com.lksnext.ParkingIMayordomo.ui.viewmodel.ProfileViewModel
import com.lksnext.ParkingIMayordomo.utils.LocaleManager
import com.lksnext.ParkingIMayordomo.utils.TestTags
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalTestApi::class)
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private fun createRepository(
        user: User? = User("u1", "test@test.com", "Test User"),
        vehicles: List<Vehicle>? = listOf(Vehicle("v1", "u1", VehicleType.CAR, "1234ABC")),
        reservations: List<Reservation> = emptyList()
    ): ParkingRepository {
        val repo = mockk<ParkingRepository>(relaxed = true)
        every { repo.user } returns MutableStateFlow(user)
        every { repo.vehicles } returns MutableStateFlow(vehicles)
        every { repo.reservations } returns MutableStateFlow(reservations)
        every { repo.notifications } returns MutableStateFlow(emptyList())
        return repo
    }

    @Test
    fun profile_displaysUserHeader() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_EDIT_PROFILE_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_LANGUAGE_SELECTOR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_LOGOUT_BUTTON).assertIsDisplayed()
    }

    @Test
    fun profile_withVehicles_showsVehicleItem() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_VEHICLE_DELETE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun profile_withoutVehicles_showsEmptyState() {
        val repo = createRepository(vehicles = emptyList())
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_VEHICLE_DELETE).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun vehicleAlertDialog_shows_whenShowVehicleAlertInit() {
        val repo = createRepository(vehicles = emptyList())
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { }, showVehicleAlertInit = true)
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_VEHICLE_ALERT_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_VEHICLE_ALERT_ADD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_VEHICLE_ALERT_CANCEL).assertIsDisplayed()
    }

    @Test
    fun vehicleAlertDialog_cancel_dismisses() {
        val repo = createRepository(vehicles = emptyList())
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { }, showVehicleAlertInit = true)
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_VEHICLE_ALERT_CANCEL).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_VEHICLE_ALERT_DIALOG).assertIsNotDisplayed()
    }

    @Test
    fun vehicleAlertDialog_add_opensAddVehicleDialog() {
        val repo = createRepository(vehicles = emptyList())
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { }, showVehicleAlertInit = true)
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_VEHICLE_ALERT_ADD).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_VEHICLE_ALERT_DIALOG).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_TYPE_FIELD).assertIsDisplayed()
    }

    @Test
    fun editProfileButton_opensEditDialog() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_EDIT_PROFILE_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_EDIT_DIALOG_NAME_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_EDIT_DIALOG_SAVE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_EDIT_DIALOG_CANCEL).assertIsDisplayed()
    }

    @Test
    fun editProfileDialog_save_callsUpdateProfile() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_EDIT_PROFILE_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_EDIT_DIALOG_SAVE).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_EDIT_DIALOG_NAME_FIELD).assertIsNotDisplayed()
    }

    @Test
    fun editProfileDialog_cancel_closesDialog() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_EDIT_PROFILE_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_EDIT_DIALOG_CANCEL).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_EDIT_DIALOG_NAME_FIELD).assertIsNotDisplayed()
    }

    @Test
    fun addVehicleButton_opensAddVehicleDialog() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_TYPE_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_PLATE_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_ADD_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_CANCEL).assertIsDisplayed()
    }

    @Test
    fun addVehicleDialog_typeMenu_opensTypeDropdown() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_TYPE_FIELD).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_TYPE_MENU).assertIsDisplayed()
    }

    @Test
    fun addVehicleDialog_cancel_closesDialog() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_CANCEL).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_TYPE_FIELD).assertIsNotDisplayed()
    }

    @Test
    fun addVehicleDialog_plateField_acceptsInput() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_ADD_VEHICLE_PLATE_FIELD).performTextInput("1234ABC")
    }

    @Test
    fun logoutButton_opensLogoutDialog() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_LOGOUT_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_LOGOUT_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_LOGOUT_CONFIRM).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_LOGOUT_CANCEL).assertIsDisplayed()
    }

    @Test
    fun logoutDialog_cancel_dismisses() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_LOGOUT_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_LOGOUT_CANCEL).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_LOGOUT_DIALOG).assertIsNotDisplayed()
    }

    @Test
    fun logoutDialog_confirm_logsOutAndNavigates() {
        val repo = createRepository()
        var navigatedRoute: String? = null
        composeTestRule.setContent {
            Profile(
                viewModel = ProfileViewModel(repo),
                onNavigate = { navigatedRoute = it }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_LOGOUT_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_LOGOUT_CONFIRM).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_LOGOUT_DIALOG).assertIsNotDisplayed()
    }

    @Test
    fun vehicleDelete_opensDeleteDialog() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_VEHICLE_DELETE).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_VEHICLE_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_VEHICLE_CONFIRM).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_VEHICLE_CANCEL).assertIsDisplayed()
    }

    @Test
    fun vehicleDeleteDialog_cancel_dismisses() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_VEHICLE_DELETE).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_VEHICLE_CANCEL).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_VEHICLE_DIALOG).assertIsNotDisplayed()
    }

    @Test
    fun vehicleDeleteDialog_confirm_removesVehicle() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_VEHICLE_DELETE).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_VEHICLE_CONFIRM).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_VEHICLE_DIALOG).assertIsNotDisplayed()
    }

    @Test
    fun cannotDeleteDialog_shows_whenVehicleHasReservations() {
        val repo = createRepository(
            reservations = listOf(
                Reservation("r1", 5, "2026-07-01", "09:00", "11:00", "u1", "v1", licensePlate = "1234ABC")
            )
        )
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_VEHICLE_DELETE).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_CANNOT_DELETE_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_CANNOT_DELETE_OK).assertIsDisplayed()
    }

    @Test
    fun cannotDeleteDialog_ok_dismisses() {
        val repo = createRepository(
            reservations = listOf(
                Reservation("r1", 5, "2026-07-01", "09:00", "11:00", "u1", "v1", licensePlate = "1234ABC")
            )
        )
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_VEHICLE_DELETE).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_CANNOT_DELETE_OK).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_CANNOT_DELETE_DIALOG).assertIsNotDisplayed()
    }

    @Test
    fun languageSelector_expandsMenu() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_LANGUAGE_SELECTOR).performClick()
    }

    @Test
    fun deleteAccountButton_isDisplayed() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_ACCOUNT_BUTTON).assertIsDisplayed()
    }

    @Test
    fun deleteAccountButton_opensDeleteDialog() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_ACCOUNT_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_ACCOUNT_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_ACCOUNT_CONFIRM).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_ACCOUNT_CANCEL).assertIsDisplayed()
    }

    @Test
    fun deleteAccountDialog_cancel_dismisses() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_ACCOUNT_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_ACCOUNT_CANCEL).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_ACCOUNT_DIALOG).assertIsNotDisplayed()
    }

    @Test
    fun deleteAccountDialog_confirm_callsDeleteAndNavigates() {
        val repo = createRepository()
        var navigatedRoute: String? = null
        composeTestRule.setContent {
            Profile(
                viewModel = ProfileViewModel(repo),
                onNavigate = { navigatedRoute = it }
            )
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_ACCOUNT_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_ACCOUNT_CONFIRM).performClick()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_DELETE_ACCOUNT_DIALOG).assertIsNotDisplayed()
        assertEquals("login", navigatedRoute)
    }

    @Test
    fun changeImageButton_isDisplayed() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_CHANGE_IMAGE_BUTTON).assertIsDisplayed()
    }

    // ── Profile image display tests ──

    @Test
    fun profile_withoutImage_showsFallbackLetter() {
        val repo = createRepository(user = User("u1", "test@test.com", "Name"))
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_FALLBACK_LETTER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_IMAGE).assertDoesNotExist()
    }

    @Test
    fun profile_withImage_hidesFallbackAndShowsImage() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        val repo = createRepository(user = User("u1", "test@test.com", "Name", profileImage = base64))
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_IMAGE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PROFILE_FALLBACK_LETTER).assertDoesNotExist()
    }

    // ── Language selector tests ──

    @Test
    fun languageSelector_selectSpanish() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_LANGUAGE_SELECTOR).performClick()
        val spanishText = targetContext.getString(R.string.language_spanish)
        composeTestRule.waitUntilExactlyOneExists(hasText(spanishText), timeoutMillis = 5000)
        composeTestRule.onNodeWithText(spanishText).performClick()

        assertEquals("es", LocaleManager.localeFlow.value)
    }

    @Test
    fun languageSelector_selectEnglish() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_LANGUAGE_SELECTOR).performClick()
        val englishText = targetContext.getString(R.string.language_english)
        composeTestRule.waitUntilExactlyOneExists(hasText(englishText), timeoutMillis = 5000)
        composeTestRule.onNodeWithText(englishText).performClick()

        assertEquals("en", LocaleManager.localeFlow.value)
    }

    @Test
    fun languageSelector_selectBasque() {
        val repo = createRepository()
        composeTestRule.setContent {
            Profile(viewModel = ProfileViewModel(repo), onNavigate = { })
        }

        composeTestRule.onNodeWithTag(TestTags.PROFILE_LANGUAGE_SELECTOR).performClick()
        val basqueText = targetContext.getString(R.string.language_basque)
        composeTestRule.waitUntilExactlyOneExists(hasText(basqueText), timeoutMillis = 5000)
        composeTestRule.onNodeWithText(basqueText).performClick()

        assertEquals("eu", LocaleManager.localeFlow.value)
    }
}
