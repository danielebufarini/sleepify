package it.danielebufarini.sleepify

import kotlinx.serialization.Serializable

@Serializable
enum class SleepMode { BEDTIME, WAKE_UP }

@Serializable
data class SleepSettings(
    val mode: SleepMode = SleepMode.BEDTIME,
    val referenceMinutes: Int = 7 * 60,
    val latencyMinutes: Int = 15,
    val minCycles: Int = 3,
    val maxCycles: Int = 6,
) {
    init {
        require(referenceMinutes in 0 until 1440)
        require(latencyMinutes in 0..45 && latencyMinutes % 5 == 0)
        require(minCycles in 2..5)
        require(maxCycles in 4..8 && maxCycles >= minCycles)
    }
}

data class SleepResult(
    val cycles: Int,
    val sleepMinutes: Int,
    val timeMinutes: Int,
    /** Relative to the reference time's day; calculations use wall-clock minutes. */
    val dayOffset: Int,
) {
    val timeText: String get() = formatTime(timeMinutes)
    val durationText: String get() = formatDuration(sleepMinutes)
}

data class CalculatorState(val settings: SleepSettings, val results: List<SleepResult>)

object SleepCalculator {
    fun calculate(settings: SleepSettings): List<SleepResult> {
        val cycles = (settings.minCycles..settings.maxCycles).toList().let {
            if (settings.mode == SleepMode.BEDTIME) it.reversed() else it
        }
        return cycles.map { count ->
            val sleep = count * 90
            val delta = sleep + settings.latencyMinutes
            val total = settings.referenceMinutes + if (settings.mode == SleepMode.BEDTIME) -delta else delta
            val normalized = ((total % 1440) + 1440) % 1440
            SleepResult(count, sleep, normalized, (total - normalized) / 1440)
        }
    }
}

internal fun formatTime(minutes: Int): String =
    "${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"

internal fun formatDuration(minutes: Int): String = when {
    minutes % 60 == 0 -> "${minutes / 60} h"
    minutes < 60 -> "$minutes min"
    else -> "${minutes / 60} h ${minutes % 60} min"
}
