package com.rubixscript.flipscout.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.ItemRepository
import com.rubixscript.flipscout.data.JsonFileItemRepository
import com.rubixscript.flipscout.data.Status
import com.rubixscript.flipscout.domain.ItemSort
import com.rubixscript.flipscout.domain.ItemQuery
import com.rubixscript.flipscout.domain.PortfolioStats
import com.rubixscript.flipscout.domain.distinctCategories
import com.rubixscript.flipscout.domain.isMockId
import com.rubixscript.flipscout.domain.mockItems
import com.rubixscript.flipscout.domain.distinctPlatforms
import com.rubixscript.flipscout.domain.parseSharedListing
import com.rubixscript.flipscout.domain.portfolioStats
import com.rubixscript.flipscout.domain.query
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** The item currently open in the editor sheet. */
data class EditorState(val item: Item, val isNew: Boolean)

data class HomeUiState(
    val items: List<Item> = emptyList(),
    val query: ItemQuery = ItemQuery(),
    val editor: EditorState? = null,
    val isLoading: Boolean = true,
) {
    val stats: PortfolioStats = items.portfolioStats()
    val visibleItems: List<Item> = items.query(query)
    val countsByStatus: Map<Status, Int> = Status.entries.associateWith { s -> items.count { it.status == s } }
    val categories: List<String> = items.distinctCategories()
    val platforms: List<String> = items.distinctPlatforms()
}

/** One-off signals the UI turns into transient feedback (snackbars). */
sealed interface HomeEvent {
    data class ItemDeleted(val title: String) : HomeEvent
    data class Imported(val items: Int, val sellers: Int) : HomeEvent
    data object DataReset : HomeEvent
}

class HomeViewModel(private val repository: ItemRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    // Conflated: if edits arrive faster than disk writes, only the latest snapshot is written, always in order.
    private val saveRequests = Channel<List<Item>>(Channel.CONFLATED)
    private var lastDeleted: Pair<Int, Item>? = null
    private var resetSnapshot: List<Item>? = null

    init {
        viewModelScope.launch {
            val loaded = repository.load()
            _state.update { it.copy(items = loaded, isLoading = false) }
        }
        viewModelScope.launch {
            for (snapshot in saveRequests) withContext(NonCancellable) { repository.save(snapshot) }
        }
    }

    fun onSearch(text: String) = _state.update { it.copy(query = it.query.copy(text = text)) }

    fun onFilterSelected(status: Status?) = _state.update { it.copy(query = it.query.copy(status = status)) }

    fun onToggleCategory(category: String) = _state.update {
        it.copy(query = it.query.copy(categories = it.query.categories.toggle(category)))
    }

    fun onTogglePlatform(platform: String) = _state.update {
        it.copy(query = it.query.copy(platforms = it.query.platforms.toggle(platform)))
    }

    fun onClearFilters() = _state.update { it.copy(query = ItemQuery(text = it.query.text)) }

    fun onSortSelected(sort: ItemSort) = _state.update { it.copy(query = it.query.copy(sort = sort)) }

    fun onNewItem() = _state.update { it.copy(editor = EditorState(Item(), isNew = true)) }

    fun onEditItem(item: Item) = _state.update { it.copy(editor = EditorState(item, isNew = false)) }

    /**
     * Adds an item straight away from whatever the share sheet gave us (link, title, asking price), then
     * opens it for tweaks. It is already saved, so dismissing the editor never loses the shared listing.
     */
    fun onSharedLink(text: String) {
        val listing = parseSharedListing(text)
        val item = Item(title = listing.title, buyPrice = listing.price, link = listing.url)
        updateItems { list -> listOf(item) + list }
        _state.update { it.copy(editor = EditorState(item, isNew = false)) }
    }

    fun onEditorDismissed() = _state.update { it.copy(editor = null) }

    fun onSave(item: Item) {
        // Stamp the sale date automatically so marking sold is still a one-tap action.
        val stamped = when {
            item.status == Status.SOLD && item.soldAt == 0L -> item.copy(soldAt = System.currentTimeMillis())
            item.status != Status.SOLD && item.soldAt != 0L -> item.copy(soldAt = 0L)
            else -> item
        }
        updateItems { list ->
            val idx = list.indexOfFirst { it.id == stamped.id }
            if (idx >= 0) list.toMutableList().also { it[idx] = stamped } else listOf(stamped) + list
        }
        onEditorDismissed()
    }

    fun onAdvance(item: Item) {
        val next = item.status.next ?: return
        onSave(item.copy(status = next))
    }

    fun onDelete(item: Item) {
        val index = _state.value.items.indexOfFirst { it.id == item.id }
        if (index < 0) return
        lastDeleted = index to item
        updateItems { list -> list.filterNot { it.id == item.id } }
        onEditorDismissed()
        _events.trySend(HomeEvent.ItemDeleted(item.title))
    }

    fun onUndoDelete() {
        val (index, item) = lastDeleted ?: return
        lastDeleted = null
        updateItems { list -> list.toMutableList().also { it.add(index.coerceAtMost(it.size), item) } }
    }

    /** Merges an import by id: existing items update in place, new ones are appended. */
    fun onImportItems(imported: List<Item>) {
        if (imported.isEmpty()) return
        updateItems { list ->
            list.map { old -> imported.firstOrNull { it.id == old.id } ?: old } +
                imported.filter { new -> list.none { it.id == new.id } }
        }
    }

    /** Debug helper: adds the sample portfolio (idempotent, ids are fixed). */
    fun onAddMockData(now: Long): Int = mockItems(now).also { onImportItems(it) }.size

    /** Debug helper: removes only the sample items and reports how many went. */
    fun onDeleteMockData(): Int {
        val count = _state.value.items.count { isMockId(it.id) }
        if (count > 0) updateItems { list -> list.filterNot { isMockId(it.id) } }
        return count
    }

    /** Wipes every item at once; the snackbar's Undo restores the whole list via [onUndoReset]. */
    fun onResetAll() {
        resetSnapshot = _state.value.items
        updateItems { emptyList() }
        onEditorDismissed()
        _events.trySend(HomeEvent.DataReset)
    }

    fun onUndoReset() {
        val snapshot = resetSnapshot ?: return
        resetSnapshot = null
        updateItems { snapshot }
    }

    /** Signals the result of a backup import (merged into both stores) to the Home snackbar. */
    fun reportImport(items: Int, sellers: Int) {
        _events.trySend(HomeEvent.Imported(items, sellers))
    }

    private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

    private fun updateItems(transform: (List<Item>) -> List<Item>) {
        var snapshot = emptyList<Item>()
        _state.update { s -> transform(s.items).also { snapshot = it }.let { s.copy(items = it) } }
        saveRequests.trySend(snapshot)
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            val file = File(context.applicationContext.filesDir, "items.json")
            initializer { HomeViewModel(JsonFileItemRepository(file)) }
        }
    }
}
