package com.lksnext.ParkingIMayordomo.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.lang.reflect.Field
import java.lang.reflect.Modifier

class ReservationReminderManagerTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var alarmManager: AlarmManager

    private lateinit var reminderManager: ReservationReminderManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        mockkStatic(PendingIntent::class)
        reminderManager = ReservationReminderManager(context)
    }

    @Test
    fun `scheduleReminders schedules both start and end alarms when in future`() {
        val reservationId = "res1"
        val now = 1000000L
        val startTime = now + 60 * 60 * 1000L // +60 min
        val endTime = now + 120 * 60 * 1000L // +120 min
        
        mockkStatic(System::class)
        every { System.currentTimeMillis() } returns now
        
        val pendingIntent = mockk<PendingIntent>()
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns pendingIntent
        every { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) } just Runs
        every { alarmManager.set(any(), any(), any()) } just Runs

        reminderManager.scheduleReminders(reservationId, startTime, endTime)

        // T-30m = now + 30m, T-15m = now + 105m
        verify { 
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTime - 30 * 60 * 1000L, any())
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime - 15 * 60 * 1000L, any())
        }
        unmockkStatic(System::class)
    }

    @Test
    fun `scheduleReminders skips start alarm if it is in the past`() {
        val now = 2000000L
        val startTime = now + 10 * 60 * 1000L // T-30m is in the past
        val endTime = now + 60 * 60 * 1000L
        
        mockkStatic(System::class)
        every { System.currentTimeMillis() } returns now
        
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockk()
        every { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) } just Runs

        reminderManager.scheduleReminders("res2", startTime, endTime)

        verify(exactly = 1) { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
        verify(exactly = 0) { alarmManager.setExactAndAllowWhileIdle(any(), startTime - 30 * 60 * 1000L, any()) }
        unmockkStatic(System::class)
    }

    @Test
    fun `cancelReminders cancels both start and end alarms`() {
        val reservationId = "res3"
        val pendingIntent = mockk<PendingIntent>()
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns pendingIntent
        every { alarmManager.cancel(any<PendingIntent>()) } just Runs
        every { pendingIntent.cancel() } just Runs

        reminderManager.cancelReminders(reservationId)

        verify(exactly = 2) { alarmManager.cancel(pendingIntent) }
        verify(exactly = 2) { pendingIntent.cancel() }
    }

    @Test
    fun `updateReminders calls cancel and then schedule`() {
        val spy = spyk(reminderManager)
        every { spy.cancelReminders(any()) } just Runs
        every { spy.scheduleReminders(any(), any(), any()) } just Runs

        spy.updateReminders("res4", 0L, 0L)

        verify(ordering = Ordering.SEQUENCE) {
            spy.cancelReminders("res4")
            spy.scheduleReminders("res4", 0L, 0L)
        }
    }
}
