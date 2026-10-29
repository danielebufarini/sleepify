package it.danielebufarini.sleepify

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Single authority for calculator inputs and results. Hosts only render and forward actions. */
class SleepifySession internal constructor(
    context: ComponentContext,
    private val onClose: () -> Unit = {},
) {
    private val stateKeeper = context.stateKeeper
    private val initial = stateKeeper.consume(STATE_KEY, SleepSettings.serializer()) ?: SleepSettings()
    private val mutableState = MutableStateFlow(CalculatorState(initial, SleepCalculator.calculate(initial)))
    val state: StateFlow<CalculatorState> = mutableState.asStateFlow()
    private var closed = false

    init {
        stateKeeper.register(STATE_KEY, SleepSettings.serializer()) { mutableState.value.settings }
    }

    fun setMode(mode: SleepMode) = update {
        it.copy(mode = mode, referenceMinutes = if (mode == SleepMode.BEDTIME) 7 * 60 else 23 * 60)
    }

    fun setTime(hour: Int, minute: Int) {
        require(hour in 0..23 && minute in 0..59)
        update { it.copy(referenceMinutes = hour * 60 + minute) }
    }

    fun setLatency(minutes: Int) = update { it.copy(latencyMinutes = minutes) }

    fun setMinCycles(cycles: Int) = update {
        it.copy(minCycles = cycles, maxCycles = maxOf(cycles, it.maxCycles))
    }

    // Match the HTML's normalization: if max falls below min, max is raised to min.
    fun setMaxCycles(cycles: Int) {
        require(cycles in 4..8)
        update { it.copy(maxCycles = maxOf(it.minCycles, cycles)) }
    }

    private inline fun update(transform: (SleepSettings) -> SleepSettings) {
        if (closed) return
        val settings = transform(mutableState.value.settings)
        mutableState.value = CalculatorState(settings, SleepCalculator.calculate(settings))
    }

    fun close() {
        if (closed) return
        closed = true
        stateKeeper.unregister(STATE_KEY)
        onClose()
    }

    private companion object { const val STATE_KEY = "sleep-calculator" }
}
