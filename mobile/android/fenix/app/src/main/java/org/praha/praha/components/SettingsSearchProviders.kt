/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.components

import org.praha.praha.settings.ToolbarShortcutSettingsSearchProvider
import org.praha.praha.settings.ai.AIControlsSearchProvider
import org.praha.praha.settings.datachoices.DataChoicesSearchProvider
import org.praha.praha.settings.labs.PrahaLabsSettingsSearchProvider
import org.praha.praha.settings.pagesummaries.PageSummariesSettingsSearchProvider
import org.praha.praha.settings.search.GoogleLensSettingsSearchProvider
import org.praha.praha.settings.settingssearch.SettingsSearchProvider
import org.praha.praha.summarization.onboarding.SummarizationFeatureDiscoveryConfiguration

/**
 * Builds the list of [SettingsSearchProvider]s covering settings the XML-parsing path in
 * [org.praha.praha.settings.settingssearch.DefaultFenixSettingsIndexer] cannot reach: Compose screens with no backing
 * preference file, and entries inside an XML screen that the parser does not emit (an empty `PreferenceCategory` whose
 * contents are built in code, for instance).
 *
 * This is the source of truth for which providers exist. It is wired into the settings indexer from [Components] and is
 * exercised by `SettingsSearchProviderRegistrationTest` so that a screen losing its provider fails a test rather than
 * silently disappearing from settings search.
 *
 * @param summarizationFeatureConfiguration Gates whether the Page Summaries screen is indexed.
 */
internal fun settingsSearchProviders(
    summarizationFeatureConfiguration: SummarizationFeatureDiscoveryConfiguration
): List<SettingsSearchProvider> =
    listOf(
        DataChoicesSearchProvider,
        AIControlsSearchProvider,
        PageSummariesSettingsSearchProvider(summarizationFeatureConfiguration),
        PrahaLabsSettingsSearchProvider(),
        ToolbarShortcutSettingsSearchProvider,
        GoogleLensSettingsSearchProvider,
    )
