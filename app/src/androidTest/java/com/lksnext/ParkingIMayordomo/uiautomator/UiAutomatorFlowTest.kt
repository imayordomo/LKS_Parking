package com.lksnext.ParkingIMayordomo.uiautomator

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiAutomatorFlowTest {

    private lateinit var device: UiDevice
    private val packageName = "com.lksnext.ParkingIMayordomo"
    private val timeout = 15000L

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        device.executeShellCommand("settings put global system_locales es-ES")
        device.executeShellCommand("am broadcast -a android.intent.action.LOCALE_CHANGED")

        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        assertNotNull("Launch intent should not be null", intent)
        context.startActivity(intent)

        device.wait(Until.hasObject(androidx.test.uiautomator.By.pkg(packageName).depth(0)), timeout)
    }

    private fun findText(texts: List<String>): String? {
        for (text in texts) {
            val obj = device.findObject(UiSelector().textContains(text))
            if (obj.waitForExists(3000)) return text
        }
        return null
    }

    private fun getRegisterTexts() = listOf("Registrarse", "Register", "Sign up")
    private fun getLoginTexts() = listOf("Iniciar Sesión", "Log in", "Sign in")
    private fun getNameTexts() = listOf("Nombre", "Name")
    private fun getEmailTexts() = listOf("Email", "Correo electrónico", "Correo")

    @Test
    fun landingScreen_hasLoginAndRegisterButtons() {
        val loginText = findText(getLoginTexts())
        assertNotNull("Login button should be displayed. Tried: ${getLoginTexts()}", loginText)
        val registerText = findText(getRegisterTexts())
        assertNotNull("Register button should be displayed. Tried: ${getRegisterTexts()}", registerText)
    }

    @Test
    fun navigateFromLandingToRegister() {
        val registerText = findText(getRegisterTexts())
        assertNotNull("Register button should be displayed. Tried: ${getRegisterTexts()}", registerText)
        device.findObject(UiSelector().textContains(registerText)).click()

        val nameText = findText(getNameTexts())
        assertNotNull("Name field after Register. Tried: ${getNameTexts()}", nameText)
    }

    @Test
    fun navigateFromLandingToLogin() {
        val loginText = findText(getLoginTexts())
        assertNotNull("Login button should be displayed. Tried: ${getLoginTexts()}", loginText)
        device.findObject(UiSelector().textContains(loginText)).click()

        val emailText = findText(getEmailTexts())
        assertNotNull("Email field after Login. Tried: ${getEmailTexts()}", emailText)
    }
}
