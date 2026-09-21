package com.aistudio.meditracker.ui.components

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.runtime.withFrameNanos
import kotlin.math.pow
import kotlinx.coroutines.delay

/* ------------------------------------------------------------------------- */
/*  LIQUID GLASS ENGINE v6 — AGSL edge refraction + intensity slider          */
/*                                                                           */
/*  v6 «iOS-style liquid glass»: on Android 13+ a per-pixel AGSL              */
/*  RuntimeShader pass bends the backdrop around the panel rim — real         */
/*  refraction streaks, chromatic aberration, a specular bevel that           */
/*  follows the light and glints as the panel moves. Older devices keep       */
/*  the v5 look (blur + zoom bleed).                                          */
/*                                                                           */
/*  One shared technique: a live snapshot of the content behind the panel     */
/*  is blurred and drawn back under the glass.                                */
/*    • Android 12+ : hardware gaussian blur (RenderEffect, CLAMP edges)      */
/*      applied to the recorded GraphicsLayer — full framerate, near-zero     */
/*      cost.                                                                 */
/*    • Android 8–11: the recorded layer is rendered to a tiny bitmap         */
/*      (¼ scale), blurred on the CPU and drawn back — re-snapshotted only    */
/*      when the content actually changed.                                    */
/*                                                                           */
/*  v4 «more liquid»: far thinner tint & scrim (the blur itself does the      */
/*  visual work), a second refractive edge-bleed pass, a richer aurora to     */
/*  refract, wider blur. And an ADAPTIVE QUALITY system:                      */
/*    FULL    → the whole effect,                                             */
/*    REDUCED → lighter blur, no bleed — for mid-range hardware,              */
/*    FROST   → recording & blur off — solid OPAQUE matte panel (zero       */
/*              see-through — the look never changes, ever).                  */
/*  The tier is picked automatically at start (CPU cores, RAM, low-RAM      */
/*  flag, Android version), then guarded live: a frame-time monitor steps     */
/*  down on sustained jank, and battery saver gates to FROST instantly.       */
/*  Weak phones never lag, strong ones shine — and the user can re-pick the   */
/*  mode in Settings at any time; a manual pick overrides everything.        */
/* ------------------------------------------------------------------------- */

/** True when the device supports hardware backdrop blur (Android 12+). */
val BackdropBlurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/* ------------------------------------------------------------------------- */
/*  Liquid Glass v6 — the AGSL edge shader (Android 13+)                      */
/* ------------------------------------------------------------------------- */

/** True when per-pixel AGSL edge refraction is available (Android 13+). */
private val AgslEdgeRefractionSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * AGSL source of the «thick glass» bevel — the pass that reads as REAL
 * liquid glass (the iOS look) instead of flat frost. Applied to a layer
 * holding a copy of the backdrop behind the panel:
 *
 *  • rounded-rect SDF → distance to the rim and the outward normal;
 *  • refraction: pixels near the rim sample the backdrop displaced
 *    OUTWARD along the normal — light bends through the thick bevel
 *    edge, streaking colours around the border (per-pixel, unlike the
 *    v5 global-zoom bleed);
 *  • chromatic aberration: R and B split along the normal at the rim;
 *  • specular bevel: the rim lights up on the side facing the (fixed)
 *    top-left light, shades on the opposite side, and a glint slides
 *    around the border as the panel moves (phase uniform);
 *  • saturation boost in the band — glass makes the backdrop pop;
 *  • the CENTER of the output is transparent, so the blurred frost
 *    layer below shows through: frosted middle + liquid clear bevel.
 *
 * Uniforms are driven live by the Settings intensity slider.
 */
private const val LIQUID_EDGE_AGSL = """
uniform shader content;
uniform float2 resolution;   // layer size, px
uniform float corner;        // corner radius, px
uniform float band;          // refractive edge band width, px
uniform float refraction;    // lens strength 0..1
uniform float chroma;        // chromatic aberration 0..1
uniform float specular;      // rim brightness 0..1
uniform float lightX;        // direction TO the light (normalized)
uniform float lightY;
uniform float phase;         // glint phase — driven by panel position
uniform float saturation;    // backdrop colour boost inside the band

float sdRoundBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 fragCoord) {
    float2 c = resolution * 0.5;
    float2 p = fragCoord - c;
    float d = sdRoundBox(p, c, corner);          // < 0 inside the shape
    float inDist = max(-d, 0.0);                 // distance from the rim in
    float t = clamp(inDist / band, 0.0, 1.0);    // 0 at rim -> 1 inner

    // outward normal = numeric SDF gradient
    float2 ex = float2(1.0, 0.0);
    float2 ey = float2(0.0, 1.0);
    float2 grad = float2(
        sdRoundBox(p + ex, c, corner) - sdRoundBox(p - ex, c, corner),
        sdRoundBox(p + ey, c, corner) - sdRoundBox(p - ey, c, corner));
    float2 n = grad / max(length(grad), 0.0001);

    // refraction: near the rim, sample the backdrop displaced outward
    float lens = pow(1.0 - t, 2.0);
    float2 uv = clamp(
        fragCoord + n * lens * refraction * band,
        float2(0.5), resolution - float2(0.5));
    float ca = chroma * lens * max(band * 0.045, 0.75);
    half4 col;
    col.r = content.eval(uv + n * ca).r;
    col.g = content.eval(uv).g;
    col.b = content.eval(uv - n * ca).b;
    col.a = 1.0;

    // saturation boost — glass makes the backdrop pop
    float3 rgb = float3(col.rgb);
    float l = dot(rgb, float3(0.2126, 0.7152, 0.0722));
    rgb = clamp(mix(float3(l), rgb, 1.0 + saturation * lens),
                float3(0.0), float3(1.0));

    // band mask: only the edge band is visible; the frosted blur below
    // keeps the centre
    float mask = 1.0 - smoothstep(0.45, 1.0, t);

    // specular bevel: lit side + moving glint + soft opposite shading
    float lambert = clamp(dot(n, float2(lightX, lightY)), 0.0, 1.0);
    float glint = 0.5 + 0.5 * sin(phase + (n.x * 1.2 + n.y * 0.8) * 1.7);
    float spec = pow(1.0 - t, 2.5) *
        (0.30 + 0.55 * pow(lambert, 2.0) +
         0.25 * glint * (0.35 + 0.65 * lambert)) * specular;
    float shade = pow(1.0 - t, 3.0) *
        pow(clamp(-dot(n, float2(lightX, lightY)), 0.0, 1.0), 1.5) * 0.30;

    rgb = rgb * (1.0 + spec * 1.1) + float3(spec * 0.30);
    rgb = rgb * (1.0 - shade);

    float a = clamp(mask + spec * 0.85, 0.0, 1.0);
    return half4(half3(rgb * a), half(a));   // premultiplied
}
"""

