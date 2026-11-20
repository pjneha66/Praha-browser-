/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.reviewprompt

import org.praha.praha.components.AppStore
import org.praha.praha.components.appstate.AppAction.ReviewPromptAction
import org.praha.praha.components.appstate.AppAction.ReviewPromptAction.CheckIfEligibleForReviewPrompt
import org.praha.praha.components.appstate.AppAction.ReviewPromptAction.DoNotShowReviewPrompt
import org.praha.praha.components.appstate.AppAction.ReviewPromptAction.ReviewPromptShown
import org.praha.praha.components.appstate.AppAction.ReviewPromptAction.ShowCustomReviewPrompt
import org.praha.praha.components.appstate.AppAction.ReviewPromptAction.ShowPlayStorePrompt
import org.praha.praha.components.appstate.AppState
import org.praha.praha.reviewprompt.ReviewPromptState.Eligible
import org.praha.praha.reviewprompt.ReviewPromptState.Eligible.Type
import org.praha.praha.reviewprompt.ReviewPromptState.NotEligible

/** [AppStore] reducer of [ReviewPromptAction]s. */
internal object ReviewPromptReducer {
    fun reduce(state: AppState, action: ReviewPromptAction): AppState {
        return when (action) {
            ShowPlayStorePrompt -> state.copy(reviewPrompt = Eligible(Type.PlayStore))
            ShowCustomReviewPrompt -> state.copy(reviewPrompt = Eligible(Type.Custom))
            DoNotShowReviewPrompt,
            ReviewPromptShown -> state.copy(reviewPrompt = NotEligible)
            CheckIfEligibleForReviewPrompt -> state
        }
    }
}
