package it.danielebufarini.sleepify

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import kotlin.test.*

class SleepCalculatorTest {
    @Test fun htmlDefaultBedtimes() {
        val results = SleepCalculator.calculate(SleepSettings())
        assertEquals(listOf("21:45", "23:15", "00:45", "02:15"), results.map { it.timeText })
        assertEquals(listOf(6, 5, 4, 3), results.map { it.cycles })
        assertEquals(listOf(-1, -1, 0, 0), results.map { it.dayOffset })
        assertEquals(listOf("9 h", "7 h 30 min", "6 h", "4 h 30 min"), results.map { it.durationText })
    }

    @Test fun htmlDefaultWakeTimes() {
        val results = SleepCalculator.calculate(SleepSettings(mode = SleepMode.WAKE_UP, referenceMinutes = 23 * 60))
        assertEquals(listOf("03:45", "05:15", "06:45", "08:15"), results.map { it.timeText })
        assertEquals(listOf(3, 4, 5, 6), results.map { it.cycles })
        assertTrue(results.all { it.dayOffset == 1 })
    }

    @Test fun zeroLatencyAtMidnight() {
        val results = SleepCalculator.calculate(SleepSettings(referenceMinutes = 0, latencyMinutes = 0))
        assertEquals(listOf("15:00", "16:30", "18:00", "19:30"), results.map { it.timeText })
        assertTrue(results.all { it.dayOffset == -1 })
    }

    @Test fun maximumLatencyAndCycleRange() {
        val results = SleepCalculator.calculate(SleepSettings(mode = SleepMode.WAKE_UP,
            referenceMinutes = 23 * 60 + 59, latencyMinutes = 45, minCycles = 2, maxCycles = 8))
        assertEquals(7, results.size)
        assertEquals("03:44", results.first().timeText)
        assertEquals("12:44", results.last().timeText)
        assertEquals(720, results.last().sleepMinutes)
    }

    @Test fun exactMidnightHasCorrectDay() {
        val result = SleepCalculator.calculate(SleepSettings(mode = SleepMode.WAKE_UP,
            referenceMinutes = 18 * 60, latencyMinutes = 0, minCycles = 4, maxCycles = 4)).single()
        assertEquals("00:00", result.timeText)
        assertEquals(1, result.dayOffset)
        val bedtime = SleepCalculator.calculate(SleepSettings(referenceMinutes = 6 * 60,
            latencyMinutes = 0, minCycles = 4, maxCycles = 4)).single()
        assertEquals("00:00", bedtime.timeText)
        assertEquals(0, bedtime.dayOffset)
    }

    @Test fun chronologicalOrderAcrossMidnight() {
        val results = SleepCalculator.calculate(SleepSettings(referenceMinutes = 7 * 60))
        assertEquals(listOf("21:45", "23:15", "00:45", "02:15"), results.map { it.timeText })
        assertTrue(results.zipWithNext().all { (a, b) ->
            a.timeMinutes + a.dayOffset * 1440 < b.timeMinutes + b.dayOffset * 1440
        })
    }

    @Test fun wallClockArithmeticForEveryMinuteAndLatency() {
        for (mode in SleepMode.entries) for (reference in 0 until 1440) for (latency in 0..45 step 5) {
            val settings = SleepSettings(mode, reference, latency, 2, 8)
            val results = SleepCalculator.calculate(settings)
            assertEquals(7, results.size)
            for (result in results) {
                assertTrue(result.timeMinutes in 0 until 1440)
                val absolute = result.timeMinutes + result.dayOffset * 1440
                val elapsed = if (mode == SleepMode.BEDTIME) reference - absolute else absolute - reference
                assertEquals(result.cycles * 90 + latency, elapsed)
                assertEquals(5, result.timeText.length)
            }
        }
    }

    @Test fun modeSwitchResetsReferenceAsInHtml() {
        val session = session()
        session.setTime(9, 42)
        session.setLatency(30)
        session.setMode(SleepMode.WAKE_UP)
        assertEquals(23 * 60, session.state.value.settings.referenceMinutes)
        assertEquals(30, session.state.value.settings.latencyMinutes)
        session.setMode(SleepMode.BEDTIME)
        assertEquals(7 * 60, session.state.value.settings.referenceMinutes)
    }

    @Test fun inputsImmediatelyRecalculate() {
        val session = session()
        session.setTime(8, 10)
        session.setLatency(45)
        assertEquals("22:25", session.state.value.results.first().timeText)
    }

    @Test fun minimumRaisesMaximum() {
        val session = session()
        session.setMaxCycles(4)
        session.setMinCycles(5)
        assertEquals(5, session.state.value.settings.maxCycles)
        assertEquals(1, session.state.value.results.size)
        assertEquals(5, session.state.value.results.single().cycles)
    }

    @Test fun lowerMaximumIsNormalizedAsInHtml() {
        val session = session()
        session.setMinCycles(5)
        session.setMaxCycles(4)
        assertEquals(5, session.state.value.settings.maxCycles)
        assertEquals(1, session.state.value.results.size)
    }

    @Test fun decomposeRestoresAllInputs() {
        val keeper = StateKeeperDispatcher()
        val original = SleepifySession(DefaultComponentContext(LifecycleRegistry(), stateKeeper = keeper))
        original.setMode(SleepMode.WAKE_UP)
        original.setTime(1, 37)
        original.setLatency(40)
        original.setMinCycles(2)
        original.setMaxCycles(8)
        val saved = keeper.save()
        val restored = SleepifySession(DefaultComponentContext(LifecycleRegistry(),
            stateKeeper = StateKeeperDispatcher(saved)))
        assertEquals(original.state.value, restored.state.value)
    }

    @Test fun closeIsIdempotentAndStopsUpdates() {
        var closed = 0
        val session = SleepifySession(DefaultComponentContext(LifecycleRegistry())) { closed++ }
        val before = session.state.value
        session.close()
        session.close()
        session.setMode(SleepMode.WAKE_UP)
        assertEquals(1, closed)
        assertEquals(before, session.state.value)
    }

    @Test fun malformedInputsAreRejected() {
        assertFailsWith<IllegalArgumentException> { SleepSettings(referenceMinutes = 1440) }
        assertFailsWith<IllegalArgumentException> { SleepSettings(latencyMinutes = 7) }
        assertFailsWith<IllegalArgumentException> { SleepSettings(latencyMinutes = 50) }
        assertFailsWith<IllegalArgumentException> { SleepSettings(minCycles = 1) }
        assertFailsWith<IllegalArgumentException> { SleepSettings(minCycles = 5, maxCycles = 4) }
        assertFailsWith<IllegalArgumentException> { session().setTime(24, 0) }
        assertFailsWith<IllegalArgumentException> { session().setTime(0, -1) }
        assertFailsWith<IllegalArgumentException> { session().setMaxCycles(3) }
    }

    private fun session() = SleepifySession(DefaultComponentContext(LifecycleRegistry()))
}