/** Creates (and remembers) the edge shader — Android 13+ only. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun rememberLiquidEdgeShader(): RuntimeShader =
    remember { RuntimeShader(LIQUID_EDGE_AGSL) }

/* ------------------------------------------------------------------------- */
/*  Adaptive quality — measured, not guessed                                  */
/* ------------------------------------------------------------------------- */

/**
 * Rendering tier of the liquid-glass system.
 *
 *  * [FULL]    — the full liquid look: live backdrop blur, refractive edge
 *                bleed, whisper-thin tint.
 *  * [REDUCED] — lighter blur, no bleed, slightly milkier tint. Chosen for
 *                mid-range hardware or after the runtime governor sees jank.
 *  * [FROST]   — no recording, no blur: a solid OPAQUE matte panel —
 *                nothing shows through, so it never changes while content
 *                scrolls behind it. For weak devices, battery saver, or
 *                persistent stutter.
 */
enum class LiquidGlassQuality { FULL, REDUCED, FROST }

/**
 * Global live state of the glass engine: written by [GlassPerformanceGovernor]
 * (automatic mode) or by the Settings picker (manual mode), read by
 * [glassSource] (recording gate), [LiquidGlassPanel] (render path) and the
 * Settings row.
 */
object LiquidGlassState {
    val quality = mutableStateOf(LiquidGlassQuality.FULL)

    /**
     * Mode the user picked manually in Settings (null = automatic — the
     * app picks by itself at start and keeps guarding it live). A manual
     * pick wins over EVERYTHING: the frame monitor and battery saver stop
     * managing the tier, what the user chose is what renders.
     */
    val userMode = mutableStateOf<LiquidGlassQuality?>(null)

    /**
     * HOW liquid the glass is, 0f..1f — the Settings slider.
     * 0f reads as an almost matte frosted panel, 1f is «very very liquid»:
     * near-zero tint, the widest blur and the strongest refractive bleed.
     * Applies to every non-FROST tier; FROST stays a fully opaque matte
     * panel no matter what. Lives in global state so panels, cards and
     * the slider all re-render LIVE while the thumb is dragged.
     */
    val intensity = mutableStateOf(GlassModeStore.DEFAULT_INTENSITY)
}

/**
 * Persists the user's manual liquid-glass mode pick so it survives restarts.
 * Kept in plain SharedPreferences on purpose: it lives inside the glass
 * engine module, so the whole feature ships as one flat file.
 */
object GlassModeStore {
    private const val PREFS = "liquid_glass"
    private const val KEY = "mode"
    private const val KEY_INTENSITY = "intensity"

    /** Slider default: noticeably liquid out of the box, room on both ends. */
    const val DEFAULT_INTENSITY = 0.75f

    fun loadMode(context: Context): LiquidGlassQuality? = try {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?.let { value -> runCatching { LiquidGlassQuality.valueOf(value) }.getOrNull() }
    } catch (t: Throwable) {
        null
    }

    fun loadIntensity(context: Context): Float = try {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat(KEY_INTENSITY, DEFAULT_INTENSITY)
            .coerceIn(0f, 1f)
    } catch (t: Throwable) {
        DEFAULT_INTENSITY
    }

    fun saveMode(context: Context, mode: LiquidGlassQuality?) = try {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, mode?.name)
            .apply()
    } catch (t: Throwable) {
        // persisting a visual preference must never crash the app
    }

    fun saveIntensity(context: Context, intensity: Float) = try {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_INTENSITY, intensity.coerceIn(0f, 1f))
            .apply()
    } catch (t: Throwable) {
        // persisting a visual preference must never crash the app
    }
}

/**
 * One-shot hardware assessment: CPU cores, RAM, the system low-RAM flag and
 * the Android version (hardware RenderEffect only exists on 12+). Cheap,
 * synchronous, runs once per process — this is the *starting* tier; the
 * runtime governor can only ever lower it (or gate it behind battery saver).
 */
object DeviceGlassPolicy {
    fun assess(context: Context): LiquidGlassQuality {
        return try {
            val am = context.getSystemService(ActivityManager::class.java)
                ?: return LiquidGlassQuality.FROST
            val mem = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mem)
            val ramGb = mem.totalMem / 1_000_000_000.0
            val cores = Runtime.getRuntime().availableProcessors()

            when {
                am.isLowRamDevice -> LiquidGlassQuality.FROST
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> when {
                    // hardware RenderEffect: modern mid-range and up run the
                    // full liquid look without breaking a sweat
                    cores >= 4 && ramGb >= 2.5 -> LiquidGlassQuality.FULL
                    cores >= 4 -> LiquidGlassQuality.REDUCED
                    else -> LiquidGlassQuality.FROST
                }
                else -> when {
                    // pre-12 the blur runs on the CPU (quarter-scale,
                    // throttled, change-gated) — only strong old flagships
                    // get it at all
                    cores >= 8 && ramGb >= 6.0 -> LiquidGlassQuality.REDUCED
                    else -> LiquidGlassQuality.FROST
                }
            }
        } catch (t: Throwable) {
            LiquidGlassQuality.FROST
        }
    }
}

/**
 * Runtime governor for the liquid-glass engine. Runs an invisible,
 * allocation-free frame monitor alongside the app:
 *
 *  0. the user is the boss: a mode picked manually in Settings (persisted
 *     via [GlassModeStore]) wins over everything — the governor bows out
 *     entirely and never touches the tier again;
 *  1. otherwise it starts from [DeviceGlassPolicy] (measured at start);
 *  2. watches real frame timestamps — when frames keep being missed
 *     (sustained: two full windows in a row, so one-off bursts like confetti
 *     or dialogs never trigger it), the tier steps down
 *     FULL → REDUCED → FROST and never oscillates back during the session;
 *  3. gates everything behind battery saver: saver on → FROST instantly,
 *     saver off → back to whatever the measurements allow.
 *
 * The monitor is free while the screen is idle — [withFrameNanos] only
 * ticks when Compose actually produces frames.
 */
