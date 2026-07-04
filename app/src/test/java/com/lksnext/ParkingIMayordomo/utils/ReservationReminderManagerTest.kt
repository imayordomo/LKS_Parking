package com.lksnext.ParkingIMayordomo.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ReservationReminderManagerTest {

    @MockK
    lateinit var context: Context
    @MockK
    lateinit var alarmManager: AlarmManager
    @MockK
    lateinit var pendingIntent: PendingIntent

    private lateinit var reminderManager: ReservationReminderManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        
        mockkStatic(PendingIntent::class)
        mockkStatic(Log::class)
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setAction(any()) } returns mockk<Intent>(relaxed = true)
        every { anyConstructed<Intent>().putExtra(any<String>(), any<String>()) } returns mockk<Intent>(relaxed = true)
        every { anyConstructed<Intent>().putExtra(any<String>(), any<Int>()) } returns mockk<Intent>(relaxed = true)
        
        // Setup default mocks
        every { PendingIntent.getBroadcast(any<Context>(), any<Int>(), any<Intent>(), any<Int>()) } returns pendingIntent
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        
        every { alarmManager.canScheduleExactAlarms() } returns true
        every { alarmManager.setExactAndAllowWhileIdle(any<Int>(), any<Long>(), any<PendingIntent>()) } just Runs
        every { alarmManager.setAndAllowWhileIdle(any<Int>(), any<Long>(), any<PendingIntent>()) } just Runs
        every { alarmManager.set(any<Int>(), any<Long>(), any<PendingIntent>()) } just Runs
        every { alarmManager.cancel(any<PendingIntent>()) } just Runs
        every { pendingIntent.cancel() } just Runs

        reminderManager = ReservationReminderManager(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `scheduleReminders schedules both start and end alarms when they are in the future`() {
        // GIVEN
        val now = 1000000L
        reminderManager.currentTimeProvider = { now }
        reminderManager.sdkVersionProvider = { Build.VERSION_CODES.M }
        val startTime = now + 40 * 60 * 1000L // 40 mins from now
        val endTime = now + 60 * 60 * 1000L  // 60 mins from now

        // WHEN
        reminderManager.scheduleReminders("res1", startTime, endTime)

        // THEN
        // Start alarm should be scheduled at startTime - 30 mins
        verify(exactly = 1) { 
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTime - 30 * 60 * 1000L, pendingIntent) 
        }
        // End alarm should be scheduled at endTime - 15 mins
        verify(exactly = 1) { 
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime - 15 * 60 * 1000L, pendingIntent) 
        }
    }

    @Test
    fun `scheduleReminders skips start alarm if it is in the past`() {
        // GIVEN
        val now = 1000000L
        reminderManager.currentTimeProvider = { now }
        reminderManager.sdkVersionProvider = { Build.VERSION_CODES.M }
        val startTime = now + 10 * 60 * 1000L // T-30 is past
        val endTime = now + 60 * 60 * 1000L

        // WHEN
        reminderManager.scheduleReminders("res2", startTime, endTime)

        // THEN
        verify(exactly = 0) { alarmManager.setExactAndAllowWhileIdle(any(), startTime - 30 * 60 * 1000L, any()) }
        verify(exactly = 1) { alarmManager.setExactAndAllowWhileIdle(any(), endTime - 15 * 60 * 1000L, any()) }
    }

    @Test
    fun `scheduleAlarm handles Android S+ logic when exact alarms are permitted`() {
        // GIVEN
        reminderManager.sdkVersionProvider = { Build.VERSION_CODES.S }
        reminderManager.currentTimeProvider = { 0L }
        every { alarmManager.canScheduleExactAlarms() } returns true

        // WHEN
        reminderManager.scheduleReminders("res_s", 10000000L, 20000000L)

        // THEN
        verify { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
    }

    @Test
    fun `scheduleAlarm fallbacks to setAndAllowWhileIdle when exact alarms are NOT permitted on Android S+`() {
        // GIVEN
        reminderManager.sdkVersionProvider = { Build.VERSION_CODES.S }
        reminderManager.currentTimeProvider = { 0L }
        every { alarmManager.canScheduleExactAlarms() } returns false

        // WHEN
        reminderManager.scheduleReminders("res_fallback", 10000000L, 20000000L)

        // THEN
        verify { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), any()) }
        verify { Log.w(any(), match<String> { it.contains("Exact alarms not permitted") }) }
    }

    @Test
    fun `scheduleAlarm handles Android versions between M and S`() {
        // GIVEN
        reminderManager.sdkVersionProvider = { Build.VERSION_CODES.M }
        reminderManager.currentTimeProvider = { 0L }

        // WHEN
        reminderManager.scheduleReminders("res_m", 10000000L, 20000000L)

        // THEN
        verify { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
    }

    @Test
    fun `scheduleAlarm handles Android versions older than M`() {
        // GIVEN
        reminderManager.sdkVersionProvider = { Build.VERSION_CODES.LOLLIPOP }
        reminderManager.currentTimeProvider = { 0L }

        // WHEN
        reminderManager.scheduleReminders("res_old", 10000000L, 20000000L)

        // THEN
        verify { alarmManager.set(any(), any(), any()) }
    }

    @Test
    fun `scheduleAlarm catches and logs exceptions`() {
        // GIVEN
        reminderManager.currentTimeProvider = { 0L }
        reminderManager.sdkVersionProvider = { 0 }
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns pendingIntent
        every { alarmManager.set(any(), any(), any()) } throws RuntimeException("Mock crash")

        // WHEN
        reminderManager.scheduleReminders("res_crash", 10000000L, 20000000L)

        // THEN
        verify { Log.e(any(), match { it.contains("Error scheduling alarm") }) }
    }

    @Test
    fun `cancelReminders cancels both start and end alarms if they exist`() {
        // WHEN
        reminderManager.cancelReminders("res_cancel")
        
        // THEN
        verify(exactly = 2) { alarmManager.cancel(any<PendingIntent>()) }
        verify(exactly = 2) { pendingIntent.cancel() }
    }

    @Test
    fun `cancelReminders handles null PendingIntents safely`() {
        // GIVEN
        every { PendingIntent.getBroadcast(any(), any(), any(), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE) } returns null
        
        // WHEN
        reminderManager.cancelReminders("res_null")
        
        // THEN
        verify(exactly = 0) { alarmManager.cancel(any<PendingIntent>()) }
    }

    @Test
    fun `updateReminders performs cancel then schedule`() {
        // GIVEN
        val spy = spyk(reminderManager)
        every { spy.cancelReminders(any()) } just Runs
        every { spy.scheduleReminders(any(), any(), any()) } just Runs

        // WHEN
        spy.updateReminders("res_upd", 123L, 456L)

        // THEN
        verify(ordering = Ordering.SEQUENCE) {
            spy.updateReminders("res_upd", 123L, 456L)
            spy.cancelReminders("res_upd")
            spy.scheduleReminders("res_upd", 123L, 456L)
        }
    }
}
