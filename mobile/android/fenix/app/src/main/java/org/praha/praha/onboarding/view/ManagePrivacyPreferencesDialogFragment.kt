/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.onboarding.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.compose.content
import mozilla.components.lib.state.helpers.StoreProvider.Companion.fragmentStore
import org.praha.praha.components.metrics.installSourcePackage
import org.praha.praha.ext.application
import org.praha.praha.ext.requireComponents
import org.praha.praha.onboarding.ManagePrivacyPreferencesDialog
import org.praha.praha.onboarding.store.DefaultPrivacyPreferencesRepository
import org.praha.praha.onboarding.store.PreferenceType
import org.praha.praha.onboarding.store.PrivacyPreferencesAction
import org.praha.praha.onboarding.store.PrivacyPreferencesMiddleware
import org.praha.praha.onboarding.store.PrivacyPreferencesState
import org.praha.praha.onboarding.store.PrivacyPreferencesStore
import org.praha.praha.onboarding.store.PrivacyPreferencesTelemetryMiddleware
import org.praha.praha.settings.SupportUtils
import org.praha.praha.settings.SupportUtils.launchSandboxCustomTab
import org.praha.praha.theme.PrahaTheme

/** Dialog fragment for managing privacy preferences. */
class ManagePrivacyPreferencesDialogFragment : DialogFragment() {

    private val crashReportingUrl by lazy { sumoUrlFor(SupportUtils.SumoTopic.CRASH_REPORTS) }
    private val usageDataUrl by lazy { sumoUrlFor(SupportUtils.SumoTopic.TECHNICAL_AND_INTERACTION_DATA) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val repository =
            DefaultPrivacyPreferencesRepository(
                settings = requireComponents.settings,
                nimbusSdk = requireComponents.nimbus.sdk,
                crashReporter = requireComponents.analytics.crashReporter,
            )
        val store by
            fragmentStore(
                PrivacyPreferencesState(
                    crashReportingEnabled = repository.getPreference(PreferenceType.CrashReporting),
                    usageDataEnabled = repository.getPreference(PreferenceType.UsageData),
                )
            ) {
                PrivacyPreferencesStore(
                    initialState = it,
                    middlewares =
                        listOf(
                            PrivacyPreferencesMiddleware(repository),
                            PrivacyPreferencesTelemetryMiddleware(
                                installSource =
                                    installSourcePackage(
                                        packageManager = requireContext().application.packageManager,
                                        packageName = requireContext().application.packageName,
                                    )
                            ),
                        ),
                )
            }

        return content {
            PrahaTheme {
                ManagePrivacyPreferencesDialog(
                    store = store,
                    onDismissRequest = { dismiss() },
                    onCrashReportingLinkClick = {
                        store.dispatch(PrivacyPreferencesAction.CrashReportingLearnMore)
                        launchSandboxCustomTab(requireContext(), crashReportingUrl)
                    },
                    onUsageDataLinkClick = {
                        store.dispatch(PrivacyPreferencesAction.UsageDataUserLearnMore)
                        launchSandboxCustomTab(requireContext(), usageDataUrl)
                    },
                )
            }
        }
    }

    private fun sumoUrlFor(topic: SupportUtils.SumoTopic) = SupportUtils.getSumoURLForTopic(requireContext(), topic)

    /** Companion object for [ManagePrivacyPreferencesDialogFragment]. */
    companion object {
        /** Tag for the [ManagePrivacyPreferencesDialogFragment]. */
        const val TAG = "Privacy preferences dialog"
    }
}