@Composable
fun GlassPerformanceGovernor(backdrop: GlassBackdrop) {
    val context = LocalContext.current
    val policyLevel = remember { DeviceGlassPolicy.assess(context) }

    LaunchedEffect(backdrop) {
        // ---- 0a. saved slider position applies to EVERYONE (manual or
        //      automatic mode) — it's a look preference, not a policy ----
        LiquidGlassState.intensity.value = GlassModeStore.loadIntensity(context)

        // ---- 0b. manual pick from Settings: the user's word is final ----
        val savedMode = GlassModeStore.loadMode(context)
        if (savedMode != null) {
            LiquidGlassState.userMode.value = savedMode
            LiquidGlassState.quality.value = savedMode
            return@LaunchedEffect
        }

        // ---- 1. static assessment first ----
        var level = policyLevel
        var saverOn = false
        var lastSaverCheck = 0L
        val powerManager = try {
            context.getSystemService(PowerManager::class.java)
        } catch (t: Throwable) {
            null
        }

        fun applyQuality() {
            LiquidGlassState.quality.value =
                if (saverOn) LiquidGlassQuality.FROST else level
        }

        LiquidGlassState.quality.value = level

        // ---- 2. live frame-time monitor ----
        val windowSize = 120                 // ~2 s of frames at 60 Hz
        val deltas = LongArray(windowSize)   // ring buffer, zero allocs
        var samples = 0
        var index = 0
        var badWindows = 0
        var cooldown = 0                     // settle frames after a switch
        val warmup = 90                      // cold start is always messy
        var warmupLeft = warmup
        var prevNanos = 0L

        while (true) {
            val nanos = withFrameNanos { it }

            // the user just picked a mode in Settings — their word is final,
            // the automatic management stops right here and now
            if (LiquidGlassState.userMode.value != null) return@LaunchedEffect

            val delta = if (prevNanos == 0L) 0L else nanos - prevNanos
            prevNanos = nanos

            // battery saver: re-checked every ~4 s, gates to FROST instantly
            if (nanos - lastSaverCheck > 4_000_000_000L) {
                lastSaverCheck = nanos
                val on = try {
                    powerManager?.isPowerSaveMode == true
                } catch (t: Throwable) {
                    false
                }
                if (on != saverOn) {
                    saverOn = on
                    applyQuality()
                    cooldown = 90
                }
            }

            if (warmupLeft > 0) { warmupLeft--; continue }
            // an idle gap between frame bursts is not jank — drop the window
            if (delta > 250_000_000L || delta <= 0L) { samples = 0; continue }
            // let a freshly-switched mode settle before judging it
            if (cooldown > 0) { cooldown--; continue }

            deltas[index] = delta
            index = (index + 1) % windowSize
            samples = (samples + 1).coerceAtMost(windowSize)

            if (samples == windowSize) {
                samples = 0
                index = 0

                // refresh interval ≈ 5th percentile of observed deltas:
                // frames physically cannot arrive faster than vsync allows,
                // so the fastest 5 % is a robust estimate of the panel rate
                val sorted = deltas.copyOf().also { it.sort() }
                val refresh = sorted[(windowSize * 0.05f).toInt()].coerceAtLeast(1L)

                var missed = 0            // dropped one vsync
                var heavy = 0             // dropped several vsyncs — counts double
                for (d in deltas) {
                    when {
                        d > refresh * 3.5f -> heavy += 2
                        d > refresh * 1.8f -> missed++
                    }
                }

                val bad = (missed + heavy) >= windowSize * 0.20f
                badWindows = if (bad) badWindows + 1 else 0

                if (badWindows >= 2 && level != LiquidGlassQuality.FROST) {
                    // sustained stutter — step the glass down one tier and
                    // give the new mode time to settle before measuring again
                    level = if (level == LiquidGlassQuality.FULL) {
                        LiquidGlassQuality.REDUCED
                    } else {
                        LiquidGlassQuality.FROST
                    }
                    applyQuality()
                    badWindows = 0
                    cooldown = 180
                }
            }
        }
    }
}

/**
 * Shared backdrop recorder. Attach [glassSource] to the content that should
 * appear blurred behind glass panels, and hand the same [GlassBackdrop] to
 * every [LiquidGlassPanel] that floats above it.
 */
class GlassBackdrop internal constructor(
    internal val layer: GraphicsLayer
) {
    /** Root position of the recorded node. */
    internal var origin: Offset? = null

    /** Size of the recorded node (in px). */
    internal var size: IntSize = IntSize.Zero

    /**
     * Bumped after every re-record of [layer]. Panels read it inside their
     * draw scope, so a redraw of the source content invalidates every glass
     * panel watching this backdrop — the blur stays live while scrolling
     * or animating, without any per-frame polling.
     */
    internal val version = mutableStateOf(0L)
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

/**
 * Records everything drawn inside the modified node into [backdrop] and keeps
 * drawing it normally. The layer is re-recorded on every draw pass, so glass
 * panels always show the live content behind them (scrolling lists,
 * animations, everything).
 */
fun Modifier.glassSource(backdrop: GlassBackdrop): Modifier =
    this.onGloballyPositioned { coords ->
        backdrop.origin = coords.positionInRoot()
        backdrop.size = coords.size
    }.drawWithCache {
        val layer = backdrop.layer
        onDrawWithContent {
            if (LiquidGlassState.quality.value == LiquidGlassQuality.FROST) {
                // economy tier: no panel needs the backdrop, so skip the
                // recording entirely — rendering stays at plain cost
                drawContent()
            } else {
                layer.record {
                    this@onDrawWithContent.drawContent()
                }
                drawLayer(layer)
                // signal live refresh to every panel drawing this backdrop
                backdrop.version.value += 1L
            }
        }
    }

/**
 * Soft ambient "aurora" gradient decor drawn behind app content.
 * Gives the liquid glass colorful content to refract and blur. All brushes
 * are built once per size (cached — zero allocations while scrolling).
 */
@Composable
fun Modifier.auroraBackdrop(): Modifier {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val blobAlpha = if (isDark) 0.36f else 0.28f
    val washTop = if (isDark) Color(0x24FFFFFF) else Color.White.copy(alpha = 0.42f)

    return this.drawWithCache {
        val w = size.width
        val h = size.height

        val wash = Brush.verticalGradient(
            colors = listOf(washTop, Color.Transparent, Color.Transparent),
            startY = 0f,
            endY = h * 0.55f
        )
        val blob1 = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = blobAlpha), Color.Transparent),
            center = Offset(0f, h * 0.08f),
            radius = w * 0.85f
        )
        val blob2 = Brush.radialGradient(
            colors = listOf(tertiary.copy(alpha = blobAlpha * 0.85f), Color.Transparent),
            center = Offset(w, h * 0.40f),
            radius = w * 0.75f
        )
        val blob3 = Brush.radialGradient(
            colors = listOf(secondary.copy(alpha = blobAlpha), Color.Transparent),
            center = Offset(w * 0.5f, h * 1.02f),
            radius = w * 0.95f
        )
        val blob4 = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = blobAlpha * 0.7f), Color.Transparent),
            center = Offset(w * 0.10f, h * 0.78f),
            radius = w * 0.55f
        )

        onDrawBehind {
            // subtle vertical wash so flat backgrounds gain depth
            drawRect(brush = wash)
            // rich color blobs — fuel the liquid glass blurs into soft washes
            drawCircle(brush = blob1, radius = w * 0.85f, center = Offset(0f, h * 0.08f))
            drawCircle(brush = blob2, radius = w * 0.75f, center = Offset(w, h * 0.40f))
            drawCircle(brush = blob3, radius = w * 0.95f, center = Offset(w * 0.5f, h * 1.02f))
            drawCircle(brush = blob4, radius = w * 0.55f, center = Offset(w * 0.10f, h * 0.78f))
        }
    }
}

