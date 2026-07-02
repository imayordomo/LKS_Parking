package com.lksnext.ParkingIMayordomo.utils

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.lksnext.ParkingIMayordomo.data.AuthManager
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationReceiverTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var notificationManager: NotificationManager

    @MockK
    lateinit var intent: Intent

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        every { context.packageName } returns "com.lksnext.ParkingIMayordomo"
        every { context.applicationContext } returns context
        
        mockkObject(AuthManager)
        coEvery { AuthManager.addExternalNotification(any(), any()) } returns Unit
    }

    @Test
    fun `onReceive triggers notification and saves to in-app history`() {
        // GIVEN
        val title = "Test Title"
        val message = "Test Message"
        val notificationId = 123
        
        every { intent.getStringExtra("title") } returns title
        every { intent.getStringExtra("message") } returns message
        every { intent.getIntExtra("notification_id", any()) } returns notificationId

        // Mocking the Notification builder chain is complex, so we usually verify the notify call
        // For simplicity in unit tests, we check if the dependencies are interacted with
        val receiver = NotificationReceiver()

        // WHEN
        receiver.onReceive(context, intent)

        // THEN
        coVerify { AuthManager.addExternalNotification(title, message) }
        verify { notificationManager.notify(eq(notificationId), any()) }
    }
}
