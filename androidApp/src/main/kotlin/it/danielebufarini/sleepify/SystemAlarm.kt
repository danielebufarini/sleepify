package it.danielebufarini.sleepify

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast

/** Let the user's clock app create and manage the alarm. */
internal fun setSystemAlarm(context: Context, minutes: Int) {
    require(minutes in 0 until 1440)
    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
        putExtra(AlarmClock.EXTRA_HOUR, minutes / 60)
        putExtra(AlarmClock.EXTRA_MINUTES, minutes % 60)
        putExtra(AlarmClock.EXTRA_MESSAGE, context.getString(R.string.app_name))
        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.alarm_unavailable, Toast.LENGTH_LONG).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, R.string.alarm_unavailable, Toast.LENGTH_LONG).show()
    }
}