/**
 * THE real liquid glass panel — live backdrop blur in a frosted-glass body.
 *
 * Draw order (all inside [shape]):
 *  1. blurred copy of the content recorded via [glassSource]
 *     (hardware path on Android 12+, software blur path below that);
 *     in FULL quality a second, wider-blurred and slightly zoomed copy is
 *     blended on top — the refractive edge bleed that makes the glass read
 *     as liquid rather than flat frost,
 *  2. whisper-light scrim + glass tint (kept thin so the blur does the
 *     visual work — v4 is noticeably more transparent than v3),
 *  3. thin specular rim.
 *  Content composables are laid out on top.
 *
 *  The panel follows the adaptive quality from [GlassPerformanceGovernor]:
 *  FROST skips the backdrop copy entirely and leans on a fully opaque matte
 *  surface — zero recording, zero blur, zero see-through, zero lag.
 *
 * @param backdrop shared backdrop recorded via [glassSource] on the background content.
 * @param blurRadius gaussian blur radius in dp — the CENTER of the slider
 *        range (the intensity scales it 0.55×–1.45× in FULL).
 * @param tint optional color cast drawn over the blur. Defaults to a very
 *        light theme surface tint — pass [Color.Transparent] for pure glass.
 */
@Composable
fun LiquidGlassPanel(
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(32.dp),
    elevation: Dp = 16.dp,
    blurRadius: Dp = 28.dp,
    tint: Color? = null,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val hardwareBlur = BackdropBlurSupported
    val quality by LiquidGlassState.quality

    // ---- manual intensity (the Settings slider): 0 = matte, 1 = ultra liquid.
    // A perceptual curve (pow 1.15) spends more of the travel on the range
    // where the eye actually notices the change. Read live from global
    // state — dragging the thumb re-renders the panel in real time.
    val rawIntensity by LiquidGlassState.intensity
    val iEff = rawIntensity.coerceIn(0f, 1f)
    val curve = iEff.pow(1.15f)

    // Effective blur radius: the slider widens/narrows it within the tier
    // (the adaptive governor may also run a lighter mode, or blur off).
    val effectiveBlur = when (quality) {
        LiquidGlassQuality.FULL -> blurRadius * lerp(0.55f, 1.45f, curve)
        LiquidGlassQuality.REDUCED -> blurRadius * 0.62f * lerp(0.70f, 1.15f, curve)
        LiquidGlassQuality.FROST -> 0.dp
    }

    // Glass body tint — driven by the slider inside each tier. Matte end:
    // a milky frosted body; ultra-liquid end: a whisper (the blur and the
    // colours behind the panel do the talking — that IS the liquid look).
    // FROST is a REAL matte panel: a fully opaque surface (alpha 1) — nothing
    // shows through, so the bar looks exactly the same no matter what
    // scrolls behind it. No shifts, no changes — and no slider either.
    val glassTint = tint
        ?: when {
            quality == LiquidGlassQuality.FROST ->
                MaterialTheme.colorScheme.surface.copy(alpha = 1f)
            !hardwareBlur ->
                // milkier over the low-fidelity CPU blur so text stays legible
                MaterialTheme.colorScheme.surface.copy(
                    alpha = if (isDark) lerp(0.48f, 0.30f, curve) else lerp(0.40f, 0.24f, curve)
                )
            quality == LiquidGlassQuality.REDUCED ->
                MaterialTheme.colorScheme.surface.copy(
                    alpha = if (isDark) lerp(0.40f, 0.12f, curve) else lerp(0.28f, 0.09f, curve)
                )
            else ->
                MaterialTheme.colorScheme.surface.copy(
                    alpha = if (isDark) lerp(0.26f, 0.035f, curve) else lerp(0.20f, 0.028f, curve)
                )
        }

    // Scrim: slightly heavier at the top edge (light comes from above),
    // gently dissolved as the glass gets more liquid. FROST needs none —
    // an opaque matte body has nothing to deepen.
    val scrimScale = lerp(1.30f, 0.70f, curve)
    val scrimTop = when {
        quality == LiquidGlassQuality.FROST -> Color.Transparent
        quality == LiquidGlassQuality.REDUCED ->
            Color.Black.copy(alpha = (if (isDark) 0.17f else 0.085f) * scrimScale)
        else -> Color.Black.copy(alpha = (if (isDark) 0.125f else 0.055f) * scrimScale)
    }
    val scrimBottom = when {
        quality == LiquidGlassQuality.FROST -> Color.Transparent
        quality == LiquidGlassQuality.REDUCED ->
            Color.Black.copy(alpha = (if (isDark) 0.07f else 0.03f) * scrimScale)
        else -> Color.Black.copy(alpha = (if (isDark) 0.047f else 0.02f) * scrimScale)
    }

    // Specular rim: bright at the top, softly lit at the bottom — slightly
    // stronger in FULL so the glass edge still reads through the thin tint.
    // FROST swaps the glossy gradient for a flat matte hairline — a solid
    // panel should read matte, not shiny.
    val rimBrush = if (quality == LiquidGlassQuality.FROST) {
        val hairline = MaterialTheme.colorScheme.outlineVariant.copy(
            alpha = if (isDark) 0.45f else 0.65f
        )
        SolidColor(hairline)
    } else {
        val rimTop = if (isDark) {
            Color(1f, 1f, 1f, if (quality == LiquidGlassQuality.FULL) 0.38f else 0.30f)
        } else {
            Color(1f, 1f, 1f, if (quality == LiquidGlassQuality.FULL) 0.85f else 0.80f)
        }
        val rimMid = if (isDark) Color(1f, 1f, 1f, 0.07f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
        val rimBottom = if (isDark) Color(1f, 1f, 1f, 0.16f) else Color(1f, 1f, 1f, 0.50f)
        Brush.verticalGradient(listOf(rimTop, rimMid, rimBottom))
    }

    val ambientShadowColor = if (isDark) Color(0x59000000) else Color(0x14000000)
    val spotShadowColor = if (isDark) Color(0x7A000000) else Color(0x29000000)

    // v6: the AGSL edge shader (Android 13+) — per-pixel refractive bevel.
    // Null everywhere else; the old zoom-bleed path keeps the look there.
    val edgeShader = if (AgslEdgeRefractionSupported) rememberLiquidEdgeShader() else null
    val layoutDir = LocalLayoutDirection.current

    // Panel origin exposed as a State object so the AGSL pass can read it
    // inside its graphicsLayer block (deferred read — the glint uniform
    // updates while the island moves without recomposition).
    val panelOriginState = remember { mutableStateOf<Offset?>(null) }
    var panelOrigin by panelOriginState
    var panelSize by remember { mutableStateOf(IntSize.Zero) }

    // Software path: live-blurred snapshot of the content behind the panel.
    // Only kept alive while a non-FROST tier actually needs it.
    val softBlur = if (!hardwareBlur && quality != LiquidGlassQuality.FROST) {
        rememberSoftBackdropBitmap(backdrop, { panelOrigin }, { panelSize }, effectiveBlur, quality)
    } else null

    Box(
        modifier = modifier
            .onGloballyPositioned {
                panelOrigin = it.positionInRoot()
                panelSize = it.size
            }
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = ambientShadowColor,
                spotColor = spotShadowColor
            )
            .clip(shape)
    ) {
        // ---- Layer 1: the blurred backdrop copy ----
        // CRITICAL: this lives in its own child node whose graphicsLayer
        // carries the BlurEffect. A graphicsLayer renders EVERYTHING drawn
        // after it in the same node — so the blur must never be attached to
        // the panel itself, otherwise the icons, labels and pill above it
        // would be blurred away too. Here only the backdrop copy is blurred.
        if (hardwareBlur && quality != LiquidGlassQuality.FROST) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        renderEffect = BlurEffect(effectiveBlur.toPx(), effectiveBlur.toPx())
                    }
                    .drawBehind {
                        // observe live re-records of the backdrop
                        backdrop.version.value
                        val srcOrigin = backdrop.origin ?: return@drawBehind
                        val pos = panelOrigin ?: return@drawBehind
                        val dx = pos.x - srcOrigin.x
                        val dy = pos.y - srcOrigin.y
                        translate(-dx, -dy) {
                            drawLayer(backdrop.layer)
                        }
                    }
            )
            if (quality == LiquidGlassQuality.FULL) {
                if (edgeShader != null) {
                    // v6: per-pixel AGSL refraction — the real «thick glass»
                    // bevel (refracted streaks, chroma split, specular rim,
                    // moving glint). The slider drives it live: from a calm
                    // hint at the matte end to a full liquid bevel at 100%.
                    AgslEdgePass(
                        backdrop = backdrop,
                        shader = edgeShader,
                        shape = shape,
                        layoutDirection = layoutDir,
                        curve = curve,
                        panelOriginState = panelOriginState,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    // Refractive edge bleed (Android 12/12L fallback): the same
                    // backdrop copy, slightly zoomed and blurred much wider,
                    // blended over the blur. Near the rim the zoomed copy
                    // samples content from further out — light bends outward
                    // like real thick glass, and the interior gains a second
                    // soft wash of depth. The slider scales the effect: nearly
                    // still at the matte end, a real refractive halo at the
                    // ultra-liquid end.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                val bleedRadius = effectiveBlur * lerp(1.30f, 1.75f, curve)
                                val bleedScale = lerp(1.03f, 1.09f, curve)
                                scaleX = bleedScale
                                scaleY = bleedScale
                                alpha = lerp(0.28f, 0.65f, curve)
                                renderEffect = BlurEffect(bleedRadius.toPx(), bleedRadius.toPx())
                            }
                            .drawBehind {
                                backdrop.version.value
                                val srcOrigin = backdrop.origin ?: return@drawBehind
                                val pos = panelOrigin ?: return@drawBehind
                                val dx = pos.x - srcOrigin.x
                                val dy = pos.y - srcOrigin.y
                                translate(-dx, -dy) {
                                    drawLayer(backdrop.layer)
                                }
                            }
                    )
                }
            }
        } else if (!hardwareBlur && quality != LiquidGlassQuality.FROST) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val bmp = softBlur ?: return@drawBehind
                        // software snapshot is already the blurred region behind
                        // the panel — draw it stretched over the whole panel
                        drawImage(
                            image = bmp,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(bmp.width, bmp.height),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt())
                        )
                    }
            )
        }
        // FROST: no backdrop copy at all — the solid matte surface below IS
        // the whole look: fully opaque, nothing ever reads through.

        // ---- Layer 2: scrim + tint, drawn in one cached pass above the blur ----
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithCache {
                    val scrim = Brush.verticalGradient(
                        colors = listOf(scrimTop, scrimBottom),
                        startY = 0f,
                        endY = size.height
                    )
                    onDrawBehind {
                        drawRect(brush = scrim)
                        drawRect(color = glassTint)
                    }
                }
        )

        // ---- Layer 3: thin specular rim ----
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(width = borderWidth, brush = rimBrush, shape = shape)
        )

        // ---- Layer 4: crisp glass UI on top ----
        content()
    }
}

