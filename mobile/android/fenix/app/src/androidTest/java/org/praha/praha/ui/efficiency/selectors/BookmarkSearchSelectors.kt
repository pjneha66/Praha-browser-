/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.selectors

import mozilla.components.compose.browser.toolbar.concept.BrowserToolbarTestTags.ADDRESSBAR_SEARCH_BOX
import org.praha.praha.bookmarks.BookmarksTestTag.BOOKMARK_SEARCH_ITEM
import org.praha.praha.ui.efficiency.helpers.PageReadinessProfiles
import org.praha.praha.ui.efficiency.helpers.Selector
import org.praha.praha.ui.efficiency.helpers.SelectorContainer
import org.praha.praha.ui.efficiency.helpers.SelectorStrategy

object BookmarkSearchSelectors : SelectorContainer {

    val SEARCH_BOX =
        Selector(
            strategy = SelectorStrategy.COMPOSE_EDITABLE_BY_ANCESTOR_TAG,
            value = ADDRESSBAR_SEARCH_BOX,
            description = "Bookmark search box",
            readiness = PageReadinessProfiles.IDENTITY_ANCHOR,
        )

    val SEARCH_ITEM =
        Selector(
            strategy = SelectorStrategy.COMPOSE_BY_TAG,
            value = BOOKMARK_SEARCH_ITEM,
            description = "Bookmark search item",
        )
}
