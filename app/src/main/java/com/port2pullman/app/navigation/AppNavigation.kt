package com.port2pullman.app.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.port2pullman.app.App
import com.port2pullman.app.debug.DebugConsoleScreen
import com.port2pullman.app.debug.DebugLog
import com.port2pullman.app.ui.ai.AIPromptDialog
import com.port2pullman.app.ui.ai.AIViewModel
import com.port2pullman.app.ui.home.AlarmListViewModel
import com.port2pullman.app.ui.home.HomeScreen
import com.port2pullman.app.ui.setup.AddConditionScreen
import com.port2pullman.app.ui.setup.SetupScreen
import com.port2pullman.app.ui.setup.SetupViewModel

object Routes {
    const val HOME = "home"
    const val SETUP_GRAPH = "setup_graph/{alarmId}"
    const val SETUP_FORM = "setup_form"
    const val ADD_CONDITION = "add_condition"
    const val DEBUG_CONSOLE = "debug_console"

    fun setupGraph(alarmId: Long = -1L) = "setup_graph/$alarmId"
}

@Composable
fun AppNavigation(app: App) {
    val navController = rememberNavController()

    // AI dialog state – shown as overlay from Home
    var showAiDialog by remember { mutableStateOf(false) }
    val aiViewModel: AIViewModel = viewModel()

    // Home ViewModel factory
    val homeVmFactory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AlarmListViewModel(app.alarmRepository) as T
        }
    }

    // Setup ViewModel factory (shared across setup graph)
    val setupVmFactory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SetupViewModel(app.alarmRepository, app.conditionRepository) as T
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {

        // ─── Home ────────────────────────────────────────────
        composable(Routes.HOME) {
            val homeViewModel: AlarmListViewModel = viewModel(factory = homeVmFactory)
            DebugLog.d("Nav", "Home composable entered")

            HomeScreen(
                viewModel = homeViewModel,
                onCreateAlarm = { navController.navigate(Routes.setupGraph(-1L)) },
                onEditAlarm = { id -> navController.navigate(Routes.setupGraph(id)) },
                onAiCreate = { showAiDialog = true },
                onOpenDebug = { navController.navigate(Routes.DEBUG_CONSOLE) },
            )
        }

        // ─── Debug Console ──────────────────────────────────
        composable(Routes.DEBUG_CONSOLE) {
            DebugConsoleScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ─── Setup Graph (shared ViewModel) ──────────────────
        navigation(
            startDestination = Routes.SETUP_FORM,
            route = Routes.SETUP_GRAPH,
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
        ) {
            composable(Routes.SETUP_FORM) { entry ->
                val parentEntry = remember(entry) {
                    navController.getBackStackEntry(Routes.SETUP_GRAPH)
                }
                val alarmId = parentEntry.arguments?.getLong("alarmId") ?: -1L

                val setupViewModel: SetupViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = setupVmFactory
                )

                DebugLog.d("Nav", "SETUP_FORM composed — VM #${System.identityHashCode(setupViewModel)}, alarmId=$alarmId")

                // Init on first composition
                LaunchedEffect(alarmId) {
                    DebugLog.d("Nav", "SETUP_FORM LaunchedEffect(alarmId=$alarmId) fired")
                    if (alarmId > 0) setupViewModel.initForEdit(alarmId)
                    else setupViewModel.initForCreate()
                }

                // Apply AI draft if one was generated
                val aiState by aiViewModel.uiState.collectAsState()
                LaunchedEffect(aiState.draft) {
                    aiState.draft?.let {
                        setupViewModel.applyDraft(it)
                        aiViewModel.clearDraft()
                    }
                }

                SetupScreen(
                    viewModel = setupViewModel,
                    onBack = { navController.popBackStack(Routes.HOME, false) },
                    onAddCondition = { navController.navigate(Routes.ADD_CONDITION) },
                )
            }

            composable(Routes.ADD_CONDITION) { entry ->
                val parentEntry = remember(entry) {
                    navController.getBackStackEntry(Routes.SETUP_GRAPH)
                }

                // Same ViewModel instance scoped to parent nav graph
                val setupViewModel: SetupViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    factory = setupVmFactory
                )

                DebugLog.d("Nav", "ADD_CONDITION composed — VM #${System.identityHashCode(setupViewModel)}")

                AddConditionScreen(
                    viewModel = setupViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }

    // ─── AI Dialog Overlay ───────────────────────────────────
    if (showAiDialog) {
        AIPromptDialog(
            viewModel = aiViewModel,
            onDraftReady = {
                showAiDialog = false
                // Navigate to setup (create mode) – the draft will be applied there
                navController.navigate(Routes.setupGraph(-1L))
            },
            onDismiss = { showAiDialog = false },
        )
    }
}