/* ------------------------------------------------------------------------- */
/*  Liquid Glass v6 — the AGSL edge pass (Android 13+)                       */
/* ------------------------------------------------------------------------- */

/**
 * Draws a copy of the recorded backdrop into a layer and runs the
 * [LIQUID_EDGE_AGSL] shader on it: only the rim band survives (the center
 * stays transparent for the frosted blur below), and inside that band the
 * backdrop is refracted outward along the rim normal, chromatically split,
 * saturation-boosted and crowned with a specular bevel whose glint slides
 * as the panel moves. Uniforms follow the Settings intensity slider live.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslEdgePass(
    backdrop: GlassBackdrop,
    shader: RuntimeShader,
    shape: Shape,
    layoutDirection: LayoutDirection,
    curve: Float,
    panelOriginState: State<Offset?>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                // Corner radius of the panel shape, resolved in px (works for
                // RoundedCornerShape and CircleShape; flat shapes → 0).
                val cornerPx = (shape.createOutline(size, layoutDirection, this)
                    as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: 0f
                // Edge band: 12dp (calm matte end) .. 26dp (ultra liquid).
                val bandPx = (12.dp + (26.dp - 12.dp) * curve).toPx()
                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("corner", cornerPx)
                shader.setFloatUniform("band", bandPx)
                shader.setFloatUniform("refraction", lerp(0.30f, 0.95f, curve))
                shader.setFloatUniform("chroma", lerp(0.20f, 0.85f, curve))
                shader.setFloatUniform("specular", lerp(0.30f, 1.0f, curve))
                shader.setFloatUniform("saturation", lerp(0.08f, 0.30f, curve))
                // Fixed top-left light (matches the rim gradient above).
                shader.setFloatUniform("lightX", -0.55f)
                shader.setFloatUniform("lightY", -0.83f)
                // The glint slides around the rim as the panel itself moves —
                // dynamic light for free, zero cost while the panel is idle.
                val pos = panelOriginState.value
                val phase = (pos?.x ?: 0f) * 0.006f + (pos?.y ?: 0f) * 0.010f
                shader.setFloatUniform("phase", phase)
                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
                    .asComposeRenderEffect()
            }
            .drawBehind {
                // observe live re-records of the backdrop
                backdrop.version.value
                val srcOrigin = backdrop.origin ?: return@drawBehind
                val pos = panelOriginState.value ?: return@drawBehind
                val dx = pos.x - srcOrigin.x
                val dy = pos.y - srcOrigin.y
                translate(-dx, -dy) {
                    drawLayer(backdrop.layer)
                }
            }
    )
}

/* ------------------------------------------------------------------------- */
/*  Software blur path (Android 8–11) — real frosted glass without RenderEffect */
/* ------------------------------------------------------------------------- */

