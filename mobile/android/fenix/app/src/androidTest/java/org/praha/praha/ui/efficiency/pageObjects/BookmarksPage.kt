/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.ui.efficiency.helpers.BasePage
import org.praha.praha.ui.efficiency.navigation.NavigationEffect
import org.praha.praha.ui.efficiency.navigation.NavigationFacts
import org.praha.praha.ui.efficiency.navigation.NavigationGraph
import org.praha.praha.ui.efficiency.navigation.NavigationOptions
import org.praha.praha.ui.efficiency.navigation.NavigationStep
import org.praha.praha.ui.efficiency.selectors.BookmarksSelectors
import org.praha.praha.ui.efficiency.selectors.HomeSelectors
import org.praha.praha.ui.efficiency.selectors.MainMenuSelectors

class BookmarksPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "BookmarksPage"

    internal override fun registerNavigation(builder: NavigationGraph.Builder) {
        builder.register(
            from = "HomePage",
            to = pageName,
            variant = "default",
            steps =
                listOf(
                    NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
                    NavigationStep.Click(MainMenuSelectors.BOOKMARKS_BUTTON),
                ),
        )

        builder.register(
            from = "HomePage",
            to = pageName,
            variant = "with-searchable-bookmark",
            effects = listOf(NavigationEffect.CreateBookmark("https://www.mozilla.org", "Mozilla")),
            steps =
                listOf(
                    NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
                    NavigationStep.Click(MainMenuSelectors.BOOKMARKS_BUTTON),
                ),
            provides = setOf(NavigationFacts.BOOKMARKS_HAVE_ITEMS),
        )

        builder.register(
            from = "MainMenuPage",
            to = pageName,
            steps = listOf(NavigationStep.Click(MainMenuSelectors.BOOKMARKS_BUTTON)),
        )

        builder.register(
            from = pageName,
            to = "BookmarkSearchPage",
            steps = listOf(NavigationStep.Click(BookmarksSelectors.SEARCH_BUTTON)),
            requires = setOf(NavigationFacts.BOOKMARKS_HAVE_ITEMS),
        )

        builder.register(
            from = pageName,
            to = "HomePage",
            steps = listOf(NavigationStep.PressBack),
        )
    }

    override fun navigateToPage(
        url: String,
        forceNavigation: Boolean,
        navigationOptions: NavigationOptions,
    ): BookmarksPage {
        super.navigateToPage(url, forceNavigation, navigationOptions)
        return this
    }

    override val selectorCatalog = BookmarksSelectors

    fun createFolder(name: String): BookmarksPage {
        mozClick(BookmarksSelectors.ADD_FOLDER_BUTTON)
        mozClearAndEnterText(name, BookmarksSelectors.ADD_FOLDER_NAME_TEXT_FIELD)
        mozClick(BookmarksSelectors.NAVIGATE_UP_BUTTON)
        return this
    }

    fun openItemMenu(title: String): BookmarksPage {
        mozClick(BookmarksSelectors.ITEM_MENU(title))
        return this
    }

    fun importBookmarksFromFile(): BookmarksPage {
        mozClick(BookmarksSelectors.IMPORT_BOOKMARKS_BUTTON)
        mozClick(BookmarksSelectors.IMPORT_MENU_BUTTON)
        return this
    }

    fun setParentFolder(folderName: String): BookmarksPage {
        mozClick(BookmarksSelectors.DEFAULT_BOOKMARKS_FOLDER_TITLE)
        mozClick(BookmarksSelectors.EXPAND_FOLDER_BUTTON("Bookmarks"))
        mozClick(BookmarksSelectors.BOOKMARK_ITEM_TITLE(folderName))
        mozClick(BookmarksSelectors.NAVIGATE_UP_BUTTON)
        return this
    }

    fun saveEditBookmark(): BookmarksPage {
        mozClick(BookmarksSelectors.NAVIGATE_UP_BUTTON)
        return this
    }

    fun longClickBookmarkedItem(title: String): BookmarksPage {
        mozLongClick(BookmarksSelectors.BOOKMARK_ITEM_TITLE(title))
        return this
    }

    fun selectBookmarkedItem(title: String): BookmarksPage {
        mozClick(BookmarksSelectors.BOOKMARK_ITEM_TITLE(title))
        return this
    }

    fun verifyMultiSelectionCounter(count: Int): BookmarksPage {
        mozVerify(BookmarksSelectors.MULTI_SELECTION_COUNTER(count))
        return this
    }

    fun clickMultiSelectThreeDotButton(): BookmarksPage {
        mozClick(BookmarksSelectors.MULTI_SELECTION_THREE_DOT_BUTTON)
        return this
    }

    fun verifyBookmarkTitle(title: String): BookmarksPage {
        mozVerify(BookmarksSelectors.BOOKMARK_ITEM_TITLE(title))
        return this
    }
}
