/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.browser.applinks

import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.compose.menu.ui.MenuItemState
import mozilla.components.ui.icons.R as iconsR
import org.junit.Test
import org.praha.praha.R
import org.praha.praha.components.AppStore
import org.praha.praha.components.appstate.AppAction
import org.praha.praha.components.menu.MenuTarget
import org.praha.praha.components.menu.fake.FakeMenuHost
import org.praha.praha.components.menu.store.MenuAction

class OpenInPrahaMenuItemProviderTest {
    private val appStore: AppStore = mockk(relaxed = true)
    private val menu = FakeMenuHost()

    @Test
    fun `WHEN building the item THEN offer opening in the application`() = runTest {
        val provider =
            OpenInPrahaMenuItemProvider(
                appName = APP_NAME,
                appStore = appStore,
                target = MenuTarget.BrowserTab,
            )

        assertEquals(
            StandardMenuItem(
                title = MenuItemTitle(Text.Resource(R.string.browser_menu_open_in_fenix, listOf(APP_NAME))),
                icon = MenuItemIconRes(iconsR.drawable.mozac_ic_external_link_24),
                onClickEvent = MenuAction.OpenInFirefox,
            ),
            provider.itemsFlow.value.single(),
        )
    }

    @Test
    fun `WHEN clicking the item THEN close the menu and request opening in Firefox`() = runTest {
        val provider =
            OpenInPrahaMenuItemProvider(
                appName = APP_NAME,
                appStore = appStore,
                target = MenuTarget.BrowserTab,
            )

        provider.onEvent(MenuAction.OpenInFirefox, menu)

        assertTrue(menu.isDismissed)
        verify { appStore.dispatch(AppAction.OpenInFirefoxStarted) }
    }

    @Test
    fun `GIVEN isSandboxCustomTab is true WHEN building the item THEN show it as disabled`() = runTest {
        val provider =
            OpenInPrahaMenuItemProvider(
                appName = APP_NAME,
                appStore = appStore,
                target = MenuTarget.CustomTab("tab-id", isSandboxed = true),
            )

        assertEquals(
            StandardMenuItem(
                title = MenuItemTitle(Text.Resource(R.string.browser_menu_open_in_fenix, listOf(APP_NAME))),
                icon = MenuItemIconRes(iconsR.drawable.mozac_ic_external_link_24),
                onClickEvent = MenuAction.OpenInFirefox,
                state = MenuItemState.DISABLED,
            ),
            provider.itemsFlow.value.single(),
        )
    }

    @Test
    fun `GIVEN isSandboxCustomTab is true WHEN clicking the item THEN keep the menu open`() = runTest {
        val provider =
            OpenInPrahaMenuItemProvider(
                appName = APP_NAME,
                appStore = appStore,
                target = MenuTarget.CustomTab("tab-id", isSandboxed = true),
            )

        provider.onEvent(MenuAction.OpenInFirefox, menu)

        assertFalse(menu.isDismissed)
        verify(exactly = 0) { appStore.dispatch(AppAction.OpenInFirefoxStarted) }
    }

    @Test
    fun `WHEN asked about the events of other items THEN don't handle them`() = runTest {
        val provider =
            OpenInPrahaMenuItemProvider(
                appName = APP_NAME,
                appStore = appStore,
                target = MenuTarget.BrowserTab,
            )

        assertFalse(provider.handles(MenuAction.OpenInApp))
        assertFalse(provider.handles(MenuAction.Navigate.Settings))
    }

    private companion object {
        const val APP_NAME = "Firefox"
    }
}
