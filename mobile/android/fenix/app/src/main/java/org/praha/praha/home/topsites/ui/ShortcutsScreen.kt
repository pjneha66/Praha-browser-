/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.home.topsites.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.annotation.FlexibleWindowLightDarkPreview
import mozilla.components.compose.base.button.IconButton
import mozilla.components.compose.base.utils.BackInvokedHandler
import mozilla.components.ui.icons.R as iconsR
import org.praha.praha.R
import org.praha.praha.home.fake.FakeHomepagePreview
import org.praha.praha.home.topsites.AddShortcutSource
import org.praha.praha.home.topsites.TopSiteColors
import org.praha.praha.home.topsites.getMenuItems
import org.praha.praha.home.topsites.interactor.TopSiteInteractor
import org.praha.praha.home.topsites.store.DialogState
import org.praha.praha.home.topsites.store.PopularSite
import org.praha.praha.home.topsites.store.ShortcutsAction
import org.praha.praha.home.topsites.store.ShortcutsState
import org.praha.praha.home.topsites.store.ShortcutsStore
import org.praha.praha.theme.PrahaTheme

/**
 * The shortcuts screen.
 *
 * @param store The [ShortcutsStore] used to observe the screen state and dispatch actions.
 * @param interactor The [TopSiteInteractor] used to handle user interactions with shortcuts.
 * @param onNavigationIconClick Callback invoked when the navigation icon is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutsScreen(
    store: ShortcutsStore,
    interactor: TopSiteInteractor,
    onNavigationIconClick: () -> Unit,
) {
    val state by remember { store.stateFlow }.collectAsState(initial = store.state)

    LaunchedEffect(Unit) {
        interactor.onShortcutsLibraryViewed()
    }

    BackInvokedHandler {
        onNavigationIconClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.homepage_shortcuts_title),
                        style = PrahaTheme.typography.headline5,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigationIconClick,
                        contentDescription = "",
                    ) {
                        Icon(
                            painter = painterResource(iconsR.drawable.mozac_ic_back_24),
                            contentDescription = null,
                        )
                    }
                },
                windowInsets =
                    WindowInsets(
                        top = 0.dp,
                        bottom = 0.dp,
                    ),
            )
        }
    ) { paddingValues ->
        ShortcutsScreenContent(
            state = state,
            paddingValues = paddingValues,
            interactor = interactor,
            onAddShortcutClicked = { store.dispatch(ShortcutsAction.ShowAddShortcutBottomSheet) },
        )
    }

    ShortcutsDialog(
        dialogState = state.dialogState,
        popularSites = state.popularSites,
        onDismiss = { store.dispatch(ShortcutsAction.CloseDialog) },
        onAddWebsiteClicked = { store.dispatch(ShortcutsAction.ShowAddShortcutDialog) },
        onSaveShortcut = { title, url ->
            store.dispatch(
                ShortcutsAction.SaveShortcut(
                    title = title,
                    url = url,
                    source = AddShortcutSource.MANUAL,
                )
            )
        },
        onAddPopularSiteClick = { site ->
            store.dispatch(
                ShortcutsAction.SaveShortcut(
                    title = site.title,
                    url = site.url,
                    source = AddShortcutSource.POPULAR,
                )
            )
        },
    )
}

@Composable
private fun ShortcutsScreenContent(
    state: ShortcutsState,
    paddingValues: PaddingValues,
    interactor: TopSiteInteractor,
    onAddShortcutClicked: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(paddingValues).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Shortcuts(
            topSites = state.topSites,
            topSiteColors = TopSiteColors.colors(),
            showAddShortcut = true,
            scrollable = true,
            menuItems = { topSite ->
                getMenuItems(
                    topSite = topSite,
                    onOpenInPrivateTabClicked = interactor::onOpenInPrivateTabClicked,
                    onEditTopSiteClicked = interactor::onEditTopSiteClicked,
                    onRemoveTopSiteClicked = interactor::onRemoveTopSiteClicked,
                    onSettingsClicked = interactor::onSettingsClicked,
                    onSponsorPrivacyClicked = interactor::onSponsorPrivacyClicked,
                )
            },
            onTopSiteClick = { topSite ->
                interactor.onSelectTopSite(topSite = topSite, position = state.topSites.indexOf(topSite))
            },
            onTopSiteLongClick = interactor::onTopSiteLongClicked,
            onTopSiteImpression = interactor::onTopSiteImpression,
            onTopSitesItemBound = {},
            onAddShortcutClicked = onAddShortcutClicked,
            modifier = Modifier.fillMaxSize().padding(PrahaTheme.layout.space.static200),
        )
    }
}

@Composable
private fun ShortcutsDialog(
    dialogState: DialogState,
    popularSites: List<PopularSite>,
    onDismiss: () -> Unit,
    onAddWebsiteClicked: () -> Unit,
    onSaveShortcut: (title: String, url: String) -> Unit,
    onAddPopularSiteClick: (PopularSite) -> Unit,
) {
    when (dialogState) {
        DialogState.AddShortcutBottomSheet -> {
            AddShortcutBottomSheet(
                popularSites = popularSites,
                onDismiss = onDismiss,
                onAddWebsiteClicked = onAddWebsiteClicked,
                onAddPopularSiteClick = onAddPopularSiteClick,
            )
        }

        DialogState.AddShortcut -> {
            AddShortcutDialog(
                onDismiss = onDismiss,
                onConfirm = onSaveShortcut,
            )
        }

        DialogState.Closed -> Unit
    }
}

@Composable
@FlexibleWindowLightDarkPreview
private fun ShortcutsScreenPreview() {
    PrahaTheme {
        ShortcutsScreen(
            store = ShortcutsStore(initialState = ShortcutsState(topSites = FakeHomepagePreview.topSites())),
            interactor = FakeHomepagePreview.topSitesInteractor,
            onNavigationIconClick = {},
        )
    }
}
