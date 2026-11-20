/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.tabgroups

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.praha.praha.tabstray.TabsTrayTestTag
import org.praha.praha.tabstray.data.createTab
import org.praha.praha.tabstray.data.createTabGroup
import org.praha.praha.theme.PrahaTheme

@RunWith(AndroidJUnit4::class)
class TabGroupRowTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun verifyMediaIndicatorVisible() {
        composeTestRule.setContent {
            PrahaTheme {
                TabGroupRow(
                    tabGroup =
                        createTabGroup(
                            title = "Group 1",
                            tabs = listOf(createTab(url = "mozilla.org", isMediaActive = true)),
                        ),
                    onClick = {},
                )
            }
        }
        composeTestRule
            .onNodeWithTag(
                TabsTrayTestTag.TAB_ITEM_MEDIA_INDICATOR,
                useUnmergedTree = true,
            )
            .assertIsDisplayed()
    }

    @Test
    fun verifyMediaIndicatorNotVisible() {
        composeTestRule.setContent {
            PrahaTheme {
                TabGroupRow(
                    tabGroup =
                        createTabGroup(
                            title = "Group 1",
                            tabs = listOf(createTab(url = "mozilla.org", isMediaActive = false)),
                        ),
                    onClick = {},
                )
            }
        }
        composeTestRule.onNodeWithTag(TabsTrayTestTag.TAB_ITEM_MEDIA_INDICATOR).assertDoesNotExist()
    }
}
