/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.components.appstate

import org.junit.Assert.assertEquals
import org.junit.Test
import org.praha.praha.components.appstate.AppAction.ShortcutAction
import org.praha.praha.components.appstate.snackbar.SnackbarState
import org.praha.praha.home.topsites.AddShortcutEntryPoint
import org.praha.praha.home.topsites.AddShortcutSource

class ShortcutStateReducerTest {

    @Test
    fun `WHEN shortcut added action is dispatched THEN state is updated`() {
        val initialState = AppState()
        assertEquals(SnackbarState.None(), initialState.snackbarState)

        val finalState =
            AppStoreReducer.reduce(
                initialState,
                ShortcutAction.ShortcutAdded(
                    url = "https://www.mozilla.org",
                    source = AddShortcutSource.MANUAL,
                    entryPoint = AddShortcutEntryPoint.PAGE_MENU,
                ),
            )

        assertEquals(SnackbarState.ShortcutAdded, finalState.snackbarState)
    }
}
