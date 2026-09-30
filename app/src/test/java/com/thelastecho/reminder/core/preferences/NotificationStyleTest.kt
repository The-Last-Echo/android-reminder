package com.thelastecho.reminder.core.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationStyleTest {
    @Test
    fun legacyReminderStylesResolveToTheLeastIntrusiveEquivalent() {
        assertEquals(NotificationStyle.LIGHT, NotificationStyle.fromPersisted("SIMPLE"))
        assertEquals(NotificationStyle.LIGHT, NotificationStyle.fromPersisted("HEADS_UP"))
        assertEquals(NotificationStyle.STRONG, NotificationStyle.fromPersisted("FULL_SCREEN"))
    }

    @Test
    fun currentAndDisabledStylesResolve() {
        assertEquals(NotificationStyle.LIGHT, NotificationStyle.fromPersisted("LIGHT"))
        assertEquals(NotificationStyle.MEDIUM, NotificationStyle.fromPersisted("MEDIUM"))
        assertEquals(NotificationStyle.STRONG, NotificationStyle.fromPersisted("STRONG"))
        assertEquals(NotificationStyle.NONE, NotificationStyle.fromPersisted("NONE"))
    }

    @Test
    fun missingAndUnknownReminderStylesRemainInheritable() {
        assertNull(NotificationStyle.fromPersisted(null))
        assertNull(NotificationStyle.fromPersisted("UNKNOWN"))
    }

    @Test
    fun levelsDeclareDistinctSoundAndFullscreenPolicies() {
        assertEquals(false, NotificationStyle.LIGHT.requestsFullScreenIntent)
        assertEquals(true, NotificationStyle.LIGHT.usesNotificationSound)
        assertEquals(false, NotificationStyle.LIGHT.startsAlarmPlayback)

        assertEquals(true, NotificationStyle.MEDIUM.requestsFullScreenIntent)
        assertEquals(true, NotificationStyle.MEDIUM.usesNotificationSound)
        assertEquals(false, NotificationStyle.MEDIUM.startsAlarmPlayback)

        assertEquals(true, NotificationStyle.STRONG.requestsFullScreenIntent)
        assertEquals(false, NotificationStyle.STRONG.usesNotificationSound)
        assertEquals(true, NotificationStyle.STRONG.startsAlarmPlayback)
    }

    @Test
    fun onlyFullscreenLevelsDirectlyLaunchWhenTheAppIsForegrounded() {
        assertEquals(true, NotificationStyle.MEDIUM.shouldLaunchAlarmScreenDirectly(true, false, false))
        assertEquals(false, NotificationStyle.STRONG.shouldLaunchAlarmScreenDirectly(true, false, true))
        assertEquals(false, NotificationStyle.MEDIUM.shouldLaunchAlarmScreenDirectly(false, false, false))
        assertEquals(false, NotificationStyle.STRONG.shouldLaunchAlarmScreenDirectly(false, true, true))
        assertEquals(true, NotificationStyle.MEDIUM.shouldLaunchAlarmScreenDirectly(false, true, false))
        assertEquals(true, NotificationStyle.STRONG.shouldLaunchAlarmScreenDirectly(false, true, false))
        assertEquals(false, NotificationStyle.LIGHT.shouldLaunchAlarmScreenDirectly(true, true, false))
        assertEquals(false, NotificationStyle.NONE.shouldLaunchAlarmScreenDirectly(true, true, false))
    }

    @Test
    fun onlyMediumAndStrongRequireFullscreenAccess() {
        assertEquals(false, NotificationStyle.LIGHT.requiresFullScreenAccess)
        assertEquals(true, NotificationStyle.MEDIUM.requiresFullScreenAccess)
        assertEquals(true, NotificationStyle.STRONG.requiresFullScreenAccess)
        assertEquals(false, NotificationStyle.NONE.requiresFullScreenAccess)
    }

    @Test
    fun startupFullscreenPromptFollowsNotificationPermissionAndIsOneTime() {
        assertEquals(false, shouldOfferFullScreenAccessPrompt(false, false))
        assertEquals(true, shouldOfferFullScreenAccessPrompt(true, false))
        assertEquals(false, shouldOfferFullScreenAccessPrompt(true, true))
    }

    @Test
    fun deniedFullscreenAccessFallsBackToLightButPreservesAllowedLevels() {
        assertEquals(NotificationStyle.LIGHT, resolveRequestedNotificationStyle(NotificationStyle.MEDIUM, false))
        assertEquals(NotificationStyle.LIGHT, resolveRequestedNotificationStyle(NotificationStyle.STRONG, false))
        assertEquals(NotificationStyle.MEDIUM, resolveRequestedNotificationStyle(NotificationStyle.MEDIUM, true))
        assertEquals(NotificationStyle.STRONG, resolveRequestedNotificationStyle(NotificationStyle.STRONG, true))
        assertEquals(NotificationStyle.LIGHT, resolveRequestedNotificationStyle(NotificationStyle.LIGHT, false))
        assertEquals(NotificationStyle.NONE, resolveRequestedNotificationStyle(NotificationStyle.NONE, false))
    }
}