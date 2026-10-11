package com.zhousl.aether.data

import org.junit.Assert.*
import org.junit.Test

class PhoneUsePolicyTest {
    @Test fun returnToAetherRequiresAnExplicitForegroundStop() {
        val settings = AppSettings()
        assertFalse(phoneUseReturnToAetherRequested(settings, "stop", org.json.JSONObject()))
        assertFalse(phoneUseReturnToAetherRequested(settings, "stop", org.json.JSONObject().put("return_to_aether", false)))
        val request = org.json.JSONObject().put("return_to_aether", true)
        assertTrue(phoneUseReturnToAetherRequested(settings, "stop", request))
        assertThrows(IllegalArgumentException::class.java) { phoneUseReturnToAetherRequested(settings, "tap", request) }
        assertThrows(IllegalArgumentException::class.java) {
            phoneUseReturnToAetherRequested(settings.copy(phoneUseVirtualDisplayEnabled = true), "stop", request)
        }
        assertThrows(IllegalArgumentException::class.java) {
            phoneUseReturnToAetherRequested(settings, "stop", org.json.JSONObject().put("return_to_aether", "true"))
        }
    }

    @Test fun permissionPromptRespectsDisabledChoiceAndExplicitReenable() {
        val policy = PhoneUsePermissionRequestPolicy()
        assertFalse(policy.shouldRequest(enabled = false, granted = false))
        assertTrue(policy.shouldRequest(enabled = true, granted = false))
        assertFalse(policy.shouldRequest(enabled = true, granted = false))
        assertFalse(policy.shouldRequest(enabled = false, granted = false))
        assertTrue(policy.shouldRequest(enabled = true, granted = false, manuallyEnabled = true))
        assertFalse(policy.shouldRequest(enabled = true, granted = true, manuallyEnabled = true))
    }

    @Test fun missingPermissionCanBeRequestedAgainOnNextLaunch() {
        assertTrue(PhoneUsePermissionRequestPolicy().shouldRequest(true, false))
        assertTrue(PhoneUsePermissionRequestPolicy().shouldRequest(true, false))
    }

    @Test fun observationsFollowUserPreferenceAndGateExplicitAlternatives() {
        val settings = AppSettings(phoneUseInputStrategy = PhoneUseInputStrategy.ScreenshotFirst,
            phoneUseAllowStrategySwitch = false)
        assertEquals(PhoneUseInputStrategy.ScreenshotFirst, resolvePhoneUseInputStrategy(settings, "observe", ""))
        assertEquals(PhoneUseInputStrategy.ScreenshotFirst, resolvePhoneUseInputStrategy(settings, "click", ""))
        assertThrows(IllegalArgumentException::class.java) { resolvePhoneUseInputStrategy(settings, "tree", "") }
        assertThrows(IllegalArgumentException::class.java) { resolvePhoneUseInputStrategy(settings, "observe", "element_tree_first") }
        assertEquals(PhoneUseInputStrategy.ElementTreeFirst,
            resolvePhoneUseInputStrategy(settings.copy(phoneUseAllowStrategySwitch = true), "tree", ""))
    }

    @Test fun outputPreferenceChoosesWhenBothTargetsExistAndRejectsDisabledFallback() {
        val elements = AppSettings(phoneUseAllowStrategySwitch = false)
        assertTrue(phoneUseUsesElementAction(elements, "click", true, true, ""))
        assertThrows(IllegalArgumentException::class.java) { phoneUseUsesElementAction(elements, "click", false, true, "") }
        // A stray node_id cannot disguise a swipe as an element operation.
        assertThrows(IllegalArgumentException::class.java) { phoneUseUsesElementAction(elements, "swipe", true, true, "") }
        val gestures = elements.copy(phoneUseOutputStrategy = PhoneUseOutputStrategy.GestureFirst)
        assertFalse(phoneUseUsesElementAction(gestures, "click", true, true, ""))
        assertThrows(IllegalArgumentException::class.java) { phoneUseUsesElementAction(gestures, "click", true, false, "") }
        assertThrows(IllegalArgumentException::class.java) { phoneUseUsesElementAction(gestures, "click", true, true, "element_first") }
    }

    @Test fun allowedFallbackAndExplicitOverrideDoNotAlterPreferences() {
        val settings = AppSettings(phoneUseOutputStrategy = PhoneUseOutputStrategy.GestureFirst)
        assertTrue(phoneUseUsesElementAction(settings, "click", true, true, "element_first"))
        assertTrue(phoneUseUsesElementAction(settings, "click", true, false, ""))
        assertEquals(PhoneUseOutputStrategy.GestureFirst, settings.phoneUseOutputStrategy)
    }
}
