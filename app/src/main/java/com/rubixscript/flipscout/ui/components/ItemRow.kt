package com.rubixscript.flipscout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.domain.ageDays
import com.rubixscript.flipscout.domain.isStale
import com.rubixscript.flipscout.ui.theme.Palette
import com.rubixscript.flipscout.ui.theme.Dimens
import com.rubixscript.flipscout.ui.theme.color
import com.rubixscript.flipscout.ui.theme.labelRes
import com.rubixscript.flipscout.util.money
import com.rubixscript.flipscout.util.openUrl

/** Swipe right to advance the status (Watching → Bought → Listed → Sold); swipe left to delete (with undo). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableItemRow(
    item: Item,
    onAdvance: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val next = item.status.next
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        when (value) {
            SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
            SwipeToDismissBoxValue.StartToEnd -> { if (next != null) onAdvance(); false } // snap back
            else -> false
        }
    })
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = next != null,
        backgroundContent = {
            val (bg, label, align) = when (state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Triple(
                    (next ?: item.status).color(),
                    next?.let { stringResource(R.string.swipe_mark, stringResource(it.labelRes)) }.orEmpty(),
                    Alignment.CenterStart,
                )
                SwipeToDismissBoxValue.EndToStart -> Triple(Palette.Loss, stringResource(R.string.swipe_delete), Alignment.CenterEnd)
                else -> Triple(Color.Transparent, "", Alignment.Center)
            }
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(Dimens.cardRadius)).background(bg.copy(alpha = 0.9f)).padding(horizontal = 22.dp),
                contentAlignment = align,
            ) { Text(label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) }
        },
    ) { ItemRow(item, onClick) }
}

@Composable
fun ItemRow(item: Item, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = item.status.color()
    val dark = MaterialTheme.colorScheme.onBackground
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Dimens.rowPadding), verticalAlignment = Alignment.CenterVertically) {
            // Solid ink disc with the initial; a small status-coloured badge sits on its corner.
            Box(Modifier.size(52.dp)) {
                Box(Modifier.matchParentSize().clip(CircleShape).background(dark), contentAlignment = Alignment.Center) {
                    Text(item.title.trim().firstOrNull()?.uppercase() ?: "?", color = MaterialTheme.colorScheme.background,
                        fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    if (item.photo.isNotBlank()) PhotoImage(item.photo, Modifier.matchParentSize())
                }
                Box(Modifier.align(Alignment.TopStart).size(16.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface).padding(2.dp).clip(CircleShape).background(c))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title.ifBlank { stringResource(R.string.item_untitled) }, fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = (-0.3).sp)
                Spacer(Modifier.height(2.dp))
                // Status is spelled out in its own colour; stale stock gets an amber age so it can't hide.
                val now = remember { System.currentTimeMillis() }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(item.status.labelRes), color = c, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        "  ·  ${money(item.buyPrice)}" + if (item.sellPrice > 0) " → ${money(item.sellPrice)}" else "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.isStale(now)) {
                        Text("  ·  " + stringResource(R.string.item_age_days, item.ageDays(now)), color = Palette.Amber,
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
            if (item.sellPrice > 0) ProfitText(item.profit)
            if (item.link.isNotBlank()) {
                val context = LocalContext.current
                Text(
                    "↗", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 2.dp).clip(CircleShape)
                        .clickable { context.openUrl(item.link) }.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfitText(profit: Double) {
    val c = if (profit >= 0) Palette.Emerald else Palette.Loss
    Text((if (profit >= 0) "+" else "") + money(profit), color = c, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, letterSpacing = (-0.3).sp, modifier = Modifier.padding(start = 8.dp))
}
