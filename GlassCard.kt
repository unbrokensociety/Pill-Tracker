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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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

val BackdropBlurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private val AgslLensSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

private const val LIQUID_LENS_AGSL = """
uniform shader content;
uniform float2 resolution;
uniform float corner;
uniform float band;
uniform float refraction;
uniform float chroma;
uniform float specular;
uniform float lightX;
uniform float lightY;
uniform float phase;
uniform float saturation;
uniform float veil;
uniform float dim;

float sdRoundBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 fragCoord) {
    float2 c = resolution * 0.5;
    float2 p = fragCoord - c;
    float d = sdRoundBox(p, c, corner);
    float inDist = max(-d, 0.0);
    float t = clamp(inDist / band, 0.0, 1.0);
    float rim = 1.0 - t;
    float reach = max(min(c.x, c.y), 1.0);
    float depth = clamp(inDist / reach, 0.0, 1.0);
    float lens = 0.06 + 0.94 * pow(1.0 - depth, 1.7);

    float2 ex = float2(1.0, 0.0);
    float2 ey = float2(0.0, 1.0);
    float2 grad = float2(
        sdRoundBox(p + ex, c, corner) - sdRoundBox(p - ex, c, corner),
        sdRoundBox(p + ey, c, corner) - sdRoundBox(p - ey, c, corner));
    float2 n = grad / max(length(grad), 0.0001);
    float2 radial = p / max(length(p), 0.0001);
    float2 md = mix(radial, n, rim * rim);
    float2 dir = md / max(length(md), 0.0001);

    float disp = lens * refraction * band;
    float2 uv = clamp(fragCoord + dir * disp, float2(0.5), resolution - float2(0.5));

    float soft = band * 0.30;
    half4 s0 = content.eval(uv);
    half4 sx1 = content.eval(uv + float2(soft, 0.0));
    half4 sx2 = content.eval(uv - float2(soft, 0.0));
    half4 sy1 = content.eval(uv + float2(0.0, soft));
    half4 sy2 = content.eval(uv - float2(0.0, soft));
    half4 col = (s0 + s0 + sx1 + sx2 + sy1 + sy2) / 6.0;

    float ca = chroma * rim * max(band * 0.045, 0.75);
    if (ca > 0.02) {
        col.r = content.eval(clamp(uv + dir * ca, float2(0.5), resolution - float2(0.5))).r;
        col.b = content.eval(clamp(uv - dir * ca, float2(0.5), resolution - float2(0.5))).b;
    }

    float3 rgb = float3(col.rgb);
    float l = dot(rgb, float3(0.2126, 0.7152, 0.0722));
    rgb = clamp(mix(float3(l), rgb, 1.0 + saturation * (0.25 + 0.75 * rim)), float3(0.0), float3(1.0));

    float lambert = clamp(dot(n, float2(lightX, lightY)), 0.0, 1.0);
    float glint = 0.5 + 0.5 * sin(phase + (n.x * 1.2 + n.y * 0.8) * 1.7);
    float edge = pow(rim, 2.2);
    float hairline = pow(rim, 9.0) * 0.55;
    float spec = (edge * (0.30 + 0.55 * pow(lambert, 2.0) + 0.22 * glint * (0.35 + 0.65 * lambert)) + hairline) * specular;

    float halfDiag = length(resolution) * 0.5;
    float sheenPos = (p.x + p.y) / max(halfDiag, 1.0);
    float sheen = pow(max(0.0, 1.0 - abs(sheenPos - 0.30 - 0.25 * sin(phase * 0.7))), 3.0) * 0.10 * specular;

    float dimAmt = dim * smoothstep(0.45, 0.95, l);
    float cover = 0.58 * edge;
    float specWhite = clamp(spec + sheen, 0.0, 1.0);
    float a = clamp(veil + dimAmt + cover + specWhite, 0.0, 1.0);
    float3 prem = rgb * cover + float3(veil + specWhite);
    return half4(half3(prem), half(a));
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun rememberLiquidLensShader(): RuntimeShader =
    remember { RuntimeShader(LIQUID_LENS_AGSL) }

enum class LiquidGlassQuality { FULL, REDUCED, FROST }

object LiquidGlassState {
    val quality = mutableStateOf(LiquidGlassQuality.FULL)
    val userMode = mutableStateOf<LiquidGlassQuality?>(null)
    val intensity = mutableStateOf(GlassModeStore.DEFAULT_INTENSITY)
}

object GlassModeStore {
    private const val PREFS = "liquid_glass"
    private const val KEY = "mode"
    private const val KEY_INTENSITY = "intensity"

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
    }

    fun saveIntensity(context: Context, intensity: Float) = try {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_INTENSITY, intensity.coerceIn(0f, 1f))
            .apply()
    } catch (t: Throwable) {
    }
}

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
                    cores >= 4 && ramGb >= 2.5 -> LiquidGlassQuality.FULL
                    cores >= 4 -> LiquidGlassQuality.REDUCED
                    else -> LiquidGlassQuality.FROST
                }
                else -> when {
                    cores >= 8 && ramGb >= 6.0 -> LiquidGlassQuality.REDUCED
                    else -> LiquidGlassQuality.FROST
                }
            }
        } catch (t: Throwable) {
            LiquidGlassQuality.FROST
        }
    }
}

@Composable
fun GlassPerformanceGovernor(backdrop: GlassBackdrop) {
    val context = LocalContext.current
    val policyLevel = remember { DeviceGlassPolicy.assess(context) }

    LaunchedEffect(backdrop) {
        LiquidGlassState.intensity.value = GlassModeStore.loadIntensity(context)

        val savedMode = GlassModeStore.loadMode(context)
        if (savedMode != null) {
            LiquidGlassState.userMode.value = savedMode
            LiquidGlassState.quality.value = savedMode
            return@LaunchedEffect
        }

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

        val windowSize = 120
        val deltas = LongArray(windowSize)
        var samples = 0
        var index = 0
        var badWindows = 0
        var cooldown = 0
        val warmup = 90
        var warmupLeft = warmup
        var prevNanos = 0L

        while (true) {
            val nanos = withFrameNanos { it }

            if (LiquidGlassState.userMode.value != null) return@LaunchedEffect

            val delta = if (prevNanos == 0L) 0L else nanos - prevNanos
            prevNanos = nanos

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
            if (delta > 250_000_000L || delta <= 0L) { samples = 0; continue }
            if (cooldown > 0) { cooldown--; continue }

            deltas[index] = delta
            index = (index + 1) % windowSize
            samples = (samples + 1).coerceAtMost(windowSize)

            if (samples == windowSize) {
                samples = 0
                index = 0

                val sorted = deltas.copyOf().also { it.sort() }
                val refresh = sorted[(windowSize * 0.05f).toInt()].coerceAtLeast(1L)

                var missed = 0
                var heavy = 0
                for (d in deltas) {
                    when {
                        d > refresh * 3.5f -> heavy += 2
                        d > refresh * 1.8f -> missed++
                    }
                }

                val bad = (missed + heavy) >= windowSize * 0.20f
                badWindows = if (bad) badWindows + 1 else 0

                if (badWindows >= 2 && level != LiquidGlassQuality.FROST) {
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

class GlassBackdrop internal constructor(
    internal val layer: GraphicsLayer
) {
    internal var origin: Offset? = null
    internal var size: IntSize = IntSize.Zero
    internal val version = mutableStateOf(0L)
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

fun Modifier.glassSource(backdrop: GlassBackdrop): Modifier =
    this.onGloballyPositioned { coords ->
        backdrop.origin = coords.positionInRoot()
        backdrop.size = coords.size
    }.drawWithCache {
        val layer = backdrop.layer
        onDrawWithContent {
            if (LiquidGlassState.quality.value == LiquidGlassQuality.FROST) {
                drawContent()
            } else {
                layer.record {
                    this@onDrawWithContent.drawContent()
                }
                drawLayer(layer)
                backdrop.version.value += 1L
            }
        }
    }

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
            center = Offset.Zero,
            radius = w * 0.85f
        )
        val blob2 = Brush.radialGradient(
            colors = listOf(tertiary.copy(alpha = blobAlpha * 0.85f), Color.Transparent),
            center = Offset.Zero,
            radius = w * 0.75f
        )
        val blob3 = Brush.radialGradient(
            colors = listOf(secondary.copy(alpha = blobAlpha), Color.Transparent),
            center = Offset.Zero,
            radius = w * 0.95f
        )
        val blob4 = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = blobAlpha * 0.7f), Color.Transparent),
            center = Offset.Zero,
            radius = w * 0.55f
        )

        onDrawBehind {
            drawRect(brush = wash)
            translate(0f, h * 0.08f) {
                drawCircle(brush = blob1, radius = w * 0.85f, center = Offset.Zero)
            }
            translate(w, h * 0.40f) {
                drawCircle(brush = blob2, radius = w * 0.75f, center = Offset.Zero)
            }
            translate(w * 0.5f, h * 1.02f) {
                drawCircle(brush = blob3, radius = w * 0.95f, center = Offset.Zero)
            }
            translate(w * 0.10f, h * 0.78f) {
                drawCircle(brush = blob4, radius = w * 0.55f, center = Offset.Zero)
            }
        }
    }
}

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

    val rawIntensity by LiquidGlassState.intensity
    val iEff = rawIntensity.coerceIn(0f, 1f)
    val curve = iEff.pow(1.15f)

    val effectiveBlur = when (quality) {
        LiquidGlassQuality.FULL -> blurRadius * lerp(0.55f, 1.30f, curve)
        LiquidGlassQuality.REDUCED -> blurRadius * 0.62f * lerp(0.70f, 1.15f, curve)
        LiquidGlassQuality.FROST -> 0.dp
    }

    val useLens = hardwareBlur &&
            quality == LiquidGlassQuality.FULL &&
            AgslLensSupported

    val glassTint = tint
        ?: when {
            quality == LiquidGlassQuality.FROST ->
                MaterialTheme.colorScheme.surface.copy(alpha = 1f)
            !hardwareBlur ->
                MaterialTheme.colorScheme.surface.copy(
                    alpha = if (isDark) lerp(0.48f, 0.30f, curve) else lerp(0.40f, 0.24f, curve)
                )
            quality == LiquidGlassQuality.REDUCED ->
                MaterialTheme.colorScheme.surface.copy(
                    alpha = if (isDark) lerp(0.40f, 0.12f, curve) else lerp(0.28f, 0.09f, curve)
                )
            useLens -> Color.Transparent
            else ->
                MaterialTheme.colorScheme.surface.copy(
                    alpha = if (isDark) lerp(0.26f, 0.035f, curve) else lerp(0.20f, 0.028f, curve)
                )
        }

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

    val lensShader = if (AgslLensSupported) rememberLiquidLensShader() else null
    val layoutDir = LocalLayoutDirection.current

    val panelOriginState = remember { mutableStateOf<Offset?>(null) }
    var panelOrigin by panelOriginState
    var panelSize by remember { mutableStateOf(IntSize.Zero) }

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
        if (hardwareBlur && quality != LiquidGlassQuality.FROST) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        renderEffect = BlurEffect(effectiveBlur.toPx(), effectiveBlur.toPx())
                    }
                    .drawBehind {
                        backdrop.version.value
                        val srcOrigin = backdrop.origin ?: return@drawBehind
                        val pos = panelOrigin ?: return@drawBehind
                        translate(srcOrigin.x - pos.x, srcOrigin.y - pos.y) {
                            drawLayer(backdrop.layer)
                        }
                    }
            )
            if (quality == LiquidGlassQuality.FULL) {
                if (lensShader != null) {
                    AgslLensPass(
                        backdrop = backdrop,
                        shader = lensShader,
                        shape = shape,
                        layoutDirection = layoutDir,
                        curve = curve,
                        isDark = isDark,
                        panelOriginState = panelOriginState,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
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
                                translate(srcOrigin.x - pos.x, srcOrigin.y - pos.y) {
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

        if (!useLens) {
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
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(width = borderWidth, brush = rimBrush, shape = shape)
            )
        } else if (tint != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind { drawRect(color = tint) }
            )
        }

        content()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslLensPass(
    backdrop: GlassBackdrop,
    shader: RuntimeShader,
    shape: Shape,
    layoutDirection: LayoutDirection,
    curve: Float,
    isDark: Boolean,
    panelOriginState: State<Offset?>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                val cornerPx = (shape.createOutline(size, layoutDirection, this)
                    as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: 0f
                val bandPx = (12.dp + (26.dp - 12.dp) * curve).toPx()
                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("corner", cornerPx)
                shader.setFloatUniform("band", bandPx)
                shader.setFloatUniform("refraction", lerp(0.35f, 0.95f, curve))
                shader.setFloatUniform("chroma", lerp(0.15f, 0.75f, curve))
                shader.setFloatUniform("specular", lerp(0.35f, 1.0f, curve))
                shader.setFloatUniform("saturation", lerp(0.10f, 0.30f, curve))
                shader.setFloatUniform("lightX", -0.55f)
                shader.setFloatUniform("lightY", -0.83f)
                shader.setFloatUniform(
                    "veil",
                    if (isDark) lerp(0.16f, 0.05f, curve) else lerp(0.40f, 0.15f, curve)
                )
                shader.setFloatUniform(
                    "dim",
                    if (isDark) lerp(0.03f, 0.01f, curve) else lerp(0.10f, 0.04f, curve)
                )
                val pos = panelOriginState.value
                val phase = (pos?.x ?: 0f) * 0.006f + (pos?.y ?: 0f) * 0.010f
                shader.setFloatUniform("phase", phase)
                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
                    .asComposeRenderEffect()
            }
            .drawBehind {
                backdrop.version.value
                val srcOrigin = backdrop.origin ?: return@drawBehind
                val pos = panelOriginState.value ?: return@drawBehind
                translate(srcOrigin.x - pos.x, srcOrigin.y - pos.y) {
                    drawLayer(backdrop.layer)
                }
            }
    )
}

private const val SOFT_BLUR_SCALE = 0.25f
private const val DOWNSCALE_STEP = 4

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

    val currentOrigin by rememberUpdatedState(panelOriginProvider)
    val currentSize by rememberUpdatedState(panelSizeProvider)

    LaunchedEffect(backdrop, quality, blurRadius) {
        if (quality == LiquidGlassQuality.FROST) {
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
            if (nano - lastNano < 50_000_000L) continue
            val origin = currentOrigin() ?: continue
            val size = currentSize()
            if (size.width <= 0 || size.height <= 0) continue

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
        val full = backdrop.layer.toImageBitmap().asAndroidBitmap()
        if (full.width < 4 || full.height < 4) return null

        val swBmp = if (full.config == Bitmap.Config.ARGB_8888) {
            full
        } else {
            full.copy(Bitmap.Config.ARGB_8888, false) ?: return null
        }

        val rx = (panelOrigin.x - origin.x).toInt().coerceIn(0, (swBmp.width - 2).coerceAtLeast(0))
        val ry = (panelOrigin.y - origin.y).toInt().coerceIn(0, (swBmp.height - 2).coerceAtLeast(0))
        val rw = panelSize.width.coerceAtMost(swBmp.width - rx).coerceAtLeast(4)
        val rh = panelSize.height.coerceAtMost(swBmp.height - ry).coerceAtLeast(4)

        val regionPixels = IntArray(rw * rh)
        swBmp.getPixels(regionPixels, 0, rw, rx, ry, rw, rh)

        val sw = (rw * SOFT_BLUR_SCALE).toInt().coerceAtLeast(1)
        val sh = (rh * SOFT_BLUR_SCALE).toInt().coerceAtLeast(1)
        val small = downscale(regionPixels, rw, rh, sw, sh)

        val radius = (blurRadiusPx * SOFT_BLUR_SCALE).toInt().coerceIn(1, 24)
        boxBlurPixels(small, sw, sh, radius)

        val out = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        out.setPixels(small, 0, sw, 0, 0, sw, sh)
        out.asImageBitmap()
    } catch (t: Throwable) {
        null
    }
}

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

private fun boxBlurPixels(px: IntArray, w: Int, h: Int, radius: Int, passes: Int = 3) {
    if (w < 3 || h < 3 || radius < 1) return
    val temp = IntArray(w * h)
    repeat(passes) {
        boxBlurPass(px, temp, w, h, radius, horizontal = true)
        boxBlurPass(px, temp, w, h, radius, horizontal = false)
    }
}

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