/**
 * Maintains a live-blurred [ImageBitmap] of the content recorded in
 * [GlassBackdrop] for the region covered by this panel. Regeneration is
 * frame-driven (only while frames are produced — idle screens cost nothing),
 * throttled to ~20 snapshots per second, gated by [LiquidGlassQuality]
 * (FROST cancels the loop entirely) and — new in v4 — skipped whenever the
 * backdrop did not actually change (same content version, same panel
 * geometry → no GPU→CPU readback at all). The snapshot is downscaled 4×
 * before blurring, so each update touches only a few thousand pixels.
 */
@Composable
private fun rememberSoftBackdropBitmap(
    backdrop: GlassBackdrop,
    panelOriginProvider: () -> Offset?,
    panelSizeProvider: () -> IntSize,
    blurRadius: Dp,
    quality: LiquidGlassQuality
): ImageBitmap? {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val density = LocalDensity.current

    // track providers without restarting the loop when they change
    val currentOrigin by rememberUpdatedState(panelOriginProvider)
    val currentSize by rememberUpdatedState(panelSizeProvider)

    LaunchedEffect(backdrop, quality, blurRadius) {
        if (quality == LiquidGlassQuality.FROST) {
            // economy tier: no panel needs a snapshot — stop the loop and
            // clear whatever the last tier left behind
            bitmap = null
            return@LaunchedEffect
        }

        var lastVersion = -1L
        var lastOrigin: Offset? = null
        var lastSize = IntSize.Zero
        var lastNano = 0L
        val blurRadiusPx = with(density) { blurRadius.toPx() }

        while (true) {
            val nano = withFrameNanos { it }
            // throttled to ~20 snapshots per second
            if (nano - lastNano < 50_000_000L) continue
            val origin = currentOrigin() ?: continue
            val size = currentSize()
            if (size.width <= 0 || size.height <= 0) continue

            // v4: the expensive GPU→CPU readback only happens when the
            // picture behind the panel actually changed — a static screen
            // costs literally nothing now
            if (backdrop.version.value == lastVersion &&
                origin == lastOrigin && size == lastSize
            ) continue

            lastNano = nano
            lastVersion = backdrop.version.value
            lastOrigin = origin
            lastSize = size
            bitmap = snapshotAndBlur(
                backdrop = backdrop,
                panelOrigin = origin,
                panelSize = size,
                blurRadiusPx = blurRadiusPx
            )
        }
    }
    return bitmap
}

/** Downscale factor of the software snapshot (4× → tiny CPU cost). */
private const val SOFT_BLUR_SCALE = 0.25f

/** Averaging window edge for the 4× downscale. */
private const val DOWNSCALE_STEP = 4

/**
 * Renders the recorded backdrop layer into a bitmap, extracts the region
 * behind the panel, downscales it 4× and blurs it on the CPU. Guarded: any
 * failure returns null and the panel falls back to its tinted look.
 */
