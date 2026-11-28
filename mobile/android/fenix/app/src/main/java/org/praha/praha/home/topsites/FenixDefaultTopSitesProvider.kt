/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.home.topsites

import android.content.res.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mozilla.components.browser.state.search.RegionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.top.sites.DefaultTopSitesProvider
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.lib.crash.CrashReporter
import org.praha.praha.R
import org.praha.praha.home.blocklist.BlocklistHandler
import org.praha.praha.home.topsites.utils.fetchDefaultTopSites

/**
 * A [DefaultTopSitesProvider] that retrieves the default shortcuts from `default_shortcuts.json`. The default shortcuts
 * are used to populate the top sites screen along with the frecent top sites.
 *
 * @param browserStore The [BrowserStore] used to read the user's region.
 * @param resources [Resources] used for accessing the raw resource.
 * @param crashReporter [CrashReporter] used for recording caught exceptions.
 * @param blocklistHandler [BlocklistHandler] holding the shortcuts the user has dismissed.
 * @param experiments Lambda that returns the experiments that the user is enrolled in.
 * @param ioDispatcher The dispatcher used for reading and parsing the raw resource.
 */
class FenixDefaultTopSitesProvider(
    private val browserStore: BrowserStore,
    private val resources: Resources,
    private val crashReporter: CrashReporter,
    private val blocklistHandler: BlocklistHandler,
    private val experiments: () -> Set<String>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DefaultTopSitesProvider {

    private val mutex = Mutex()

    @Volatile private var cachedTopSites: CachedTopSites? = null

    override suspend fun getDefaultTopSites(): List<TopSite.Default> {
        val region = browserStore.state.search.region?.current ?: RegionState.Default.current
        val experiments = experiments()

        val osint4all = TopSite.Default(
            id = 1001L,
            title = "OSINT4ALL",
            url = "https://start.me/p/L1rEYQ/osint4all",
            createdAt = null,
        )

        val beckyfinds = TopSite.Default(
            id = 1002L,
            title = "BeckyFinds",
            url = "https://pjneha66.github.io/beckyfinds/",
            createdAt = null,
        )

        val fmhy = TopSite.Default(
            id = 1003L,
            title = "FMHY",
            url = "https://fmhy.net/",
            createdAt = null,
        )

        val customSites = listOf(osint4all, beckyfinds, fmhy)
        val customUrls = customSites.map { it.url }.toSet()

        val defaultSites =
            topSitesList(region = region, experiments = experiments).map {
                TopSite.Default(
                    id = it.id.toLongOrNull(),
                    title = it.displayTitle(),
                    url = it.url,
                    createdAt = null,
                )
            }

        val combined = customSites + defaultSites.filter { it.url !in customUrls }
        return with(blocklistHandler) { combined.filteredByBlocklist() }
    }

    private suspend fun topSitesList(region: String, experiments: Set<String>): List<DefaultTopSiteItem> {
        val cached = cachedTopSitesFor(region, experiments)
        if (cached != null) return cached

        return mutex.withLock {
            // Another caller may have populated the cache for this region while we waited for the lock.
            cachedTopSitesFor(region, experiments)
                ?: readTopSites(region, experiments).also { topSites ->
                    cachedTopSites = CachedTopSites(region = region, experiments = experiments, topSites = topSites)
                }
        }
    }

    private fun cachedTopSitesFor(region: String, experiments: Set<String>): List<DefaultTopSiteItem>? =
        cachedTopSites?.takeIf { it.region == region && it.experiments == experiments }?.topSites

    private suspend fun readTopSites(region: String, experiments: Set<String>): List<DefaultTopSiteItem> =
        withContext(ioDispatcher) {
            fetchDefaultTopSites(
                resources = resources,
                rawResId = R.raw.default_shortcuts,
                crashReporter = crashReporter,
                region = region,
                experiments = experiments,
            )
        }

    /**
     * The top sites that were read for a region.
     *
     * @property region The region the top sites were read for.
     * @property experiments The experiments the top sites were read for.
     * @property topSites The top sites available in [region] for [experiments].
     */
    private data class CachedTopSites(
        val region: String,
        val experiments: Set<String>,
        val topSites: List<DefaultTopSiteItem>,
    )

    companion object {
        /** The experiment that the regional default shortcuts in `default_shortcuts.json` are included in. */
        const val REGIONAL_DEFAULT_SHORTCUTS_EXPERIMENT = "regional-default-shortcuts"
    }
}
