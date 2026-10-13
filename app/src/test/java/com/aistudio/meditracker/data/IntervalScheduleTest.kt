package com.aistudio.meditracker.data

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntervalScheduleTest {

    private val zone = ZoneId.of("Europe/Kyiv")

    private fun millis(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun due(type: String, start: LocalDate, interval: Int, on: LocalDate) =
        isDueOn(type, millis(start), interval, on, zone)

    @Test
    fun dailyIsDueEveryDay() {
        val start = LocalDate.of(2026, 1, 10)
        for (offset in 0L..10L) assertTrue(due("daily", start, 1, start.plusDays(offset)))
    }

    @Test
    fun intervalIsDueOnStartDayThenEveryNDays() {
        val start = LocalDate.of(2026, 1, 10)
        assertTrue(due("interval", start, 3, start))
        assertFalse(due("interval", start, 3, start.plusDays(1)))
        assertFalse(due("interval", start, 3, start.plusDays(2)))
        assertTrue(due("interval", start, 3, start.plusDays(3)))
        assertTrue(due("interval", start, 3, start.plusDays(6)))
    }

    @Test
    fun intervalIsNeverDueBeforeStart() {
        val start = LocalDate.of(2026, 1, 10)
        assertFalse(due("interval", start, 2, start.minusDays(2)))
        assertFalse(due("interval", start, 2, start.minusDays(1)))
    }

    @Test
    fun nonPositiveIntervalFallsBackToDaily() {
        val start = LocalDate.of(2026, 1, 10)
        assertTrue(due("interval", start, 0, start.plusDays(1)))
        assertTrue(due("interval", start, -5, start.plusDays(2)))
    }

    @Test
    fun intervalWorksAcrossMonthBoundary() {
        val start = LocalDate.of(2026, 1, 30)
        assertTrue(due("interval", start, 2, LocalDate.of(2026, 2, 1)))
        assertFalse(due("interval", start, 2, LocalDate.of(2026, 2, 2)))
    }

    @Test
    fun intervalSurvivesDaylightSavingChange() {
        // Europe/Kyiv switches to summer time on 2026-03-29.
        val start = LocalDate.of(2026, 3, 27)
        assertTrue(due("interval", start, 2, LocalDate.of(2026, 3, 29)))
        assertFalse(due("interval", start, 2, LocalDate.of(2026, 3, 30)))
        assertTrue(due("interval", start, 2, LocalDate.of(2026, 3, 31)))
    }

    @Test
    fun exactlyEveryNthDayInAYear() {
        val start = LocalDate.of(2026, 1, 1)
        val dueDays = (0L until 365L).count { due("interval", start, 7, start.plusDays(it)) }
        assertEquals(53, dueDays)
    }
}
