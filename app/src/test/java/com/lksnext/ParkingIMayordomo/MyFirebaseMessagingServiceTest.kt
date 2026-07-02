package com.lksnext.ParkingIMayordomo

import android.app.NotificationManager
import android.content.Context
import com.google.firebase.messaging.RemoteMessage
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
class MyFirebaseMessagingServiceTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var notificationManager: NotificationManager

    private lateinit var service: MyFirebaseMessagingService

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        val testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        // Mock Dispatchers.IO to use test dispatcher
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher

        service = spyk(MyFirebaseMessagingService())
        
        // Mock context and system services
        every { service.applicationContext } returns context
        every { service.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        
        mockkObject(AuthManager)
        coEvery { AuthManager.addExternalNotification(any(), any()) } returns Unit
        coEvery { AuthManager.updateFcmToken(any()) } returns Unit
    }

    @Test
    fun `onMessageReceived saves notification and shows it`() {
        val remoteMessage = mockk<RemoteMessage>()
        val notification = mockk<RemoteMessage.Notification>()
        
        every { remoteMessage.notification } returns notification
        every { notification.title } returns "Test Title"
        every { notification.body } returns "Test Body"
        every { remoteMessage.data } returns emptyMap()

        service.onMessageReceived(remoteMessage)

        coVerify { AuthManager.addExternalNotification("Test Title", "Test Body") }
        verify { notificationManager.notify(any(), any()) }
    }

    @Test
    fun `onNewToken updates token in AuthManager`() {
        val token = "new_test_token"
        
        service.onNewToken(token)

        coVerify { AuthManager.updateFcmToken(token) }
    }
}
