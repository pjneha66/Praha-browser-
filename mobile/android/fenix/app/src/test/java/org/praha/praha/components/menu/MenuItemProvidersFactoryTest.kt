/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.components.menu

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import mozilla.components.ExperimentalAndroidComponentsApi
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.ipprotection.store.IPProtectionStore
import mozilla.components.feature.ipprotection.store.state.EligibilityStatus
import mozilla.components.feature.ipprotection.store.state.IPProtectionState
import mozilla.components.feature.tabdata.coordinator.fakes.FakeTabDataCoordinator
import mozilla.components.service.fxa.store.SyncStore
import mozilla.components.support.test.robolectric.testContext
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.praha.praha.addons.ExtensionsMenuItemProvider
import org.praha.praha.bookmarks.BookmarkMenuItemProvider
import org.praha.praha.bookmarks.BookmarksScreenMenuItemProvider
import org.praha.praha.browser.BackMenuItemProvider
import org.praha.praha.browser.DefaultBrowserMenuItemProvider
import org.praha.praha.browser.DesktopSiteMenuItemProvider
import org.praha.praha.browser.ForwardMenuItemProvider
import org.praha.praha.browser.RefreshMenuItemProvider
import org.praha.praha.browser.ShareMenuItemProvider
import org.praha.praha.browser.applinks.OpenInAppMenuItemProvider
import org.praha.praha.browser.applinks.OpenInPrahaMenuItemProvider
import org.praha.praha.browser.menu.MoreMenuItemsProvider
import org.praha.praha.browser.menu.MoveToNormalTabsMenuItemProvider
import org.praha.praha.browser.menu.NewPrivateTabMenuItemProvider
import org.praha.praha.browser.menu.NewTabMenuItemProvider
import org.praha.praha.browser.readermode.ReaderViewMenuItemProvider
import org.praha.praha.components.AppStore
import org.praha.praha.components.FindInPageMenuItemProvider
import org.praha.praha.components.accounts.MozillaAccountMenuItemProvider
import org.praha.praha.components.menu.configuration.FenixMenuItem
import org.praha.praha.components.menu.configuration.HomeMenuConfiguration
import org.praha.praha.components.menu.fake.reachableEvents
import org.praha.praha.customtabs.CustomTabAppMenuItemProvider
import org.praha.praha.downloads.DownloadsMenuItemProvider
import org.praha.praha.home.CustomizeHomepageMenuItemProvider
import org.praha.praha.home.topsites.ShortcutMenuItemProvider
import org.praha.praha.ipprotection.VpnMenuItemProvider
import org.praha.praha.library.history.HistoryMenuItemProvider
import org.praha.praha.pdf.SaveAsPdfMenuItemProvider
import org.praha.praha.print.PrintMenuItemProvider
import org.praha.praha.settings.SettingsMenuItemProvider
import org.praha.praha.settings.deletebrowsingdata.QuitMenuItemProvider
import org.praha.praha.settings.logins.PasswordsMenuItemProvider
import org.praha.praha.shortcut.AddToHomeScreenMenuItemProvider
import org.praha.praha.summarization.SummarizePageMenuItemProvider
import org.praha.praha.tabgroups.TabGroupMenuItemProvider
import org.praha.praha.tabgroups.fake.FakeTabGroupsFeatureDiscoveryConfiguration
import org.praha.praha.translations.TranslationsMenuItemProvider
import org.praha.praha.utils.Settings
import org.praha.praha.webcompat.ReportBrokenSiteMenuItemProvider

@RunWith(AndroidJUnit4::class)
class MenuItemProvidersFactoryTest {
    private val scope = CoroutineScope(StandardTestDispatcher())

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `WHEN building the provider of a menu item THEN get the right instance`() {
        val factory = createFactory()

        allMenuItems().forEach { item ->
            assertEquals(expectedProviders.getValue(item::class), factory.buildProviderFor(item)::class, "$item")
        }
    }

    @OptIn(ExperimentalAndroidComponentsApi::class)
    @Test
    fun `WHEN building the home menu THEN show all its items and handle each of their events exactly once`() {
        val settings: Settings =
            mockk(relaxed = true) {
                every { shouldShowMenuBanner } returns true
                every { isDefaultBrowser } returns false
                every { isAutofillSupported } returns true
                every { shouldDeleteBrowsingDataOnQuit } returns true
            }
        val eligibleState = IPProtectionState(eligibilityStatus = EligibilityStatus.Eligible)
        val factory =
            createFactory(
                accessPoint = MenuAccessPoint.Home,
                target = MenuTarget.Home,
                ipProtectionStore = IPProtectionStore(eligibleState),
                settings = settings,
            )
        val registry =
            MenuItemsRegistry(configuration = HomeMenuConfiguration.sections(), resolver = factory::buildProviderFor)

        val shown = registry.providers.filterValues { it.itemsFlow.value.isNotEmpty() }.keys
        assertEquals(
            listOf(
                FenixMenuItem.DefaultBrowserBanner,
                FenixMenuItem.IPProtection,
                FenixMenuItem.Extensions,
                FenixMenuItem.History,
                FenixMenuItem.Bookmarks,
                FenixMenuItem.Downloads,
                FenixMenuItem.Passwords,
                FenixMenuItem.MozillaAccount,
                FenixMenuItem.CustomizeHomepage,
                FenixMenuItem.Settings,
                FenixMenuItem.Quit,
            ),
            shown.toList(),
        )
        registry.providers.values
            .flatMap { it.itemsFlow.value }
            .flatMap { it.reachableEvents() }
            .forEach { event ->
                assertEquals(1, registry.providers.values.count { it.handles(event) }, "Handlers of $event")
            }
    }

