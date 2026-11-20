/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.helpers

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2
import org.praha.praha.helpers.HomeActivityIntentTestRule
import org.praha.praha.ui.efficiency.navigation.NavigationGraph
import org.praha.praha.ui.efficiency.navigation.NavigationNode
import org.praha.praha.ui.efficiency.navigation.NavigationNodeId
import org.praha.praha.ui.efficiency.navigation.NavigationNodeKind
import org.praha.praha.ui.efficiency.navigation.NavigationNodes
import org.praha.praha.ui.efficiency.navigation.PageCatalog
import org.praha.praha.ui.efficiency.pageObjects.AddToHomeScreenComponent
import org.praha.praha.ui.efficiency.pageObjects.BookmarkSearchPage
import org.praha.praha.ui.efficiency.pageObjects.BookmarksPage
import org.praha.praha.ui.efficiency.pageObjects.BrowserPage
import org.praha.praha.ui.efficiency.pageObjects.CollectionsPage
import org.praha.praha.ui.efficiency.pageObjects.CustomTabsPage
import org.praha.praha.ui.efficiency.pageObjects.DownloadsPage
import org.praha.praha.ui.efficiency.pageObjects.FindInPagePage
import org.praha.praha.ui.efficiency.pageObjects.HistoryPage
import org.praha.praha.ui.efficiency.pageObjects.HomePage
import org.praha.praha.ui.efficiency.pageObjects.MainMenuPage
import org.praha.praha.ui.efficiency.pageObjects.MicrosurveysPage
import org.praha.praha.ui.efficiency.pageObjects.NotificationPage
import org.praha.praha.ui.efficiency.pageObjects.OnboardingPage
import org.praha.praha.ui.efficiency.pageObjects.RecentlyClosedTabsPage
import org.praha.praha.ui.efficiency.pageObjects.SearchBarComponent
import org.praha.praha.ui.efficiency.pageObjects.SettingsAboutPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsAccessibilityPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsAddonsManagerPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsAdvancedDownloadSettingsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsAdvancedRemoteImprovementsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsAiControlsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsAppIconPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsAutofillPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsCustomizePage
import org.praha.praha.ui.efficiency.pageObjects.SettingsDataCollectionPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsDeleteBrowsingDataOnQuitPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsDeleteBrowsingDataPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsEnhancedTrackingProtectionExceptionsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsEnhancedTrackingProtectionPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsExperimentsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsHTTPSOnlyModePage
import org.praha.praha.ui.efficiency.pageObjects.SettingsHomepagePage
import org.praha.praha.ui.efficiency.pageObjects.SettingsLanguagePage
import org.praha.praha.ui.efficiency.pageObjects.SettingsLoginExceptionsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsOpenLinksInAppsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsPageSummariesPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsPasswordsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsPrivateBrowsingPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsSavePasswordsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsSavedPasswordsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsSearchAddSearchEnginePage
import org.praha.praha.ui.efficiency.pageObjects.SettingsSearchDefaultSearchEnginePage
import org.praha.praha.ui.efficiency.pageObjects.SettingsSearchManageShortcutsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsSearchPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsSiteSettingsAutoplayPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsSiteSettingsExceptionsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsSiteSettingsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsSiteSettingsPermissionsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsTabsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsTranslationsPage
import org.praha.praha.ui.efficiency.pageObjects.SettingsTurnOnSyncPage
import org.praha.praha.ui.efficiency.pageObjects.ShareOverlayPage
import org.praha.praha.ui.efficiency.pageObjects.ShortcutsPage
import org.praha.praha.ui.efficiency.pageObjects.StoriesPage
import org.praha.praha.ui.efficiency.pageObjects.TabDrawerPage
import org.praha.praha.ui.efficiency.pageObjects.TabHistoryPage
import org.praha.praha.ui.efficiency.pageObjects.ToolbarComponent
import org.praha.praha.ui.efficiency.pageObjects.UnifiedTrustPanelPage
import org.praha.praha.ui.efficiency.pageObjects.WebCompatReporterPage

/** Composes the page catalog, readiness oracles, and one isolated navigation graph for a running test. */
class PageContext(val composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) {
    // Let's make sure we have them in a lexicographic order
    val addToHomescreen = AddToHomeScreenComponent(composeRule)
    val bookmarkSearch = BookmarkSearchPage(composeRule)
    val bookmarks = BookmarksPage(composeRule)
    val browserPage = BrowserPage(composeRule)
    val collections = CollectionsPage(composeRule)
    val customTabs = CustomTabsPage(composeRule)
    val downloads = DownloadsPage(composeRule)
    val findInPage = FindInPagePage(composeRule)
    val history = HistoryPage(composeRule)
    val home = HomePage(composeRule)
    val mainMenu = MainMenuPage(composeRule)
    val microsurveys = MicrosurveysPage(composeRule)
    val notification = NotificationPage(composeRule)
    val onboarding = OnboardingPage(composeRule)

