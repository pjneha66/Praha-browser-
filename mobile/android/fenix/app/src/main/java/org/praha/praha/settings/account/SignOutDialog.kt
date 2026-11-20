/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.settings.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import mozilla.components.compose.base.annotation.FlexibleWindowPreview
import mozilla.components.compose.base.button.TextButton
import mozilla.components.compose.base.theme.PreviewThemeProvider
import mozilla.components.compose.base.theme.Theme
import org.praha.praha.R
import org.praha.praha.theme.PrahaTheme

/**
 * This dialog is used to prompt the user to confirm if they want to sign out of its account. It provides options to
 * confirm or cancel the logout.
 *
 * @param onConfirmSignOut Callback invoked when the user confirms the logout.
 * @param onCancel Callback invoked when the user cancels the deletion.
 */
@Composable
internal fun SignOutDialog(
    onConfirmSignOut: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.sign_out_dialog_title),
                style = PrahaTheme.typography.headline5,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.sign_out_dialog_message),
                style = PrahaTheme.typography.body2,
            )
        },
        confirmButton = {
            TextButton(
                text = stringResource(R.string.sign_out_dialog_confirm_button),
                onClick = onConfirmSignOut,
                modifier = Modifier.testTag(tag = AccountSettingsTestTag.SIGN_OUT_DIALOG_CONFIRM_BUTTON),
            )
        },
        dismissButton = {
            TextButton(
                text = stringResource(R.string.sign_out_cancel),
                onClick = onCancel,
                modifier = Modifier.testTag(tag = AccountSettingsTestTag.SIGN_OUT_DIALOG_CANCEL_BUTTON),
            )
        },
    )
}

@FlexibleWindowPreview
@Composable
private fun SignOutDialogPreview(@PreviewParameter(PreviewThemeProvider::class) theme: Theme) {
    PrahaTheme(theme) {
        Surface {
            SignOutDialog(
                onConfirmSignOut = {},
                onCancel = {},
            )
        }
    }
}
