package it.danielebufarini.sleepify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext

class MainActivity : ComponentActivity() {
    private lateinit var session: SleepifySession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        session = createSleepifySession(defaultComponentContext())
        setContent { SleepifyScreen(session) }
    }

    override fun onDestroy() {
        session.close()
        super.onDestroy()
    }
}
