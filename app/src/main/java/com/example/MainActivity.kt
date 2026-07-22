package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.navigation.compose.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.ui.AddMedicationScreen
import com.example.ui.CalendarScreen
import com.example.ui.HomeScreen
import com.example.ui.MedicationsListScreen
import com.example.ui.SettingsScreen
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.data.ThemeMode

@Composable
fun MyAppThemeWrapper(viewModel: MainViewModel, content: @Composable () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsState()
    MyApplicationTheme(themeMode = themeMode) {
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
        
        // Request highest refresh rate (e.g. 120 FPS) for buttery smooth animations
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val highestMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
                if (highestMode != null) {
                    val params = window.attributes
                    params.preferredDisplayModeId = highestMode.modeId
                    window.attributes = params
                }
            } catch (e: Exception) {
                // Ignore if display info or window params are restricted
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                window.attributes.preferredRefreshRate = 120f
            } catch (e: Exception) {
                // Ignore
            }
        }

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
    
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination?.route ?: "home"
    
    val showBottomBar = currentDestination in listOf("home", "calendar", "meds", "settings")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                if (showBottomBar) {
                    FloatingActionButton(
                        onClick = { navController.navigate("add") },
                        modifier = Modifier.padding(bottom = 100.dp), // Float elegantly above the hovering floating navigation bar
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, stringResource(R.string.action_add))
                    }
                }
            }
        ) { innerPadding ->
            val getRouteIndex = { route: String? ->
                when (route) {
                    "home" -> 0
                    "calendar" -> 1
                    "meds" -> 2
                    "settings" -> 3
                    else -> 0
                }
            }

            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                enterTransition = {
                    val targetRoute = targetState.destination.route
                    val initialRoute = initialState.destination.route
                    if (targetRoute == "add") {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Up,
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )
                    } else if (initialRoute == "add") {
                        fadeIn(animationSpec = tween(200))
                    } else {
                        val targetIndex = getRouteIndex(targetRoute)
                        val initialIndex = getRouteIndex(initialRoute)
                        val direction = if (targetIndex > initialIndex) {
                            AnimatedContentTransitionScope.SlideDirection.Left
                        } else {
                            AnimatedContentTransitionScope.SlideDirection.Right
                        }
                        slideIntoContainer(
                            direction,
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(250))
                    }
                },
                exitTransition = {
                    val targetRoute = targetState.destination.route
                    val initialRoute = initialState.destination.route
                    if (initialRoute == "add") {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Down,
                            animationSpec = tween(280, easing = FastOutLinearInEasing)
                        )
                    } else if (targetRoute == "add") {
                        fadeOut(animationSpec = tween(200))
                    } else {
                        val targetIndex = getRouteIndex(targetRoute)
                        val initialIndex = getRouteIndex(initialRoute)
                        val direction = if (targetIndex > initialIndex) {
                            AnimatedContentTransitionScope.SlideDirection.Left
                        } else {
                            AnimatedContentTransitionScope.SlideDirection.Right
                        }
                        slideOutOfContainer(
                            direction,
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(250))
                    }
                }
            ) {
                composable("home") { HomeScreen(viewModel, 120.dp) }
                composable("calendar") { 
                    CalendarScreen(viewModel, 120.dp)
                }
                composable("meds") { 
                    MedicationsListScreen(viewModel, 120.dp)
                }
                composable("settings") { 
                    SettingsScreen(viewModel, 120.dp)
                }
                composable("add") {
                    AddMedicationScreen(
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = viewModel
                    )
                }
            }
        }

        if (showBottomBar) {
            // Liquid Glass experimental floating navigation capsule
            val isDark = isSystemInDarkTheme()
            val glassBgGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = if (isDark) {
                    listOf(
                        Color.White.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.60f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                    )
                }
            )

            val glassBorderBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.65f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                        clip = false,
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    )
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                    .background(brush = glassBgGradient)
                    .border(
                        width = 1.2.dp,
                        brush = glassBorderBrush,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingNavItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.nav_today)) },
                        label = stringResource(R.string.nav_today),
                        selected = currentDestination == "home",
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    FloatingNavItem(
                        icon = { Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.nav_calendar)) },
                        label = stringResource(R.string.nav_calendar),
                        selected = currentDestination == "calendar",
                        onClick = {
                            navController.navigate("calendar") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    FloatingNavItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.nav_meds)) },
                        label = stringResource(R.string.nav_meds),
                        selected = currentDestination == "meds",
                        onClick = {
                            navController.navigate("meds") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    FloatingNavItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                        label = stringResource(R.string.nav_settings),
                        selected = currentDestination == "settings",
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
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
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillScale"
    )

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (selected) 0.18f else 0.0f,
        animationSpec = tween(durationMillis = 200),
        label = "pillBgAlpha"
    )

    val translationY by animateDpAsState(
        targetValue = if (selected) (-4).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "translationY"
    )

    val targetContentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    }

    val contentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(durationMillis = 200),
        label = "contentColorAnim"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .offset(y = translationY)
                .scale(scale)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha))
                .padding(horizontal = 20.dp, vertical = 6.dp),
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
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
