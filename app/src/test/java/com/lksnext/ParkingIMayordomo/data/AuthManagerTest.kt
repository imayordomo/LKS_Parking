package com.lksnext.ParkingIMayordomo.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthManagerTest {

    @Before
    fun setup() {
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)
        mockkStatic(FirebaseMessaging::class)
        
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true)
        every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
        every { FirebaseMessaging.getInstance() } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isEmailAuthorized should return true for lksnext domain`() {
        assertTrue(AuthManager.isEmailAuthorized("test@lksnext.com"))
        assertTrue(AuthManager.isEmailAuthorized("  USER@LKSNEXT.COM  "))
    }

    @Test
    fun `isEmailAuthorized should return false for other domains`() {
        assertFalse(AuthManager.isEmailAuthorized("test@gmail.com"))
        assertFalse(AuthManager.isEmailAuthorized("test@lks.com"))
        assertFalse(AuthManager.isEmailAuthorized(""))
    }
}
