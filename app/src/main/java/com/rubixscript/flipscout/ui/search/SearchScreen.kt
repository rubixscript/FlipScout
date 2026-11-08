package com.rubixscript.flipscout.ui.search

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.data.SavedSearch
import com.rubixscript.flipscout.domain.SearchTarget
import com.rubixscript.flipscout.domain.keywordsPrompt
import com.rubixscript.flipscout.domain.parseKeywords
import com.rubixscript.flipscout.domain.searchUrl
import com.rubixscript.flipscout.ui.components.AddPill
import com.rubixscript.flipscout.ui.components.GradientButton
import com.rubixscript.flipscout.ui.components.Pill
import com.rubixscript.flipscout.ui.theme.Dimens
import com.rubixscript.flipscout.ui.theme.Palette
import com.rubixscript.flipscout.util.copyToClipboard
import com.rubixscript.flipscout.util.openUrl
import com.rubixscript.flipscout.util.readClipboardText
import kotlinx.coroutines.launch

/**
 * Search: pick where to look once (eBay / Marketplace / eBay sold), then type and tap the search button, or just tap
 * any saved keyword. [interests] are the categories of the user's own items; they float matching keyword packs first.
 */
@Composable
fun SearchScreen(viewModel: SearchViewModel, interests: List<String>, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var showIdeas by rememberSaveable { mutableStateOf(false) }
    var showPacks by rememberSaveable { mutableStateOf(false) }
    var categoryTarget by remember { mutableStateOf<SavedSearch?>(null) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    // The last marketplace the user picked is kept, so searching stays a single tap.
    var targetName by rememberSaveable { mutableStateOf(SearchTarget.MARKETPLACE.name) }
    val target = SearchTarget.valueOf(targetName)
    // null = every category; "" = uncategorised.
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is SearchEvent.KeywordDeleted -> context.getString(R.string.deleted_message, event.query)
                is SearchEvent.CategoryDeleted -> context.getString(R.string.category_deleted_message, event.category, event.count)
                is SearchEvent.CategoryUngrouped -> context.getString(R.string.category_ungrouped_message, event.category)
            }
            launch {
                snackbar.currentSnackbarData?.dismiss()
                val result = snackbar.showSnackbar(message = message, actionLabel = context.getString(R.string.undo))
                if (result == SnackbarResult.ActionPerformed) viewModel.onUndo()
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        viewModel.onSearched(query)
        context.openUrl(searchUrl(target, query))
    }

    /** One tap: read the clipboard, import whatever keywords it holds, and say what happened. */
    fun importFromClipboard() {
        val text = context.readClipboardText()
        val message = when {
            text == null || parseKeywords(text).isEmpty() -> context.getString(R.string.import_clipboard_empty)
            else -> viewModel.onImport(text).let {
                if (it > 0) context.getString(R.string.imported_count, it) else context.getString(R.string.import_nothing_new)
            }
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun actionsFor(saved: SavedSearch, pinnedRow: Boolean) = KeywordActions(
        onSearch = { search(saved.query) },
        onPick = { viewModel.onQueryChange(saved.query) },
        onTogglePin = { viewModel.onTogglePin(saved) },
        onChangeCategory = if (pinnedRow) ({ categoryTarget = saved }) else null,
        onDelete = { viewModel.onDelete(saved) },
    )

    // A category that was renamed, deleted or emptied falls back to "All".
    val activeCategory = selectedCategory?.takeIf { c -> state.pinnedByCategory.any { it.first == c } }
    val shownKeywords = if (activeCategory == null) state.pinnedByCategory.flatMap { it.second }
    else state.pinnedByCategory.first { it.first == activeCategory }.second

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = Dimens.screen, end = Dimens.screen, top = 8.dp, bottom = Dimens.bottomBarSpace),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(Modifier.padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.search_title), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.search_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                    AddPill({ showPacks = true }, label = stringResource(R.string.packs_button))
                }
            }
            item { TargetSwitch(target, onSelect = { targetName = it.name }) }
            item {
                SearchBox(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    accent = target.color,
                    onSearch = { search(state.query) },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (state.recent.isNotEmpty()) {
                item(key = "recent") { RecentChips(state.recent, ::search, viewModel::onTogglePin, viewModel::onDelete) }
            }

            if (!state.isLoading && state.pinned.isEmpty()) {
                item(key = "empty") { PacksEmptyState(onBrowse = { showPacks = true }) }
            } else if (state.pinned.isNotEmpty()) {
                item(key = "chips") {
                    CategoryChips(
                        all = state.pinned.size,
                        categories = state.pinnedByCategory.map { it.first to it.second.size },
                        selected = activeCategory,
                        onSelect = { selectedCategory = it },
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (activeCategory != null && activeCategory.isNotBlank()) {
                    item(key = "catbar") {
                        CategoryBar(
                            name = activeCategory,
                            onRename = { renameTarget = activeCategory },
                            onUngroup = { viewModel.onUngroupCategory(activeCategory); selectedCategory = null },
                            onDelete = { viewModel.onDeleteCategory(activeCategory); selectedCategory = null },
                        )
                    }
                }
                items(shownKeywords, key = { it.id }) { saved ->
                    KeywordRow(
                        saved = saved,
                        actions = actionsFor(saved, pinnedRow = true),
                        target = target,
                        subtitle = if (activeCategory == null) saved.category else "",
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (showPacks) {
        KeywordPacksSheet(
            interests = interests,
            existingQueries = state.history.map { it.query }.toSet(),
            onDismiss = { showPacks = false },
            onAdd = { keywords ->
                val added = viewModel.onAddKeywords(keywords)
                Toast.makeText(context, context.getString(R.string.imported_count, added), Toast.LENGTH_SHORT).show()
                showPacks = false
            },
            onAskAi = { showPacks = false; showIdeas = true },
            onImportClipboard = { showPacks = false; importFromClipboard() },
        )
    }
    if (showIdeas) {
        KeywordIdeasSheet(
            onDismiss = { showIdeas = false },
            onAskAi = { topic, assistant ->
                context.copyToClipboard(context.getString(R.string.keywords_prompt_label), keywordsPrompt(topic))
                Toast.makeText(context, R.string.keywords_prompt_copied, Toast.LENGTH_LONG).show()
                showIdeas = false
                context.openUrl(assistant.url)
            },
        )
    }
    renameTarget?.let { old ->
        CategoryDialog(
            current = old,
            suggestions = state.categories.filterNot { it == old },
            titleRes = R.string.category_rename_title,
            allowRemove = false,
            onDismiss = { renameTarget = null },
            onSave = { new ->
                if (new.isNotBlank()) { viewModel.onRenameCategory(old, new); selectedCategory = new.trim() }
                renameTarget = null
            },
        )
    }
    categoryTarget?.let { saved ->
        CategoryDialog(
            current = saved.category,
            suggestions = state.categories,
            onDismiss = { categoryTarget = null },
            onSave = { viewModel.onSetCategory(saved, it); categoryTarget = null },
        )
    }
}

private val SearchTarget.labelRes: Int
    get() = when (this) {
        SearchTarget.EBAY -> R.string.search_ebay
        SearchTarget.EBAY_SOLD -> R.string.search_ebay_sold
        SearchTarget.MARKETPLACE -> R.string.search_marketplace
    }

/** Three-way switch for where to search; the chosen segment slides to an ink pill. */
@Composable
private fun TargetSwitch(selected: SearchTarget, onSelect: (SearchTarget) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(MaterialTheme.colorScheme.surface).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SearchTarget.entries.forEach { t ->
            val on = t == selected
            val bg by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else Color.Transparent, tween(220), label = "segBg")
            val fg by animateColorAsState(
                if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, tween(220), label = "segFg")
            Box(
                Modifier.weight(1f).height(40.dp).clip(CircleShape).background(bg).clickable { onSelect(t) },
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(t.labelRes), color = fg, fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

/** Big soft search field with a round go button inside it (also triggered by the keyboard's search key). */
@Composable
private fun SearchBox(value: String, onValueChange: (String) -> Unit, accent: Color, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    val ready = value.isNotBlank()
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        placeholder = { Text(stringResource(R.string.search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        singleLine = true, textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(Dimens.buttonRadius), modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        trailingIcon = {
            val bg by animateColorAsState(
                if (ready) accent else MaterialTheme.colorScheme.surfaceVariant, tween(200), label = "goBg")
            Box(
                Modifier.padding(end = 6.dp).size(40.dp).clip(CircleShape).background(bg).clickable(enabled = ready, onClick = onSearch),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_title),
                    tint = if (ready) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
        ),
    )
}

/** Recent searches as one scrolling row of chips: tap to search again, long-press to save or remove. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentChips(
    recent: List<SavedSearch>,
    onSearch: (String) -> Unit,
    onPin: (SavedSearch) -> Unit,
    onDelete: (SavedSearch) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
        Text(stringResource(R.string.search_recent), color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            recent.forEach { saved ->
                var menu by remember { mutableStateOf(false) }
                Box {
                    Text(
                        saved.query, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                            .combinedClickable(onClick = { onSearch(saved.query) }, onLongClick = { menu = true })
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.menu_pin)) }, onClick = { menu = false; onPin(saved) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.delete), color = Palette.Loss) },
                            onClick = { menu = false; onDelete(saved) })
                    }
                }
            }
        }
    }
}

/** "All" plus one chip per category (with counts); replaces the long stack of collapsible cards. */
@Composable
private fun CategoryChips(
    all: Int,
    categories: List<Pair<String, Int>>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("${stringResource(R.string.filter_all)}  $all", selected == null, Palette.Emerald, onClick = { onSelect(null) })
        categories.forEach { (name, count) ->
            val label = name.ifBlank { stringResource(R.string.search_uncategorised) }
            Pill("$label  $count", selected == name, Palette.Emerald, onClick = { onSelect(if (selected == name) null else name) })
        }
    }
}

/** Shown under the chips while one category is open: its name and a menu to rename, ungroup or delete it. */
@Composable
private fun CategoryBar(name: String, onRename: () -> Unit, onUngroup: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, modifier = Modifier.weight(1f))
        Box {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { menu = true }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.menu_rename_category)) }, onClick = { menu = false; onRename() })
                DropdownMenuItem(text = { Text(stringResource(R.string.menu_ungroup)) }, onClick = { menu = false; onUngroup() })
                DropdownMenuItem(text = { Text(stringResource(R.string.menu_delete_category), color = Palette.Loss) },
                    onClick = { menu = false; onDelete() })
            }
        }
    }
}

/** First-run state: nothing saved yet, so the next step is one obvious button. */
@Composable
private fun PacksEmptyState(onBrowse: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(Dimens.cardRadius))
            .background(MaterialTheme.colorScheme.surface).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.packs_empty_title), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
        Text(stringResource(R.string.packs_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        GradientButton(stringResource(R.string.packs_empty_cta), onClick = onBrowse, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}
