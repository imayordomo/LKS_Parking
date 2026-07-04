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

        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        assertNotNull("Launch intent should not be null", intent)
        context.startActivity(intent)

        device.wait(Until.hasObject(androidx.test.uiautomator.By.pkg(packageName).depth(0)), timeout)
    }

    @Test
    fun landingScreen_hasLoginAndRegisterButtons() {
        val loginButton = device.findObject(UiSelector().textContains("Iniciar Sesión"))
        loginButton.waitForExists(timeout)
        assertNotNull("Login button should be displayed", loginButton)

        val registerButton = device.findObject(UiSelector().textContains("Registrarse"))
        registerButton.waitForExists(timeout)
        assertNotNull("Register button should be displayed", registerButton)
    }

    @Test
    fun navigateFromLandingToRegister() {
        val registerButton = device.findObject(UiSelector().textContains("Registrarse"))
        registerButton.waitForExists(timeout)
        assertNotNull(registerButton)
        registerButton.click()

        device.wait(Until.hasObject(androidx.test.uiautomator.By.textContains("Nombre")), timeout)
        val nameField = device.findObject(androidx.test.uiautomator.By.textContains("Nombre"))
        assertNotNull("Name field should be visible after clicking Register", nameField)
    }

    @Test
    fun navigateFromLandingToLogin() {
        val loginButton = device.findObject(UiSelector().textContains("Iniciar Sesión"))
        loginButton.waitForExists(timeout)
        assertNotNull(loginButton)
        loginButton.click()

        device.wait(Until.hasObject(androidx.test.uiautomator.By.textContains("Email")), timeout)
        val emailField = device.findObject(androidx.test.uiautomator.By.textContains("Email"))
        assertNotNull("Email field should be visible after clicking Login", emailField)
    }
}
