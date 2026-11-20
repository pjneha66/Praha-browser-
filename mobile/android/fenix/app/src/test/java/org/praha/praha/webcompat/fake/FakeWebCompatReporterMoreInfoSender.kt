/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.webcompat.fake

import mozilla.components.concept.engine.EngineSession
import org.praha.praha.webcompat.WebCompatReporterMoreInfoSender
import org.praha.praha.webcompat.store.WebCompatReporterState

class FakeWebCompatReporterMoreInfoSender : WebCompatReporterMoreInfoSender {
    override suspend fun sendMoreWebCompatInfo(
        reason: WebCompatReporterState.BrokenSiteReason?,
        problemDescription: String?,
        enteredUrl: String?,
        tabUrl: String?,
        engineSession: EngineSession?,
    ) {}
}
