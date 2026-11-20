/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.ui.efficiency.helpers.BasePage
import org.praha.praha.ui.efficiency.navigation.PageObjectContract
import org.praha.praha.ui.efficiency.navigation.PageObjectKind
import org.praha.praha.ui.efficiency.selectors.CollectionsSelectors

@PageObjectContract(PageObjectKind.SELECTOR_ONLY)
class CollectionsPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "CollectionsPage"

    // No navigation edge is registered on purpose. This page previously registered one from HomePage
    // with an empty step list, which made navigateToPage() a silent no-op. Collections are a section of
    // the tabs tray rather than a screen of their own, and CollectionsTest only uses this page as a
    // selector namespace after arriving by other means — it never calls navigateToPage() on it. With no
    // edge, navigateToPage() fails loudly instead of pretending to have navigated.

    override val selectorCatalog = CollectionsSelectors
}
