/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.components.appstate.readerview

import org.praha.praha.components.appstate.AppAction.ReaderViewAction
import org.praha.praha.components.appstate.AppState

/** A [ReaderViewAction] reducer that updates [AppState.readerViewState]. */
internal object ReaderViewStateReducer {
    fun reduce(state: AppState, action: ReaderViewAction): AppState =
        when (action) {
            is ReaderViewAction.ReaderViewStarted -> state.copy(readerViewState = ReaderViewState.Active)

            is ReaderViewAction.ReaderViewDismissed -> state.copy(readerViewState = ReaderViewState.Dismiss)

            is ReaderViewAction.ReaderViewControlsShown -> state.copy(readerViewState = ReaderViewState.ShowControls)

            is ReaderViewAction.Reset -> state.copy(readerViewState = ReaderViewState.None)
        }
}