    private fun allMenuItems(): List<FenixMenuItem> =
        FenixMenuItem::class.sealedSubclasses.map { type ->
            type.objectInstance
                ?: when (type) {
                    FenixMenuItem.More::class -> FenixMenuItem.More(subMenuItems = emptyList())
                    else -> error("Unknown menu item type $type")
                }
        }

    private fun createFactory(
        accessPoint: MenuAccessPoint = MenuAccessPoint.Browser,
        target: MenuTarget = MenuTarget.BrowserTab,
        ipProtectionStore: IPProtectionStore = IPProtectionStore(),
        settings: Settings = mockk(relaxed = true),
    ) =
        MenuItemProvidersFactory(
            context = testContext,
            accessPoint = accessPoint,
            target = target,
            browserStore = BrowserStore(),
            appStore = AppStore(),
            ipProtectionStore = ipProtectionStore,
            syncStore = SyncStore(),
            httpClient = mockk(relaxed = true),
            historyStorage = mockk(relaxed = true),
            bookmarksStorage = mockk(relaxed = true),
            pinnedSiteStorage = mockk(relaxed = true),
            permissionStorage = mockk(relaxed = true),
            useCases = mockk(relaxed = true),
            addonManager = mockk(relaxed = true),
            engine = mockk(relaxed = true),
            settings = settings,
            summarizeFeatureSettings = mockk(relaxed = true),
            summarizeEligibilityChecker = mockk(relaxed = true),
            translationsSettings = mockk(relaxed = true),
            isAndroidAutomotiveAvailable = false,
            quitApplicationDelegate = {},
            setAsDefaultBrowserDelegate = {},
            menuViewScope = scope,
            applicationScope = scope,
            browsingModeManager = mockk(relaxed = true),
            tabDataCoordinator = FakeTabDataCoordinator(),
            tabGroupFeatureConfiguration = FakeTabGroupsFeatureDiscoveryConfiguration(),
        )

    private val expectedProviders: Map<KClass<out FenixMenuItem>, KClass<out MenuItemProvider>> =
        mapOf(
            FenixMenuItem.NewTab::class to NewTabMenuItemProvider::class,
            FenixMenuItem.NewPrivateTab::class to NewPrivateTabMenuItemProvider::class,
            FenixMenuItem.DefaultBrowserBanner::class to DefaultBrowserMenuItemProvider::class,
            FenixMenuItem.CustomizeReaderView::class to ReaderViewMenuItemProvider::class,
            FenixMenuItem.IPProtection::class to VpnMenuItemProvider::class,
            FenixMenuItem.Bookmark::class to BookmarkMenuItemProvider::class,
            FenixMenuItem.Extensions::class to ExtensionsMenuItemProvider::class,
            FenixMenuItem.FindInPage::class to FindInPageMenuItemProvider::class,
            FenixMenuItem.DesktopSite::class to DesktopSiteMenuItemProvider::class,
            FenixMenuItem.More::class to MoreMenuItemsProvider::class,
            FenixMenuItem.Translate::class to TranslationsMenuItemProvider::class,
            FenixMenuItem.SummarizePage::class to SummarizePageMenuItemProvider::class,
            FenixMenuItem.MoveToNormalTabs::class to MoveToNormalTabsMenuItemProvider::class,
            FenixMenuItem.ReportBrokenSite::class to ReportBrokenSiteMenuItemProvider::class,
            FenixMenuItem.Shortcut::class to ShortcutMenuItemProvider::class,
            FenixMenuItem.AddToHomeScreen::class to AddToHomeScreenMenuItemProvider::class,
            FenixMenuItem.OpenInApp::class to OpenInAppMenuItemProvider::class,
            FenixMenuItem.OpenInFirefox::class to OpenInPrahaMenuItemProvider::class,
            FenixMenuItem.CustomTabAppItems::class to CustomTabAppMenuItemProvider::class,
            FenixMenuItem.SaveAsPdf::class to SaveAsPdfMenuItemProvider::class,
            FenixMenuItem.Print::class to PrintMenuItemProvider::class,
            FenixMenuItem.MozillaAccount::class to MozillaAccountMenuItemProvider::class,
            FenixMenuItem.CustomizeHomepage::class to CustomizeHomepageMenuItemProvider::class,
            FenixMenuItem.Settings::class to SettingsMenuItemProvider::class,
            FenixMenuItem.Quit::class to QuitMenuItemProvider::class,
            FenixMenuItem.Back::class to BackMenuItemProvider::class,
            FenixMenuItem.History::class to HistoryMenuItemProvider::class,
            FenixMenuItem.Bookmarks::class to BookmarksScreenMenuItemProvider::class,
            FenixMenuItem.Downloads::class to DownloadsMenuItemProvider::class,
            FenixMenuItem.Passwords::class to PasswordsMenuItemProvider::class,
            FenixMenuItem.Forward::class to ForwardMenuItemProvider::class,
            FenixMenuItem.Share::class to ShareMenuItemProvider::class,
            FenixMenuItem.Refresh::class to RefreshMenuItemProvider::class,
            FenixMenuItem.TabGroup::class to TabGroupMenuItemProvider::class,
        )
}
