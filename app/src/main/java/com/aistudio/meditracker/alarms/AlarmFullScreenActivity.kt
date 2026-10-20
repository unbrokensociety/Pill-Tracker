package com.aistudio.meditracker.alarms

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.meditracker.R
import com.aistudio.meditracker.ui.locale.LocaleHelper
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Full-screen alarm presentation for a medication reminder. Launched by the
// notification's full-screen intent: turns the display on over the lock
// screen and rings on the alarm stream. It is built not to wear the user out:
// the volume rises gradually, and the sound stops by itself after
// RING_LIMIT_MS (or at once on "Silence"). The reminder stays on screen and in
// the notification shade, and AlarmReceiver sends at most two quiet follow-ups.
class AlarmFullScreenActivity : ComponentActivity() {

    private companion object {
        const val RING_LIMIT_MS = 60_000L
        const val RAMP_STEPS = 20
        const val RAMP_STEP_MS = 500L
        const val RAMP_START = 0.15f
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var rampStep = 0
    private var silenced by mutableStateOf(false)

    private val rampRunnable = object : Runnable {
        override fun run() {
            rampStep++
            val volume = (RAMP_START + (1f - RAMP_START) * rampStep / RAMP_STEPS).coerceAtMost(1f)
            try {
                mediaPlayer?.setVolume(volume, volume)
            } catch (_: Exception) {
            }
            if (rampStep < RAMP_STEPS) handler.postDelayed(this, RAMP_STEP_MS)
        }
    }

    private val silenceRunnable = Runnable { silence() }

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleHelper.getLanguage(newBase)
        val contextWithLocale = LocaleHelper.updateResources(newBase, lang)
        super.attachBaseContext(contextWithLocale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val scheduleId = intent?.getIntExtra("EXTRA_SCHEDULE_ID", -1) ?: -1
        val medicationName = intent?.getStringExtra("EXTRA_MEDICATION_NAME") ?: ""

        startRinging()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = AlarmAccent,
                    background = Color.Black,
                    surface = Color(0xFF0C0D10)
                )
            ) {
                AlarmScreen(
                    medicationName = medicationName,
                    silenced = silenced,
                    onSilence = { silence() },
                    onTaken = {
                        fireAction(AlarmReceiver.ACTION_TAKEN, scheduleId, medicationName, 0)
                    },
                    onSnooze15 = {
                        fireAction(AlarmReceiver.ACTION_SNOOZE, scheduleId, medicationName, 15)
                    },
                    onSnooze30 = {
                        fireAction(AlarmReceiver.ACTION_SNOOZE, scheduleId, medicationName, 30)
                    }
                )
            }
        }
    }

    private fun fireAction(action: String, scheduleId: Int, medicationName: String, snoozeMinutes: Int) {
        stopRinging()
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra("EXTRA_SCHEDULE_ID", scheduleId)
            putExtra("EXTRA_MEDICATION_NAME", medicationName)
            if (snoozeMinutes > 0) {
                putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            }
        }
        sendBroadcast(intent)
        finish()
    }

    private fun startRinging() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            if (uri != null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@AlarmFullScreenActivity, uri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    setVolume(RAMP_START, RAMP_START)
                    prepare()
                    start()
                }
                rampStep = 0
                handler.postDelayed(rampRunnable, RAMP_STEP_MS)
                handler.postDelayed(silenceRunnable, RING_LIMIT_MS)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val vibe = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibe != null && vibe.hasVibrator()) {
                vibe.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 700), 0))
                vibrator = vibe
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Sound and vibration off, screen free to sleep; the reminder itself stays.
    private fun silence() {
        stopRinging()
        silenced = true
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun stopRinging() {
        handler.removeCallbacks(rampRunnable)
        handler.removeCallbacks(silenceRunnable)
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }
        vibrator = null
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }
}

private val AlarmAccent = Color(0xFF38BDF8)
private val AlarmOnAccent = Color(0xFF00263A)
private val AlarmText = Color(0xFFEDEEF2)
private val AlarmMuted = Color(0xFF9A9FAB)

@Composable
private fun AlarmScreen(
    medicationName: String,
    silenced: Boolean,
    onSilence: () -> Unit,
    onTaken: () -> Unit,
    onSnooze15: () -> Unit,
    onSnooze30: () -> Unit
) {
    val defaultName = stringResource(R.string.alarm_default_med)
    val finalName = if (medicationName.isBlank()) defaultName else medicationName
    val timeText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

    // A slow, small breathing motion while it rings; completely still once
    // the sound is off.
    val transition = rememberInfiniteTransition(label = "alarmPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alarmPulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = timeText,
                fontSize = 60.sp,
                fontWeight = FontWeight.Light,
                color = AlarmText
            )

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(92.dp)
                    .scale(if (silenced) 1f else pulse)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(AlarmAccent.copy(alpha = 0.28f), AlarmAccent.copy(alpha = 0.06f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Alarm,
                    contentDescription = null,
                    tint = AlarmAccent,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.alarm_title, finalName),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = AlarmText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(if (silenced) R.string.alarm_silenced else R.string.alarm_text),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = AlarmMuted
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onTaken,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlarmAccent,
                    contentColor = AlarmOnAccent
                )
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.notif_taken),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSnooze15,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlarmText)
                ) {
                    Text(text = stringResource(R.string.snooze_15m))
                }
                OutlinedButton(
                    onClick = onSnooze30,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlarmText)
                ) {
                    Text(text = stringResource(R.string.snooze_30m))
                }
            }

            if (!silenced) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onSilence) {
                    Text(
                        text = stringResource(R.string.alarm_silence),
                        color = AlarmMuted
                    )
                }
            }
        }
    }
}