    // ReaderViewPage is intentionally NOT registered here. The reader-view appearance controls are a
    // transient overlay reachable only on a reader-capable page via the toolbar toggle + Customize
    // Reader View menu item, which the navigation graph cannot express. Registering it would make the
    // reachability suite fail. Tests drive that sequence explicitly and instantiate ReaderViewPage locally.
    val recentlyClosedTabs = RecentlyClosedTabsPage(composeRule)
    val searchBar = SearchBarComponent(composeRule)
    val settings = SettingsPage(composeRule)
    val settingsAbout = SettingsAboutPage(composeRule)
    val settingsAiControls = SettingsAiControlsPage(composeRule)
    val settingsAccessibility = SettingsAccessibilityPage(composeRule)
    val settingsAddonsManager = SettingsAddonsManagerPage(composeRule)
    val settingsAdvancedDownloadSettings = SettingsAdvancedDownloadSettingsPage(composeRule)
    val settingsAdvancedRemoteImprovements = SettingsAdvancedRemoteImprovementsPage(composeRule)
    val settingsAppIcon = SettingsAppIconPage(composeRule)
    val settingsAutofill = SettingsAutofillPage(composeRule)
    val settingsCustomize = SettingsCustomizePage(composeRule)
    val settingsDataCollection = SettingsDataCollectionPage(composeRule)
    val settingsDeleteBrowsingData = SettingsDeleteBrowsingDataPage(composeRule)
    val settingsDeleteBrowsingDataOnQuit = SettingsDeleteBrowsingDataOnQuitPage(composeRule)
    val settingsEnhancedTrackingProtection = SettingsEnhancedTrackingProtectionPage(composeRule)
    val settingsEnhancedTrackingProtectionExceptions = SettingsEnhancedTrackingProtectionExceptionsPage(composeRule)
    val settingsExperiments = SettingsExperimentsPage(composeRule)
    val settingsHomepage = SettingsHomepagePage(composeRule)
    val settingsHTTPSOnlyMode = SettingsHTTPSOnlyModePage(composeRule)
    val settingsLanguage = SettingsLanguagePage(composeRule)
    val settingsLoginExceptions = SettingsLoginExceptionsPage(composeRule)
    val settingsOpenLinksInApps = SettingsOpenLinksInAppsPage(composeRule)
    val settingsPageSummaries = SettingsPageSummariesPage(composeRule)
    val settingsPasswords = SettingsPasswordsPage(composeRule)
    val settingsPrivateBrowsing = SettingsPrivateBrowsingPage(composeRule)
    val settingsSavePasswords = SettingsSavePasswordsPage(composeRule)
    val settingsSavedPasswords = SettingsSavedPasswordsPage(composeRule)
    val settingsSearch = SettingsSearchPage(composeRule)
    val settingsSearchAddSearchEngine = SettingsSearchAddSearchEnginePage(composeRule)
    val settingsSearchDefaultSearchEngine = SettingsSearchDefaultSearchEnginePage(composeRule)
    val settingsSearchManageShortcuts = SettingsSearchManageShortcutsPage(composeRule)
    val settingsSiteSettings = SettingsSiteSettingsPage(composeRule)
    val settingsSiteSettingsAutoplay = SettingsSiteSettingsAutoplayPage(composeRule)
    val settingsSiteSettingsPermissions = SettingsSiteSettingsPermissionsPage(composeRule)
    val settingsSiteSettingsExceptions = SettingsSiteSettingsExceptionsPage(composeRule)
    val settingsTabs = SettingsTabsPage(composeRule)
    val settingsTranslations = SettingsTranslationsPage(composeRule)
    val settingsTurnOnSync = SettingsTurnOnSyncPage(composeRule)
    val shareOverlay = ShareOverlayPage(composeRule)
    val shortcuts = ShortcutsPage(composeRule)
    val stories = StoriesPage(composeRule)
    val tabDrawer = TabDrawerPage(composeRule)
    val tabHistory = TabHistoryPage(composeRule)
    val toolbar = ToolbarComponent(composeRule)
    val unifiedTrustPanel = UnifiedTrustPanelPage(composeRule)
    val webCompatReporter = WebCompatReporterPage(composeRule)

    val navigationGraph: NavigationGraph

    init {
        val pages =
            PageCatalog.discoverNavigablePages().map { pageRef ->
                val page = pageRef.getter(this)
                check(page.declaredReadinessProfiles() == PageReadinessProfile.entries.toSet()) {
                    "${page.pageName} must declare every page readiness profile"
                }
                page
            }
        val nodes =
            pages.mapTo(mutableSetOf()) {
                NavigationNode(NavigationNodeId(it.pageName), NavigationNodeKind.PAGE)
            } + setOf(NavigationNodes.APP_ENTRY, NavigationNodes.GOOGLE_PLAY)
        val builder = NavigationGraph.Builder(nodes)
        pages.forEach { page ->
            page.registerNavigation(builder)
            builder.registerCheckpointVerifier(page.pageName, page::waitForNavigationCheckpoint)
        }
        navigationGraph = builder.build()
        pages.forEach { it.bindNavigationGraph(navigationGraph) }
    }

    fun initTestRule(
        skipOnboarding: Boolean = true,
        isPageLoadTranslationsPromptEnabled: Boolean = false,
    ): AndroidComposeTestRule<HomeActivityIntentTestRule, *> {
        return AndroidComposeTestRuleV2(
            HomeActivityIntentTestRule(
                skipOnboarding = skipOnboarding,
                isPageLoadTranslationsPromptEnabled = isPageLoadTranslationsPromptEnabled,
            )
        ) {
            it.activity
        }
    }
}
