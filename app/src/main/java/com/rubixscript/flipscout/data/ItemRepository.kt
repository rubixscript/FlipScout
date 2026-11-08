package com.rubixscript.flipscout.data

import java.io.File

/** Source of truth for the user's items. Swap the implementation (e.g. Room) without touching the UI. */
interface ItemRepository {
    suspend fun load(): List<Item>
    suspend fun save(items: List<Item>)
}

class JsonFileItemRepository(file: File) : ItemRepository {
    private val store = JsonListStore(file, Item::toJson, ::itemFromJson)
    override suspend fun load() = store.load()
    override suspend fun save(items: List<Item>) = store.save(items)
}
