/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.settings.account

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.PromoCard
import org.praha.praha.R
import org.praha.praha.settings.ComposePreference
import org.praha.praha.theme.PrahaTheme

private val PROMO_ILLUSTRATION_SIZE = 64.dp

/** A [ComposePreference] showing the sign-in promo card in Settings. */
class SignInPromoPreference
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ComposePreference(context, attrs) {

    var onClick: (() -> Unit)? = null

    @Composable
    override fun Content() {
        SignInPromoCard(onClick = { onClick?.invoke() })
    }
}

@Composable
private fun SignInPromoCard(onClick: () -> Unit) {
    PromoCard(
        modifier =
            Modifier.padding(
                    start = PrahaTheme.layout.space.static200,
                    end = PrahaTheme.layout.space.static200,
                    bottom = PrahaTheme.layout.space.static100,
                )
                .fillMaxWidth()
                .wrapContentWidth()
                .widthIn(max = PrahaTheme.layout.size.containerMaxWidth)
                .clip(MaterialTheme.shapes.large)
                .clickable(role = Role.Button, onClick = onClick)
                .testTag(AccountSettingsTestTag.SIGN_IN_PROMO_CARD),
        title = stringResource(R.string.sign_in_promo_title),
        description = stringResource(R.string.sign_in_promo_description),
        illustration = {
            Image(
                painter = painterResource(R.drawable.kit_sign_in_promo),
                contentDescription = null,
                modifier = Modifier.size(PROMO_ILLUSTRATION_SIZE),
            )
        },
    )
}

@PreviewLightDark
@Composable
private fun SignInPromoCardPreview() {
    PrahaTheme {
        Surface {
            SignInPromoCard(onClick = {})
        }
    }
}
