package com.rubixscript.flipscout.ui.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rubixscript.flipscout.data.JsonFileSavedSearchRepository
import com.rubixscript.flipscout.data.SavedSearch
import com.rubixscript.flipscout.domain.ParsedKeyword
import com.rubixscript.flipscout.domain.isMockId
import com.rubixscript.flipscout.domain.mockSearches
import com.rubixscript.flipscout.data.SavedSearchRepository
import com.rubixscript.flipscout.domain.parseKeywords
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

data class SearchUiState(
    val query: String = "",
    val history: List<SavedSearch> = emptyList(),
    val isLoading: Boolean = true,
) {
    val pinned: List<SavedSearch> = history.filter { it.pinned }
    val recent: List<SavedSearch> = history.filterNot { it.pinned }

    /** Existing category names, for suggesting when (re)categorising a keyword. */
    val categories: List<String> = pinned.map { it.category }.filter { it.isNotBlank() }.distinct().sortedBy { it.lowercase() }

    /** The library as (category, keywords) sections: named categories A–Z, then uncategorised (blank name) last. */
    val pinnedByCategory: List<Pair<String, List<SavedSearch>>> =
        pinned.groupBy { it.category }.toList().sortedWith(compareBy({ it.first.isBlank() }, { it.first.lowercase() }))
}

/** Things the user can Undo; the screen turns them into a snackbar. */
sealed interface SearchEvent {
    data class KeywordDeleted(val query: String) : SearchEvent
    data class CategoryDeleted(val category: String, val count: Int) : SearchEvent
    data class CategoryUngrouped(val category: String) : SearchEvent
}

class SearchViewModel(private val repository: SavedSearchRepository) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val _events = Channel<SearchEvent>(Channel.BUFFERED)
    val events: Flow<SearchEvent> = _events.receiveAsFlow()

    private val saveRequests = Channel<List<SavedSearch>>(Channel.CONFLATED)
    private var undoSnapshot: List<SavedSearch>? = null

    init {
        viewModelScope.launch {
            val loaded = repository.load()
            _state.update { it.copy(history = loaded, isLoading = false) }
        }
        viewModelScope.launch {
            for (snapshot in saveRequests) withContext(NonCancellable) { repository.save(snapshot) }
        }
    }

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }

    /** Remembers [query] (most recent first, no duplicates) once the user actually runs a search. */
    fun onSearched(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        updateHistory { list ->
            val existing = list.firstOrNull { it.query.equals(q, ignoreCase = true) }
            val entry = existing?.copy(createdAt = System.currentTimeMillis()) ?: SavedSearch(query = q)
            capRecent(listOf(entry) + list.filterNot { it.id == entry.id })
        }
    }

    fun onTogglePin(search: SavedSearch) = updateHistory { list ->
        list.map { if (it.id == search.id) it.copy(pinned = !it.pinned) else it }
    }

    fun onDelete(search: SavedSearch) {
        if (_state.value.history.none { it.id == search.id }) return
        rememberForUndo()
        updateHistory { list -> list.filterNot { it.id == search.id } }
        _events.trySend(SearchEvent.KeywordDeleted(search.query))
    }

    /** Renaming to an existing category name merges the two. */
    fun onRenameCategory(old: String, new: String) = updateHistory { list ->
        list.map { if (it.pinned && it.category == old) it.copy(category = new.trim()) else it }
    }

    /** Keeps the keywords but drops the category, so they show up as uncategorised. */
    fun onUngroupCategory(category: String) {
        rememberForUndo()
        updateHistory { list -> list.map { if (it.pinned && it.category == category) it.copy(category = "") else it } }
        _events.trySend(SearchEvent.CategoryUngrouped(category))
    }

    /** Deletes the category together with all keywords in it. */
    fun onDeleteCategory(category: String) {
        val count = _state.value.pinned.count { it.category == category }
        if (count == 0) return
        rememberForUndo()
        updateHistory { list -> list.filterNot { it.pinned && it.category == category } }
        _events.trySend(SearchEvent.CategoryDeleted(category, count))
    }

    /** Restores everything as it was before the last delete / ungroup. */
    fun onUndo() {
        val snapshot = undoSnapshot ?: return
        undoSnapshot = null
        updateHistory { snapshot }
    }

    /** Debug helper: adds the sample keywords, skipping any already present (ids are fixed). */
    fun onAddMockData(now: Long) = updateHistory { list ->
        val have = list.map { it.id }.toSet()
        mockSearches(now).filterNot { it.id in have } + list
    }

    /** Debug helper: removes only the sample keywords. */
    fun onDeleteMockData() {
        if (_state.value.history.any { isMockId(it.id) }) updateHistory { list -> list.filterNot { isMockId(it.id) } }
    }

    /** Part of "Reset all data": clears saved and recent keywords; restored by [onUndoReset]. */
    fun onResetAll() {
        resetSnapshot = _state.value.history
        updateHistory { emptyList() }
    }

    fun onUndoReset() {
        val snapshot = resetSnapshot ?: return
        resetSnapshot = null
        updateHistory { snapshot }
    }

    private var resetSnapshot: List<SavedSearch>? = null

    private fun rememberForUndo() { undoSnapshot = _state.value.history }

    /** Moves a keyword into [category] (blank removes it from its category). Also pins it, so it lands in the library. */
    fun onSetCategory(search: SavedSearch, category: String) = updateHistory { list ->
        list.map { if (it.id == search.id) it.copy(category = category.trim(), pinned = true) else it }
    }

    /**
     * Adds keywords pasted from an AI reply to the pinned library, filed under the categories the AI chose.
     * Returns how many were new, newly pinned, or newly categorised.
     */
    fun onImport(text: String): Int = onAddKeywords(parseKeywords(text))

    /** Pins [keywords] into the library (new, newly pinned or newly categorised ones count). Used by paste-import and keyword packs. */
    fun onAddKeywords(keywords: List<ParsedKeyword>): Int {
        var changed = 0
        updateHistory { list ->
            val byKey = list.associateBy { it.query.lowercase() }
            val updated = list.toMutableList()
            val additions = mutableListOf<SavedSearch>()
            keywords.forEach { k ->
                val existing = byKey[k.query.lowercase()]
                if (existing == null) {
                    additions += SavedSearch(query = k.query, pinned = true, category = k.category); changed++
                } else if (!existing.pinned || (existing.category.isBlank() && k.category.isNotBlank())) {
                    val category = existing.category.ifBlank { k.category }
                    updated[updated.indexOfFirst { it.id == existing.id }] = existing.copy(pinned = true, category = category)
                    changed++
                }
            }
            additions + updated
        }
        return changed
    }

    /** Pinned keywords are never dropped; only the oldest unpinned (recent) ones are trimmed. */
    private fun capRecent(list: List<SavedSearch>): List<SavedSearch> {
        var recent = 0
        return list.filter { it.pinned || ++recent <= MAX_RECENT }
    }

    private fun updateHistory(transform: (List<SavedSearch>) -> List<SavedSearch>) {
        var snapshot = emptyList<SavedSearch>()
        _state.update { s -> transform(s.history).also { snapshot = it }.let { s.copy(history = it) } }
        saveRequests.trySend(snapshot)
    }

    companion object {
        private const val MAX_RECENT = 30

        fun factory(context: Context) = viewModelFactory {
            val file = File(context.applicationContext.filesDir, "searches.json")
            initializer { SearchViewModel(JsonFileSavedSearchRepository(file)) }
        }
    }
}
