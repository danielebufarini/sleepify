package it.danielebufarini.sleepify

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume

object IosSleepify {
    fun createSession(): SleepifySession {
        val lifecycle = LifecycleRegistry()
        lifecycle.resume()
        return SleepifySession(DefaultComponentContext(lifecycle), onClose = lifecycle::destroy)
    }
}
