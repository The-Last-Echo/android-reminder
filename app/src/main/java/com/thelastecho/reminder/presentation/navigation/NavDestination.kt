package com.thelastecho.reminder.presentation.navigation

sealed class NavDestination(val route: String) {
    data object Home : NavDestination("home")
    data object Editor : NavDestination("editor?reminderId={reminderId}") {
        fun createRoute(reminderId: Long? = null): String =
            if (reminderId != null && reminderId > 0) "editor?reminderId=$reminderId" else "editor?reminderId=-1"
    }

    data object SettingsGraph : NavDestination("settings_graph")
    data object Settings : NavDestination("settings")
    data object SettingsGeneral : NavDestination("settings/general")
    data object SettingsAppearance : NavDestination("settings/appearance")
    data object SettingsReminders : NavDestination("settings/reminders")
    data object SettingsNotifications : NavDestination("settings/notifications")
    data object SettingsWidgets : NavDestination("settings/widgets")
    data object SettingsData : NavDestination("settings/data")
    data object SettingsAbout : NavDestination("settings/about")

    data object Categories : NavDestination("settings/reminders/categories")
    data object Trash : NavDestination("settings/data/trash")
    data object BackupRestore : NavDestination("settings/data/backup_restore")
    data object AutomaticBackup : NavDestination("settings/data/backup_automatic")
    data object Privacy : NavDestination("settings/privacy")
}
