package com.lksnext.ParkingIMayordomo.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object LocaleManager {
    private const val TAG = "LocaleManager"
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LOCALE = "selected_locale"

    private val supportedLanguages = listOf("es", "en", "eu")

    private val _localeFlow = MutableStateFlow("es")
    val localeFlow: StateFlow<String> = _localeFlow.asStateFlow()

    private var systemLocaleCode: String = "es"
    private var systemLocale: Locale = Locale("es")

    fun captureSystemLocale(context: Context) {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        // Fallback to Spanish if system language isn't supported
        systemLocaleCode = if (raw.language in supportedLanguages) raw.language else "es"
        systemLocale = Locale(systemLocaleCode)
    }

    /**
     * Initializes the locale state.
     * Logic: Saved Choice -> System Language (if supported) -> Spanish (Fallback)
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var savedLocale = prefs.getString(KEY_LOCALE, null)

        // If no preference yet or legacy "system" value exists, determine the best real default
        if (savedLocale == null || savedLocale == "system") {
            val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0].language
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale.language
            }

            savedLocale = if (systemLocale in supportedLanguages) systemLocale else "es"
            // Persist the choice immediately so it stays fixed unless changed by user
            prefs.edit().putString(KEY_LOCALE, savedLocale).apply()
        }

        _localeFlow.value = savedLocale!!
        
        val expectedLocales = LocaleListCompat.forLanguageTags(savedLocale)
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != expectedLocales.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(expectedLocales)
        }
    }

    /**
     * Updates the locale, persists it synchronously, and restarts the Activity.
     */
    fun updateLocale(context: Context, languageCode: String) {
        if (_localeFlow.value == languageCode) return

        Log.d(TAG, "updateLocale: Changing to $languageCode")
        
        // Save selection synchronously
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LOCALE, languageCode)
            .commit()

        _localeFlow.value = languageCode

        // Notify AppCompatDelegate
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)

        // Force Activity restart to apply new strings.xml
        findActivity(context)?.recreate()
    }

    private fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }

    /**
     * Injects the correct locale into the Context.
     */
    fun wrapContext(context: Context): Context {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val language = prefs.getString(KEY_LOCALE, "es") ?: "es"
        
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val configuration = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }
        configuration.setLayoutDirection(locale)
        
        return context.createConfigurationContext(configuration)
    }

    fun getCurrentLocale(): String = _localeFlow.value

    fun getSystemLocaleContext(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(systemLocale))
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = systemLocale
        }
        configuration.setLayoutDirection(systemLocale)
        return context.createConfigurationContext(configuration)
    }
}
