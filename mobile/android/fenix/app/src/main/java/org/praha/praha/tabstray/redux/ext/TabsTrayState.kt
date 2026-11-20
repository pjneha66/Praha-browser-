/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.tabstray.redux.ext

import android.util.Log
import org.praha.praha.tabstray.data.TabStorageUpdate
import org.praha.praha.tabstray.data.TabsTrayItem
import org.praha.praha.tabstray.navigation.TabManagerNavDestination
import org.praha.praha.tabstray.redux.state.Page
import org.praha.praha.tabstray.redux.state.TabsTrayState

internal fun TabsTrayState.withStorageUpdate(storageUpdate: TabStorageUpdate) =
    copy(
        selectedTabId = storageUpdate.selectedTabId,
        normalTabsState =
            normalTabsState.copy(
                items = storageUpdate.normalItems,
                selectedItemIndex = storageUpdate.selectedNormalItemIndex,
                tabCount = storageUpdate.normalTabCount,
            ),
        inactiveTabs = inactiveTabs.copy(tabs = storageUpdate.inactiveTabs),
        privateBrowsing =
            privateBrowsing.copy(
                tabs = storageUpdate.privateTabs,
                selectedItemIndex = storageUpdate.selectedPrivateItemIndex,
            ),
        tabGroupState = tabGroupState.copy(groups = storageUpdate.tabGroups),
        hasTabDataLoaded = true,
    )

/**
 * Returns a copy of this state with the [TabsTrayItem.TabGroup] matching [groupId] expanded, or this state unchanged if
 * [groupId] is null or no such group exists.
 */
internal fun TabsTrayState.withExpandedTabGroup(groupId: String?): TabsTrayState {
    val group =
        groupId?.let { id -> tabGroupState.groups.find { it.id == id } }
            ?: run {
                Log.w("TabsTrayState", "Failed to find and expand group matching id $groupId.")
                return this
            }
    return copy(
        selectedPage = Page.NormalTabs,
        backStack = backStack + TabManagerNavDestination.ExpandedTabGroup(group = group),
    )
}
