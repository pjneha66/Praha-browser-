/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.compose.base.modifier.thenConditional
import mozilla.components.compose.base.theme.Theme
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.support.ktx.android.net.hostWithoutCommonPrefixes
import mozilla.components.ui.icons.R as iconsR
import mozilla.telemetry.glean.private.NoExtras
import org.praha.praha.GleanMetrics.History
import org.praha.praha.GleanMetrics.HomeBookmarks
import org.praha.praha.GleanMetrics.RecentlyVisitedHomepage
import org.praha.praha.R
import org.praha.praha.components.appstate.AppAction
import org.praha.praha.components.appstate.AppAction.ShortcutAction
import org.praha.praha.components.appstate.setup.checklist.SetupChecklistState
import org.praha.praha.components.components
import org.praha.praha.compose.MessageCard
import org.praha.praha.compose.home.HomeSectionHeader
import org.praha.praha.ext.components
import org.praha.praha.home.bookmarks.Bookmark
import org.praha.praha.home.bookmarks.interactor.BookmarksInteractor
import org.praha.praha.home.bookmarks.view.Bookmarks
import org.praha.praha.home.bookmarks.view.BookmarksMenuItem
import org.praha.praha.home.collections.Collections
import org.praha.praha.home.collections.CollectionsMigrationPromoCard
import org.praha.praha.home.collections.CollectionsMigrationPromoSource
import org.praha.praha.home.collections.CollectionsState
import org.praha.praha.home.collections.migration.CollectionsMigrationCardAction
import org.praha.praha.home.collections.migration.CollectionsMigrationCardAction.ViewTabGroupsClicked
import org.praha.praha.home.fake.FakeHomepagePreview
import org.praha.praha.home.interactor.HomepageInteractor
import org.praha.praha.home.recentsyncedtabs.view.RecentSyncedTab
import org.praha.praha.home.recenttabs.RecentTab
import org.praha.praha.home.recenttabs.interactor.RecentTabInteractor
import org.praha.praha.home.recenttabs.view.RecentTabMenuItem
import org.praha.praha.home.recenttabs.view.RecentTabs
import org.praha.praha.home.recentvisits.RecentlyVisitedItem
import org.praha.praha.home.recentvisits.RecentlyVisitedItem.RecentHistoryGroup
import org.praha.praha.home.recentvisits.RecentlyVisitedItem.RecentHistoryHighlight
import org.praha.praha.home.recentvisits.interactor.RecentVisitsInteractor
import org.praha.praha.home.recentvisits.view.RecentVisitMenuItem
import org.praha.praha.home.recentvisits.view.RecentlyVisited
import org.praha.praha.home.sessioncontrol.CollectionInteractor
import org.praha.praha.home.sessioncontrol.MessageCardInteractor
import org.praha.praha.home.setup.ui.SetupChecklist
import org.praha.praha.home.sports.ui.SportsWidget
import org.praha.praha.home.store.HeaderState
import org.praha.praha.home.store.HomepageState
import org.praha.praha.home.store.MiddleSearchState
import org.praha.praha.home.store.NimbusMessageState
import org.praha.praha.home.store.RecentSyncedTabSectionState
import org.praha.praha.home.termsofuse.PrivacyNoticeBanner
import org.praha.praha.home.termsofuse.PrivacyNoticeBannerInteractor
import org.praha.praha.home.topsites.AddShortcutEntryPoint
import org.praha.praha.home.topsites.AddShortcutSource
import org.praha.praha.home.topsites.TopSiteColors
import org.praha.praha.home.topsites.TopSiteState
import org.praha.praha.home.topsites.TopSites
import org.praha.praha.home.topsites.interactor.TopSiteInteractor
import org.praha.praha.home.topsites.store.DialogState
import org.praha.praha.home.topsites.store.PopularSite
import org.praha.praha.home.topsites.store.toPopularSite
import org.praha.praha.home.topsites.ui.AddShortcutBottomSheet
import org.praha.praha.home.topsites.ui.AddShortcutDialog
import org.praha.praha.home.ui.HomepageTestTag.HOMEPAGE
import org.praha.praha.theme.PrahaTheme
import org.praha.praha.trackingprotection.TrackersBlockedCard
import org.praha.praha.utils.isLargeScreenSize
import org.praha.praha.wallpapers.WallpaperTheme

