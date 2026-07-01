package com.lksnext.ParkingIMayordomo.data.model

import org.junit.Assert.*
import org.junit.Test

class ModelsTest {

    @Test
    fun `VehicleType fromString parses correctly`() {
        assertEquals(VehicleType.CAR, VehicleType.fromString("car"))
        assertEquals(VehicleType.ELECTRIC, VehicleType.fromString("electric"))
        assertEquals(VehicleType.MOTORCYCLE, VehicleType.fromString("motorcycle"))
        assertEquals(VehicleType.DISABLED, VehicleType.fromString("disabled"))
    }

    @Test
    fun `VehicleType fromString is case insensitive`() {
        assertEquals(VehicleType.CAR, VehicleType.fromString("CAR"))
        assertEquals(VehicleType.ELECTRIC, VehicleType.fromString("Electric"))
    }

    @Test
    fun `VehicleType fromString defaults to CAR for unknown`() {
        assertEquals(VehicleType.CAR, VehicleType.fromString("unknown"))
        assertEquals(VehicleType.CAR, VehicleType.fromString(""))
    }

    @Test
    fun `ReportStatus fromString parses English values`() {
        assertEquals(ReportStatus.PENDING, ReportStatus.fromString("PENDING"))
        assertEquals(ReportStatus.IN_REVIEW, ReportStatus.fromString("IN_REVIEW"))
        assertEquals(ReportStatus.RESOLVED, ReportStatus.fromString("RESOLVED"))
    }

    @Test
    fun `ReportStatus fromString parses Spanish values`() {
        assertEquals(ReportStatus.PENDING, ReportStatus.fromString("PENDIENTE"))
        assertEquals(ReportStatus.IN_REVIEW, ReportStatus.fromString("EN_REVISION"))
        assertEquals(ReportStatus.RESOLVED, ReportStatus.fromString("RESUELTO"))
    }

    @Test
    fun `ReportStatus fromString is case insensitive`() {
        assertEquals(ReportStatus.PENDING, ReportStatus.fromString("pending"))
        assertEquals(ReportStatus.IN_REVIEW, ReportStatus.fromString("en_revision"))
    }

    @Test
    fun `ReportStatus fromString defaults to PENDING for unknown`() {
        assertEquals(ReportStatus.PENDING, ReportStatus.fromString("UNKNOWN"))
        assertEquals(ReportStatus.PENDING, ReportStatus.fromString(""))
    }

    @Test
    fun `ReportStatus fromString handles null`() {
        assertEquals(ReportStatus.PENDING, ReportStatus.fromString(null))
    }
}
