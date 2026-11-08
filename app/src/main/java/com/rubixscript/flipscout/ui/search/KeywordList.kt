package com.rubixscript.flipscout.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.data.SavedSearch
import com.rubixscript.flipscout.domain.SearchTarget
import com.rubixscript.flipscout.ui.theme.Palette

internal val SearchTarget.color: Color
    get() = when (this) {
        SearchTarget.EBAY -> Palette.Blue
        SearchTarget.EBAY_SOLD -> Palette.Emerald
        SearchTarget.MARKETPLACE -> Palette.Gemini
    }

class MenuItem(val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

/** What a keyword can do. Tapping the row itself always searches; the rest lives in its "more" menu. */
class KeywordActions(
    val onSearch: () -> Unit,
    val onPick: () -> Unit,
    val onTogglePin: () -> Unit,
    val onChangeCategory: (() -> Unit)?,
    val onDelete: () -> Unit,
)

/**
 * One saved keyword as a compact rounded row: tap anywhere to search on the marketplace chosen above,
 * with a coloured arrow showing where it will go. [subtitle] (the category) is shown when browsing "All".
 */
@Composable
fun KeywordRow(
    saved: SavedSearch,
    actions: KeywordActions,
    target: SearchTarget,
    modifier: Modifier = Modifier,
    subtitle: String = "",
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = actions.onSearch).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
            Text(saved.query, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        Text("↗", color = target.color, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
        Box {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { menuOpen = true }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(menuOpen, { menuOpen = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.menu_use_in_search)) },
                    onClick = { menuOpen = false; actions.onPick() })
                actions.onChangeCategory?.let { change ->
                    DropdownMenuItem(text = { Text(stringResource(R.string.menu_change_category)) },
                        onClick = { menuOpen = false; change() })
                }
                DropdownMenuItem(
                    text = { Text(stringResource(if (saved.pinned) R.string.menu_unpin else R.string.menu_pin)) },
                    onClick = { menuOpen = false; actions.onTogglePin() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete), color = Palette.Loss) },
                    onClick = { menuOpen = false; actions.onDelete() },
                )
            }
        }
    }
}