private suspend fun snapshotAndBlur(
    backdrop: GlassBackdrop,
    panelOrigin: Offset,
    panelSize: IntSize,
    blurRadiusPx: Float
): ImageBitmap? {
    val src = backdrop.size
    if (src.width <= 0 || src.height <= 0) return null
    val origin = backdrop.origin ?: return null
    return try {
        // 1) render the whole recorded layer into a bitmap
        val full = backdrop.layer.toImageBitmap().asAndroidBitmap()
        if (full.width < 4 || full.height < 4) return null

        // 2) make the pixels readable on the CPU
        val swBmp = if (full.config == Bitmap.Config.ARGB_8888) {
            full
        } else {
            full.copy(Bitmap.Config.ARGB_8888, false) ?: return null
        }

        // 3) region behind the panel, in layer coordinates
        val rx = (panelOrigin.x - origin.x).toInt().coerceIn(0, (swBmp.width - 2).coerceAtLeast(0))
        val ry = (panelOrigin.y - origin.y).toInt().coerceIn(0, (swBmp.height - 2).coerceAtLeast(0))
        val rw = panelSize.width.coerceAtMost(swBmp.width - rx).coerceAtLeast(4)
        val rh = panelSize.height.coerceAtMost(swBmp.height - ry).coerceAtLeast(4)

        // 4) read just that rectangle
        val regionPixels = IntArray(rw * rh)
        swBmp.getPixels(regionPixels, 0, rw, rx, ry, rw, rh)

        // 5) downscale 4× by block averaging
        val sw = (rw * SOFT_BLUR_SCALE).toInt().coerceAtLeast(1)
        val sh = (rh * SOFT_BLUR_SCALE).toInt().coerceAtLeast(1)
        val small = downscale(regionPixels, rw, rh, sw, sh)

        // 6) real blur: 3-pass box filter ≈ gaussian
        val radius = (blurRadiusPx * SOFT_BLUR_SCALE).toInt().coerceIn(1, 24)
        boxBlurPixels(small, sw, sh, radius)

        // 7) wrap back into an ImageBitmap
        val out = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        out.setPixels(small, 0, sw, 0, 0, sw, sh)
        out.asImageBitmap()
    } catch (t: Throwable) {
        null
    }
}

/** Block-average downscale of an RGBA int array. */
private fun downscale(px: IntArray, w: Int, h: Int, sw: Int, sh: Int): IntArray {
    val out = IntArray(sw * sh)
    for (y in 0 until sh) {
        for (x in 0 until sw) {
            var rs = 0; var gs = 0; var bs = 0
            val x0 = (x * DOWNSCALE_STEP).coerceAtMost(w - 1)
            val y0 = (y * DOWNSCALE_STEP).coerceAtMost(h - 1)
            val x1 = (x0 + DOWNSCALE_STEP).coerceAtMost(w)
            val y1 = (y0 + DOWNSCALE_STEP).coerceAtMost(h)
            var n = 0
            var yy = y0
            while (yy < y1) {
                var xx = x0
                while (xx < x1) {
                    val p = px[yy * w + xx]
                    rs += (p shr 16) and 0xFF
                    gs += (p shr 8) and 0xFF
                    bs += p and 0xFF
                    n++
                    xx++
                }
                yy++
            }
            if (n == 0) n = 1
            out[y * sw + x] = (0xFF shl 24) or ((rs / n shl 16)) or ((gs / n shl 8)) or (bs / n)
        }
    }
    return out
}

/**
 * In-place 3-pass box blur over an RGBA int array (approximates a gaussian
 * blur — stable, fast and allocation-free apart from one scanline buffer).
 */
private fun boxBlurPixels(px: IntArray, w: Int, h: Int, radius: Int, passes: Int = 3) {
    if (w < 3 || h < 3 || radius < 1) return
    val temp = IntArray(w * h)
    repeat(passes) {
        boxBlurPass(px, temp, w, h, radius, horizontal = true)
        boxBlurPass(px, temp, w, h, radius, horizontal = false)
    }
}

/** One horizontal or vertical box-blur pass with a sliding-window sum. */
private fun boxBlurPass(pixels: IntArray, temp: IntArray, w: Int, h: Int, radius: Int, horizontal: Boolean) {
    val window = 2 * radius + 1
    if (horizontal) {
        for (y in 0 until h) {
            val row = y * w
            var rs = 0; var gs = 0; var bs = 0
            for (i in -radius..radius) {
                val p = pixels[row + i.coerceIn(0, w - 1)]
                rs += (p shr 16) and 0xFF
                gs += (p shr 8) and 0xFF
                bs += p and 0xFF
            }
            for (x in 0 until w) {
                temp[row + x] =
                    (0xFF shl 24) or ((rs / window shl 16)) or ((gs / window shl 8)) or (bs / window)
                val pOut = pixels[row + (x - radius).coerceIn(0, w - 1)]
                val pIn = pixels[row + (x + radius + 1).coerceIn(0, w - 1)]
                rs += ((pIn shr 16) and 0xFF) - ((pOut shr 16) and 0xFF)
                gs += ((pIn shr 8) and 0xFF) - ((pOut shr 8) and 0xFF)
                bs += (pIn and 0xFF) - (pOut and 0xFF)
            }
        }
    } else {
        for (x in 0 until w) {
            var rs = 0; var gs = 0; var bs = 0
            for (i in -radius..radius) {
                val p = pixels[(i.coerceIn(0, h - 1)) * w + x]
                rs += (p shr 16) and 0xFF
                gs += (p shr 8) and 0xFF
                bs += p and 0xFF
            }
            for (y in 0 until h) {
                temp[y * w + x] =
                    (0xFF shl 24) or ((rs / window shl 16)) or ((gs / window shl 8)) or (bs / window)
                val pOut = pixels[(y - radius).coerceIn(0, h - 1) * w + x]
                val pIn = pixels[(y + radius + 1).coerceIn(0, h - 1) * w + x]
                rs += ((pIn shr 16) and 0xFF) - ((pOut shr 16) and 0xFF)
                gs += ((pIn shr 8) and 0xFF) - ((pOut shr 8) and 0xFF)
                bs += (pIn and 0xFF) - (pOut and 0xFF)
            }
        }
    }
    System.arraycopy(temp, 0, pixels, 0, pixels.size)
}

/* ------------------------------------------------------------------------- */
/*  Animation helpers                                                         */
/* ------------------------------------------------------------------------- */

/**
 * Staggered entrance animation for list items: fades + slides + gently
 * scales in with a per-index delay and an organic settle curve.
 */
@Composable
fun StaggeredAppear(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        if (!visibleState.targetState) {
            delay(index.coerceAtMost(10) * 36L)
            visibleState.targetState = true
        }
    }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(animationSpec = tween(340, easing = EaseOutQuint)) +
                slideInVertically(
                    animationSpec = spring(
                        dampingRatio = 0.80f,
                        stiffness = 380f
                    ),
                    initialOffsetY = { it / 6 }
                ) +
                scaleIn(
                    initialScale = 0.965f,
                    animationSpec = tween(340, easing = EaseOutQuint)
                ),
        exit = fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Gently floating icon used in empty states.
 */
@Composable
fun BobbingIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    circleSize: Dp = 64.dp,
    iconSize: Dp = 32.dp,
    circleAlpha: Float = 0.15f
) {
    val transition = rememberInfiniteTransition(label = "bob")
    val bobY by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobY"
    )
    val glow by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .size(circleSize)
            .graphicsLayer {
                translationY = bobY.dp.toPx()
            }
            .clip(CircleShape)
            .background(tint.copy(alpha = circleAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint.copy(alpha = glow),
            modifier = Modifier.size(iconSize)
        )
    }
}

