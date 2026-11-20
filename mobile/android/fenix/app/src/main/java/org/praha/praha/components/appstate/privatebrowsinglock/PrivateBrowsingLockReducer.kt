/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.components.appstate.privatebrowsinglock

import org.praha.praha.components.AppStore
import org.praha.praha.components.appstate.AppAction.PrivateBrowsingLockAction
import org.praha.praha.components.appstate.AppState

/** [AppStore] reducer of [PrivateBrowsingLockAction]s. */
internal object PrivateBrowsingLockReducer {
    fun reduce(state: AppState, action: PrivateBrowsingLockAction): AppState =
        when (action) {
            is PrivateBrowsingLockAction.UpdatePrivateBrowsingLock ->
                state.copy(isPrivateScreenLocked = action.isLocked)
        }
}
