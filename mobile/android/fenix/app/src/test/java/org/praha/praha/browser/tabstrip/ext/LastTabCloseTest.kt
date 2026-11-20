/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.browser.tabstrip.ext

import androidx.navigation.NavController
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.praha.praha.NavGraphDirections
import org.praha.praha.R
import org.praha.praha.browser.browsingmode.BrowsingMode
import org.praha.praha.components.AppStore
import org.praha.praha.components.appstate.AppState
import org.praha.praha.ext.nav
import org.praha.praha.tabstray.redux.state.Page

class LastTabCloseTest {

    @Test
    fun `GIVEN homepage as new tab is disabled WHEN handleTabStripLastTabClosed is called THEN call the provided lambda`() {
        val isHomepageAsNewTabEnabled = false
        val appStore = AppStore(AppState(isTabsTrayVisible = false, wasLastTabClosedPrivate = false))
        val navController = mockk<NavController>(relaxed = true)
        var homepageAsNewTabDisabledLambdaCalled = false
        handleTabStripLastTabClosed(
            isHomepageAsNewTabEnabled = isHomepageAsNewTabEnabled,
            isLastTabClosedPrivate = false,
            navigateFromId = R.id.homeFragment,
            appStore = appStore,
            browsingMode = BrowsingMode.Normal,
            navController = navController,
            onHomepageAsNewTabDisabled = { homepageAsNewTabDisabledLambdaCalled = true },
        )

        assertTrue(homepageAsNewTabDisabledLambdaCalled)
        assertFalse(appStore.state.isTabsTrayVisible)
        assertFalse(appStore.state.wasLastTabClosedPrivate!!)

        verify(exactly = 0) {
            navController.nav(
                R.id.homeFragment,
                NavGraphDirections.actionGlobalTabManagementFragment(page = Page.NormalTabs),
            )
        }
    }

    @Test
    fun `GIVEN homepage as new tab is enabled and normal browsing mode WHEN handleTabStripLastTabClosed is called THEN dispatch to the app store and navigate`() {
        val isHomepageAsNewTabEnabled = true
        val appStore = AppStore(AppState(isTabsTrayVisible = false, wasLastTabClosedPrivate = false))
        val navController = mockk<NavController>(relaxed = true)
        var homepageAsNewTabDisabledLambdaCalled = false
        handleTabStripLastTabClosed(
            isHomepageAsNewTabEnabled = isHomepageAsNewTabEnabled,
            isLastTabClosedPrivate = false,
            navigateFromId = R.id.homeFragment,
            appStore = appStore,
            browsingMode = BrowsingMode.Normal,
            navController = navController,
            onHomepageAsNewTabDisabled = { homepageAsNewTabDisabledLambdaCalled = true },
        )

        assertFalse(homepageAsNewTabDisabledLambdaCalled)
        assertTrue(appStore.state.isTabsTrayVisible)
        assertFalse(appStore.state.wasLastTabClosedPrivate!!)

        verify {
            navController.nav(
                R.id.homeFragment,
                NavGraphDirections.actionGlobalTabManagementFragment(page = Page.NormalTabs),
            )
        }
    }

    @Test
    fun `GIVEN homepage as new tab is enabled and private browsing mode WHEN handleTabStripLastTabClosed is called THEN dispatch to the app store and navigate`() {
        val isHomepageAsNewTabEnabled = true
        val appStore = AppStore(AppState(isTabsTrayVisible = false, wasLastTabClosedPrivate = true))
        val navController = mockk<NavController>(relaxed = true)
        var homepageAsNewTabDisabledLambdaCalled = false
        handleTabStripLastTabClosed(
            isHomepageAsNewTabEnabled = isHomepageAsNewTabEnabled,
            isLastTabClosedPrivate = true,
            navigateFromId = R.id.homeFragment,
            appStore = appStore,
            browsingMode = BrowsingMode.Private,
            navController = navController,
            onHomepageAsNewTabDisabled = { homepageAsNewTabDisabledLambdaCalled = true },
        )

        assertFalse(homepageAsNewTabDisabledLambdaCalled)
        assertTrue(appStore.state.isTabsTrayVisible)
        assertTrue(appStore.state.wasLastTabClosedPrivate!!)

        verify {
            navController.nav(
                R.id.homeFragment,
                NavGraphDirections.actionGlobalTabManagementFragment(page = Page.PrivateTabs),
            )
        }
    }
}
