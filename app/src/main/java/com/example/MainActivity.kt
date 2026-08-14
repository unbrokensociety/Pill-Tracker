package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.navigation.compose.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.ui.AddMedicationScreen
import com.example.ui.CalendarScreen
import com.example.ui.HomeScreen
import com.example.ui.MedicationsListScreen
import com.example.ui.SettingsScreen
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.components.liquidGlass
import com.example.ui.components.islandGlass
import com.example.ui.components.tactilePress
import com.example.ui.components.GlassFAB
import com.example.ui.theme.MyApplicationTheme
import com.example.data.ThemeMode
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView

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

    override fun attachBaseContext(newBase: android.content.Context) {
        val lang = com.example.ui.locale.LocaleHelper.getLanguage(newBase)
        val contextWithLocale = com.example.ui.locale.LocaleHelper.updateResources(newBase, lang)
        super.attachBaseContext(contextWithLocale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notifications permission on Android 13+ dynamically to guarantee notifications are delivered
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 1001)
            }
        }

        setContent {
            MyAppThemeWrapper(viewModel) {
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()

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
}

@Composable
fun MainPagerScreen(
    viewModel: MainViewModel,
    onNavigateToAdd: (Int?) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val pagerFraction by remember {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 3f)
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

    val liveFraction = if (isDraggingIsland) islandFractionAnim.value else pagerFraction

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                GlassFAB(
                    onClick = { onNavigateToAdd(null) },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 84.dp),
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
                userScrollEnabled = true
            ) { page ->
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

        // Floating Tactile Glass Navigation Island Capsule with Backdrop Blur, Specular Rim & Fluid Dragging
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .islandGlass(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                    elevation = 18.dp
                )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                val tabWidth = maxWidth / 4

                val indicatorOffset = tabWidth * liveFraction

                // Dynamic liquid stretch effect proportional to movement
                val stretchFactor = if (isDraggingIsland) 0.08f else {
                    val offsetFraction = pagerState.currentPageOffsetFraction
                    (abs(offsetFraction) * 0.16f).coerceAtMost(0.22f)
                }

                // Smooth sliding Liquid Glass active tab pill indicator with specular edge & glow
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .height(54.dp)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .graphicsLayer {
                            scaleX = 1f + stretchFactor
                            scaleY = if (isDraggingIsland) 1.05f else 1f
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
                            clip = true
                        }
                        .liquidGlass(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                            customGlassColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                            elevation = if (isDraggingIsland) 8.dp else 6.dp,
                            borderWidth = 1.2.dp
                        )
                )

                // Navigation Row with Interactive Drag Gesture & Smooth Snapping
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDraggingIsland = true
                                    val tabW = size.width / 4f
                                    if (tabW > 0f) {
                                        val targetFrac = (offset.x / tabW - 0.5f).coerceIn(0f, 3f)
                                        coroutineScope.launch {
                                            islandFractionAnim.animateTo(
                                                targetValue = targetFrac,
                                                animationSpec = spring(
                                                    dampingRatio = 0.85f,
                                                    stiffness = 500f
                                                )
                                            )
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val tabW = size.width / 4f
                                    if (tabW > 0f) {
                                        val newFrac = (islandFractionAnim.value + dragAmount.x / tabW).coerceIn(0f, 3f)
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
                                                    dampingRatio = 0.76f,
                                                    stiffness = 340f
                                                )
                                            )
                                            isDraggingIsland = false
                                        }
                                        pagerState.animateScrollToPage(
                                            targetTab,
                                            animationSpec = spring(
                                                dampingRatio = 0.80f,
                                                stiffness = 340f
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
                                                    dampingRatio = 0.76f,
                                                    stiffness = 340f
                                                )
                                            )
                                            isDraggingIsland = false
                                        }
                                        pagerState.animateScrollToPage(
                                            targetTab,
                                            animationSpec = spring(
                                                dampingRatio = 0.80f,
                                                stiffness = 340f
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
                        currentFraction = liveFraction,
                        onClick = {
                            isDraggingIsland = false
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    0,
                                    animationSpec = spring(
                                        dampingRatio = 0.80f,
                                        stiffness = 340f
                                    )
                                )
                            }
                        }
                    )
                    FloatingNavItem(
                        icon = { Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.nav_calendar)) },
                        label = stringResource(R.string.nav_calendar),
                        itemIndex = 1,
                        currentFraction = liveFraction,
                        onClick = {
                            isDraggingIsland = false
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    1,
                                    animationSpec = spring(
                                        dampingRatio = 0.80f,
                                        stiffness = 340f
                                    )
                                )
                            }
                        }
                    )
                    FloatingNavItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.nav_meds)) },
                        label = stringResource(R.string.nav_meds),
                        itemIndex = 2,
                        currentFraction = liveFraction,
                        onClick = {
                            isDraggingIsland = false
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    2,
                                    animationSpec = spring(
                                        dampingRatio = 0.80f,
                                        stiffness = 340f
                                    )
                                )
                            }
                        }
                    )
                    FloatingNavItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                        label = stringResource(R.string.nav_settings),
                        itemIndex = 3,
                        currentFraction = liveFraction,
                        onClick = {
                            isDraggingIsland = false
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    3,
                                    animationSpec = spring(
                                        dampingRatio = 0.80f,
                                        stiffness = 340f
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

    val scale = 1f + 0.16f * proximity
    val yOffset = (-3.5f * proximity).dp

    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
    val activeColor = MaterialTheme.colorScheme.onPrimaryContainer
    val contentColor = lerp(inactiveColor, activeColor, proximity)

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
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

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = if (proximity > 0.5f) FontWeight.ExtraBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier
                .graphicsLayer {
                    translationY = (yOffset / 2.5f).toPx()
                }
                .padding(horizontal = 2.dp)
        )
    }
}
