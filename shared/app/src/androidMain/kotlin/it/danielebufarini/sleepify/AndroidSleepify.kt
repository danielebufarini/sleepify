package it.danielebufarini.sleepify

import com.arkivanov.decompose.ComponentContext

fun createSleepifySession(context: ComponentContext): SleepifySession = SleepifySession(context)
