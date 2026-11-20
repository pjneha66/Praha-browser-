/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.messaging.state

import mozilla.components.service.nimbus.messaging.Message
import org.praha.praha.components.appstate.AppAction
import org.praha.praha.components.appstate.AppAction.MessagingAction.ConsumeMessageToShow
import org.praha.praha.components.appstate.AppAction.MessagingAction.UpdateMessageToShow
import org.praha.praha.components.appstate.AppAction.MessagingAction.UpdateMessages
import org.praha.praha.components.appstate.AppState
import org.praha.praha.messaging.FenixMessageSurfaceId
import org.praha.praha.messaging.MessagingState
import org.praha.praha.microsurvey.MicrosurveyState
import org.praha.praha.microsurvey.ui.ext.toMicrosurveyUIData

/** Reducer for [MessagingState]. */
internal object MessagingReducer {
    fun reduce(state: AppState, action: AppAction): AppState =
        when (action) {
            is UpdateMessageToShow -> {
                val messageToShow = state.messaging.messageToShow.toMutableMap()
                messageToShow[action.message.surface] = action.message
                val newState = state.copy(messaging = state.messaging.copy(messageToShow = messageToShow))
                if (action.message.surface == FenixMessageSurfaceId.MICROSURVEY) {
                    reduceMicrosurvey(newState, action.message)
                } else {
                    newState
                }
            }
            is UpdateMessages -> {
                state.copy(messaging = state.messaging.copy(messages = action.messages))
            }
            is ConsumeMessageToShow -> {
                val messageToShow = state.messaging.messageToShow.toMutableMap()
                messageToShow.remove(action.surface)
                val newState = state.copy(messaging = state.messaging.copy(messageToShow = messageToShow))
                if (action.surface == FenixMessageSurfaceId.MICROSURVEY) {
                    newState.copy(microsurvey = MicrosurveyState())
                } else {
                    newState
                }
            }
            else -> state
        }

    /**
     * Derives [AppState.microsurvey] from [message], skipping the update if it is already the current microsurvey so
     * its identity stays stable across repeated evaluations of the same survey (e.g. on every fragment resume).
     */
    private fun reduceMicrosurvey(state: AppState, message: Message): AppState {
        if (message.id == state.microsurvey.current?.id) {
            return state
        }
        val microsurvey = message.toMicrosurveyUIData() ?: return state
        return state.copy(microsurvey = MicrosurveyState(current = microsurvey))
    }
}
