/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.tabstray

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import mozilla.components.lib.state.helpers.AbstractBinding
import org.praha.praha.components.AppStore
import org.praha.praha.components.appstate.AppState
import org.praha.praha.tabstray.redux.action.TabsTrayAction
import org.praha.praha.tabstray.redux.store.TabsTrayStore

/** Binding to update the [TabsTrayStore] when the Private Browsing Mode lock status changes in [AppStore]. */
class PbmLockStatusBinding(
    appStore: AppStore,
    private val tabsTrayStore: TabsTrayStore,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : AbstractBinding<AppState>(appStore, mainDispatcher) {

    override suspend fun onState(flow: Flow<AppState>) {
        flow
            .distinctUntilChangedBy { it.isPrivateScreenLocked }
            .collectLatest { state ->
                tabsTrayStore.dispatch(TabsTrayAction.UpdatePbmLockStatus(isLocked = state.isPrivateScreenLocked))
            }
    }
}
