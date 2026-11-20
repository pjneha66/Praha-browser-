/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.home.bookmarks.controller

import androidx.navigation.NavController
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSession.LoadUrlFlags.Companion.ALLOW_JAVASCRIPT_URL
import mozilla.components.feature.tabs.TabsUseCases
import org.praha.praha.GleanMetrics.HomeBookmarks
import org.praha.praha.R
import org.praha.praha.components.AppStore
import org.praha.praha.components.appstate.AppAction
import org.praha.praha.components.usecases.FenixBrowserUseCases
import org.praha.praha.home.HomeFragmentDirections
import org.praha.praha.home.bookmarks.Bookmark
import org.praha.praha.home.bookmarks.interactor.BookmarksInteractor
import org.praha.praha.utils.Settings

/** An interface that handles the view manipulation of the bookmarks on the Home screen. */
interface BookmarksController {

    /** @see [BookmarksInteractor.onBookmarkClicked] */
    fun handleBookmarkClicked(bookmark: Bookmark)

    /** @see [BookmarksInteractor.onShowAllBookmarksClicked] */
    fun handleShowAllBookmarksClicked()

    /** @see [BookmarksInteractor.onBookmarkRemoved] */
    fun handleBookmarkRemoved(bookmark: Bookmark)
}

/** The default implementation of [BookmarksController]. */
class DefaultBookmarksController(
    private val navController: NavController,
    private val appStore: AppStore,
    private val browserStore: BrowserStore,
    private val settings: Settings,
    private val fenixBrowserUseCases: FenixBrowserUseCases,
    private val selectTabUseCase: TabsUseCases.SelectTabUseCase,
) : BookmarksController {

    override fun handleBookmarkClicked(bookmark: Bookmark) {
        if (settings.enableHomepageAsNewTab) {
            navController.navigate(R.id.browserFragment)
            fenixBrowserUseCases.loadUrlOrSearch(
                searchTermOrURL = bookmark.url!!,
                newTab = false,
                private = false,
                flags = EngineSession.LoadUrlFlags.select(ALLOW_JAVASCRIPT_URL),
            )
        } else {
            val existingTabForBookmark =
                browserStore.state.tabs.firstOrNull {
                    it.content.url == bookmark.url
                }

            if (existingTabForBookmark == null) {
                navController.navigate(R.id.browserFragment)
                fenixBrowserUseCases.loadUrlOrSearch(
                    searchTermOrURL = bookmark.url!!,
                    newTab = true,
                    private = false,
                    flags = EngineSession.LoadUrlFlags.select(ALLOW_JAVASCRIPT_URL),
                )
            } else {
                selectTabUseCase.invoke(existingTabForBookmark.id)
                navController.navigate(R.id.browserFragment)
            }
        }

        HomeBookmarks.bookmarkClicked.add()
    }

    override fun handleShowAllBookmarksClicked() {
        HomeBookmarks.showAllBookmarks.add()
        navController.navigate(HomeFragmentDirections.actionGlobalBookmarkFragment(BookmarkRoot.Mobile.id))
    }

    override fun handleBookmarkRemoved(bookmark: Bookmark) {
        appStore.dispatch(AppAction.RemoveBookmark(bookmark))
    }
}
