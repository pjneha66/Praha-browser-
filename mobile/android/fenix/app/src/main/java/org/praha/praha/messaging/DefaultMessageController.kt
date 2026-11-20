/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.messaging

import android.content.Intent
import mozilla.components.service.nimbus.messaging.Message
import mozilla.components.service.nimbus.messaging.NimbusMessagingControllerInterface
import org.praha.praha.components.AppStore
import org.praha.praha.components.appstate.AppAction.MessagingAction.MessageClicked
import org.praha.praha.components.appstate.AppAction.MessagingAction.MessageDismissed

/** Handles default interactions with the ui of Nimbus Messaging messages. */
class DefaultMessageController(
    private val appStore: AppStore,
    private val messagingController: NimbusMessagingControllerInterface,
    private val processIntent: (Intent?) -> Unit,
) : MessageController {
    override fun onMessagePressed(message: Message) {
        val intent = messagingController.getIntentForMessage(message)
        processIntent(intent)

        appStore.dispatch(MessageClicked(message))
    }

    override fun onMessageDismissed(message: Message) {
        appStore.dispatch(MessageDismissed(message))
    }
}
