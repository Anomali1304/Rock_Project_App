package com.rockproject.mt6789

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rockproject.mt6789.ui.components.FloatingToast
import com.rockproject.mt6789.ui.components.RockNavbar
import com.rockproject.mt6789.ui.screens.*
import com.rockproject.mt6789.ui.theme.RockProjectTheme
import com.rockproject.mt6789.ui.theme.RockTheme
import com.rockproject.mt6789.viewmodel.MainViewModel

enum class AppScreen(val title: String) {
    HOME("Rock Project"),
    CPU_MENU("CPU"),
    GPU_MENU("GPU"),
    CPU_OC("CPU Overclock"),
    GPU_OC("GPU Overclock"),
    CPU_CONTROL("CPU Control"),
    GPU_CONTROL("GPU Control"),
    BOOT_SETTINGS("Apply on Boot")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
            val moduleState by viewModel.moduleState.collectAsStateWithLifecycle()
            val stockFloors by viewModel.stockFloors.collectAsStateWithLifecycle()
            val liveStats by viewModel.liveStats.collectAsStateWithLifecycle()
            val cpuBrandInfo by viewModel.cpuBrandInfo.collectAsStateWithLifecycle()
            val gpuBrandInfo by viewModel.gpuBrandInfo.collectAsStateWithLifecycle()
            val bootApplyEnabled by viewModel.bootApplyEnabled.collectAsStateWithLifecycle()
            val bootResult by viewModel.bootResult.collectAsStateWithLifecycle()
            val showMiuiCard by viewModel.showMiuiCard.collectAsStateWithLifecycle()
            val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
            val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
            val isCpuDirty by viewModel.isCpuDirty.collectAsStateWithLifecycle()
            val isGpuDirty by viewModel.isGpuDirty.collectAsStateWithLifecycle()
            val isCpuRestoreVisible by viewModel.isCpuRestoreVisible.collectAsStateWithLifecycle()
            val isGpuRestoreVisible by viewModel.isGpuRestoreVisible.collectAsStateWithLifecycle()

            val cpuLLSlider by viewModel.cpuLLSlider.collectAsStateWithLifecycle()
            val cpuBSlider by viewModel.cpuBSlider.collectAsStateWithLifecycle()
            val gpuFreqSlider by viewModel.gpuFreqSlider.collectAsStateWithLifecycle()
            val gpuVoltSlider by viewModel.gpuVoltSlider.collectAsStateWithLifecycle()

            val cpuLittleControl by viewModel.cpuLittleControl.collectAsStateWithLifecycle()
            val cpuBigControl by viewModel.cpuBigControl.collectAsStateWithLifecycle()
            val gpuControl by viewModel.gpuControl.collectAsStateWithLifecycle()

            val backStack = remember { mutableStateListOf(AppScreen.HOME) }
            val currentScreen = backStack.lastOrNull() ?: AppScreen.HOME

