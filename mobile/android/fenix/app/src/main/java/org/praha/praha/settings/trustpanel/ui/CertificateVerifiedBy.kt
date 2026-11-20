/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.settings.trustpanel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import mozilla.components.compose.base.theme.PreviewThemeProvider
import mozilla.components.compose.base.theme.Theme
import mozilla.components.support.utils.CertificateDetails
import mozilla.components.support.utils.issuerDisplayName
import org.praha.praha.R
import org.praha.praha.theme.PrahaTheme

/**
 * Shows which certificate authority verified the website's certificate. Nothing is shown when the issuer name is
 * absent.
 *
 * @param certificateDetails The certificate details of the current website, or null if there is no certificate.
 * @param modifier [Modifier] to be applied to the layout.
 */
@Composable
internal fun CertificateVerifiedBy(
    certificateDetails: CertificateDetails?,
    modifier: Modifier = Modifier,
) {
    val issuerName = certificateDetails.issuerDisplayName() ?: return

    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = stringResource(R.string.certificate_details_panel_verified_by_label),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = PrahaTheme.typography.body2,
        )

        Spacer(modifier = Modifier.height(PrahaTheme.layout.space.static50))

        Text(
            text = issuerName,
            color = MaterialTheme.colorScheme.onSurface,
            style = PrahaTheme.typography.body2,
        )
    }
}

@Composable
private fun CertificateVerifiedByPreviewContent(certificateDetails: CertificateDetails) {
    CertificateVerifiedBy(
        certificateDetails = certificateDetails,
        modifier =
            Modifier.background(color = MaterialTheme.colorScheme.surfaceBright)
                .padding(all = PrahaTheme.layout.space.static200),
    )
}

@Preview
@Composable
private fun CertificateVerifiedByPreview(@PreviewParameter(PreviewThemeProvider::class) theme: Theme) {
    PrahaTheme(theme) {
        CertificateVerifiedByPreviewContent(
            CertificateDetails(
                issuerOrganization = "GlobalSign nv-sa",
                issuerCommonName = "GlobalSign GCC R3 OV TLS CA",
            )
        )
    }
}

@Preview
@Composable
private fun CertificateVerifiedByCommonNamePreview() {
    PrahaTheme {
        CertificateVerifiedByPreviewContent(CertificateDetails(issuerCommonName = "Example Enterprise Root CA"))
    }
}

@Preview(fontScale = 2f)
@Composable
private fun CertificateVerifiedByLongNamePreview() {
    PrahaTheme {
        CertificateVerifiedByPreviewContent(
            CertificateDetails(
                issuerOrganization = "A Very Long Certificate Authority Organization Name That Does Not Fit On One Line"
            )
        )
    }
}

@Preview(locale = "ar")
@Composable
private fun CertificateVerifiedByRtlPreview() {
    PrahaTheme { CertificateVerifiedByPreviewContent(CertificateDetails(issuerOrganization = "GlobalSign nv-sa")) }
}
