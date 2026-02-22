package com.example.eventtriggeralarm.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.eventtriggeralarm.ui.AppViewModel
import com.example.eventtriggeralarm.ui.screens.AddConditionScreen
import com.example.eventtriggeralarm.ui.screens.AiPromptDialog
import com.example.eventtriggeralarm.ui.screens.BoolOperatorDialog
import com.example.eventtriggeralarm.ui.screens.ConfirmDeleteDialog
import com.example.eventtriggeralarm.ui.screens.CustomConditionDialog
import com.example.eventtriggeralarm.ui.screens.ManageCustomDialog
import com.example.eventtriggeralarm.ui.screens.NumValDialog
import com.example.eventtriggeralarm.ui.screens.TriggeredDialog
import com.example.eventtriggeralarm.ui.screens.HomeScreen
import com.example.eventtriggeralarm.ui.screens.SetupScreen

object Routes {
    const val Home = "home"
    const val Setup = "setup"
    const val AddCondition = "add_condition"
}

@Composable
fun AppNavGraph(
    viewModel: AppViewModel,
    navController: NavHostController = rememberNavController()
) {
    val state by viewModel.state.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.Home
    ) {
        composable(Routes.Home) {
            HomeScreen(
                state = state,
                viewModel = viewModel,
                onNavigateToSetup = { navController.navigate(Routes.Setup) },
                onNavigateToAddCondition = { },
                onOpenAiDialog = {
                    viewModel.openCreateAlarm()
                    navController.navigate(Routes.Setup)
                    viewModel.openAiDialog()
                }
            )
        }
        composable(Routes.Setup) {
            SetupScreen(
                state = state,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToAddCondition = { navController.navigate(Routes.AddCondition) },
                onSave = { navController.popBackStack(Routes.Home, false) }
            )
        }
        composable(Routes.AddCondition) {
            AddConditionScreen(
                state = state,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onConditionSelected = { navController.popBackStack() },
                onCreateCustom = { viewModel.openCreateCustomCond() }
            )
        }
    }

    // Dialogs (overlay)
    BoolOperatorDialog(state, viewModel)
    AiPromptDialog(state, viewModel) { /* stays on setup */ }
    CustomConditionDialog(state, viewModel)
    NumValDialog(state, viewModel)
    ConfirmDeleteDialog(state, viewModel) {
        navController.popBackStack(Routes.Home, false)
    }
    ManageCustomDialog(state, viewModel)
    TriggeredDialog(state, viewModel)
}
