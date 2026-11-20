/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.ui.efficiency.selectors

import org.praha.praha.ui.efficiency.helpers.PageReadinessProfiles
import org.praha.praha.ui.efficiency.helpers.Selector
import org.praha.praha.ui.efficiency.helpers.SelectorGroup
import org.praha.praha.ui.efficiency.helpers.SelectorStrategy

internal fun navigationToolbarTitle(
    title: String,
    description: String,
    groups: Set<SelectorGroup> = emptySet(),
) =
    Selector(
        strategy = SelectorStrategy.ESPRESSO_BY_ID_WITH_DESCENDANT_TEXT,
        value = "navigationToolbar",
        secondaryValue = title,
        description = description,
        groups = groups,
        readiness = PageReadinessProfiles.IDENTITY_ANCHOR,
    )
