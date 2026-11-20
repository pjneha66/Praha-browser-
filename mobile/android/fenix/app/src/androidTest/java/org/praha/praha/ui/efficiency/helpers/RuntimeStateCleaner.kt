/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.helpers

import org.praha.praha.components.appstate.AppAction.SearchAction.SearchEnded
import org.praha.praha.components.appstate.VoiceSearchAction.VoiceInputRequestCleared
import org.praha.praha.ext.components
import org.praha.praha.helpers.TestHelper.appContext
import org.praha.praha.ui.efficiency.logging.TestLogging

object RuntimeStateCleaner {
    fun restore(phase: String, testId: String) {
        val appStore = appContext.components.appStore
        val before = appStore.state

        appStore.dispatch(SearchEnded)
        appStore.dispatch(VoiceInputRequestCleared)

        val after = appStore.state
        try {
            TestLogging.installed()
                .record(
                    "runtimeCleanup",
                    mapOf(
                        "phase" to phase,
                        "testId" to testId,
                        "searchActiveBefore" to before.searchState.isSearchActive,
                        "searchActiveAfter" to after.searchState.isSearchActive,
                        "voiceInputRequestedBefore" to before.voiceSearchState.isRequestingVoiceInput,
                        "voiceInputRequestedAfter" to after.voiceSearchState.isRequestingVoiceInput,
                        "voiceInputResultBefore" to (before.voiceSearchState.voiceInputResult != null),
                        "voiceInputResultAfter" to (after.voiceSearchState.voiceInputResult != null),
                    ),
                )
        } catch (_: Throwable) {}
        check(!after.searchState.isSearchActive) {
            "Search remained active after $phase runtime cleanup for $testId"
        }
        check(!after.voiceSearchState.isRequestingVoiceInput && after.voiceSearchState.voiceInputResult == null) {
            "Voice-input state remained active after $phase runtime cleanup for $testId"
        }
    }
}
