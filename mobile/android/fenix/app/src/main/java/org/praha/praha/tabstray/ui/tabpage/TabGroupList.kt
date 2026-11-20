/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.tabstray.ui.tabpage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import mozilla.components.browser.state.state.createTab
import org.praha.praha.tabgroups.TabGroupRow
import org.praha.praha.tabstray.data.TabGroupTheme
import org.praha.praha.tabstray.data.TabsTrayItem
import org.praha.praha.tabstray.ui.tabitems.TabGroupMenuButton
import org.praha.praha.tabstray.ui.tabitems.TabListBorderMiddleItemShape
import org.praha.praha.tabstray.ui.tabitems.TabListFirstItemShape
import org.praha.praha.tabstray.ui.tabitems.TabListLastItemShape
import org.praha.praha.tabstray.ui.tabitems.TabListShapeInfo
import org.praha.praha.tabstray.ui.tabitems.TabListSingleItemShape
import org.praha.praha.tabstray.ui.tabitems.TabsTrayItemSelectionState
import org.praha.praha.tabstray.ui.tabitems.tabListItemShapeStyling
import org.praha.praha.theme.PrahaTheme

/**
 * Top-level UI for displaying a list of tab groups.
 *
 * @param groups The list of tab groups to display.
 * @param modifier: The Modifier applied to the tab group list.
 * @param topPadding The padding applied above the first tab group.
 * @param onTabGroupClick Invoked when the user clicks on a tab group.
 * @param onEditTabGroupClick Invoked when the user clicks to edit the tab group.
 * @param onShareTabGroupClick Invoked when the user clicks to share the tab group.
 * @param onDeleteTabGroupClick Invoked when the user clicks on delete tab group.
 */
@Composable
fun TabGroupList(
    groups: List<TabsTrayItem.TabGroup>,
    modifier: Modifier = Modifier,
    topPadding: Dp = PrahaTheme.layout.space.dynamic200,
    onTabGroupClick: (TabsTrayItem.TabGroup) -> Unit,
    onEditTabGroupClick: (TabsTrayItem.TabGroup) -> Unit,
    onShareTabGroupClick: (TabsTrayItem.TabGroup) -> Unit,
    onDeleteTabGroupClick: (TabsTrayItem.TabGroup) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier =
                modifier
                    .width(PrahaTheme.layout.size.containerMaxWidth)
                    .padding(
                        start = PrahaTheme.layout.space.dynamic200,
                        top = topPadding,
                        end = PrahaTheme.layout.space.dynamic200,
                    ),
            verticalArrangement = Arrangement.spacedBy(PrahaTheme.layout.space.static25),
        ) {
            itemsIndexed(
                items = groups,
                key = { _, group -> group.id },
            ) { index, group ->
                val selectionState = TabsTrayItemSelectionState(isFocused = group.isFocused)
                val tabShapeInfo =
                    when {
                        groups.size == 1 -> TabListShapeInfo(TabListSingleItemShape, true)
                        index == 0 -> TabListShapeInfo(TabListFirstItemShape, true)
                        index == groups.lastIndex -> TabListShapeInfo(TabListLastItemShape, true)
                        else -> TabListShapeInfo(TabListBorderMiddleItemShape, false)
                    }

                TabGroupRow(
                    tabGroup = group,
                    onClick = { onTabGroupClick(group) },
                    modifier =
                        Modifier.tabListItemShapeStyling(
                                tabShapeInfo = tabShapeInfo,
                                selectionState =
                                    TabsTrayItemSelectionState(
                                        isFocused = group.isFocused,
                                        multiSelectEnabled = false,
                                        focusEnabled = true,
                                    ),
                            )
                            .background(MaterialTheme.colorScheme.surfaceBright),
                    selectionState = selectionState,
                    trailingContent = {
                        TabGroupMenuButton(
                            includeCloseOption = false,
                            onEditTabGroupClick = { onEditTabGroupClick(group) },
                            onCloseTabGroupClick = {},
                            onShareTabGroupClick = { onShareTabGroupClick(group) },
                            onUngroupTabGroupClick = {},
                            onDeleteTabGroupClick = { onDeleteTabGroupClick(group) },
                        )
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TabGroupListPreview() {
    val firstGroupTabs =
        mutableListOf(
            TabsTrayItem.Tab(createTab(url = "https://www.mozilla.org")),
            TabsTrayItem.Tab(createTab(url = "https://www.mozilla.org/en-US")),
            TabsTrayItem.Tab(createTab(url = "https://www.firefox.com")),
        )
    val secondGroupTabs = mutableListOf(TabsTrayItem.Tab(createTab(url = "https://www.google.com")))

    PrahaTheme {
        TabGroupList(
            groups =
                listOf(
                    TabsTrayItem.TabGroup(
                        title = "Work",
                        theme = TabGroupTheme.Blue,
                        tabs = firstGroupTabs,
                    ),
                    TabsTrayItem.TabGroup(
                        title = "Search",
                        theme = TabGroupTheme.Purple,
                        tabs = secondGroupTabs,
                    ),
                ),
            onTabGroupClick = {},
            onEditTabGroupClick = {},
            onShareTabGroupClick = {},
            onDeleteTabGroupClick = {},
        )
    }
}
