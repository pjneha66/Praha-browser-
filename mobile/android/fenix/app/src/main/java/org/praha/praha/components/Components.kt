/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.components

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import com.google.android.play.core.review.ReviewManagerFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import mozilla.components.concept.ai.controls.AIFeatureBlock
import mozilla.components.concept.ai.controls.AIFeatureRegistry
import mozilla.components.concept.integrity.RequestHashProvider
import mozilla.components.feature.addons.AddonManager
import mozilla.components.feature.addons.amo.AMOAddonsProvider
import mozilla.components.feature.addons.migration.DefaultSupportedAddonsChecker
import mozilla.components.feature.addons.update.DefaultAddonUpdater
import mozilla.components.feature.autofill.AutofillConfiguration
import mozilla.components.feature.summarize.PageSummaryFeature
import mozilla.components.feature.summarize.settings.SummarizationSettings
import mozilla.components.feature.tabdata.coordinator.DefaultTabDataCoordinator
import mozilla.components.feature.tabdata.coordinator.TabDataCoordinator
import mozilla.components.lib.ai.controls.AIFeatureBlockStorage
import mozilla.components.lib.ai.controls.dataStore
import mozilla.components.lib.ai.controls.default
import mozilla.components.lib.crash.store.CrashAction
import mozilla.components.lib.crash.store.CrashMiddleware
import mozilla.components.lib.integrity.googleplay.GooglePlayIntegrityClient
import mozilla.components.lib.integrity.googleplay.IntegrityConsumer
import mozilla.components.lib.llm.mlpa.MlpaTokenStorage
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import mozilla.components.service.fxrelay.eligibility.RelayEligibilityStore
import mozilla.components.service.fxrelay.eligibility.middlewares.ClearLastUsedMiddleware
import mozilla.components.support.base.android.DefaultProcessInfoProvider
import mozilla.components.support.base.android.NotificationsDelegate
import mozilla.components.support.base.worker.Frequency
import mozilla.components.support.remotesettings.DefaultRemoteSettingsSyncScheduler
import mozilla.components.support.remotesettings.RemoteSettingsServer
import mozilla.components.support.remotesettings.RemoteSettingsService
import mozilla.components.support.remotesettings.into
import mozilla.components.support.utils.BuildManufacturerChecker
import mozilla.components.support.utils.BuildMetadata
import mozilla.components.support.utils.ClipboardHandler
import mozilla.components.support.utils.ext.packageManagerWrapper
import org.praha.praha.BuildConfig
import org.praha.praha.Config
import org.praha.praha.FeatureFlags
import org.praha.praha.R
import org.praha.praha.autofill.AutofillConfirmActivity
import org.praha.praha.autofill.AutofillSearchActivity
import org.praha.praha.autofill.AutofillUnlockActivity
import org.praha.praha.browser.relay.ErrorMessages
import org.praha.praha.browser.relay.RelayFeatureIntegration
import org.praha.praha.components.appstate.AppAction
import org.praha.praha.components.appstate.AppState
import org.praha.praha.components.appstate.setup.checklist.SetupChecklistState
import org.praha.praha.components.appstate.setup.checklist.getSetupChecklistCollection
import org.praha.praha.components.appstate.sports.SportsWidgetState
import org.praha.praha.components.bookmarks.lastSavedFolderCache
import org.praha.praha.components.ipprotection.IPProtection
import org.praha.praha.components.ipprotection.IPProtectionAuthSources
import org.praha.praha.components.lens.LensImageSearch
import org.praha.praha.components.listentopage.ListenToPage
import org.praha.praha.components.llm.Llm
import org.praha.praha.components.llm.ext.accessTokenProvider
import org.praha.praha.components.menu.MenuCustomizationRepository
import org.praha.praha.components.menu.configuration.BrowserMenuConfiguration
import org.praha.praha.components.metrics.MetricsMiddleware
import org.praha.praha.components.tabs.DefaultTabRepository
import org.praha.praha.crashes.CrashReportingAppMiddleware
import org.praha.praha.crashes.SettingsCrashReportCache
import org.praha.praha.datastore.customizedBrowserMenuInfoDataStore
import org.praha.praha.datastore.pocketStoriesSelectedCategoriesDataStore
import org.praha.praha.distributions.DefaultDistributionBrowserStoreProvider
import org.praha.praha.distributions.DefaultDistributionProviderChecker
import org.praha.praha.distributions.DefaultDistributionSettings
import org.praha.praha.distributions.DistributionIdManager
import org.praha.praha.ext.asRecentTabs
import org.praha.praha.ext.components
import org.praha.praha.ext.filterState
import org.praha.praha.ext.isToolbarAtBottom
import org.praha.praha.ext.sort
import org.praha.praha.home.PocketMiddleware
import org.praha.praha.home.SettingsBackedPocketSettings
import org.praha.praha.home.blocklist.BlocklistHandler
import org.praha.praha.home.blocklist.BlocklistMiddleware
import org.praha.praha.home.collections.migration.CollectionsMigrationRepository
import org.praha.praha.home.collections.migration.DefaultCollectionsMigrationRepository
import org.praha.praha.home.middleware.HomeTelemetryMiddleware
import org.praha.praha.home.setup.store.DefaultSetupChecklistRepository
import org.praha.praha.home.setup.store.SetupChecklistPreferencesMiddleware
import org.praha.praha.home.setup.store.SetupChecklistTelemetryMiddleware
import org.praha.praha.ipprotection.store.DefaultIPProtectionPromptRepository
import org.praha.praha.listentopage.ListenToPageAIControlFeature
import org.praha.praha.messaging.state.MessagingMiddleware
import org.praha.praha.nimbus.FxNimbus
import org.praha.praha.onboarding.FenixOnboarding
import org.praha.praha.perf.AppLinkIntentLaunchTypeProvider
import org.praha.praha.perf.AppStartReasonProvider
import org.praha.praha.perf.StartupActivityLog
import org.praha.praha.perf.StartupStateProvider
import org.praha.praha.perf.StrictModeManager
import org.praha.praha.reviewprompt.ReviewPromptMiddleware
import org.praha.praha.search.VoiceSearchAIControlFeature
import org.praha.praha.settings.emailmasks.middleware.DefaultEmailMasksRepository
import org.praha.praha.settings.emailmasks.middleware.EmailMasksRepository
import org.praha.praha.settings.settingssearch.DefaultFenixSettingsIndexer
import org.praha.praha.termsofuse.TermsOfUseManager
import org.praha.praha.termsofuse.store.DefaultTermsOfUsePromptRepository
import org.praha.praha.utils.Settings
import org.praha.praha.utils.isLargeScreenSize
import org.praha.praha.wifi.WifiConnectionMonitor
import org.mozilla.gecko.search.SearchWidgetProvider

