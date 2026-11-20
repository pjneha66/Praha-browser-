/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.browser.tabstrip.ext

import androidx.annotation.IdRes
import androidx.navigation.NavController
import org.praha.praha.NavGraphDirections
import org.praha.praha.browser.browsingmode.BrowsingMode
import org.praha.praha.components.AppStore
import org.praha.praha.components.appstate.AppAction
import org.praha.praha.ext.nav
import org.praha.praha.tabstray.redux.state.Page

/**
 * Handles the last remaining tab being closed from the tab strip. When homepage as a new tab is enabled, the tabs tray
 * is opened, otherwise [onHomepageAsNewTabDisabled] is invoked.
 *
 * @param isHomepageAsNewTabEnabled Whether homepage as new tab is enabled.
 * @param isLastTabClosedPrivate Whether the closed tab was private.
 * @param navigateFromId The id of the current navigation destination.
 * @param appStore [AppStore] used to update tabs tray visibility and last closed tab information.
 * @param browsingMode The current [BrowsingMode], used to pick the tabs tray page to open.
 * @param navController [NavController] used to navigate to the tabs tray.
 * @param onHomepageAsNewTabDisabled Invoked instead of opening the tabs tray when homepage as a new tab is disabled.
 */
internal fun handleTabStripLastTabClosed(
    isHomepageAsNewTabEnabled: Boolean,
    isLastTabClosedPrivate: Boolean,
    @IdRes navigateFromId: Int,
    appStore: AppStore,
    browsingMode: BrowsingMode,
    navController: NavController,
    onHomepageAsNewTabDisabled: () -> Unit,
) {
    if (!isHomepageAsNewTabEnabled) {
        onHomepageAsNewTabDisabled()
        return
    }

    with(appStore) {
        // Prevents HomepageTabBinding from adding a new homepage tab before the tabs tray is shown.
        dispatch(AppAction.UpdateTabsTrayVisibility(true))
        dispatch(AppAction.TabStripAction.UpdateLastTabClosed(isLastTabClosedPrivate))
    }

    navController.nav(
        id = navigateFromId,
        directions =
            NavGraphDirections.actionGlobalTabManagementFragment(
                page = if (browsingMode.isPrivate) Page.PrivateTabs else Page.NormalTabs
            ),
    )
}
