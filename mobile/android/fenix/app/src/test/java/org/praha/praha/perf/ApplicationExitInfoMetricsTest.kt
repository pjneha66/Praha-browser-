/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.perf

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.test.filters.SdkSuppress
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApplicationExitInfoMetricsTest {

    @Test
    fun `TRACKED_REASONS includes system-driven reasons the user cannot control`() {
        val expected =
            listOf(
                ApplicationExitInfo.REASON_ANR,
                ApplicationExitInfo.REASON_CRASH,
                ApplicationExitInfo.REASON_CRASH_NATIVE,
                ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
                ApplicationExitInfo.REASON_LOW_MEMORY,
                ApplicationExitInfo.REASON_SIGNALED,
                ApplicationExitInfo.REASON_OTHER,
                ApplicationExitInfo.REASON_DEPENDENCY_DIED,
                ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
            )
        expected.forEach {
            assertTrue(
                "TRACKED_REASONS is missing reason code $it",
                ApplicationExitInfoMetrics.TRACKED_REASONS.contains(it),
            )
        }
    }

    @Test
    fun `TRACKED_REASONS includes REASON_FREEZER when running on A13+`() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            assertTrue(ApplicationExitInfoMetrics.TRACKED_REASONS.contains(ApplicationExitInfo.REASON_FREEZER))
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun `GIVEN AMS getHistoricalProcessExitReasons throws WHEN getProcessExitsForDisplay is called THEN we do not crash and return an empty list`() {
        val throwingContext = mockk<Context>(relaxed = true)
        val throwingActivityManager = mockk<ActivityManager>()
        every { throwingContext.getSystemService(Context.ACTIVITY_SERVICE) } returns throwingActivityManager
        every {
            throwingActivityManager.getHistoricalProcessExitReasons(any(), any(), any())
        } throws IllegalArgumentException("simulated OEM AMS bug 2072086")

        // Must not throw — this is the crash under bug 2072086.
        val result = ApplicationExitInfoMetrics.getProcessExitsForDisplay(throwingContext)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `WHEN toProcessType is called THEN process names are mapped to the expected process types`() {
        val expected =
            mapOf(
                "org.praha.praha" to "parent",
                "org.praha.praha:sandboxed_tab_disable_art_image_3" to "content",
                "org.praha.praha:isolatedTab_disable_art_image_7" to "content",
                "org.praha.praha:zygoteTab_disable_art_image_" to "content",
                "org.praha.praha:gpu_disable_art_image_" to "gpu",
                "org.praha.praha:media" to "media",
                "org.praha.praha:utility_disable_art_image_" to "utility",
                "org.praha.praha:isolatedGmplugin_disable_art_image_" to "other",
                "org.praha.praha:rdd_disable_art_image_" to "other",
                "org.example.stable:gpu_disable_art_image_" to "gpu",
            )
        expected.forEach { (name, type) ->
            assertEquals(name, type, with(ApplicationExitInfoMetrics) { name.toProcessType() })
        }
    }

    @Test
    fun `TRACKED_REASONS excludes user-triggered reasons`() {
        val userTriggered =
            listOf(
                ApplicationExitInfo.REASON_USER_REQUESTED,
                ApplicationExitInfo.REASON_USER_STOPPED,
                ApplicationExitInfo.REASON_PERMISSION_CHANGE,
                ApplicationExitInfo.REASON_PACKAGE_UPDATED,
                ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE,
            )
        userTriggered.forEach {
            assertFalse(
                "TRACKED_REASONS should not include user-triggered reason $it",
                ApplicationExitInfoMetrics.TRACKED_REASONS.contains(it),
            )
        }
    }
}
