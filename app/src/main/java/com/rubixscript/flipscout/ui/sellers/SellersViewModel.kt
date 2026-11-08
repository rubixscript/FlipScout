package com.rubixscript.flipscout.ui.sellers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rubixscript.flipscout.data.JsonFileSellerRepository
import com.rubixscript.flipscout.data.Seller
import com.rubixscript.flipscout.domain.isMockId
import com.rubixscript.flipscout.domain.mockSellers
import com.rubixscript.flipscout.data.SellerRepository
import com.rubixscript.flipscout.domain.parseSharedListing
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

data class SellerEditorState(val seller: Seller, val isNew: Boolean)

data class SellersUiState(
    val sellers: List<Seller> = emptyList(),
    val editor: SellerEditorState? = null,
    val isLoading: Boolean = true,
) {
    /** Favourites first; otherwise newest first (the stored order). */
    val visibleSellers: List<Seller> = sellers.sortedByDescending { it.favorite }
}

sealed interface SellersEvent {
    data class SellerDeleted(val name: String) : SellersEvent
}

class SellersViewModel(private val repository: SellerRepository) : ViewModel() {

    private val _state = MutableStateFlow(SellersUiState())
    val state: StateFlow<SellersUiState> = _state.asStateFlow()

    private val _events = Channel<SellersEvent>(Channel.BUFFERED)
    val events: Flow<SellersEvent> = _events.receiveAsFlow()

    private val saveRequests = Channel<List<Seller>>(Channel.CONFLATED)
    private var lastDeleted: Pair<Int, Seller>? = null

    init {
        viewModelScope.launch {
            val loaded = repository.load()
            _state.update { it.copy(sellers = loaded, isLoading = false) }
        }
        viewModelScope.launch {
            for (snapshot in saveRequests) withContext(NonCancellable) { repository.save(snapshot) }
        }
    }

    fun onNewSeller() = _state.update { it.copy(editor = SellerEditorState(Seller(), isNew = true)) }

    fun onEditSeller(seller: Seller) = _state.update { it.copy(editor = SellerEditorState(seller, isNew = false)) }

    /** Opens a new seller pre-filled from shared text (profile/store link plus any name the app included). */
    fun onSharedSeller(text: String) {
        val shared = parseSharedListing(text)
        val seller = Seller(name = shared.title, url = shared.url)
        _state.update { it.copy(editor = SellerEditorState(seller, isNew = true)) }
    }

    fun onEditorDismissed() = _state.update { it.copy(editor = null) }

    fun onSave(seller: Seller) {
        updateSellers { list ->
            val idx = list.indexOfFirst { it.id == seller.id }
            if (idx >= 0) list.toMutableList().also { it[idx] = seller } else listOf(seller) + list
        }
        onEditorDismissed()
    }

    fun onToggleFavorite(seller: Seller) = onSave(seller.copy(favorite = !seller.favorite))

    /** Merges a backup import by id: existing sellers update in place, new ones are appended. */
    fun onImportSellers(imported: List<Seller>) {
        if (imported.isEmpty()) return
        updateSellers { list ->
            list.map { old -> imported.firstOrNull { it.id == old.id } ?: old } +
                imported.filter { new -> list.none { it.id == new.id } }
        }
    }

    fun onDelete(seller: Seller) {
        val index = _state.value.sellers.indexOfFirst { it.id == seller.id }
        if (index < 0) return
        lastDeleted = index to seller
        updateSellers { list -> list.filterNot { it.id == seller.id } }
        onEditorDismissed()
        _events.trySend(SellersEvent.SellerDeleted(seller.name))
    }

    fun onUndoDelete() {
        val (index, seller) = lastDeleted ?: return
        lastDeleted = null
        updateSellers { list -> list.toMutableList().also { it.add(index.coerceAtMost(it.size), seller) } }
    }

    /** Debug helper: adds the sample sellers (idempotent, ids are fixed). */
    fun onAddMockData(now: Long) = onImportSellers(mockSellers(now))

    /** Debug helper: removes only the sample sellers. */
    fun onDeleteMockData() {
        if (_state.value.sellers.any { isMockId(it.id) }) updateSellers { list -> list.filterNot { isMockId(it.id) } }
    }

    /** Part of "Reset all data"; restored by [onUndoReset]. */
    fun onResetAll() {
        resetSnapshot = _state.value.sellers
        updateSellers { emptyList() }
        onEditorDismissed()
    }

    fun onUndoReset() {
        val snapshot = resetSnapshot ?: return
        resetSnapshot = null
        updateSellers { snapshot }
    }

    private var resetSnapshot: List<Seller>? = null

    private fun updateSellers(transform: (List<Seller>) -> List<Seller>) {
        var snapshot = emptyList<Seller>()
        _state.update { s -> transform(s.sellers).also { snapshot = it }.let { s.copy(sellers = it) } }
        saveRequests.trySend(snapshot)
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            val file = File(context.applicationContext.filesDir, "sellers.json")
            initializer { SellersViewModel(JsonFileSellerRepository(file)) }
        }
    }
}