private const val POPULAR_SITES_TO_SHOW = 8

/**
 * Top level composable for the homepage.
 *
 * @param state State representing the homepage.
 * @param interactor [HomepageInteractor] for interactions with the homepage UI.
 * @param onCollectionsMigrationCardAction Invoked with the [CollectionsMigrationCardAction] to dispatch.
 * @param onTopSitesItemBound Invoked during the composition of a top site item.
 * @param onMiddleSearchBarVisibilityChanged Invoked when the middle search bar is shown/hidden.
 * @param modifier [Modifier] to be applied to the layout.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
internal fun Homepage(
    state: HomepageState,
    interactor: HomepageInteractor,
    onCollectionsMigrationCardAction: (CollectionsMigrationCardAction) -> Unit,
    onTopSitesItemBound: () -> Unit,
    onMiddleSearchBarVisibilityChanged: (isVisible: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val browsingModeChanged = interactor::onPrivateModeButtonClicked
    var shortcutsDialogState by remember { mutableStateOf<DialogState>(DialogState.Closed) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .semantics {
                        testTagsAsResourceId = true
                        testTag = HOMEPAGE
                    }
                    .pointerInput(state.isSearchInProgress) {
                        if (state.isSearchInProgress) {
                            awaitEachGesture {
                                awaitFirstDown(false, PointerEventPass.Initial)
                                interactor.onHomeContentFocusedWhileSearchIsActive()
                            }
                        }
                    }
                    .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state is HomepageState.Normal) {
                BannerCardSection(
                    shouldShowPrivacyNoticeBanner = state.shouldShowPrivacyNoticeBanner,
                    nimbusMessage = state.nimbusMessage,
                    privacyNoticeBannerInteractor = interactor,
                    messageCardInteractor = interactor,
                )
            }

            HomepageHeader(
                browsingMode = state.browsingMode,
                showStoriesButton = state.headerState.showStoriesButton,
                showButtons = state.headerState.showButtons,
                browsingModeChanged = browsingModeChanged,
                onStoriesTapped = { interactor.onDiscoverMoreClicked() },
            )

            if (state.firstFrameDrawn) {
                with(state) {
                    when (this) {
                        is HomepageState.Private -> {
                            if (middleSearchState.searchBarEnabled) {
                                LaunchedEffect(middleSearchState.searchBarVisible) {
                                    onMiddleSearchBarVisibilityChanged(middleSearchState.searchBarVisible)
                                }
                            }

                            Image(
                                painter = painterResource(id = iconsR.drawable.mozac_ic_private_mode_circle_fill_72),
                                contentDescription = null,
                                modifier =
                                    Modifier.thenConditional(
                                            Modifier.padding(vertical = PrahaTheme.layout.space.static50)
                                        ) {
                                            middleSearchState.isShown
                                        }
                                        .size(40.dp),
                            )

                            if (middleSearchState.isShown) {
                                Spacer(Modifier.height(PrahaTheme.layout.space.static400))

                                SearchBar(
                                    modifier = Modifier.padding(horizontal = horizontalMargin),
                                    onClick = interactor::onNavigateSearch,
                                )

                                Spacer(Modifier.height(PrahaTheme.layout.space.static400))
                            } else {
                                Spacer(Modifier.height(PrahaTheme.layout.space.static500))
                            }

                            PrivateBrowsingDescription(onLearnMoreClick = interactor::onLearnMoreClicked)
                        }

                        is HomepageState.Normal -> {
                            val context = LocalContext.current

                            LaunchedEffect(showLongfoxAnimation) {
                                if (showLongfoxAnimation) {
                                    with(context.components) {
                                        settings.longfoxPeekAnimationShownCount++
                                        appStore.dispatch(AppAction.UpdateShowFoxPeekAnimation(false))
                                    }
                                }
                            }

                            val longfoxEntryPointShown = longfoxEnabled && showPrivacyReport
                            LaunchedEffect(longfoxEntryPointShown) {
                                if (longfoxEntryPointShown) {
                                    interactor.onLongfoxEntryPointShown()
                                }
                            }

                            if (middleSearchState.searchBarEnabled) {
                                LaunchedEffect(middleSearchState.searchBarVisible) {
                                    onMiddleSearchBarVisibilityChanged(middleSearchState.searchBarVisible)
                                }

                                if (middleSearchState.searchBarVisible) {
                                    SearchBar(
                                        modifier = Modifier.padding(horizontal = horizontalMargin),
                                        onClick = interactor::onNavigateSearch,
                                    )

                                    Spacer(Modifier.height(PrahaTheme.layout.space.static500))
                                }
                            }

                            if (topSiteState != null) {
                                TopSitesSection(
                                    state = topSiteState,
                                    showHeader = showTopSitesHeader,
                                    interactor = interactor,
                                    onTopSitesItemBound = onTopSitesItemBound,
                                    onAddShortcutClicked = {
                                        context.components.appStore.dispatch(
                                            ShortcutAction.AddShortcutSheetShown(
                                                entryPoint = AddShortcutEntryPoint.HOMEPAGE
                                            )
                                        )
                                        shortcutsDialogState = DialogState.AddShortcutBottomSheet
                                    },
                                )
                            }

                            if (showPrivacyReport) {
                                TrackersBlockedCard(
                                    trackersBlockedCount = trackersBlockedCount,
                                    onPrivacyReportTapped = interactor::onPrivacyReportTapped,
                                    onLongfoxEntryPointClicked = interactor::onLongfoxEntryPointClicked,
                                    modifier = Modifier.padding(top = 16.dp),
                                    longfoxEnabled = longfoxEnabled,
                                    showLongfoxAnimation = showLongfoxAnimation,
                                )
                            }

                            if (sportsWidgetState.isShown) {
                                SportsWidget(
                                    state = sportsWidgetState,
                                    modifier = Modifier.padding(top = PrahaTheme.layout.space.static500),
                                )
                            }

                            MaybeAddSetupChecklist(setupChecklistState, interactor)

                            if (recentTabs != null) {
                                RecentTabsSection(
                                    interactor = interactor,
                                    recentTabs = recentTabs,
                                    reducedTopSpacing = showPrivacyReport && showLongfoxAnimation,
                                )

                                when (val syncedTabState = recentSyncedTabSectionState) {
                                    RecentSyncedTabSectionState.Gone -> Unit
                                    RecentSyncedTabSectionState.Loading,
                                    is RecentSyncedTabSectionState.Visible -> {
                                        val syncedTab = (syncedTabState as? RecentSyncedTabSectionState.Visible)?.tab
                                        Box(
                                            modifier =
                                                Modifier.padding(
                                                    start = horizontalMargin,
                                                    end = horizontalMargin,
                                                    top = verticalMargin,
                                                )
                                        ) {
                                            RecentSyncedTab(
                                                tab = syncedTab,
                                                buttonBackgroundColor =
                                                    if (syncedTab != null) {
                                                        WallpaperTheme.buttonBackgroundColor
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                                    },
                                                buttonTextColor = WallpaperTheme.buttonTextColor,
                                                onRecentSyncedTabClick = interactor::onRecentSyncedTabClicked,
                                                onSeeAllSyncedTabsButtonClick = interactor::onSyncedTabShowAllClicked,
                                                onRemoveSyncedTab = interactor::onRemovedRecentSyncedTab,
                                            )
                                        }
                                    }
                                }
                            }

                            if (bookmarks != null) {
                                BookmarksSection(
                                    bookmarks = bookmarks,
                                    interactor = interactor,
                                )
                            }

                            if (recentlyVisited != null) {
                                RecentlyVisitedSection(
                                    recentVisits = recentlyVisited,
                                    interactor = interactor,
                                )
                            }

                            CollectionsSection(
                                collectionsState = collectionsState,
                                interactor = interactor,
                                onCollectionsMigrationCardAction = onCollectionsMigrationCardAction,
                            )

                            Spacer(Modifier.height(bottomPadding.dp))

                            val popularSites = observePopularSites(topSites = topSiteState?.topSites)

                            when (shortcutsDialogState) {
                                DialogState.AddShortcutBottomSheet -> {
                                    AddShortcutBottomSheet(
                                        popularSites = popularSites,
                                        onDismiss = { shortcutsDialogState = DialogState.Closed },
                                        onAddWebsiteClicked = {
                                            context.components.appStore.dispatch(ShortcutAction.AddWebsiteDialogShown)
                                            shortcutsDialogState = DialogState.AddShortcut
                                        },
                                        onAddPopularSiteClick = { site ->
                                            interactor.onSaveShortcut(
                                                title = site.title,
                                                url = site.url,
                                                source = AddShortcutSource.POPULAR,
                                                entryPoint = AddShortcutEntryPoint.HOMEPAGE,
                                            )
                                            shortcutsDialogState = DialogState.Closed
                                        },
                                    )
                                }

                                DialogState.AddShortcut -> {
                                    AddShortcutDialog(
                                        onDismiss = { shortcutsDialogState = DialogState.Closed },
                                        onConfirm = { title, url ->
                                            interactor.onSaveShortcut(
                                                title = title,
                                                url = url,
                                                source = AddShortcutSource.MANUAL,
                                                entryPoint = AddShortcutEntryPoint.HOMEPAGE,
                                            )
                                            shortcutsDialogState = DialogState.Closed
                                        },
                                    )
                                }

                                DialogState.Closed -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaybeAddSetupChecklist(
    setupChecklistState: SetupChecklistState?,
    interactor: HomepageInteractor,
) {
    val isTabletDevice = LocalContext.current.isLargeScreenSize()
    if (!isTabletDevice && setupChecklistState != null && setupChecklistState.isVisible) {
        SetupChecklist(
            setupChecklistState = setupChecklistState,
            interactor = interactor,
        )
    }
}

@Composable
private fun BannerCardSection(
    shouldShowPrivacyNoticeBanner: Boolean,
    nimbusMessage: NimbusMessageState?,
    privacyNoticeBannerInteractor: PrivacyNoticeBannerInteractor,
    messageCardInteractor: MessageCardInteractor,
) {
    if (shouldShowPrivacyNoticeBanner) {
        Spacer(modifier = Modifier.height(12.dp))

        PrivacyNoticeBanner(
            modifier = Modifier.padding(horizontal = 16.dp),
            interactor = privacyNoticeBannerInteractor,
        )
    }
    nimbusMessage?.apply {
        Spacer(modifier = Modifier.height(12.dp))

        MessageCard(
            messageCardState = cardState,
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = { messageCardInteractor.onMessageClicked(message) },
            onCloseButtonClick = { messageCardInteractor.onMessageClosedClicked(message) },
        )
    }
}

@Composable
private fun TopSitesSection(
    state: TopSiteState,
    showHeader: Boolean = true,
    interactor: TopSiteInteractor,
    onTopSitesItemBound: () -> Unit,
    onAddShortcutClicked: () -> Unit,
) {
    if (showHeader) {
        HomeSectionHeader(
            headerText = stringResource(R.string.homepage_shortcuts_title),
            modifier = Modifier.padding(horizontal = horizontalMargin),
            description = stringResource(R.string.homepage_shortcuts_show_all_content_description),
            onButtonClick =
                if (state.showShortcutsLibraryButton) {
                    interactor::onShowAllTopSitesClicked
                } else {
                    null
                },
        )

        Spacer(Modifier.height(16.dp))
    }

    TopSites(
        state = state,
        interactor = interactor,
        onTopSitesItemBound = onTopSitesItemBound,
        onAddShortcutClicked = onAddShortcutClicked,
    )
}

@Composable
private fun RecentTabsSection(
    interactor: RecentTabInteractor,
    recentTabs: List<RecentTab>,
    reducedTopSpacing: Boolean = false,
) {
    val topSpacing = if (reducedTopSpacing) 16.dp else 40.dp
    Spacer(modifier = Modifier.height(topSpacing))

    Column(modifier = Modifier.padding(horizontal = horizontalMargin)) {
        HomeSectionHeader(headerText = stringResource(R.string.recent_tabs_header_2))

        Spacer(Modifier.height(16.dp))

        RecentTabs(
            recentTabs = recentTabs,
            onRecentTabClick = { interactor.onRecentTabClicked(it) },
            menuItems =
                listOf(
                    RecentTabMenuItem(
                        title = stringResource(id = R.string.recent_tab_menu_item_remove),
                        onClick = interactor::onRemoveRecentTab,
                    )
                ),
        )
    }
}

@Composable
private fun BookmarksSection(
    bookmarks: List<Bookmark>,
    interactor: BookmarksInteractor,
) {
    LaunchedEffect(Unit) {
        HomeBookmarks.shown.record(NoExtras())
    }

    Spacer(modifier = Modifier.height(40.dp))

    HomeSectionHeader(
        headerText = stringResource(R.string.home_bookmarks_title),
        modifier = Modifier.padding(horizontal = horizontalMargin),
        description = stringResource(R.string.home_bookmarks_show_all_content_description),
        onButtonClick = interactor::onShowAllBookmarksClicked,
    )

    Spacer(Modifier.height(16.dp))

    Bookmarks(
        bookmarks = bookmarks,
        menuItems =
            listOf(
                BookmarksMenuItem(
                    stringResource(id = R.string.home_bookmarks_menu_item_remove),
                    onClick = interactor::onBookmarkRemoved,
                )
            ),
        onBookmarkClick = interactor::onBookmarkClicked,
    )
}

@Composable
private fun RecentlyVisitedSection(
    recentVisits: List<RecentlyVisitedItem>,
    interactor: RecentVisitsInteractor,
) {
    Spacer(modifier = Modifier.height(40.dp))

    Box(modifier = Modifier.padding(horizontal = horizontalMargin)) {
        HomeSectionHeader(
            headerText = stringResource(R.string.history_metadata_header_2),
            description = stringResource(R.string.past_explorations_show_all_content_description_2),
            onButtonClick = interactor::onHistoryShowAllClicked,
        )
    }

    Spacer(Modifier.height(16.dp))

    RecentlyVisited(
        recentVisits = recentVisits,
        menuItems =
            listOfNotNull(
                RecentVisitMenuItem(
                    title = stringResource(R.string.recently_visited_menu_item_remove),
                    onClick = { visit ->
                        when (visit) {
                            is RecentHistoryGroup -> interactor.onRemoveRecentHistoryGroup(visit.title)
                            is RecentHistoryHighlight -> interactor.onRemoveRecentHistoryHighlight(visit.url)
                        }
                    },
                )
            ),
        onRecentVisitClick = { recentlyVisitedItem, pageNumber ->
            when (recentlyVisitedItem) {
                is RecentHistoryHighlight -> {
                    RecentlyVisitedHomepage.historyHighlightOpened.record(NoExtras())
                    interactor.onRecentHistoryHighlightClicked(recentlyVisitedItem)
                }

                is RecentHistoryGroup -> {
                    RecentlyVisitedHomepage.searchGroupOpened.record(NoExtras())
                    History.recentSearchesTapped.record(History.RecentSearchesTappedExtra(pageNumber.toString()))
                    interactor.onRecentHistoryGroupClicked(recentlyVisitedItem)
                }
            }
        },
    )
}

@Composable
private fun CollectionsSection(
    collectionsState: CollectionsState,
    interactor: CollectionInteractor,
    onCollectionsMigrationCardAction: (CollectionsMigrationCardAction) -> Unit,
) {
    when (collectionsState) {
        is CollectionsState.Content -> {
            CollectionsSectionContent {
                Collections(
                    collections = collectionsState.collections,
                    expandedCollections = collectionsState.expandedCollections,
                    interactor = interactor,
                )
            }
        }

        CollectionsState.MigrationCard -> {
            CollectionsSectionContent {
                CollectionsMigrationPromoCard(
                    source = CollectionsMigrationPromoSource.HOMEPAGE,
                    onClick = { onCollectionsMigrationCardAction(ViewTabGroupsClicked) },
                )
            }
        }

        CollectionsState.Gone -> {} // no-op. Nothing is shown where there are no collections.
    }
}

@Composable
private fun observePopularSites(topSites: List<TopSite>?): List<PopularSite> {
    val merinoManifestProvider = components.core.merinoManifestProvider
    val popularSites by
        produceState(
            initialValue = emptyList(),
            key1 = merinoManifestProvider,
            key2 = topSites,
        ) {
            value =
                withContext(Dispatchers.IO) {
                    merinoManifestProvider
                        .getTopDomains(
                            limit = POPULAR_SITES_TO_SHOW,
                            excludedDomains =
                                topSites.orEmpty().mapNotNullTo(mutableSetOf()) {
                                    it.url.toUri().hostWithoutCommonPrefixes
                                },
                        )
                        .map { it.toPopularSite() }
                }
        }
    return popularSites
}

@Composable
private fun CollectionsSectionContent(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = horizontalMargin)) {
        Spacer(Modifier.height(56.dp))

        HomeSectionHeader(headerText = stringResource(R.string.collections_header))

        Spacer(Modifier.height(10.dp))

        content()
    }
}

@Composable
@PreviewLightDark
private fun HomepagePreview() {
    PrahaTheme {
        Surface {
            Homepage(
                state =
                    HomepageState.Normal(
                        shouldShowPrivacyNoticeBanner = false,
                        nimbusMessage = null,
                        topSiteState =
                            TopSiteState(
                                topSites = FakeHomepagePreview.topSites(),
                                colors = TopSiteColors.colors(),
                            ),
                        recentTabs = FakeHomepagePreview.recentTabs(),
                        recentSyncedTabSectionState =
                            RecentSyncedTabSectionState.Visible(FakeHomepagePreview.recentSyncedTab()),
                        bookmarks = FakeHomepagePreview.bookmarks(),
                        recentlyVisited = FakeHomepagePreview.recentHistory(),
                        collectionsState = CollectionsState.Gone,
                        showPrivacyReport = true,
                        longfoxEnabled = false,
                        showLongfoxAnimation = false,
                        trackersBlockedCount = 754,
                        headerState = HeaderState(showStoriesButton = true),
                        middleSearchState = MiddleSearchState(searchBarVisible = true, searchBarEnabled = false),
                        firstFrameDrawn = true,
                        setupChecklistState = null,
                        isSearchInProgress = false,
                        bottomPadding = 68,
                        showTopSitesHeader = true,
                    ),
                interactor = FakeHomepagePreview.homepageInteractor,
                onCollectionsMigrationCardAction = {},
                onTopSitesItemBound = {},
                onMiddleSearchBarVisibilityChanged = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
@PreviewLightDark
private fun HomepageBannerPreview() {
    PrahaTheme {
        Surface {
            Homepage(
                state =
                    HomepageState.Normal(
                        shouldShowPrivacyNoticeBanner = true,
                        nimbusMessage = null,
                        topSiteState =
                            TopSiteState(
                                topSites = FakeHomepagePreview.topSites(),
                                colors = TopSiteColors.colors(),
                            ),
                        recentTabs = FakeHomepagePreview.recentTabs(),
                        recentSyncedTabSectionState =
                            RecentSyncedTabSectionState.Visible(FakeHomepagePreview.recentSyncedTab()),
                        bookmarks = FakeHomepagePreview.bookmarks(),
                        recentlyVisited = FakeHomepagePreview.recentHistory(),
                        collectionsState = CollectionsState.Gone,
                        showPrivacyReport = true,
                        longfoxEnabled = false,
                        showLongfoxAnimation = false,
                        trackersBlockedCount = 754,
                        headerState = HeaderState(showStoriesButton = true),
                        middleSearchState = MiddleSearchState(searchBarVisible = true, searchBarEnabled = false),
                        firstFrameDrawn = true,
                        setupChecklistState = null,
                        isSearchInProgress = false,
                        bottomPadding = 68,
                        showTopSitesHeader = true,
                    ),
                interactor = FakeHomepagePreview.homepageInteractor,
                onCollectionsMigrationCardAction = {},
                onTopSitesItemBound = {},
                onMiddleSearchBarVisibilityChanged = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
@PreviewLightDark
private fun HomepagePreviewCollections() {
    PrahaTheme {
        Surface {
            Homepage(
                state =
                    HomepageState.Normal(
                        shouldShowPrivacyNoticeBanner = false,
                        nimbusMessage = null,
                        recentlyVisited = FakeHomepagePreview.recentHistory(),
                        collectionsState = FakeHomepagePreview.collectionState(),
                        showPrivacyReport = true,
                        longfoxEnabled = false,
                        showLongfoxAnimation = false,
                        trackersBlockedCount = 754,
                        headerState = HeaderState(showStoriesButton = true),
                        middleSearchState = MiddleSearchState(searchBarVisible = true, searchBarEnabled = false),
                        firstFrameDrawn = true,
                        setupChecklistState = null,
                        isSearchInProgress = false,
                        bottomPadding = 68,
                        showTopSitesHeader = true,
                    ),
                interactor = FakeHomepagePreview.homepageInteractor,
                onCollectionsMigrationCardAction = {},
                onTopSitesItemBound = {},
                onMiddleSearchBarVisibilityChanged = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
@PreviewLightDark
private fun HomepageCollectionsMigrationCardPreview() {
    PrahaTheme {
        Surface {
            Homepage(
                state =
                    HomepageState.Normal(
                        shouldShowPrivacyNoticeBanner = false,
                        nimbusMessage = null,
                        recentlyVisited = FakeHomepagePreview.recentHistory(),
                        collectionsState = CollectionsState.MigrationCard,
                        showPrivacyReport = true,
                        longfoxEnabled = false,
                        showLongfoxAnimation = false,
                        trackersBlockedCount = 754,
                        headerState = HeaderState(showStoriesButton = true),
                        middleSearchState = MiddleSearchState(searchBarVisible = true, searchBarEnabled = false),
                        firstFrameDrawn = true,
                        setupChecklistState = null,
                        isSearchInProgress = false,
                        bottomPadding = 68,
                        showTopSitesHeader = true,
                    ),
                interactor = FakeHomepagePreview.homepageInteractor,
                onCollectionsMigrationCardAction = {},
                onTopSitesItemBound = {},
                onMiddleSearchBarVisibilityChanged = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
@PreviewLightDark
private fun MinimalHomepagePreview() {
    PrahaTheme {
        Surface {
            Homepage(
                state =
                    HomepageState.Normal(
                        shouldShowPrivacyNoticeBanner = false,
                        nimbusMessage = null,
                        topSiteState =
                            TopSiteState(
                                topSites = FakeHomepagePreview.topSites(),
                                colors = TopSiteColors.colors(),
                            ),
                        collectionsState = CollectionsState.Gone,
                        showPrivacyReport = true,
                        longfoxEnabled = false,
                        showLongfoxAnimation = false,
                        trackersBlockedCount = 754,
                        headerState = HeaderState(showStoriesButton = true),
                        firstFrameDrawn = true,
                        setupChecklistState = null,
                        isSearchInProgress = false,
                        bottomPadding = 68,
                        showTopSitesHeader = true,
                    ),
                interactor = FakeHomepagePreview.homepageInteractor,
                onCollectionsMigrationCardAction = {},
                onTopSitesItemBound = {},
                onMiddleSearchBarVisibilityChanged = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
@Preview
private fun PrivateHomepagePreview() {
    PrahaTheme(theme = Theme.Private) {
        Homepage(
            state =
                HomepageState.Private(
                    headerState = HeaderState(showStoriesButton = true),
                    firstFrameDrawn = true,
                    isSearchInProgress = false,
                ),
            interactor = FakeHomepagePreview.homepageInteractor,
            onCollectionsMigrationCardAction = {},
            onTopSitesItemBound = {},
            onMiddleSearchBarVisibilityChanged = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

internal val horizontalMargin: Dp
    @Composable @ReadOnlyComposable get() = dimensionResource(R.dimen.home_item_horizontal_margin)

private val verticalMargin: Dp
    @Composable @ReadOnlyComposable get() = dimensionResource(R.dimen.home_item_vertical_margin)
