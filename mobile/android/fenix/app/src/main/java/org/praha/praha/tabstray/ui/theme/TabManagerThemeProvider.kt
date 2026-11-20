/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.tabstray.ui.theme

import androidx.compose.runtime.Composable
import mozilla.components.compose.base.theme.Theme
import org.praha.praha.tabstray.redux.state.Page
import org.praha.praha.theme.ThemeProvider
import org.praha.praha.theme.getThemeProvider

/**
 * [ThemeProvider] for the Tab Manager.
 *
 * When on [Page.PrivateTabs], [Theme.Private] is used. Otherwise, we fallback to
 * [ThemeProvider.provideNonPrivateTheme].
 */
class TabManagerThemeProvider(val selectedPage: Page) : ThemeProvider {
    @Composable
    override fun provideTheme(): Theme =
        if (selectedPage == Page.PrivateTabs) {
            Theme.Private
        } else {
            provideNonPrivateTheme()
        }

    @Composable override fun provideNonPrivateTheme() = getThemeProvider().provideNonPrivateTheme()
}
