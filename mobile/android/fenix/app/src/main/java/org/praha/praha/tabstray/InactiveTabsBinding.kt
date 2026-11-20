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

/**
 * Binding to update the [TabsTrayStore] by listening to changes to [AppState].
 *
 * @param appStore [AppStore] used to listen for changes to [AppState].
 * @param tabsTrayStore [TabsTrayStore] used to listen for changes to [TabsTrayState].
 * @param mainDispatcher The [CoroutineDispatcher] on which the state observation and updates will occur. Defaults to
 *   [Dispatchers.Main].
 */
class InactiveTabsBinding(
    appStore: AppStore,
    private val tabsTrayStore: TabsTrayStore,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : AbstractBinding<AppState>(appStore, mainDispatcher) {
    override suspend fun onState(flow: Flow<AppState>) {
        flow
            .distinctUntilChangedBy { it.inactiveTabsExpanded }
            .collectLatest {
                tabsTrayStore.dispatch(TabsTrayAction.UpdateInactiveExpanded(expanded = it.inactiveTabsExpanded))
            }
    }
}