/* ------------------------------------------------------------------------- */
/*  Tactile interactions                                                      */
/* ------------------------------------------------------------------------- */

/**
 * Tactile spring press modifier for buttons, cards, and nav items.
 * Scales down smoothly on press and rebounds with organic spring physics.
 * Deliberately NO haptic feedback: taps here lead to navigation/dialogs,
 * and page transitions must stay silent — vibration is reserved for
 * confirming real actions (logging a dose, saving a form).
 */
@Composable
fun Modifier.tactilePress(
    pressScale: Float = 0.94f,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tactileScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
}

/* ------------------------------------------------------------------------- */
/*  Legacy (non-blur) glass looks — kept as tasteful fallbacks / variants     */
/* ------------------------------------------------------------------------- */

/**
 * Core Liquid Glass Modifier applying translucent tinting, a specular edge
 * highlight and a soft ambient shadow.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    customGlassColor: Color? = null,
    elevation: Dp = 10.dp,
    borderWidth: Dp = 1.dp
): Modifier {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val specularTopLeft = if (isDark) {
        Color(1.0f, 1.0f, 1.0f, 0.22f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f)
    }
    val specularBottomRight = if (isDark) {
        Color(1.0f, 1.0f, 1.0f, 0.04f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
    }
    val borderBrush = Brush.linearGradient(
        colors = listOf(specularTopLeft, specularBottomRight)
    )

    val defaultGlassColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val glassColor = customGlassColor ?: defaultGlassColor

    val ambientShadowColor = if (isDark) Color(0x66000000) else Color(0x0D000000)
    val spotShadowColor = if (isDark) Color(0x80000000) else Color(0x1A000000)

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = ambientShadowColor,
            spotColor = spotShadowColor
        )
        .clip(shape)
        .background(glassColor)
        .border(
            width = borderWidth,
            brush = borderBrush,
            shape = shape
        )
}

/**
 * Special Floating Island Glass Modifier tuned for floating bars.
 * Soft translucent glass tinting and a high-contrast specular rim.
 */
@Composable
fun Modifier.islandGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    elevation: Dp = 16.dp,
    borderWidth: Dp = 1.dp
): Modifier {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val specularTopLeft = if (isDark) {
        Color(1.0f, 1.0f, 1.0f, 0.30f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.50f)
    }
    val specularBottomRight = if (isDark) {
        Color(1.0f, 1.0f, 1.0f, 0.05f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    }
    val borderBrush = Brush.linearGradient(
        colors = listOf(specularTopLeft, specularBottomRight)
    )

    val glassColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    }

    val ambientShadowColor = if (isDark) Color(0x66000000) else Color(0x12000000)
    val spotShadowColor = if (isDark) Color(0x80000000) else Color(0x24000000)

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = ambientShadowColor,
            spotColor = spotShadowColor
        )
        .clip(shape)
        .background(glassColor)
        .border(
            width = borderWidth,
            brush = borderBrush,
            shape = shape
        )
}

/* ------------------------------------------------------------------------- */
/*  Glass building blocks                                                     */
/* ------------------------------------------------------------------------- */

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    onClick: (() -> Unit)? = null,
    glassAlpha: Float = 1.0f,
    contentPadding: Dp = 16.dp,
    elevation: Dp = 8.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // The slider also breathes through the CARDS: at the matte end they sit
    // calm and solid, at the ultra-liquid end they turn translucent enough
    // for the aurora behind to glow through. FROST ignores the slider —
    // the economy look stays fixed and fully opaque-ish by design.
    val glassQuality by LiquidGlassState.quality
    val rawIntensity by LiquidGlassState.intensity
    val iEff = rawIntensity.coerceIn(0f, 1f)

    val defaultSurface = MaterialTheme.colorScheme.surface
    val glassColor = when {
        glassQuality == LiquidGlassQuality.FROST -> if (isDark) {
            defaultSurface.copy(alpha = 0.85f * glassAlpha)
        } else {
            defaultSurface.copy(alpha = 0.96f * glassAlpha)
        }
        isDark -> defaultSurface.copy(alpha = lerp(0.88f, 0.60f, iEff) * glassAlpha)
        else -> defaultSurface.copy(alpha = lerp(0.97f, 0.78f, iEff) * glassAlpha)
    }

    val baseModifier = modifier
        .liquidGlass(
            shape = shape,
            customGlassColor = glassColor,
            elevation = elevation
        )

    val finalModifier = if (onClick != null) {
        baseModifier.tactilePress(pressScale = 0.96f, onClick = onClick)
    } else {
        baseModifier
    }

    Box(modifier = finalModifier) {
        Column(
            modifier = Modifier.padding(contentPadding),
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

/**
 * Floating Mini Glass Circle for medication icon / status badges.
 */
@Composable
fun GlassCircleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tintColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tintColor.copy(alpha = if (isDark) 0.22f else 0.14f))
            .border(
                width = 1.dp,
                color = tintColor.copy(alpha = if (isDark) 0.35f else 0.25f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Liquid Glass Status Chip (e.g. Taken, Missed, Scheduled).
 */
@Composable
fun GlassChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val specularTopLeft = if (isDark) Color(1f, 1f, 1f, 0.30f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)
    val specularBottomRight = if (isDark) Color(1f, 1f, 1f, 0.05f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)

    Surface(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(specularTopLeft, specularBottomRight)),
                shape = RoundedCornerShape(20.dp)
            ),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                icon()
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Volumetric Liquid Glass Floating Action Button with spring tactile bounce
 * and specular glow.
 */
@Composable
fun GlassFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable () -> Unit
) {
    val specularTopLeft = Color(1f, 1f, 1f, 0.85f)
    val specularBottomRight = Color(1f, 1f, 1f, 0.20f)

    // spring entrance
    val appearScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appearScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.55f,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = appearScale.value
                scaleY = appearScale.value
            }
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = containerColor.copy(alpha = 0.4f),
                spotColor = containerColor.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.lerp(containerColor, Color.White, 0.10f),
                        containerColor,
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(listOf(specularTopLeft, specularBottomRight)),
                shape = RoundedCornerShape(24.dp)
            )
            .tactilePress(pressScale = 0.88f, onClick = onClick)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
