package com.lksnext.ParkingIMayordomo.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class LocaleManagerTest {

    @MockK
    lateinit var context: Context
    @MockK
    lateinit var prefs: SharedPreferences
    @MockK
    lateinit var editor: SharedPreferences.Editor
    @MockK
    lateinit var resources: Resources
    @MockK
    lateinit var configuration: Configuration

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        // Mock Android Log to prevent RuntimeException during tests
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0

        // Mock AppCompatDelegate static methods
        mockkStatic(AppCompatDelegate::class)
        every { AppCompatDelegate.setApplicationLocales(any<LocaleListCompat>()) } just Runs
        every { AppCompatDelegate.getApplicationLocales() } returns LocaleListCompat.getEmptyLocaleList()

        // Mock LocaleListCompat static methods
        mockkStatic(LocaleListCompat::class)
        every { LocaleListCompat.forLanguageTags(any<String>()) } returns LocaleListCompat.getEmptyLocaleList()

        // Setup SharedPreferences mocks
        every { context.getSharedPreferences(any<String>(), any<Int>()) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any<String>(), any<String>()) } returns editor
        every { editor.apply() } just Runs
        
        // Setup Resources and Configuration mocks
        every { context.resources } returns resources
        every { resources.configuration } returns configuration
        
        // Setup LocaleList mock for configuration.locales[0]
        val mockLocaleList = mockk<LocaleList>(relaxed = true)
        every { mockLocaleList.get(0) } returns Locale.forLanguageTag("en")
        every { mockLocaleList.size() } returns 1
        every { configuration.locales } returns mockLocaleList
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `init with no saved locale fallbacks to es if system not supported`() {
        every { prefs.getString("selected_locale", null) } returns null
        
        val unsupportedLocaleList = mockk<LocaleList>(relaxed = true)
        every { unsupportedLocaleList.get(0) } returns Locale.forLanguageTag("fr") // French not in supported list
        every { unsupportedLocaleList.size() } returns 1
        every { configuration.locales } returns unsupportedLocaleList

        LocaleManager.init(context)

        assertEquals("es", LocaleManager.getCurrentLocale())
        verify { editor.putString("selected_locale", "es") }
    }

    @Test
    fun `init with saved locale uses it`() {
        every { prefs.getString("selected_locale", null) } returns "eu"
        
        LocaleManager.init(context)

        assertEquals("eu", LocaleManager.getCurrentLocale())
        // Verification metadata confirms it shouldn't try to re-save if already present
        verify(exactly = 0) { editor.putString(any<String>(), any<String>()) }
    }

    @Test
    fun `updateLocale updates flow and prefs`() {
        // First init to set current internal state
        every { prefs.getString("selected_locale", null) } returns "en"
        LocaleManager.init(context)
        
        // Act: change to Spanish
        LocaleManager.updateLocale(context, "es")
        
        // Assert
        assertEquals("es", LocaleManager.getCurrentLocale())
        verify { editor.putString("selected_locale", "es") }
        verify { AppCompatDelegate.setApplicationLocales(any<LocaleListCompat>()) }
    }
}
