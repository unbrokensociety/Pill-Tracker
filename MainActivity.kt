package com.aistudio.meditracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.navigation.compose.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.aistudio.meditracker.ui.AddMedicationScreen
import com.aistudio.meditracker.ui.CalendarScreen
import com.aistudio.meditracker.ui.HomeScreen
import com.aistudio.meditracker.ui.MedicationsListScreen
import com.aistudio.meditracker.ui.SettingsScreen
import com.aistudio.meditracker.ui.MainViewModel
import com.aistudio.meditracker.ui.MainViewModelFactory
import com.aistudio.meditracker.ui.components.LiquidGlassPanel
import com.aistudio.meditracker.ui.components.GlassFAB
import com.aistudio.meditracker.ui.components.GlassPerformanceGovernor
import com.aistudio.meditracker.ui.components.auroraBackdrop
import com.aistudio.meditracker.ui.components.glassSource
import com.aistudio.meditracker.ui.components.rememberGlassBackdrop
import com.aistudio.meditracker.ui.components.tactilePress
import com.aistudio.meditracker.ui.components.OnboardingBus
import com.aistudio.meditracker.ui.components.OnboardingOverlay
import com.aistudio.meditracker.ui.components.OnboardingPrefs
import com.aistudio.meditracker.ui.components.UpdateGate
import com.aistudio.meditracker.ui.components.coachTag
import com.aistudio.meditracker.ui.theme.MyApplicationTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.flow.filter
import com.aistudio.meditracker.data.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MyAppThemeWrapper(viewModel: MainViewModel, content: @Composable () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsState()
    MyApplicationTheme(themeMode = themeMode) {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as? android.app.Activity)?.window
                if (window != null) {
                    val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = !isDark
                    insetsController.isAppearanceLightNavigationBars = !isDark
                }
            }
        }
        content()
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(this.applicationContext)
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Permission state is naturally reflected in the Settings toggle.
        }

    override fun attachBaseContext(newBase: android.content.Context) {
        val lang = com.aistudio.meditracker.ui.locale.LocaleHelper.getLanguage(newBase)
        val contextWithLocale = com.aistudio.meditracker.ui.locale.LocaleHelper.updateResources(newBase, lang)
        super.attachBaseContext(contextWithLocale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        capFrameRateAt60()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
            }
        }

        setContent {
            MyAppThemeWrapper(viewModel) {
                MainScreen(viewModel)
            }
        }
    }

    private fun capFrameRateAt60() {
        try {
            val modes = (
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    display?.supportedModes
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay?.supportedModes
                }
                ) ?: emptyArray<android.view.Display.Mode>()
            val target = modes
                .filter { it.refreshRate in 55f..65f }
                .minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }
            window.attributes = window.attributes.apply {
                if (target != null) preferredDisplayModeId = target.modeId
                preferredRefreshRate = target?.refreshRate ?: 60f
            }
        } catch (t: Throwable) {
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    var onboardingDone by remember { mutableStateOf(OnboardingPrefs.isCompleted(context)) }
    LaunchedEffect(Unit) {
        snapshotFlow { OnboardingBus.replayRequested }
            .filter { it }
            .collect {
                OnboardingBus.consume()
                onboardingDone = false
            }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { OnboardingBus.addRequested }
            .filter { it }
            .collect {
                OnboardingBus.consumeAdd()
                navController.navigate("add")
            }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { OnboardingBus.backRequested }
            .filter { it }
            .collect {
                OnboardingBus.consumeBack()
                navController.popBackStack()
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    NavHost(
        navController = navController,
        startDestination = "main",
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        enterTransition = {
            if (targetState.destination.route?.startsWith("add") == true) {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 320f
                    )
                ) + fadeIn(animationSpec = tween(220, easing = EaseOutCubic))
            } else {
                fadeIn(animationSpec = tween(200, easing = EaseOutCubic))
            }
        },
        exitTransition = {
            if (initialState.destination.route?.startsWith("add") == true) {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(220, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(180))
            } else {
                fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing))
            }
        }
    ) {
        composable("main") {
            MainPagerScreen(
                viewModel = viewModel,
                onNavigateToAdd = { medId ->
                    if (medId != null) {
                        navController.navigate("add?medicationId=$medId")
                    } else {
                        navController.navigate("add")
                    }
                }
            )
        }
        composable(
            route = "add?medicationId={medicationId}",
            arguments = listOf(
                navArgument("medicationId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val medIdArg = backStackEntry.arguments?.getInt("medicationId") ?: -1
            val editMedId = if (medIdArg != -1) medIdArg else null
            AddMedicationScreen(
                editingMedicationId = editMedId,
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
    }

        AnimatedVisibility(
            visible = !onboardingDone,
            exit = fadeOut(animationSpec = tween(240, easing = FastOutLinearInEasing)) +
                scaleOut(targetScale = 0.96f, animationSpec = tween(240, easing = FastOutLinearInEasing))
        ) {
            OnboardingOverlay(
                onFinished = {
                    OnboardingPrefs.setCompleted(context)
                    OnboardingBus.tourFinished()
                    onboardingDone = true
                }
            )
        }

        UpdateGate()
    }
}

@Composable
fun MainPagerScreen(
    viewModel: MainViewModel,
    onNavigateToAdd: (Int?) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    val tourActive = OnboardingBus.tourActive

    LaunchedEffect(Unit) {
        snapshotFlow { OnboardingBus.pageRequested }
            .filter { it >= 0 }
            .collect { page ->
                OnboardingBus.consumePage()
                pagerState.animateScrollToPage(
                    page,
                    animationSpec = spring(dampingRatio = 0.84f, stiffness = 320f)
                )
            }
    }

    val pagerFraction by remember {
        derivedStateOf {
            pagerState.currentPage + pagerState.currentPageOffsetFraction
        }
    }

    val islandFractionAnim = remember { Animatable(0f) }
    var isDraggingIsland by remember { mutableStateOf(false) }

    // Synchronize bubble position with pager when not manually dragging
    LaunchedEffect(pagerFraction, isDraggingIsland) {
        if (!isDraggingIsland) {
            islandFractionAnim.snapTo(pagerFraction)
        }
    }

    val rawFraction = if (isDraggingIsland) islandFractionAnim.value else pagerFraction

    val effectiveFraction = when {
        rawFraction < 0f -> rawFraction * 0.25f
        rawFraction > 3f -> 3f + (rawFraction - 3f) * 0.25f
        else -> rawFraction
    }

    // Dynamic Liquid Stretch & Wall Squeeze Calculation
    val (bubbleScaleX, bubbleScaleY) = if (rawFraction < 0f) {
        // Squish against left wall
        val squish = (abs(rawFraction) * 0.35f).coerceIn(0f, 0.40f)
        (1f - squish) to (1f + squish * 0.5f)
    } else if (rawFraction > 3f) {
        // Squish against right wall
        val squish = ((rawFraction - 3f) * 0.35f).coerceIn(0f, 0.40f)
        (1f - squish) to (1f + squish * 0.5f)
    } else {
        val distFromCenter = abs(rawFraction - rawFraction.roundToInt())
        val stretch = (distFromCenter * 0.38f).coerceIn(0f, 0.30f)
        val sx = 1f + stretch
        val sy = (1f / kotlin.math.sqrt(sx)) * (if (isDraggingIsland) 0.94f else 1f)
        sx to sy
    }

    val backdrop = rememberGlassBackdrop()

    GlassPerformanceGovernor(backdrop)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .auroraBackdrop()
                // v2.4.13: backdrop recording is back — the nav bar blurs the
                // real content behind it (Telegram-style glass), so the layer
                // must be captured again. Skipped automatically when the glass
                // quality is FROST (low RAM / battery saver).
                .glassSource(backdrop)

                .pointerInput(tourActive) {
                    if (tourActive) return@pointerInput
                    axisLockedPagerGestures(
                        state = pagerState,
                        scope = coroutineScope
                    )
                }
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                floatingActionButton = {
                    GlassFAB(
                        onClick = { onNavigateToAdd(null) },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 84.dp)
                            .coachTag("fab_add"),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_add),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            ) { innerPadding ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,

                    userScrollEnabled = false
                ) { page ->
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val pageAlpha = 1f - (abs(pageOffset) * 0.12f).coerceIn(0f, 0.25f)
                    val pageScale = 1f - (abs(pageOffset) * 0.03f).coerceIn(0f, 0.04f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = pageAlpha
                                scaleX = pageScale
                                scaleY = pageScale
                            }
                    ) {
                        when (page) {
                            0 -> HomeScreen(
                                viewModel = viewModel,
                                bottomPadding = 120.dp
                            )
                            1 -> CalendarScreen(
                                viewModel = viewModel,
                                bottomPadding = 120.dp
                            )
                            2 -> MedicationsListScreen(
                                viewModel = viewModel,
                                bottomPadding = 120.dp,
                                onEditMedication = { medId -> onNavigateToAdd(medId) }
                            )
                            3 -> SettingsScreen(
                                viewModel = viewModel,
                                bottomPadding = 120.dp
                            )
                        }
                    }
                }
            }
        }

        LiquidGlassPanel(
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth()
                .coachTag("nav_island"),
            shape = RoundedCornerShape(32.dp),
            elevation = 18.dp,
            // Telegram-grade radius (the v2.0–v2.1 bar value) — real visible
            // blur, whisper tint, no lens.
            blurRadius = 30.dp,
            classic = true
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                val tabWidth = maxWidth / 4

                val indicatorOffset = tabWidth * effectiveFraction

                // Smooth sliding active-tab pill: clean, borderless, softly

                // just marks the selected tab. Jelly stretch physics intact.
                val pillColor = MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .height(54.dp)
                        .graphicsLayer {
                            scaleX = bubbleScaleX
                            scaleY = bubbleScaleY
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .drawBehind {
                                // soft vertical pill gradient: barely-there at the

                                // light pooling inside the glass
                                val r = 20.dp.toPx()
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            pillColor.copy(alpha = if (isDraggingIsland) 0.20f else 0.14f),
                                            pillColor.copy(alpha = if (isDraggingIsland) 0.34f else 0.24f)
                                        ),
                                        startY = 0f,
                                        endY = size.height
                                    ),
                                    cornerRadius = CornerRadius(r, r)
                                )
                            }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDraggingIsland = true
                                    val tabW = size.width / 4f
                                    if (tabW > 0f) {
                                        val targetFrac = (offset.x / tabW - 0.5f)
                                        coroutineScope.launch {
                                            islandFractionAnim.animateTo(
                                                targetValue = targetFrac,
                                                animationSpec = spring(
                                                    dampingRatio = 0.75f,
                                                    stiffness = 450f
                                                )
                                            )
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val tabW = size.width / 4f
                                    if (tabW > 0f) {
                                        val currentVal = islandFractionAnim.value
                                        // When past boundaries, apply drag resistance
                                        val resistance = if (currentVal < 0f || currentVal > 3f) 0.35f else 1f
                                        val newFrac = currentVal + (dragAmount.x / tabW) * resistance
                                        coroutineScope.launch {
                                            islandFractionAnim.snapTo(newFrac)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    val targetTab = islandFractionAnim.value.roundToInt().coerceIn(0, 3)
                                    coroutineScope.launch {
                                        launch {
                                            islandFractionAnim.animateTo(
                                                targetValue = targetTab.toFloat(),
                                                animationSpec = spring(
                                                    dampingRatio = 0.68f,
                                                    stiffness = 300f
                                                )
                                            )
                                            isDraggingIsland = false
                                        }
                                        pagerState.animateScrollToPage(
                                            targetTab,
                                            animationSpec = spring(
                                                dampingRatio = 0.84f,
                                                stiffness = 320f
                                            )
                                        )
                                    }
                                },
                                onDragCancel = {
                                    val targetTab = islandFractionAnim.value.roundToInt().coerceIn(0, 3)
                                    coroutineScope.launch {
                                        launch {
                                            islandFractionAnim.animateTo(
                                                targetValue = targetTab.toFloat(),
                                                animationSpec = spring(
                                                    dampingRatio = 0.68f,
                                                    stiffness = 300f
                                                )
                                            )
                                            isDraggingIsland = false
                                        }
                                        pagerState.animateScrollToPage(
                                            targetTab,
                                            animationSpec = spring(
                                                dampingRatio = 0.84f,
                                                stiffness = 320f
                                            )
                                        )
                                    }
                                }
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingNavItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.nav_today)) },
                        label = stringResource(R.string.nav_today),
                        itemIndex = 0,
                        currentFraction = effectiveFraction,
                        onClick = {
                            isDraggingIsland = false
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    0,
                                    animationSpec = spring(
                                        dampingRatio = 0.84f,
                                        stiffness = 320f
                                    )
                                )
                            }
                        }
                    )
                    FloatingNavItem(
                        icon = { Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.nav_calendar)) },
                        label = stringResource(R.string.nav_calendar),
                        itemIndex = 1,
                        currentFraction = effectiveFraction,
                        onClick = {
                            isDraggingIsland = false
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    1,
                                    animationSpec = spring(
                                        dampingRatio = 0.84f,
                                        stiffness = 320f
                                    )
                                )
                            }
                        }
                    )
                    FloatingNavItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.nav_meds)) },
                        label = stringResource(R.string.nav_meds),
                        itemIndex = 2,
                        currentFraction = effectiveFraction,
                        onClick = {
                            isDraggingIsland = false
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    2,
                                    animationSpec = spring(
                                        dampingRatio = 0.84f,
                                        stiffness = 320f
                                    )
                                )
                            }
                        }
                    )
                    FloatingNavItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                        label = stringResource(R.string.nav_settings),
                        itemIndex = 3,
                        currentFraction = effectiveFraction,
                        onClick = {
                            isDraggingIsland = false
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    3,
                                    animationSpec = spring(
                                        dampingRatio = 0.84f,
                                        stiffness = 320f
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.FloatingNavItem(
    icon: @Composable () -> Unit,
    label: String,
    itemIndex: Int,
    currentFraction: Float,
    onClick: () -> Unit
) {
    // Calculate continuous proximity (1.0 = fully active, 0.0 = inactive)
    val distance = abs(currentFraction - itemIndex)
    val proximity = (1f - distance).coerceIn(0f, 1f)

    // Little organic pop when the item becomes the active one
    val activationPop by animateFloatAsState(
        targetValue = if (proximity > 0.6f) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navPop"
    )

    val scale = 1f + 0.16f * proximity + 0.03f * activationPop
    val yOffset = (-3.5f * proximity).dp

    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.66f)
    val activeColor = MaterialTheme.colorScheme.primary
    val contentColor = lerp(inactiveColor, activeColor, proximity)

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(22.dp))
            .tactilePress(pressScale = 0.90f, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = yOffset.toPx()
                    scaleX = scale
                    scaleY = scale
                }
                .padding(horizontal = 10.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                LocalContentColor provides contentColor
            ) {
                icon()
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        val textMeasurer = rememberTextMeasurer()
        val baseLabelStyle = MaterialTheme.typography.labelSmall
        BoxWithConstraints(
            modifier = Modifier
                .graphicsLayer {
                    translationY = (yOffset / 2.5f).toPx()
                }
        ) {
            val naturalWidthPx = remember(label, baseLabelStyle) {
                textMeasurer.measure(
                    text = label,
                    style = baseLabelStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    maxLines = 1,
                    constraints = Constraints(maxWidth = 100_000)
                ).size.width
            }

            val density = LocalDensity.current
            val availableWidthPx = with(density) { constraints.maxWidth - 4.dp.roundToPx() }
            val fitScale = if (naturalWidthPx > 0) {
                (availableWidthPx.toFloat() / naturalWidthPx).coerceIn(0.5f, 1f)
            } else 1f
            Text(
                text = label,
                style = baseLabelStyle,
                fontSize = 10.sp * fitScale,
                fontWeight = if (proximity > 0.5f) FontWeight.ExtraBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            )
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope
        .axisLockedPagerGestures(
    state: PagerState,
    scope: CoroutineScope
) {
    val slop = viewConfiguration.touchSlop
    val flingPx = 560.dp.toPx()
    val axisRatio = 2.2f
    val commitPx = slop * 1.6f
    var settleJob: Job? = null

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)

        val tracker = VelocityTracker()
        tracker.resetTracking()
        tracker.addPosition(down.uptimeMillis, down.position)

        var horizontal = false
        var dx = 0f
        var dy = 0f

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break

            if (change.isConsumed) {
                continue
            }

            val delta = change.positionChange()
            if (delta.x == 0f && delta.y == 0f) continue

            tracker.addPosition(change.uptimeMillis, change.position)
            dx += delta.x
            dy += delta.y

            if (!horizontal) {
                val pageWantsNext = dx < 0f
                val pagerCanMove = if (pageWantsNext) {
                    state.currentPage < state.pageCount - 1
                } else {
                    state.currentPage > 0
                }
                if (pagerCanMove &&
                    abs(dx) > commitPx &&
                    abs(dx) > axisRatio * abs(dy)
                ) {
                    settleJob?.cancel()
                    horizontal = true
                }
            }

            if (horizontal) {
                change.consume()

                state.dispatchRawDelta(-delta.x)
            }
        }

        if (horizontal) {
            val velocity = tracker.calculateVelocity().x
            val base = state.currentPage + state.currentPageOffsetFraction
            val target = when {
                velocity < -flingPx -> kotlin.math.ceil(base)
                velocity > flingPx -> kotlin.math.floor(base)
                else -> kotlin.math.round(base)
            }.toInt().coerceIn(0, state.pageCount - 1)

            settleJob = scope.launch {
                state.animateScrollToPage(
                    target,
                    animationSpec = spring(dampingRatio = 0.84f, stiffness = 320f)
                )
            }
        } else if (state.currentPageOffsetFraction != 0f) {
            settleJob = scope.launch {
                state.animateScrollToPage(
                    state.currentPage,
                    animationSpec = spring(dampingRatio = 0.84f, stiffness = 320f)
                )
            }
        }
    }
}
