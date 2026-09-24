package com.thelastecho.reminder.presentation.navigation

sealed class NavDestination(val route: String) {
    data object Home : NavDestination("home")
    data object Editor : NavDestination("editor?reminderId={reminderId}") {
        fun createRoute(reminderId: Long? = null): String =
            if (reminderId != null && reminderId > 0) "editor?reminderId=$reminderId" else "editor?reminderId=-1"
    }
    data object Settings : NavDestination("settings")
    data object Categories : NavDestination("categories")
    data object Trash : NavDestination("trash")
    data object BackupRestore : NavDestination("backup_restore")
    data object Privacy : NavDestination("privacy")
}
