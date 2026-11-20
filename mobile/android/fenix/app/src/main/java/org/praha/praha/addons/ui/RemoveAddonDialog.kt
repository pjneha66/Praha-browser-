/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.addons.ui

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
import org.praha.praha.addons.AddonsTestTag
import org.praha.praha.theme.PrahaTheme

/**
 * This dialog is used to prompt the user to confirm if they want to remove an add-on. It provides options to confirm or
 * cancel the removal.
 *
 * @param addonName The name of the add-on to remove.
 * @param onConfirm Callback invoked when the user confirms the removal.
 * @param onDismiss Callback invoked when the user cancels the removal.
 */
@Composable
internal fun RemoveAddonDialog(
    addonName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.addon_remove_dialog_title, addonName),
                style = PrahaTheme.typography.headline5,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.addon_remove_dialog_body),
                style = PrahaTheme.typography.body2,
            )
        },
        confirmButton = {
            TextButton(
                text = stringResource(R.string.addon_remove_dialog_confirm),
                onClick = onConfirm,
                modifier = Modifier.testTag(tag = AddonsTestTag.REMOVE_ADDON_DIALOG_CONFIRM_BUTTON),
            )
        },
        dismissButton = {
            TextButton(
                text = stringResource(R.string.addon_remove_dialog_cancel),
                onClick = onDismiss,
                modifier = Modifier.testTag(tag = AddonsTestTag.REMOVE_ADDON_DIALOG_CANCEL_BUTTON),
            )
        },
    )
}

@FlexibleWindowPreview
@Composable
private fun RemoveAddonDialogPreview(@PreviewParameter(PreviewThemeProvider::class) theme: Theme) {
    PrahaTheme(theme) {
        Surface {
            RemoveAddonDialog(
                addonName = "Example Add-on",
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}
