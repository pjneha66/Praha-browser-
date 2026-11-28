/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.praha.praha.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mozilla.components.ui.icons.R as iconsR

private val EmeraldDark = Color(0xFF0F2D2A)
private val EmeraldTeal = Color(0xFF1E4D47)
private val NeonTeal = Color(0xFFADF3CE)

@Composable
fun PrahaWebAppsHub(
    onOsintClick: () -> Unit,
    onBeckyFindsClick: () -> Unit,
    onFmhyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Text(
                text = "PRAHA WEB APPS & HUBS",
                color = NeonTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NeonTeal.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "3 INTEGRATED APPS",
                    color = NeonTeal,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
            }
        }

        PrahaHubItemCard(
            title = "OSINT4ALL",
            badge = "OSINT HUB",
            subtitle = "Open Source Intelligence & Investigation Tools",
            iconRes = iconsR.drawable.mozac_ic_search_24,
            onClick = onOsintClick,
        )

        PrahaHubItemCard(
            title = "BeckyFinds",
            badge = "WEB APP",
            subtitle = "Curated AI Tools & Software Directory",
            iconRes = iconsR.drawable.mozac_ic_globe_24,
            onClick = onBeckyFindsClick,
        )

        PrahaHubItemCard(
            title = "FMHY",
            badge = "KNOWLEDGE",
            subtitle = "FreeMediaHeckYeah Free Resources Vault",
            iconRes = iconsR.drawable.mozac_ic_collection_24,
            onClick = onFmhyClick,
        )
    }
}

@Composable
private fun PrahaHubItemCard(
    title: String,
    badge: String,
    subtitle: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(EmeraldDark, EmeraldTeal),
                ),
            )
            .border(
                width = 1.dp,
                color = NeonTeal.copy(alpha = 0.35f),
                shape = cardShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(NeonTeal.copy(alpha = 0.15f))
                    .border(1.dp, NeonTeal.copy(alpha = 0.4f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = title,
                    tint = NeonTeal,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(NeonTeal)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = badge,
                            color = EmeraldDark,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }

            Icon(
                painter = painterResource(iconsR.drawable.mozac_ic_forward_24),
                contentDescription = null,
                tint = NeonTeal.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Legacy compat for single OSINT4ALL card */
@Composable
fun Osint4AllHomeTabCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PrahaHubItemCard(
        title = "OSINT4ALL",
        badge = "HOME TAB",
        subtitle = "Open Source Intelligence Hub & Tools",
        iconRes = iconsR.drawable.mozac_ic_search_24,
        onClick = onClick,
        modifier = modifier,
    )
}
