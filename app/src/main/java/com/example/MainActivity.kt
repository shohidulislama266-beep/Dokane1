package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.repository.StoreRepository
import com.example.domain.ShopType
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainShopScreen
import com.example.ui.screens.StoreOnboardingScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.StoreOSTheme
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.LocalAppLanguage
import com.example.util.NetworkMonitor
import com.google.firebase.FirebaseApp

sealed class AppSessionMode {
    data class Real(val generation: Int = 0) : AppSessionMode()
    data class Demo(val shopType: ShopType, val generation: Int = 0) : AppSessionMode()
}

class MainActivity : ComponentActivity() {
    private var pendingIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingIntent = intent
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        configureSystemBarAppearance()

        setContent {
            StoreOSTheme {
                StoreOsAppHost(
                    pendingIntent = pendingIntent,
                    onIntentHandled = { pendingIntent = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntent = intent
    }

    override fun onResume() {
        super.onResume()
        configureSystemBarAppearance()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            configureSystemBarAppearance()
        }
    }

    private fun configureSystemBarAppearance() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
fun StoreOsAppHost(
    initialMode: AppSessionMode = AppSessionMode.Real(0),
    pendingIntent: Intent? = null,
    onIntentHandled: () -> Unit = {},
    ensureFirebaseInitialized: (Context) -> Unit = { ctx ->
        if (FirebaseApp.getApps(ctx).isEmpty()) {
            FirebaseApp.initializeApp(ctx)
        }
    },
    realRepositoryFactory: (Context) -> StoreRepository = { ctx ->
        StoreRepository(ctx)
    },
    demoRepositoryFactory: (Context) -> StoreRepository = { ctx ->
        StoreRepository(db = null, appContext = ctx.applicationContext)
    },
    realAuthViewModelFactory: (StoreRepository) -> AuthViewModel = { repo ->
        AuthViewModel(repo)
    },
    demoAuthViewModelFactory: (StoreRepository, ShopType, () -> Unit) -> AuthViewModel = { repo, shopType, onExitDemo ->
        AuthViewModel.createDemo(
            repository = repo,
            shopType = shopType,
            onExitDemo = onExitDemo
        )
    },
    onSessionControllersChanged: ((AuthViewModel, StoreViewModel?) -> Unit)? = null
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    var sessionMode by remember { mutableStateOf(initialMode) }
    var sessionGeneration by remember {
        mutableIntStateOf(
            when (initialMode) {
                is AppSessionMode.Real -> initialMode.generation
                is AppSessionMode.Demo -> initialMode.generation
            }
        )
    }

    when (val mode = sessionMode) {
        is AppSessionMode.Demo -> {
            // Completely isolated local session: never initializes or touches FirebaseAuth or FirebaseFirestore.
            key("demo", mode.shopType, mode.generation) {
                val demoRepository = remember(mode) {
                    demoRepositoryFactory(appContext).apply {
                        configureDemoStore(mode.shopType)
                    }
                }
                val demoAuthViewModel = remember(mode) {
                    demoAuthViewModelFactory(demoRepository, mode.shopType) {
                        demoRepository.clearDemoLocalPrefs()
                        val nextGen = sessionGeneration + 1
                        sessionGeneration = nextGen
                        sessionMode = AppSessionMode.Real(nextGen)
                    }
                }
                val networkMonitor = remember(mode) { NetworkMonitor(appContext) }
                val demoStoreViewModel = remember(mode) {
                    StoreViewModel(
                        storeId = "demo_store",
                        repository = demoRepository,
                        networkMonitor = networkMonitor,
                        currentUserId = "demo_user"
                    )
                }

                LaunchedEffect(demoAuthViewModel, demoStoreViewModel) {
                    onSessionControllersChanged?.invoke(demoAuthViewModel, demoStoreViewModel)
                }

                DisposableEffect(demoAuthViewModel, demoStoreViewModel) {
                    onDispose {
                        demoStoreViewModel.dispose()
                        demoAuthViewModel.dispose()
                    }
                }

                LaunchedEffect(pendingIntent, demoStoreViewModel) {
                    if (pendingIntent != null) {
                        demoStoreViewModel.handleAppIntent(pendingIntent)
                        onIntentHandled()
                    }
                }

                val currentLanguage by demoAuthViewModel.currentLanguage.collectAsState()
                CompositionLocalProvider(LocalAppLanguage provides currentLanguage) {
                    MainShopScreen(
                        authViewModel = demoAuthViewModel,
                        storeViewModel = demoStoreViewModel
                    )
                }
            }
        }

        is AppSessionMode.Real -> {
            key("real", mode.generation) {
                val realRepository = remember(mode) {
                    ensureFirebaseInitialized(appContext)
                    realRepositoryFactory(appContext)
                }
                val realAuthViewModel = remember(mode) {
                    realAuthViewModelFactory(realRepository)
                }

                LaunchedEffect(realAuthViewModel) {
                    onSessionControllersChanged?.invoke(realAuthViewModel, null)
                }

                DisposableEffect(realAuthViewModel) {
                    onDispose {
                        realAuthViewModel.dispose()
                    }
                }

                AppRoot(
                    authViewModel = realAuthViewModel,
                    repository = realRepository,
                    pendingIntent = pendingIntent,
                    onIntentHandled = onIntentHandled,
                    onEnterDemo = { shopType ->
                        val nextGen = sessionGeneration + 1
                        sessionGeneration = nextGen
                        sessionMode = AppSessionMode.Demo(shopType = shopType, generation = nextGen)
                    },
                    onRealStoreViewModelReady = { storeVm ->
                        onSessionControllersChanged?.invoke(realAuthViewModel, storeVm)
                    }
                )
            }
        }
    }
}

@Composable
fun AppRoot(
    authViewModel: AuthViewModel,
    repository: StoreRepository,
    pendingIntent: Intent? = null,
    onIntentHandled: () -> Unit = {},
    onEnterDemo: ((ShopType) -> Unit)? = null,
    onRealStoreViewModelReady: ((StoreViewModel) -> Unit)? = null
) {
    val uiState by authViewModel.uiState.collectAsState()
    val currentLanguage by authViewModel.currentLanguage.collectAsState()

    CompositionLocalProvider(LocalAppLanguage provides currentLanguage) {
        when (val state = uiState) {
            is AuthUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                        .statusBarsPadding()
                        .displayCutoutPadding()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = VividGreen,
                        strokeWidth = 3.dp
                    )
                }
            }

            is AuthUiState.Unauthenticated -> {
                AuthScreen(
                    authViewModel = authViewModel,
                    onEnterDemo = onEnterDemo
                )
            }

            is AuthUiState.AuthenticatedNoStore -> {
                StoreOnboardingScreen(authViewModel = authViewModel)
            }

            is AuthUiState.AuthenticatedWithStore -> {
                // Keyed by uid and storeId to ensure zero state leaks across accounts or stores
                key(state.user.uid, state.storeId) {
                    val context = LocalContext.current
                    val networkMonitor = remember { NetworkMonitor(context) }
                    val storeViewModel = remember(state.user.uid, state.storeId) {
                        StoreViewModel(state.storeId, repository, networkMonitor, currentUserId = state.user.uid)
                    }
                    LaunchedEffect(storeViewModel) {
                        onRealStoreViewModelReady?.invoke(storeViewModel)
                    }
                    DisposableEffect(storeViewModel) {
                        onDispose {
                            storeViewModel.dispose()
                        }
                    }
                    LaunchedEffect(pendingIntent, storeViewModel) {
                        if (pendingIntent != null) {
                            storeViewModel.handleAppIntent(pendingIntent)
                            onIntentHandled()
                        }
                    }
                    MainShopScreen(
                        authViewModel = authViewModel,
                        storeViewModel = storeViewModel
                    )
                }
            }

            is AuthUiState.DemoMode -> {
                key("demo_user", state.storeId) {
                    val context = LocalContext.current
                    val networkMonitor = remember { NetworkMonitor(context) }
                    val storeViewModel = remember(state.storeId) {
                        StoreViewModel(state.storeId, repository, networkMonitor, currentUserId = "demo_user")
                    }
                    LaunchedEffect(storeViewModel) {
                        onRealStoreViewModelReady?.invoke(storeViewModel)
                    }
                    DisposableEffect(storeViewModel) {
                        onDispose {
                            storeViewModel.dispose()
                        }
                    }
                    LaunchedEffect(pendingIntent, storeViewModel) {
                        if (pendingIntent != null) {
                            storeViewModel.handleAppIntent(pendingIntent)
                            onIntentHandled()
                        }
                    }
                    MainShopScreen(
                        authViewModel = authViewModel,
                        storeViewModel = storeViewModel
                    )
                }
            }
        }
    }
}
