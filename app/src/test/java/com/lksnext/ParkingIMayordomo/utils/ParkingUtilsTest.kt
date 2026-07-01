package com.lksnext.ParkingIMayordomo.utils

import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.data.model.VehicleType
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class ParkingUtilsTest {

    @Test
    fun `formatDate formats date correctly`() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse("2026-07-01")!!
        assertEquals("2026-07-01", ParkingUtils.formatDate(date))
    }

    @Test
    fun `formatTime formats time correctly`() {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).parse("09:30")!!
        assertEquals("09:30", ParkingUtils.formatTime(time))
    }

    @Test
    fun `parseDate parses valid date string`() {
        val result = ParkingUtils.parseDate("2026-07-01")
        assertNotNull(result)
    }

    @Test
    fun `parseDate returns null for invalid string`() {
        assertNull(ParkingUtils.parseDate("invalid-date"))
    }

    @Test
    fun `parseTime parses valid time string`() {
        val result = ParkingUtils.parseTime("14:30")
        assertNotNull(result)
    }

    @Test
    fun `parseTime returns null for invalid string`() {
        assertNull(ParkingUtils.parseTime("invalid-time"))
    }

    @Test
    fun `formatDate and parseDate are round-trippable`() {
        val original = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse("2026-12-25")!!
        val formatted = ParkingUtils.formatDate(original)
        val parsed = ParkingUtils.parseDate(formatted)
        assertEquals(ParkingUtils.formatDate(original), ParkingUtils.formatDate(parsed!!))
    }

    @Test
    fun `getSpotType returns MOTORCYCLE for spots 1-5`() {
        assertEquals(SpotType.MOTORCYCLE, ParkingUtils.getSpotType(1))
        assertEquals(SpotType.MOTORCYCLE, ParkingUtils.getSpotType(3))
        assertEquals(SpotType.MOTORCYCLE, ParkingUtils.getSpotType(5))
    }

    @Test
    fun `getSpotType returns DISABLED for spots 6-7`() {
        assertEquals(SpotType.DISABLED, ParkingUtils.getSpotType(6))
        assertEquals(SpotType.DISABLED, ParkingUtils.getSpotType(7))
    }

    @Test
    fun `getSpotType returns ELECTRIC for spots 8-9`() {
        assertEquals(SpotType.ELECTRIC, ParkingUtils.getSpotType(8))
        assertEquals(SpotType.ELECTRIC, ParkingUtils.getSpotType(9))
    }

    @Test
    fun `getSpotType returns NORMAL for spots 10+`() {
        assertEquals(SpotType.NORMAL, ParkingUtils.getSpotType(10))
        assertEquals(SpotType.NORMAL, ParkingUtils.getSpotType(25))
        assertEquals(SpotType.NORMAL, ParkingUtils.getSpotType(50))
    }

    @Test
    fun `getSpotType returns NORMAL for spot 0 and negative`() {
        assertEquals(SpotType.NORMAL, ParkingUtils.getSpotType(0))
        assertEquals(SpotType.NORMAL, ParkingUtils.getSpotType(-1))
    }

    @Test
    fun `getSpotLabelRes returns correct resource ids`() {
        assertEquals(R.string.spot_type_normal, ParkingUtils.getSpotLabelRes(SpotType.NORMAL))
        assertEquals(R.string.spot_type_electric, ParkingUtils.getSpotLabelRes(SpotType.ELECTRIC))
        assertEquals(R.string.spot_type_motorcycle, ParkingUtils.getSpotLabelRes(SpotType.MOTORCYCLE))
        assertEquals(R.string.spot_type_disabled, ParkingUtils.getSpotLabelRes(SpotType.DISABLED))
    }

    @Test
    fun `getVehicleTypeLabelRes returns correct resource ids`() {
        assertEquals(R.string.spot_type_normal, ParkingUtils.getVehicleTypeLabelRes(VehicleType.CAR))
        assertEquals(R.string.spot_type_electric, ParkingUtils.getVehicleTypeLabelRes(VehicleType.ELECTRIC))
        assertEquals(R.string.spot_type_motorcycle, ParkingUtils.getVehicleTypeLabelRes(VehicleType.MOTORCYCLE))
        assertEquals(R.string.spot_type_disabled, ParkingUtils.getVehicleTypeLabelRes(VehicleType.DISABLED))
    }

    @Test
    fun `timeToMinutes converts correctly`() {
        assertEquals(0, ParkingUtils.timeToMinutes("00:00"))
        assertEquals(60, ParkingUtils.timeToMinutes("01:00"))
        assertEquals(90, ParkingUtils.timeToMinutes("01:30"))
        assertEquals(1439, ParkingUtils.timeToMinutes("23:59"))
    }

    @Test
    fun `minutesToTime converts correctly`() {
        assertEquals("00:00", ParkingUtils.minutesToTime(0))
        assertEquals("01:00", ParkingUtils.minutesToTime(60))
        assertEquals("01:30", ParkingUtils.minutesToTime(90))
        assertEquals("23:59", ParkingUtils.minutesToTime(1439))
    }

    @Test
    fun `timeToMinutes and minutesToTime are inverse`() {
        for (minutes in listOf(0, 1, 60, 90, 1439)) {
            assertEquals(minutes, ParkingUtils.timeToMinutes(ParkingUtils.minutesToTime(minutes)))
        }
    }

    @Test
    fun `addDays adds days correctly`() {
        assertEquals("2026-07-03", ParkingUtils.addDays("2026-07-01", 2))
        assertEquals("2026-06-30", ParkingUtils.addDays("2026-07-01", -1))
    }

    @Test
    fun `addDays handles month and year boundaries`() {
        assertEquals("2026-08-01", ParkingUtils.addDays("2026-07-31", 1))
        assertEquals("2027-01-01", ParkingUtils.addDays("2026-12-31", 1))
    }

    @Test
    fun `addDays with 0 returns same date`() {
        assertEquals("2026-07-01", ParkingUtils.addDays("2026-07-01", 0))
    }

    @Test
    fun `isMidnightCrossing returns true when end is before start`() {
        assertTrue(ParkingUtils.isMidnightCrossing("22:00", "02:00"))
        assertTrue(ParkingUtils.isMidnightCrossing("23:00", "01:00"))
    }

    @Test
    fun `isMidnightCrossing returns false when end is after start`() {
        assertFalse(ParkingUtils.isMidnightCrossing("08:00", "10:00"))
        assertFalse(ParkingUtils.isMidnightCrossing("00:00", "23:59"))
    }

    @Test
    fun `isMidnightCrossing returns false when times are equal`() {
        assertFalse(ParkingUtils.isMidnightCrossing("10:00", "10:00"))
    }

    @Test
    fun `calculateDurationMinutes for normal range`() {
        assertEquals(120, ParkingUtils.calculateDurationMinutes("08:00", "10:00"))
        assertEquals(30, ParkingUtils.calculateDurationMinutes("09:00", "09:30"))
    }

    @Test
    fun `calculateDurationMinutes for midnight crossing`() {
        assertEquals(240, ParkingUtils.calculateDurationMinutes("22:00", "02:00"))
        assertEquals(120, ParkingUtils.calculateDurationMinutes("23:00", "01:00"))
    }

    @Test
    fun `isTimeOverlapping different dates returns false`() {
        assertFalse(ParkingUtils.isTimeOverlapping("2026-07-01", "08:00", "10:00", "2026-07-02", "08:00", "10:00"))
    }

    @Test
    fun `isTimeOverlapping same time range returns true`() {
        assertTrue(ParkingUtils.isTimeOverlapping("2026-07-01", "08:00", "10:00", "2026-07-01", "08:00", "10:00"))
    }

    @Test
    fun `isTimeOverlapping start1 inside range2 returns true`() {
        assertTrue(ParkingUtils.isTimeOverlapping("2026-07-01", "09:00", "11:00", "2026-07-01", "08:00", "10:00"))
    }

    @Test
    fun `isTimeOverlapping end1 inside range2 returns true`() {
        assertTrue(ParkingUtils.isTimeOverlapping("2026-07-01", "07:00", "09:00", "2026-07-01", "08:00", "10:00"))
    }

    @Test
    fun `isTimeOverlapping range1 contains range2 returns true`() {
        assertTrue(ParkingUtils.isTimeOverlapping("2026-07-01", "07:00", "12:00", "2026-07-01", "08:00", "10:00"))
    }

    @Test
    fun `isTimeOverlapping non-overlapping ranges returns false`() {
        assertFalse(ParkingUtils.isTimeOverlapping("2026-07-01", "08:00", "09:00", "2026-07-01", "10:00", "11:00"))
    }

    @Test
    fun `isTimeOverlapping adjacent ranges returns false`() {
        assertFalse(ParkingUtils.isTimeOverlapping("2026-07-01", "08:00", "10:00", "2026-07-01", "10:00", "12:00"))
    }

    @Test
    fun `isVehicleAllowedInSpot motorcycle only in motorcycle spot`() {
        assertTrue(ParkingUtils.isVehicleAllowedInSpot(1, VehicleType.MOTORCYCLE))
        assertFalse(ParkingUtils.isVehicleAllowedInSpot(6, VehicleType.MOTORCYCLE))
        assertFalse(ParkingUtils.isVehicleAllowedInSpot(8, VehicleType.MOTORCYCLE))
        assertFalse(ParkingUtils.isVehicleAllowedInSpot(10, VehicleType.MOTORCYCLE))
    }

    @Test
    fun `isVehicleAllowedInSpot disabled only in disabled spot`() {
        assertFalse(ParkingUtils.isVehicleAllowedInSpot(1, VehicleType.DISABLED))
        assertTrue(ParkingUtils.isVehicleAllowedInSpot(6, VehicleType.DISABLED))
        assertFalse(ParkingUtils.isVehicleAllowedInSpot(8, VehicleType.DISABLED))
        assertTrue(ParkingUtils.isVehicleAllowedInSpot(10, VehicleType.DISABLED))
    }

    @Test
    fun `isVehicleAllowedInSpot electric only in electric or normal spots`() {
        assertFalse(ParkingUtils.isVehicleAllowedInSpot(1, VehicleType.ELECTRIC))
        assertFalse(ParkingUtils.isVehicleAllowedInSpot(6, VehicleType.ELECTRIC))
        assertTrue(ParkingUtils.isVehicleAllowedInSpot(8, VehicleType.ELECTRIC))
        assertTrue(ParkingUtils.isVehicleAllowedInSpot(10, VehicleType.ELECTRIC))
    }

    @Test
    fun `isVehicleAllowedInSpot car only in normal spots`() {
        assertFalse(ParkingUtils.isVehicleAllowedInSpot(1, VehicleType.CAR))
        assertFalse(ParkingUtils.isVehicleAllowedInSpot(6, VehicleType.CAR))
        assertFalse(ParkingUtils.isVehicleAllowedInSpot(8, VehicleType.CAR))
        assertTrue(ParkingUtils.isVehicleAllowedInSpot(10, VehicleType.CAR))
    }
}
