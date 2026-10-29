package it.danielebufarini.sleepify

import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

private val Night = Color(0xFF101827)
private val Panel = Color(0xFF192437)
private val Raised = Color(0xFF222F45)
private val Lavender = Color(0xFFB6A8FF)
private val Muted = Color(0xFFACBAD0)
private val Outline = Color(0xFF35435A)
private val Text = Color(0xFFF3F6FC)

@Composable
fun SleepifyScreen(session: SleepifySession) {
    val state by session.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val bedtime = settings.mode == SleepMode.BEDTIME
    val context = LocalContext.current
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Lavender, onPrimary = Color(0xFF20183C),
        background = Night, surface = Panel, onSurface = Text,
        onSurfaceVariant = Muted, outline = Outline, secondaryContainer = Raised,
    )) {
        Surface(color = Night) {
            Box(
                Modifier.fillMaxSize().background(Brush.verticalGradient(
                    listOf(Color(0xFF253052), Night, Night), endY = 1050f,
                )), contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    Modifier.widthIn(max = 600.dp).fillMaxWidth().safeDrawingPadding()
                        .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Header()
                    SleepCard {
                        Text(stringResource(R.string.mode_label), style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ModeButton(stringResource(R.string.bedtime), R.drawable.bed, bedtime,
                                Modifier.weight(1f).testTag("mode_bedtime")) { session.setMode(SleepMode.BEDTIME) }
                            ModeButton(stringResource(R.string.wake_up), R.drawable.alarm, !bedtime,
                                Modifier.weight(1f).testTag("mode_wake")) { session.setMode(SleepMode.WAKE_UP) }
                        }
                        Spacer(Modifier.height(4.dp))
                        val timeLabel = stringResource(if (bedtime) R.string.wake_time else R.string.bed_time)
                        Text(timeLabel, style = MaterialTheme.typography.titleSmall, color = Muted)
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(context, { _, hour, minute -> session.setTime(hour, minute) },
                                    settings.referenceMinutes / 60, settings.referenceMinutes % 60, true).show()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("reference_time")
                                .semantics { contentDescription = timeLabel },
                            shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(12.dp),
                        ) {
                            Text(timeText(settings.referenceMinutes), fontSize = 44.sp,
                                fontWeight = FontWeight.Light, letterSpacing = 2.sp)
                            Spacer(Modifier.width(16.dp))
                            Icon(painterResource(R.drawable.alarm), null, Modifier.size(22.dp))
                        }
                        if (bedtime) {
                            Button(onClick = { setSystemAlarm(context, settings.referenceMinutes) },
                                modifier = Modifier.fillMaxWidth().testTag("set_reference_alarm")) {
                                Icon(painterResource(R.drawable.alarm), null, Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.set_alarm, timeText(settings.referenceMinutes)))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.latency), Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.minutes, settings.latencyMinutes), color = Lavender,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("latency_value"))
                        }
                        val latencyLabel = stringResource(R.string.latency)
                        Slider(value = settings.latencyMinutes.toFloat(),
                            onValueChange = { session.setLatency((it / 5).roundToInt() * 5) },
                            valueRange = 0f..45f, steps = 8,
                            modifier = Modifier.fillMaxWidth().testTag("latency")
                                .semantics { contentDescription = latencyLabel })
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf(0, 15, 30, 45).forEach { Text("$it min", color = Muted, fontSize = 12.sp) }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CyclePicker(stringResource(R.string.minimum_cycles), settings.minCycles, 2..5,
                                "min_cycles", Modifier.weight(1f), session::setMinCycles)
                            CyclePicker(stringResource(R.string.maximum_cycles), settings.maxCycles, 4..8,
                                "max_cycles", Modifier.weight(1f), session::setMaxCycles)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth().testTag("results_heading"),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(if (bedtime) R.string.bedtime_results else R.string.wake_results),
                                Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(8.dp))
                            Text(pluralStringResource(R.plurals.result_count, state.results.size, state.results.size),
                                style = MaterialTheme.typography.labelMedium, color = Muted)
                        }
                        state.results.forEach { result ->
                            val copiedMessage = stringResource(R.string.copied, result.timeText)
                            ResultRow(result,
                                onAlarm = if (bedtime) null else ({ setSystemAlarm(context, result.timeMinutes) }),
                            ) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Sleepify", result.timeText))
                                // Android 13+ already displays a system clipboard confirmation.
                                if (Build.VERSION.SDK_INT < 33) Toast.makeText(context,
                                    copiedMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.estimate_title), color = Lavender,
                            style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(R.string.estimate_note), color = Muted,
                            style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = Outline)
                        Text(stringResource(R.string.disclaimer), color = Muted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painterResource(R.drawable.moon), null,
            Modifier.size(60.dp).padding(bottom = 8.dp), tint = Lavender)
        Text("Sleepify", fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
        Text(stringResource(R.string.calculator_title), color = Text, style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp))
        Text(stringResource(R.string.subtitle), color = Muted, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun SleepCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().border(1.dp, Outline, RoundedCornerShape(24.dp))
        .background(Panel, RoundedCornerShape(24.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun ModeButton(label: String, icon: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick, modifier = modifier.heightIn(min = 64.dp).semantics { this.selected = selected },
        shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (selected) Lavender else Raised,
            contentColor = if (selected) Color(0xFF20183C) else Muted,
        )) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(painterResource(icon), null, Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CyclePicker(label: String, selected: Int, range: IntRange, tag: String,
    modifier: Modifier, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Muted)
        Box {
            OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag(tag).semantics { contentDescription = label }) {
                Text(stringResource(R.string.cycles, selected), Modifier.weight(1f))
                Text("⌄", fontSize = 18.sp)
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                range.forEach { count ->
                    DropdownMenuItem(text = { Text(stringResource(R.string.cycles, count)) },
                        onClick = { onSelect(count); expanded = false }, modifier = Modifier.testTag("${tag}_$count"))
                }
            }
        }
    }
}

@Composable
private fun ResultRow(result: SleepResult, onAlarm: (() -> Unit)?, onCopy: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Raised, RoundedCornerShape(18.dp)).padding(start = 18.dp, end = 8.dp,
        top = 14.dp, bottom = 14.dp).testTag("result_${result.cycles}"), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(result.timeText, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            }
            Text(stringResource(R.string.result_detail, result.cycles, result.durationText),
                color = Muted, style = MaterialTheme.typography.bodySmall)
            Text(stringResource(when {
                result.dayOffset < 0 -> R.string.previous_day
                result.dayOffset > 0 -> R.string.next_day
                else -> R.string.same_day
            }), color = Lavender, style = MaterialTheme.typography.labelSmall)
        }
        Column {
            if (onAlarm != null) {
                IconButton(onAlarm, Modifier.testTag("alarm_${result.cycles}")) {
                    Icon(painterResource(R.drawable.alarm), stringResource(R.string.set_alarm, result.timeText),
                        tint = Lavender)
                }
            }
            IconButton(onCopy, Modifier.testTag("copy_${result.cycles}")) {
                Icon(painterResource(R.drawable.copy), stringResource(R.string.copy_time, result.timeText), tint = Lavender)
            }
        }
    }
}

private fun timeText(minutes: Int) =
    "${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"
