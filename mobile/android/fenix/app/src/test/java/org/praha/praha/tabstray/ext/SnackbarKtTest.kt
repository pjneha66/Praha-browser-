/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.tabstray.ext

import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertIs
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.praha.praha.R
import org.praha.praha.components.SnackbarBehavior
import org.praha.praha.components.toolbar.ToolbarPosition
import org.praha.praha.compose.snackbar.Snackbar
import org.praha.praha.compose.snackbar.SnackbarState
import org.praha.praha.ext.components
import org.praha.praha.helpers.MockkRetryTestRule
import org.praha.praha.utils.Settings
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SnackbarKtTest {

    @get:Rule val mockkRule = MockkRetryTestRule()

    @Test
    fun `GIVEN the snackbar is a child of dynamic container WHEN it is shown THEN enable the dynamic behavior`() {
        val container =
            FrameLayout(testContext).apply {
                id = R.id.dynamicSnackbarContainer
                layoutParams = CoordinatorLayout.LayoutParams(0, 0)
            }
        val settings: Settings =
            mockk(relaxed = true) {
                every { toolbarPosition } returns ToolbarPosition.BOTTOM
            }

        every { testContext.components.settings } returns settings

        Snackbar.make(
            snackBarParentView = container,
            snackbarState = SnackbarState(message = "test"),
        )

        val behavior = (container.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior
        assertIs<SnackbarBehavior<*>>(behavior)
        assertEquals(ToolbarPosition.BOTTOM, behavior.toolbarPosition)
    }
}
