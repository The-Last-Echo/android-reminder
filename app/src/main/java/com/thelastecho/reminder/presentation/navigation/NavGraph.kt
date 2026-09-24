package com.thelastecho.reminder.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
import com.thelastecho.reminder.presentation.settings.BackupRestoreScreen
import com.thelastecho.reminder.presentation.settings.CategoriesScreen
import com.thelastecho.reminder.presentation.settings.CategoriesViewModel
import com.thelastecho.reminder.presentation.settings.SettingsScreen
import com.thelastecho.reminder.presentation.settings.PrivacyScreen
import com.thelastecho.reminder.presentation.settings.SettingsViewModel
import com.thelastecho.reminder.presentation.settings.TrashScreen
import com.thelastecho.reminder.presentation.settings.TrashViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { ReminderDatabase.getInstance(context) }
    val repository = remember { ReminderRepositoryImpl(database.reminderDao(), database.categoryDao()) }
    val alarmScheduler = remember { AndroidAlarmScheduler(context) }
    val preferencesRepository = remember { UserPreferencesRepository(context) }
    val notificationManager = remember { ReminderNotificationManager(context, preferencesRepository) }

    NavHost(
        navController = navController,
        startDestination = NavDestination.Home.route,
        modifier = modifier,
        enterTransition = { fadeIn(tween(180)) + slideInHorizontally(tween(180)) { it / 24 } },
        exitTransition = { fadeOut(tween(120)) + slideOutHorizontally(tween(120)) { -it / 24 } },
        popEnterTransition = { fadeIn(tween(180)) + slideInHorizontally(tween(180)) { -it / 24 } },
        popExitTransition = { fadeOut(tween(120)) + slideOutHorizontally(tween(120)) { it / 24 } }
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
                onNavigateToEditor = { reminderId ->
                    navController.navigate(NavDestination.Editor.createRoute(reminderId))
                },
                onNavigateToSettings = {
                    navController.navigate(NavDestination.Settings.route)
                }
            )
        }

        composable(
            route = "editor?reminderId={reminderId}",
            arguments = listOf(
                navArgument("reminderId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
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

            ReminderEditorScreen(
                viewModel = editorViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavDestination.Settings.route) {
            val settingsViewModel = remember {
                SettingsViewModel(preferencesRepository = preferencesRepository)
            }
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCategories = {
                    navController.navigate(NavDestination.Categories.route)
                },
                onNavigateToTrash = { navController.navigate(NavDestination.Trash.route) },
                onNavigateToBackup = { navController.navigate(NavDestination.BackupRestore.route) },
                onNavigateToPrivacy = { navController.navigate(NavDestination.Privacy.route) }
            )
        }

        composable(NavDestination.Categories.route) {
            val categoriesViewModel = remember {
                CategoriesViewModel(repository = repository)
            }
            CategoriesScreen(
                viewModel = categoriesViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavDestination.Privacy.route) {
            PrivacyScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavDestination.BackupRestore.route) {
            BackupRestoreScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavDestination.Trash.route) {
            val trashViewModel = remember {
                TrashViewModel(repository = repository)
            }
            TrashScreen(
                viewModel = trashViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
