package com.thelastecho.reminder.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.navArgument
import com.thelastecho.reminder.ReminderApp
import com.thelastecho.reminder.core.alarm.AndroidAlarmScheduler
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.core.notification.ReminderNotificationManager
import com.thelastecho.reminder.data.local.ReminderDatabase
import com.thelastecho.reminder.data.repository.ReminderRepositoryImpl
import com.thelastecho.reminder.domain.usecase.DeleteReminderUseCase
import com.thelastecho.reminder.domain.usecase.GetRemindersUseCase
import com.thelastecho.reminder.domain.usecase.SaveReminderUseCase
import com.thelastecho.reminder.domain.usecase.RestoreReminderUseCase
import com.thelastecho.reminder.domain.usecase.ToggleReminderCompleteUseCase
import com.thelastecho.reminder.presentation.editor.EditorViewModel
import com.thelastecho.reminder.presentation.editor.ReminderEditorScreen
import com.thelastecho.reminder.presentation.home.HomeScreen
import com.thelastecho.reminder.presentation.home.HomeViewModel
import com.thelastecho.reminder.presentation.settings.AboutSettingsScreen
import com.thelastecho.reminder.presentation.settings.AppearanceSettingsScreen
import com.thelastecho.reminder.presentation.settings.AutomaticBackupScreen
import com.thelastecho.reminder.presentation.settings.ManualBackupScreen
import com.thelastecho.reminder.presentation.settings.CategoriesScreen
import com.thelastecho.reminder.presentation.settings.CategoriesViewModel
import com.thelastecho.reminder.presentation.settings.DataSettingsScreen
import com.thelastecho.reminder.presentation.settings.GeneralSettingsScreen
import com.thelastecho.reminder.presentation.settings.NotificationsAlarmsSettingsScreen
import com.thelastecho.reminder.presentation.settings.PrivacyScreen
import com.thelastecho.reminder.presentation.settings.RemindersSettingsScreen
import com.thelastecho.reminder.presentation.settings.SettingsGroup
import com.thelastecho.reminder.presentation.settings.SettingsHomeScreen
import com.thelastecho.reminder.presentation.settings.SettingsViewModel
import com.thelastecho.reminder.presentation.settings.TrashScreen
import com.thelastecho.reminder.presentation.settings.TrashViewModel
import com.thelastecho.reminder.presentation.settings.WidgetSettingsScreen

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val distributionFeatures = remember(context) {
        (context.applicationContext as ReminderApp).distributionFeatures
    }
    val database = remember { ReminderDatabase.getInstance(context) }
    val repository = remember { ReminderRepositoryImpl(database.reminderDao(), database.categoryDao()) }
    val alarmScheduler = remember { AndroidAlarmScheduler(context) }
    val preferencesRepository = remember { UserPreferencesRepository(context) }
    val notificationManager = remember { ReminderNotificationManager(context, preferencesRepository) }

    NavHost(
        navController = navController,
        startDestination = NavDestination.Home.route,
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        enterTransition = { fadeIn(tween(240, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 32 } },
        exitTransition = { fadeOut(tween(180, easing = FastOutSlowInEasing)) + slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { -it / 32 } },
        popEnterTransition = { fadeIn(tween(240, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { -it / 32 } },
        popExitTransition = { fadeOut(tween(180, easing = FastOutSlowInEasing)) + slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { it / 32 } }
    ) {
        composable(NavDestination.Home.route) {
            val homeViewModel = remember {
                HomeViewModel(
                    getRemindersUseCase = GetRemindersUseCase(repository),
                    toggleReminderCompleteUseCase = ToggleReminderCompleteUseCase(repository, alarmScheduler),
                    deleteReminderUseCase = DeleteReminderUseCase(repository, alarmScheduler),
                    repository = repository,
                    notificationManager = notificationManager,
                    preferencesRepository = preferencesRepository,
                    restoreReminderUseCase = RestoreReminderUseCase(repository, alarmScheduler)
                )
            }
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToEditor = { reminderId -> navController.navigate(NavDestination.Editor.createRoute(reminderId)) },
                onNavigateToSettings = { navController.navigate(NavDestination.SettingsGraph.route) }
            )
        }

        composable(
            route = "editor?reminderId={reminderId}",
            arguments = listOf(navArgument("reminderId") { type = NavType.LongType; defaultValue = -1L }),
            enterTransition = {
                fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.96f, animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow))
            },
            exitTransition = { fadeOut(tween(150, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 0.985f, animationSpec = tween(150)) }
        ) { backStackEntry ->
            val reminderIdArg = backStackEntry.arguments?.getLong("reminderId")
            val reminderId = if (reminderIdArg != null && reminderIdArg > 0) reminderIdArg else null
            val editorViewModel = remember(reminderId) {
                EditorViewModel(
                    reminderId = reminderId,
                    saveReminderUseCase = SaveReminderUseCase(repository, alarmScheduler),
                    deleteReminderUseCase = DeleteReminderUseCase(repository, alarmScheduler),
                    repository = repository,
                    notificationManager = notificationManager,
                    restoreReminderUseCase = RestoreReminderUseCase(repository, alarmScheduler)
                )
            }
            ReminderEditorScreen(viewModel = editorViewModel, onNavigateBack = { navController.popBackStack() })
        }

        navigation(startDestination = NavDestination.Settings.route, route = NavDestination.SettingsGraph.route) {
            composable(NavDestination.Settings.route) { backStackEntry ->
                val graphEntry = remember(backStackEntry) { navController.getBackStackEntry(NavDestination.SettingsGraph.route) }
                val factory = remember(graphEntry) { SettingsGraphViewModelFactory(preferencesRepository, repository) }
                val trashViewModel: TrashViewModel = viewModel(viewModelStoreOwner = graphEntry, key = "trash", factory = factory)
                SettingsHomeScreen(
                    trashViewModel = trashViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToGroup = { group ->
                        val route = when (group) {
                            SettingsGroup.GENERAL -> NavDestination.SettingsGeneral.route
                            SettingsGroup.APPEARANCE -> NavDestination.SettingsAppearance.route
                            SettingsGroup.REMINDERS -> NavDestination.SettingsReminders.route
                            SettingsGroup.NOTIFICATIONS -> NavDestination.SettingsNotifications.route
                            SettingsGroup.WIDGETS -> NavDestination.SettingsWidgets.route
                            SettingsGroup.DATA -> NavDestination.SettingsData.route
                            SettingsGroup.PRIVACY -> NavDestination.Privacy.route
                            SettingsGroup.ABOUT -> NavDestination.SettingsAbout.route
                        }
                        navController.navigate(route)
                    }
                )
            }

            composable(NavDestination.SettingsGeneral.route) { backStackEntry ->
                val graphEntry = remember(backStackEntry) { navController.getBackStackEntry(NavDestination.SettingsGraph.route) }
                val factory = remember(graphEntry) { SettingsGraphViewModelFactory(preferencesRepository, repository) }
                val viewModel: SettingsViewModel = viewModel(viewModelStoreOwner = graphEntry, key = "settings", factory = factory)
                GeneralSettingsScreen(viewModel, onNavigateBack = { navController.popBackStack() })
            }
            composable(NavDestination.SettingsAppearance.route) { backStackEntry ->
                val graphEntry = remember(backStackEntry) { navController.getBackStackEntry(NavDestination.SettingsGraph.route) }
                val factory = remember(graphEntry) { SettingsGraphViewModelFactory(preferencesRepository, repository) }
                val viewModel: SettingsViewModel = viewModel(viewModelStoreOwner = graphEntry, key = "settings", factory = factory)
                AppearanceSettingsScreen(viewModel, onNavigateBack = { navController.popBackStack() })
            }
            composable(NavDestination.SettingsReminders.route) { backStackEntry ->
                val graphEntry = remember(backStackEntry) { navController.getBackStackEntry(NavDestination.SettingsGraph.route) }
                val factory = remember(graphEntry) { SettingsGraphViewModelFactory(preferencesRepository, repository) }
                val viewModel: SettingsViewModel = viewModel(viewModelStoreOwner = graphEntry, key = "settings", factory = factory)
                RemindersSettingsScreen(viewModel, onNavigateBack = { navController.popBackStack() }, onNavigateToCategories = { navController.navigate(NavDestination.Categories.route) })
            }
            composable(NavDestination.SettingsNotifications.route) { backStackEntry ->
                val graphEntry = remember(backStackEntry) { navController.getBackStackEntry(NavDestination.SettingsGraph.route) }
                val factory = remember(graphEntry) { SettingsGraphViewModelFactory(preferencesRepository, repository) }
                val viewModel: SettingsViewModel = viewModel(viewModelStoreOwner = graphEntry, key = "settings", factory = factory)
                NotificationsAlarmsSettingsScreen(viewModel, onNavigateBack = { navController.popBackStack() })
            }
            composable(NavDestination.SettingsWidgets.route) { backStackEntry ->
                val graphEntry = remember(backStackEntry) { navController.getBackStackEntry(NavDestination.SettingsGraph.route) }
                val factory = remember(graphEntry) { SettingsGraphViewModelFactory(preferencesRepository, repository) }
                val viewModel: SettingsViewModel = viewModel(viewModelStoreOwner = graphEntry, key = "settings", factory = factory)
                WidgetSettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAppearance = { navController.navigate(NavDestination.SettingsAppearance.route) }
                )
            }
            composable(NavDestination.SettingsData.route) {
                DataSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBackup = { navController.navigate(NavDestination.BackupRestore.route) },
                    onNavigateToAutomaticBackup = { navController.navigate(NavDestination.AutomaticBackup.route) },
                    onNavigateToTrash = { navController.navigate(NavDestination.Trash.route) }
                )
            }
            composable(NavDestination.Privacy.route) {
                PrivacyScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(NavDestination.SettingsAbout.route) { backStackEntry ->
                val graphEntry = remember(backStackEntry) { navController.getBackStackEntry(NavDestination.SettingsGraph.route) }
                val factory = remember(graphEntry) { SettingsGraphViewModelFactory(preferencesRepository, repository) }
                val viewModel: SettingsViewModel = viewModel(viewModelStoreOwner = graphEntry, key = "settings", factory = factory)
                AboutSettingsScreen(viewModel, updateChecks = distributionFeatures.updateChecks, onNavigateBack = { navController.popBackStack() })
            }

            composable(NavDestination.Categories.route) { backStackEntry ->
                val categoriesViewModel = remember(backStackEntry) { CategoriesViewModel(repository = repository) }
                CategoriesScreen(viewModel = categoriesViewModel, onNavigateBack = { navController.popBackStack() })
            }
            composable(NavDestination.BackupRestore.route) {
                ManualBackupScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(NavDestination.AutomaticBackup.route) {
                AutomaticBackupScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(NavDestination.Trash.route) { backStackEntry ->
                val graphEntry = remember(backStackEntry) { navController.getBackStackEntry(NavDestination.SettingsGraph.route) }
                val factory = remember(graphEntry) { SettingsGraphViewModelFactory(preferencesRepository, repository) }
                val trashViewModel: TrashViewModel = viewModel(viewModelStoreOwner = graphEntry, key = "trash", factory = factory)
                TrashScreen(viewModel = trashViewModel, onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}

private class SettingsGraphViewModelFactory(
    private val preferencesRepository: UserPreferencesRepository,
    private val repository: com.thelastecho.reminder.domain.repository.ReminderRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(preferencesRepository) as T
        modelClass.isAssignableFrom(TrashViewModel::class.java) -> TrashViewModel(repository) as T
        else -> error("Unknown settings ViewModel: ${modelClass.name}")
    }
}