private const val AMO_COLLECTION_MAX_CACHE_AGE = 2 * 24 * 60L // Two days in minutes

/**
 * Provides access to all components. This class is an implementation of the Service Locator pattern, which helps us
 * manage the dependencies in our app.
 *
 * Note: these aren't just "components" from "android-components": they're any "component" that can be considered a
 * building block of our app.
 */
class Components(
    private val context: Context,
    /**
     * A [CoroutineScope] that is tied to the lifetime of the application process.
     *
     * Note: Tasks should be scoped to the container which holds their UI. If necessary, applicationScope can be used
     * for top-level background work that must remain active for the whole duration of the application.
     */
    val applicationScope: CoroutineScope,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) {
    val backgroundServices by lazy {
        BackgroundServices(
            context,
            push,
            settings,
            analytics.crashReporter,
            core.lazyHistoryStorage,
            core.lazyBookmarksStorage,
            core.lazyPasswordsStorage,
            core.lazyRemoteTabsStorage,
            core.lazyAutofillStorage,
            strictMode,
            applicationScope,
        )
    }
    val services by lazy { Services(context, core.store, backgroundServices.accountManager, settings) }
    val core by lazy {
        Core(context, analytics.crashReporter, strictMode, performance.visualCompletenessQueue, applicationScope)
    }

    val useCases by lazy {
        UseCases(
            context = context,
            crashReporter = lazy { analytics.crashReporter },
            engine = lazy { core.engine },
            store = lazy { core.store },
            shortcutManager = lazy { core.webAppShortcutManager },
            topSitesStorage = lazy { core.topSitesStorage },
            bookmarksStorage = lazy { core.bookmarksStorage },
            historyStorage = lazy { core.historyStorage },
            lastSavedFolderCache = lazy { context.components.settings.lastSavedFolderCache },
            syncedTabsCommands = lazy { backgroundServices.syncedTabsCommands },
            adsClientProvider = ads.lazyAdsClientProvider,
            appStore = lazy { appStore },
            client = lazy { core.client },
            strictMode = lazy { strictMode },
        )
    }

    private val notificationManagerCompat = NotificationManagerCompat.from(context)

    val notificationsDelegate: NotificationsDelegate by lazy {
        NotificationsDelegate(notificationManagerCompat)
    }

    val intentProcessors by lazy {
        IntentProcessors(
            context,
            core.store,
            useCases.sessionUseCases,
            useCases.tabsUseCases,
            useCases.customTabsUseCases,
            useCases.searchUseCases,
            core.webAppManifestStorage,
            core.engine,
            applicationScope,
        )
    }

    val addonsProvider by lazy {
        // Check if we have a customized (overridden) AMO collection (supported in Nightly & Beta)
        if (FeatureFlags.customExtensionCollectionFeature && settings.amoCollectionOverrideConfigured()) {
            AMOAddonsProvider(
                context,
                core.client,
                collectionUser = settings.overrideAmoUser,
                collectionName = settings.overrideAmoCollection,
            )
        }
        // Use build config otherwise
        else if (BuildConfig.AMO_COLLECTION_USER.isNotEmpty() && BuildConfig.AMO_COLLECTION_NAME.isNotEmpty()) {
            AMOAddonsProvider(
                context,
                core.client,
                serverURL = BuildConfig.AMO_SERVER_URL,
                collectionUser = BuildConfig.AMO_COLLECTION_USER,
                collectionName = BuildConfig.AMO_COLLECTION_NAME,
                maxCacheAgeInMinutes = AMO_COLLECTION_MAX_CACHE_AGE,
            )
        }
        // Fall back to defaults
        else {
            AMOAddonsProvider(context, core.client, maxCacheAgeInMinutes = AMO_COLLECTION_MAX_CACHE_AGE)
        }
    }

    @Suppress("MagicNumber")
    val addonUpdater by lazy {
        DefaultAddonUpdater(context, Frequency(12, TimeUnit.HOURS), core.store)
    }

    @Suppress("MagicNumber")
    val supportedAddonsChecker by lazy {
        DefaultSupportedAddonsChecker(
            context,
            Frequency(12, TimeUnit.HOURS),
        )
    }

    @Suppress("MagicNumber")
    val remoteSettingsSyncScheduler by lazy {
        DefaultRemoteSettingsSyncScheduler(
            context,
            Frequency(2, TimeUnit.HOURS),
        )
    }

    val addonManager by lazy {
        AddonManager(core.store, core.engine, addonsProvider, addonUpdater)
    }

    val analytics by lazy {
        Analytics(context, settings, nimbus, performance.visualCompletenessQueue, buildMetadata)
    }

    val remoteSettingsService = lazy {
        RemoteSettingsService(
            context,
            when (settings.remoteSettingsServer) {
                context.getString(R.string.remote_settings_server_prod) -> RemoteSettingsServer.Prod.into()
                context.getString(R.string.remote_settings_server_dev) -> RemoteSettingsServer.Dev.into()
                context.getString(R.string.remote_settings_server_stage) -> RemoteSettingsServer.Stage.into()
                else -> RemoteSettingsServer.Prod.into()
            },
            channel = BuildConfig.BUILD_TYPE,
            // Need to send this value separately, since `isLargeScreenSize()` is a fenix extension
            isLargeScreenSize = context.isLargeScreenSize(),
        )
    }
    val nimbus: NimbusComponents by lazy {
        NimbusComponents(
            context = context,
            engine = lazy { core.engine },
            remoteSettingsService = remoteSettingsService.value.remoteSettingsService,
        )
    }
    val publicSuffixList by lazy { PublicSuffixList(context) }
    val clipboardHandler by lazy { ClipboardHandler(context) }
    val buildMetadata by lazy { BuildMetadata.fromAssets(context.assets) }
    val performance by lazy { PerformanceComponent() }
    val push by lazy { Push(context, analytics.crashReporter) }
    val wifiConnectionMonitor by lazy { WifiConnectionMonitor(context as Application) }

    val strictMode by lazy {
        StrictModeManager(
            Config.channel.isDebug,
            this,
            BuildManufacturerChecker(),
        )
    }
    val settings by lazy { Settings(context) }
    val fenixOnboarding by lazy { FenixOnboarding(context) }

    val playStoreReviewPromptController by lazy {
        PlayStoreReviewPromptController(
            manager = ReviewManagerFactory.create(context),
            numberOfAppLaunches = { settings.numberOfAppLaunches },
        )
    }

    val autofillConfiguration by lazy {
        AutofillConfiguration(
            storage = core.passwordsStorage,
            publicSuffixList = publicSuffixList,
            unlockActivity = AutofillUnlockActivity::class.java,
            confirmActivity = AutofillConfirmActivity::class.java,
            searchActivity = AutofillSearchActivity::class.java,
            applicationName = context.getString(R.string.app_name),
            httpClient = core.client,
        )
    }

    val appStartReasonProvider by lazy {
        AppStartReasonProvider(processInfoProvider = DefaultProcessInfoProvider())
    }
    val startupActivityLog by lazy { StartupActivityLog() }
    val startupStateProvider by lazy { StartupStateProvider(startupActivityLog, appStartReasonProvider) }

    val appLinkIntentLaunchTypeProvider by lazy { AppLinkIntentLaunchTypeProvider(appStartReasonProvider) }

    val blocklistHandler by lazy { BlocklistHandler(settings) }

    val appStore by lazy {
        AppStore(
                initialState =
                    AppState(
                            collections = core.tabCollectionStorage.cachedTabCollections,
                            expandedCollections = emptySet(),
                            topSites = core.topSitesStorage.cachedTopSites.sort(),
                            bookmarks = emptyList(),
                            // Provide an initial state for recent tabs to prevent re-rendering on the home screen.
                            //  This will otherwise cause a visual jump as the section gets rendered from no state
                            //  to some state.
                            recentTabs =
                                if (settings.showRecentTabsFeature) {
                                    core.store.state.asRecentTabs()
                                } else {
                                    emptyList()
                                },
                            recentHistory = emptyList(),
                            setupChecklistState = setupChecklistState(),
                            sportsWidgetState = setupSportsWidgetState(),
                        )
                        .run { filterState(blocklistHandler) },
                middlewares =
                    listOf(
                        ProfileMarkerMiddleware(markerName = "AppStore", profiler = core.engine.profiler),
                        LogMiddleware(tag = "AppStore", shouldIncludeDetailedData = { Config.channel.isDebug }),
                        BlocklistMiddleware(blocklistHandler),
                        PocketMiddleware(
                            lazy { core.pocketStoriesService },
                            context.pocketStoriesSelectedCategoriesDataStore,
                            SettingsBackedPocketSettings(settings),
                            performance.visualCompletenessQueue,
                        ),
                        MessagingMiddleware(controller = nimbus.messaging),
                        MetricsMiddleware(
                            metrics = analytics.metrics,
                            nimbusEventStore = nimbus.events,
                        ),
                        CrashReportingAppMiddleware(
                            CrashMiddleware(
                                cache = SettingsCrashReportCache(settings),
                                crashReporter = analytics.crashReporter,
                                currentTimeInMillis = currentTimeMillis,
                            )
                        ),
                        HomeTelemetryMiddleware(),
                        SetupChecklistPreferencesMiddleware(DefaultSetupChecklistRepository(context, settings)),
                        SetupChecklistTelemetryMiddleware(),
                        ReviewPromptMiddleware(
                                continuousOnboardingInProgress = {
                                    settings.continuousOnboardingFeatureEnabled &&
                                        !settings.continuousOnboardingCompleted
                                },
                                shouldShowCustomPrompt = {
                                    settings.customReviewPromptUiEnabled && settings.isTelemetryEnabled
                                },
                                disableCustomPrompt = { settings.customReviewPromptUiEnabled = false },
                                createJexlHelper = nimbus::createJexlHelper,
                                nimbusEventStore = nimbus.events,
                            )
                            .also {
                                settings.migrateLastReviewPromptTimePrefIfNeeded(nimbus.events)
                            },
                        AppVisualCompletenessMiddleware(performance.visualCompletenessQueue),
                    ),
            )
            .also {
                it.dispatch(AppAction.SetupChecklistAction.Init)
                it.dispatch(AppAction.CrashActionWrapper(CrashAction.Initialize))
            }
    }

    val lensImageUploader by lazy {
        LensImageUploader(
            context = context,
            client = core.client,
            userAgent = core.engine.settings.userAgentString ?: "",
        )
    }

    val lensImageSearch by lazy {
        LensImageSearch(
            appStore = appStore,
            uploader = { lensImageUploader },
            browserUseCases = { useCases.fenixBrowserUseCases },
        )
    }

    private fun setupSportsWidgetState() =
        SportsWidgetState(
            isFeatureEnabled = settings.enableHomepageWrexhamWidget,
            isVisible = settings.showHomepageWrexhamWidget,
        )

    private fun setupChecklistState() =
        if (settings.showSetupChecklist) {
            val type = FxNimbus.features.setupChecklist.value().setupChecklistType
            SetupChecklistState(
                checklistItems =
                    getSetupChecklistCollection(
                        settings = settings,
                        collection = type,
                        tabStripEnabled = settings.isTabStripEnabled,
                    )
            )
        } else {
            null
        }

    val fxSuggest by lazy { FxSuggest(context, remoteSettingsService.value, analytics.crashReporter) }

    val menuCustomizationRepository by lazy {
        MenuCustomizationRepository(
            dataStore = context.customizedBrowserMenuInfoDataStore,
            defaultMenuInfo = {
                BrowserMenuConfiguration.defaultMenuInfo(
                    isToolbarAtBottom = context.isToolbarAtBottom(),
                    isExpandedToolbarEnabled = settings.shouldUseExpandedToolbar,
                )
            },
            requiredSectionIds = BrowserMenuConfiguration.REQUIRED_SECTION_IDS,
            scope = applicationScope,
        )
    }

    val distributionIdManager by lazy {
        DistributionIdManager(
            packageManager = context.packageManagerWrapper,
            browserStoreProvider = DefaultDistributionBrowserStoreProvider(core.store),
            distributionProviderChecker = DefaultDistributionProviderChecker(context),
            distributionSettings = DefaultDistributionSettings(settings),
            metricController = analytics.metrics,
        )
    }

    val integrityClient by lazy {
        GooglePlayIntegrityClient.create(
            context = context,
            projectNumberToken = BuildConfig.GPS_INTEGRITY_TOKEN,
            requestHashProvider = RequestHashProvider { clientUUID.generateHash() },
        )
    }

    val termsOfUsePromptRepository by lazy {
        DefaultTermsOfUsePromptRepository(settings)
    }

    val termsOfUseManager by lazy {
        TermsOfUseManager(termsOfUsePromptRepository)
    }

    val settingsIndexer by lazy {
        DefaultFenixSettingsIndexer(
            context = context,
            preferenceFileInformationList =
                DefaultFenixSettingsIndexer.defaultPreferenceFileInformationList(
                    includeAutofillPreferences = settings.isAutofillSupported
                ),
            additionalProviders =
                settingsSearchProviders(summarizationFeatureConfiguration = core.summarizeFeatureSettings),
            excludedPreferenceKeys = ::createSettingsIndexerExclusions,
        )
    }

    private fun createSettingsIndexerExclusions(): Set<String> = buildSet {
        if (!settings.isAutofillSupported) {
            add(context.getString(R.string.pref_key_passwords))
            add(context.getString(R.string.pref_key_credit_cards))
        }

        if (!settings.enableHomepageWeatherWidget) {
            add(context.getString(R.string.pref_key_show_homepage_weather_widget))
        }

        if (!settings.enableHomepageWrexhamWidget) {
            add(context.getString(R.string.pref_key_show_homepage_wrexham_widget))
        }

        if (!settings.enableHomepageCustomization) {
            add(context.getString(R.string.pref_key_homepage_search_location_top))
            add(context.getString(R.string.pref_key_enable_homepage_searchbar2))
        }

        if (!settings.enableOledTheme) {
            add(context.getString(R.string.pref_key_oled_theme))
        }
    }

    val ipProtectionPromptRepository by lazy {
        DefaultIPProtectionPromptRepository(settings)
    }

    val ads by lazy {
        Ads(context = context)
    }

    val sports by lazy { Sports(context = context, crashReporter = analytics.crashReporter) }

    val relayEligibilityStore by lazy {
        RelayEligibilityStore(middleware = listOf(ClearLastUsedMiddleware()))
    }

    val emailMasksRepository: EmailMasksRepository by lazy {
        DefaultEmailMasksRepository(settings)
    }

    val collectionsMigrationRepository: CollectionsMigrationRepository by lazy {
        DefaultCollectionsMigrationRepository(settings)
    }

    val relayFeatureIntegration by lazy {
        RelayFeatureIntegration(
            engine = core.engine,
            accountManager = backgroundServices.accountManager,
            store = relayEligibilityStore,
            appStore = appStore,
            errorMessages =
                ErrorMessages(
                    maxMasksReached = context.getString(R.string.email_masks_max_free_tier_reached),
                    errorRetrievingMasks = context.getString(R.string.email_masks_error_retrieving_masks),
                ),
        )
    }

    val aiFeatureBlockStorage by lazy {
        AIFeatureBlockStorage.dataStore(context)
    }

    val summarizationSettings: SummarizationSettings by lazy {
        SummarizationSettings.dataStore(context)
    }

    val listenToPage by lazy {
        ListenToPage(
            browserStore = core.store,
            context = context,
            scope = applicationScope,
            profiler = core.engine.profiler,
            isAvailable = settings.listenToPageFeatureFlagEnabled,
        )
    }

    val aiFeatureRegistry by lazy {
        AIFeatureRegistry.default(scope = applicationScope, context = context).also {
            if (settings.shakeToSummarizeFeatureFlagEnabled) {
                it.register(PageSummaryFeature(summarizationSettings))
            }
            if (listenToPage.settings.available) {
                it.register(ListenToPageAIControlFeature(listenToPage.settings))
            }
            it.register(
                VoiceSearchAIControlFeature(
                    settings = settings,
                    onUpdateWidget = { SearchWidgetProvider.updateAllWidgets(context) },
                )
            )
        }
    }

    @Suppress("unused")
    val aiControlsFeatureBlock by lazy {
        AIFeatureBlock.default(
            storage = aiFeatureBlockStorage,
            registry = aiFeatureRegistry,
        )
    }

    val llm: Llm by lazy {
        Llm(
            client = core.client,
            storage = MlpaTokenStorage.sharedPrefs(context),
            fxaTokenProvider = backgroundServices.accountManager.accessTokenProvider,
            integrityClient = integrityClient.forConsumer(IntegrityConsumer.Summarize),
            userIdProvider = clientUUID,
        )
    }

    val clientUUID by lazy { ClientUuid.build(context) }

    val ipProtection by lazy {
        IPProtection(
            engine = core.engine,
            browserStore = core.store,
            syncStore = backgroundServices.syncStore,
            authSources =
                IPProtectionAuthSources(
                    fxaAccountManager = lazy { backgroundServices.accountManager },
                    integrityClient = lazy { integrityClient },
                ),
            lazyAppStore = lazy { appStore },
            settings = settings,
            context = context,
        )
    }

    val tabDataCoordinator: TabDataCoordinator by lazy {
        DefaultTabDataCoordinator(
            tabRepository =
                DefaultTabRepository(
                    browserStore = core.store,
                    tabsUseCases = useCases.tabsUseCases,
                    fenixBrowserUseCases = useCases.fenixBrowserUseCases,
                ),
            tabGroupRepository = core.tabGroupRepository,
            isInactiveTabsEnabled = settings::inactiveTabsAreEnabled,
            isTabGroupsEnabled = settings::tabGroupsEnabled,
            scope = applicationScope,
        )
    }
}

/** Returns the [Components] object from within a [Composable]. */
val components: Components
    @Composable @ReadOnlyComposable get() = LocalContext.current.components
