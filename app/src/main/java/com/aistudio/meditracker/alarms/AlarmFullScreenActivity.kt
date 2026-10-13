package com.aistudio.meditracker.alarms

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
// screen and rings (alarm stream, looping) until the dose is taken or snoozed.
class AlarmFullScreenActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

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
                    primary = Color(0xFF2DD4BF),
                    background = Color(0xFF0A121F),
                    surface = Color(0xFF111C2E)
                )
            ) {
                AlarmScreen(
                    medicationName = medicationName,
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
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val vibe = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibe != null && vibe.hasVibrator()) {
                vibe.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 250, 600), 0))
                vibrator = vibe
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRinging() {
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

@Composable
private fun AlarmScreen(
    medicationName: String,
    onTaken: () -> Unit,
    onSnooze15: () -> Unit,
    onSnooze30: () -> Unit
) {
    val defaultName = stringResource(R.string.alarm_default_med)
    val finalName = if (medicationName.isBlank()) defaultName else medicationName
    val timeText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

    val transition = rememberInfiniteTransition(label = "alarmPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alarmPulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A121F), Color(0xFF102A3C), Color(0xFF0A121F))
                )
            )
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
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF2DD4BF)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF2DD4BF).copy(alpha = 0.35f), Color(0x142DD4BF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Alarm,
                    contentDescription = null,
                    tint = Color(0xFF2DD4BF),
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.alarm_title, finalName),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.alarm_text),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF9DB4C6)
            )

            Spacer(modifier = Modifier.height(44.dp))

            Button(
                onClick = onTaken,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2DD4BF),
                    contentColor = Color(0xFF062019)
                )
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.notif_taken),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSnooze15,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF9DB4C6)
                    )
                ) {
                    Text(text = stringResource(R.string.snooze_15m))
                }
                OutlinedButton(
                    onClick = onSnooze30,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF9DB4C6)
                    )
                ) {
                    Text(text = stringResource(R.string.snooze_30m))
                }
            }
        }
    }
}
