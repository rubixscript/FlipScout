package com.rubixscript.flipscout.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.shadow
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubixscript.flipscout.R
import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Seller
import com.rubixscript.flipscout.data.Status
import com.rubixscript.flipscout.domain.ItemSort
import com.rubixscript.flipscout.ui.components.AddPill
import com.rubixscript.flipscout.ui.components.EmptyState
import com.rubixscript.flipscout.ui.components.HeroCard
import com.rubixscript.flipscout.ui.components.Pill
import com.rubixscript.flipscout.ui.components.SwipeableItemRow
import com.rubixscript.flipscout.ui.editor.ItemEditor
import com.rubixscript.flipscout.ui.theme.Palette
import com.rubixscript.flipscout.ui.theme.Dimens
import com.rubixscript.flipscout.ui.theme.color
import com.rubixscript.flipscout.ui.theme.labelRes
import kotlinx.coroutines.launch

@get:StringRes
private val ItemSort.labelRes: Int
    get() = when (this) {
        ItemSort.NEWEST -> R.string.sort_newest
        ItemSort.OLDEST -> R.string.sort_oldest
        ItemSort.PROFIT -> R.string.sort_profit
        ItemSort.ROI -> R.string.sort_roi
        ItemSort.PRICE -> R.string.sort_price
    }

/** Stateful entry point: wires the [HomeViewModel] to the stateless [HomeContent]. */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    sellers: List<Seller>,
    onOpenSellers: () -> Unit,
    onOpenSettings: () -> Unit,
    onUndoReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.ItemDeleted -> launch {
                    val name = event.title.ifBlank { context.getString(R.string.item_fallback_name) }
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(
                        message = context.getString(R.string.deleted_message, name),
                        actionLabel = context.getString(R.string.undo),
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete()
                }
                is HomeEvent.DataReset -> launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(
                        message = context.getString(R.string.reset_done),
                        actionLabel = context.getString(R.string.undo),
                    )
                    if (result == SnackbarResult.ActionPerformed) onUndoReset()
                }
                is HomeEvent.Imported -> snackbar.showSnackbar(
                    context.getString(R.string.imported_items_sellers, event.items, event.sellers),
                )
            }
        }
    }

    HomeContent(
        modifier = modifier,
        state = state,
        snackbar = snackbar,
        haptics = haptics,
        onSearch = viewModel::onSearch,
        onFilterSelected = viewModel::onFilterSelected,
        onToggleCategory = viewModel::onToggleCategory,
        onTogglePlatform = viewModel::onTogglePlatform,
        onClearFilters = viewModel::onClearFilters,
        onSortSelected = viewModel::onSortSelected,
        onNewItem = viewModel::onNewItem,
        onEditItem = viewModel::onEditItem,
        onAdvance = viewModel::onAdvance,
        onDelete = viewModel::onDelete,
        onOpenSellers = onOpenSellers,
        onOpenSettings = onOpenSettings,
    )

    state.editor?.let { editor ->
        ItemEditor(
            item = editor.item,
            isNew = editor.isNew,
            categories = state.categories,
            sellers = sellers,
            onDismiss = viewModel::onEditorDismissed,
            onSave = viewModel::onSave,
            onDelete = { viewModel.onDelete(editor.item) },
        )
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier,
    state: HomeUiState,
    snackbar: SnackbarHostState,
    haptics: HapticFeedback,
    onSearch: (String) -> Unit,
    onFilterSelected: (Status?) -> Unit,
    onToggleCategory: (String) -> Unit,
    onTogglePlatform: (String) -> Unit,
    onClearFilters: () -> Unit,
    onSortSelected: (ItemSort) -> Unit,
    onNewItem: () -> Unit,
    onEditItem: (Item) -> Unit,
    onAdvance: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    onOpenSellers: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0), // system bars are handled by the app shell
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = Dimens.screen, end = Dimens.screen, top = 8.dp, bottom = Dimens.bottomBarSpace),
            verticalArrangement = Arrangement.spacedBy(Dimens.listGap),
        ) {
            item { Header(searchOpen || state.query.text.isNotEmpty(), { searchOpen = !searchOpen }, onOpenSellers, onOpenSettings, onNewItem) }
            item { HeroCard(state.stats) }
            item {
                AnimatedVisibility(searchOpen || state.query.text.isNotEmpty(),
                    enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    SearchField(state.query.text, onSearch)
                }
            }
            item { SectionHeader(stringResource(R.string.section_items), state.items.size) }
            item { FilterRow(state, onFilterSelected, onSortSelected) }
            if (state.categories.isNotEmpty() || state.platforms.isNotEmpty()) {
                item {
                    TextButton(onClick = { showFilters = true }) {
                        val active = state.query.categories.size + state.query.platforms.size
                        Text(
                            if (active > 0) stringResource(R.string.filters_active, active)
                            else stringResource(R.string.filters),
                            color = if (active > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            if (!state.isLoading && state.visibleItems.isEmpty()) item { EmptyState(filtered = state.items.isNotEmpty()) }
            items(state.visibleItems, key = { it.id }) { item ->
                SwipeableItemRow(
                    item = item,
                    modifier = Modifier.animateItem(),
                    onAdvance = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onAdvance(item) },
                    onDelete = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onDelete(item) },
                    onClick = { onEditItem(item) },
                )
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            state = state,
            onToggleCategory = onToggleCategory,
            onTogglePlatform = onTogglePlatform,
            onClear = onClearFilters,
            onDismiss = { showFilters = false },
        )
    }
}

/** Logo disc on the left; a floating pill of icons (search, sellers, settings) and the "+ Add" pill on the right. */
@Composable
private fun Header(
    searchActive: Boolean,
    onToggleSearch: () -> Unit,
    onOpenSellers: () -> Unit,
    onOpenSettings: () -> Unit,
    onAdd: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground),
            contentAlignment = Alignment.Center) {
            Text("F", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.shadow(12.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.10f))
                .clip(CircleShape).background(MaterialTheme.colorScheme.background).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderIcon(onClick = onToggleSearch) {
                Icon(if (searchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_items_hint), modifier = Modifier.size(22.dp))
            }
            HeaderIcon(onClick = onOpenSellers) {
                Icon(Icons.Default.Person, contentDescription = stringResource(R.string.tab_sellers), modifier = Modifier.size(22.dp))
            }
            HeaderIcon(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title), modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        AddPill(onAdd)
    }
}