            var showSplash by remember { mutableStateOf(true) }
            var startFadeOut by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1100)
                startFadeOut = true
                kotlinx.coroutines.delay(300)
                showSplash = false
            }

            val splashAlpha by animateFloatAsState(
                targetValue = if (startFadeOut) 0f else 1f,
                animationSpec = tween(durationMillis = 300),
                label = "splashAlpha"
            )
            val splashScale by animateFloatAsState(
                targetValue = if (startFadeOut) 1.1f else 1f,
                animationSpec = tween(durationMillis = 300),
                label = "splashScale"
            )

            // Lifecycle-aware polling control
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> viewModel.startPolling()
                        Lifecycle.Event.ON_STOP -> viewModel.stopPolling()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            // Handle hardware back press
            BackHandler(enabled = backStack.size > 1) {
                backStack.removeAt(backStack.size - 1)
            }

            // Auto dismiss toast after 2.6s
            LaunchedEffect(toastMessage) {
                if (toastMessage != null) {
                    kotlinx.coroutines.delay(2600)
                    viewModel.clearToast()
                }
            }

            fun navigateTo(screen: AppScreen) {
                if (currentScreen != screen) {
                    backStack.add(screen)
                }
            }

            fun goBack() {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.size - 1)
                }
            }

            RockProjectTheme(darkTheme = darkTheme) {
                val colors = RockTheme.colors
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
                ) {
                    if (showSplash) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = splashAlpha
                                    scaleX = splashScale
                                    scaleY = splashScale
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .background(
                                            color = colors.surfaceRaised,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(40.dp)
                                        )
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_rock_logo),
                                        contentDescription = "App Logo",
                                        modifier = Modifier.size(104.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Rock Project",
                                    color = colors.label,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                        }
                    } else {
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colors.background),
                            topBar = {
                                RockNavbar(
                                    title = currentScreen.title,
                                    statusText = moduleState.statusText,
                                    dotStatus = moduleState.dotStatus,
                                    showBackButton = backStack.size > 1,
                                    onBackClick = { goBack() },
                                    isDarkTheme = darkTheme,
                                    onToggleTheme = { viewModel.toggleTheme() },
                                    onNavigateBootSettings = if (currentScreen == AppScreen.HOME) {
                                        { navigateTo(AppScreen.BOOT_SETTINGS) }
                                    } else null
                                )
                            },
                            containerColor = colors.background,
                            contentWindowInsets = WindowInsets(0.dp)
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = innerPadding.calculateTopPadding())
                                    .navigationBarsPadding()
                            ) {
                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = {
                                        val isPush = targetState.ordinal > initialState.ordinal
                                        if (isPush) {
                                            (slideInHorizontally(
                                                animationSpec = spring(dampingRatio = 0.9f, stiffness = 300f),
                                                initialOffsetX = { fullWidth -> fullWidth }
                                            ) + fadeIn(animationSpec = tween(220))) togetherWith
                                            (slideOutHorizontally(
                                                animationSpec = spring(dampingRatio = 0.9f, stiffness = 300f),
                                                targetOffsetX = { fullWidth -> -fullWidth / 3 }
                                            ) + fadeOut(animationSpec = tween(220)))
                                        } else {
                                            (slideInHorizontally(
                                                animationSpec = spring(dampingRatio = 0.9f, stiffness = 300f),
                                                initialOffsetX = { fullWidth -> -fullWidth / 3 }
                                            ) + fadeIn(animationSpec = tween(220))) togetherWith
                                            (slideOutHorizontally(
                                                animationSpec = spring(dampingRatio = 0.9f, stiffness = 300f),
                                                targetOffsetX = { fullWidth -> fullWidth }
                                            ) + fadeOut(animationSpec = tween(220)))
                                        }
                                    },
                                    label = "IOSScreenTransition"
                                ) { screen ->
                                    when (screen) {
                                        AppScreen.HOME -> HomeScreen(
                                            cpuInfo = cpuBrandInfo,
                                            gpuInfo = gpuBrandInfo,
                                            liveStats = liveStats,
                                            onNavigateCpuMenu = { navigateTo(AppScreen.CPU_MENU) },
                                            onNavigateGpuMenu = { navigateTo(AppScreen.GPU_MENU) }
                                        )
                                        AppScreen.CPU_MENU -> CpuMenuScreen(
                                            onNavigateCpuOc = { navigateTo(AppScreen.CPU_OC) },
                                            onNavigateCpuControl = {
                                                viewModel.refreshControls()
                                                navigateTo(AppScreen.CPU_CONTROL)
                                            }
                                        )
                                        AppScreen.GPU_MENU -> GpuMenuScreen(
                                            onNavigateGpuOc = { navigateTo(AppScreen.GPU_OC) },
                                            onNavigateGpuControl = {
                                                viewModel.refreshControls()
                                                navigateTo(AppScreen.GPU_CONTROL)
                                            }
                                        )
                                         AppScreen.CPU_OC -> CpuOcScreen(
                                            cpuLLValue = cpuLLSlider,
                                            cpuBValue = cpuBSlider,
                                            stockFloors = stockFloors,
                                            isBusy = isBusy,
                                            isDirty = isCpuDirty,
                                            isRestoreVisible = isCpuRestoreVisible,
                                            onCpuLLChanged = { viewModel.onCpuLLChanged(it) },
                                            onCpuBChanged = { viewModel.onCpuBChanged(it) },
                                            onSliderDragStart = { viewModel.onSliderDragStart() },
                                            onSliderDragEnd = { viewModel.onSliderDragEnd() },
                                            onApply = { viewModel.applyCpu() },
                                            onRestore = { viewModel.restoreCpu() }
                                        )
                                        AppScreen.GPU_OC -> GpuOcScreen(
                                            gpuFreqValue = gpuFreqSlider,
                                            gpuVoltValue = gpuVoltSlider,
                                            stockFloors = stockFloors,
                                            isBusy = isBusy,
                                            isDirty = isGpuDirty,
                                            isRestoreVisible = isGpuRestoreVisible,
                                            onGpuFreqChanged = { viewModel.onGpuFreqChanged(it) },
                                            onGpuVoltChanged = { viewModel.onGpuVoltChanged(it) },
                                            onSliderDragStart = { viewModel.onSliderDragStart() },
                                            onSliderDragEnd = { viewModel.onSliderDragEnd() },
                                            onApply = { viewModel.applyGpu() },
                                            onRestore = { viewModel.restoreGpu() }
                                        )
                                        AppScreen.CPU_CONTROL -> CpuControlScreen(
                                            littleControl = cpuLittleControl,
                                            bigControl = cpuBigControl,
                                            onSetLittleGov = { viewModel.setCpuLittleGov(it) },
                                            onSetLittleMin = { viewModel.setCpuLittleMin(it) },
                                            onSetLittleMax = { viewModel.setCpuLittleMax(it) },
                                            onSetBigGov = { viewModel.setCpuBigGov(it) },
                                            onSetBigMin = { viewModel.setCpuBigMin(it) },
                                            onSetBigMax = { viewModel.setCpuBigMax(it) }
                                        )
                                        AppScreen.GPU_CONTROL -> GpuControlScreen(
                                            gpuControl = gpuControl,
                                            onSetGpuGov = { viewModel.setGpuGov(it) },
                                            onSetGpuMin = { viewModel.setGpuMin(it) },
                                            onSetGpuMax = { viewModel.setGpuMax(it) }
                                        )
                                        AppScreen.BOOT_SETTINGS -> BootApplyScreen(
                                            bootApplyEnabled = bootApplyEnabled,
                                            bootResult = bootResult,
                                            showMiuiCard = showMiuiCard,
                                            isBusy = isBusy,
                                            onToggleBootApply = { viewModel.setBootApplyEnabled(it) },
                                            onTestBootApplyNow = { viewModel.testBootApplyNow() },
                                            onDismissMiuiCard = { viewModel.dismissMiuiCard() }
                                        )
                                    }
                                }

                                // Floating Toast
                                FloatingToast(
                                    message = toastMessage,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
