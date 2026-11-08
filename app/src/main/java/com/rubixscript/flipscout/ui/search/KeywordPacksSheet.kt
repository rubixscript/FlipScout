package com.rubixscript.flipscout.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.domain.KEYWORD_PACKS
import com.rubixscript.flipscout.domain.KeywordPack
import com.rubixscript.flipscout.domain.ParsedKeyword
import com.rubixscript.flipscout.domain.isForUser
import com.rubixscript.flipscout.domain.rankPacks
import com.rubixscript.flipscout.ui.components.GradientButton
import com.rubixscript.flipscout.ui.components.Pill
import com.rubixscript.flipscout.ui.theme.Palette

/**
 * Pick the areas you flip and get ready-made keywords filed under matching categories.
 * Interests drawn from the user's own items come first and are marked with a star. Every keyword of a chosen
 * interest is pre-selected, so the fastest path is: tap an interest, tap Add.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KeywordPacksSheet(
    interests: List<String>,
    existingQueries: Set<String>,
    onAdd: (List<ParsedKeyword>) -> Unit,
    onAskAi: () -> Unit,
    onImportClipboard: () -> Unit,
    onDismiss: () -> Unit,
) {
    val packs = remember(interests) { rankPacks(KEYWORD_PACKS, interests) }
    var chosen by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var skipped by rememberSaveable { mutableStateOf(emptySet<String>()) } // keywords the user un-ticked
    val existing = remember(existingQueries) { existingQueries.map { it.lowercase() }.toSet() }

    val selectedPacks = packs.filter { it.name in chosen }
    val toAdd = selectedPacks.flatMap { pack ->
        pack.keywords.filter { it.lowercase() !in existing && it !in skipped }.map { ParsedKeyword(it, pack.name) }
    }.distinctBy { it.query.lowercase() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.packs_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.packs_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                packs.forEach { pack ->
                    val label = (if (isForUser(pack, interests)) "★ " else "") + pack.name
                    Pill(label, pack.name in chosen, Palette.Emerald,
                        onClick = { chosen = if (pack.name in chosen) chosen - pack.name else chosen + pack.name })
                }
            }

            selectedPacks.forEach { pack -> PackKeywords(pack, existing, skipped) { kw ->
                skipped = if (kw in skipped) skipped - kw else skipped + kw
            } }

            if (toAdd.isNotEmpty()) {
                GradientButton(
                    pluralStringResource(R.plurals.packs_add, toAdd.size, toAdd.size),
                    onClick = { onAdd(toAdd) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            } else {
                Text(
                    stringResource(if (chosen.isEmpty()) R.string.packs_hint_pick else R.string.packs_hint_all_added),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                )
            }

            // Other ways to fill the library live here, out of the way of the main screen.
            Text(stringResource(R.string.packs_more_ways), color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            SoftAction(stringResource(R.string.keywords_open), onAskAi)
            SoftAction(stringResource(R.string.keywords_import_clipboard), onImportClipboard)
        }
    }
}

@Composable
private fun SoftAction(text: String, onClick: () -> Unit) = GradientButton(
    text, onClick = onClick, modifier = Modifier.fillMaxWidth(),
    container = MaterialTheme.colorScheme.surface, content = MaterialTheme.colorScheme.onSurface,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PackKeywords(pack: KeywordPack, existing: Set<String>, skipped: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(pack.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pack.keywords.forEach { kw ->
                if (kw.lowercase() in existing) {
                    Pill("✓ $kw", false, Palette.Emerald, onClick = {})
                } else {
                    Pill(kw, kw !in skipped, Palette.Emerald, onClick = { onToggle(kw) })
                }
            }
        }
    }
}