@Composable
private fun HeaderIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { content() }
}

/** "Items  12": a heading with a count chip, so the list reads at a glance. */
@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text("$count", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SearchField(text: String, onSearch: (String) -> Unit) {
    OutlinedTextField(
        value = text,
        onValueChange = onSearch,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_items_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingIcon = if (text.isNotEmpty()) {
            { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(CircleShape).clickable { onSearch("") }.padding(6.dp)) }
        } else null,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(Dimens.buttonRadius),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FilterRow(state: HomeUiState, onFilterSelected: (Status?) -> Unit, onSortSelected: (ItemSort) -> Unit) {
    var sortOpen by remember { mutableStateOf(false) }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("${stringResource(R.string.filter_all)}  ${state.items.size}", state.query.status == null,
            MaterialTheme.colorScheme.primary, onClick = { onFilterSelected(null) })
        Status.entries.forEach { s ->
            Pill("${stringResource(s.labelRes)}  ${state.countsByStatus[s] ?: 0}", state.query.status == s, s.color(),
                onClick = { onFilterSelected(if (state.query.status == s) null else s) })
        }
        Row {
            Pill(stringResource(state.query.sort.labelRes) + "  ⇅", false, MaterialTheme.colorScheme.primary,
                onClick = { sortOpen = true })
            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                ItemSort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = { Text(stringResource(sort.labelRes)) },
                        trailingIcon = { if (sort == state.query.sort) Text("✓", fontWeight = FontWeight.Bold) },
                        onClick = { onSortSelected(sort); sortOpen = false },
                    )
                }
            }
        }
    }
}

/** Category + platform multi-select for the item list; opens from the Filters button. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    state: HomeUiState,
    onToggleCategory: (String) -> Unit,
    onTogglePlatform: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.filters), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f))
                if (state.query.categories.isNotEmpty() || state.query.platforms.isNotEmpty()) {
                    TextButton(onClear) { Text(stringResource(R.string.clear_filters), color = MaterialTheme.colorScheme.primary) }
                }
            }
            if (state.categories.isNotEmpty()) {
                Text(stringResource(R.string.filter_category), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.categories.forEach { category ->
                        Pill(
                            category, category in state.query.categories, MaterialTheme.colorScheme.primary,
                            onClick = { onToggleCategory(category) },
                        )
                    }
                }
            }
            if (state.platforms.isNotEmpty()) {
                Text(stringResource(R.string.filter_platform), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.platforms.forEach { platform ->
                        Pill(
                            platform, platform in state.query.platforms, Palette.Violet,
                            onClick = { onTogglePlatform(platform) },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                TextButton(onDismiss) { Text(stringResource(R.string.done), fontWeight = FontWeight.Bold) }
            }
        }
    }
}
