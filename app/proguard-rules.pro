# Proguard / R8 configuration for 100% FOSS Reminder

# Room SQLite
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences {
    *;
}
