/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.home.recentvisits.interactor

import io.mockk.mockk
import io.mockk.verify
import mozilla.components.concept.storage.DocumentType
import mozilla.components.concept.storage.HistoryMetadata
import mozilla.components.concept.storage.HistoryMetadataKey
import org.junit.Before
import org.junit.Test
import org.praha.praha.home.bookmarks.controller.BookmarksController
import org.praha.praha.home.logo.LogoController
import org.praha.praha.home.logo.TrackingProtectionController
import org.praha.praha.home.pocket.controller.PocketStoriesController
import org.praha.praha.home.privatebrowsing.controller.PrivateBrowsingController
import org.praha.praha.home.recentsyncedtabs.controller.RecentSyncedTabController
import org.praha.praha.home.recenttabs.controller.RecentTabController
import org.praha.praha.home.recentvisits.RecentlyVisitedItem.RecentHistoryGroup
import org.praha.praha.home.recentvisits.RecentlyVisitedItem.RecentHistoryHighlight
import org.praha.praha.home.recentvisits.controller.RecentVisitsController
import org.praha.praha.home.search.HomeSearchController
import org.praha.praha.home.sessioncontrol.DefaultSessionControlController
import org.praha.praha.home.sessioncontrol.SessionControlInteractor
import org.praha.praha.home.termsofuse.PrivacyNoticeBannerController
import org.praha.praha.home.toolbar.ToolbarController
import org.praha.praha.home.topsites.controller.TopSiteController

class RecentVisitsInteractorTest {
    private val defaultSessionControlController: DefaultSessionControlController = mockk(relaxed = true)
    private val recentTabController: RecentTabController = mockk(relaxed = true)
    private val recentSyncedTabController: RecentSyncedTabController = mockk(relaxed = true)
    private val bookmarksController: BookmarksController = mockk(relaxed = true)
    private val recentVisitsController: RecentVisitsController = mockk(relaxed = true)
    private val pocketStoriesController: PocketStoriesController = mockk(relaxed = true)
    private val privateBrowsingController: PrivateBrowsingController = mockk(relaxed = true)
    private val toolbarController: ToolbarController = mockk(relaxed = true)
    private val homeSearchController: HomeSearchController = mockk(relaxed = true)
    private val topSiteController: TopSiteController = mockk(relaxed = true)
    private val privacyNoticeBannerController: PrivacyNoticeBannerController = mockk(relaxed = true)
    private val trackingProtectionController: TrackingProtectionController = mockk(relaxed = true)
    private val logoController: LogoController = mockk(relaxed = true)

    private lateinit var interactor: SessionControlInteractor

    @Before
    fun setup() {
        interactor =
            SessionControlInteractor(
                defaultSessionControlController,
                recentTabController,
                recentSyncedTabController,
                bookmarksController,
                recentVisitsController,
                pocketStoriesController,
                privateBrowsingController,
                toolbarController,
                homeSearchController,
                topSiteController,
                privacyNoticeBannerController,
                trackingProtectionController,
                logoController,
            )
    }

    @Test
    fun handleRecentHistoryGroupClicked() {
        val historyGroup =
            RecentHistoryGroup(
                title = "mozilla",
                historyMetadata =
                    listOf(
                        HistoryMetadata(
                            key = HistoryMetadataKey("http://www.mozilla.com", null, null),
                            title = "mozilla",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            totalViewTime = 10,
                            documentType = DocumentType.Regular,
                            previewImageUrl = null,
                        )
                    ),
            )

        interactor.onRecentHistoryGroupClicked(historyGroup)
        verify {
            recentVisitsController.handleRecentHistoryGroupClicked(historyGroup)
        }
    }

    @Test
    fun handleHistoryShowAllClicked() {
        interactor.onHistoryShowAllClicked()
        verify { recentVisitsController.handleHistoryShowAllClicked() }
    }

    @Test
    fun onRemoveRecentHistoryGroup() {
        val historyMetadataKey =
            HistoryMetadataKey(
                "http://www.mozilla.com",
                "mozilla",
                null,
            )

        val historyGroup =
            RecentHistoryGroup(
                title = "mozilla",
                historyMetadata =
                    listOf(
                        HistoryMetadata(
                            key = historyMetadataKey,
                            title = "mozilla",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            totalViewTime = 10,
                            documentType = DocumentType.Regular,
                            previewImageUrl = null,
                        )
                    ),
            )

        interactor.onRemoveRecentHistoryGroup(historyGroup.title)

        verify {
            recentVisitsController.handleRemoveRecentHistoryGroup(historyGroup.title)
        }
    }

    @Test
    fun onRecentHistoryHighlightClicked() {
        val historyHighlight: RecentHistoryHighlight = mockk()

        interactor.onRecentHistoryHighlightClicked(historyHighlight)

        verify { recentVisitsController.handleRecentHistoryHighlightClicked(historyHighlight) }
    }

    @Test
    fun onRemoveRecentHistoryHighlight() {
        interactor.onRemoveRecentHistoryHighlight("url")

        verify { recentVisitsController.handleRemoveRecentHistoryHighlight("url") }
    }
}
