package com.rubixscript.flipscout.data

import org.json.JSONObject
import java.io.File
import java.util.UUID

/** A person or store worth buying from again (good prices, reliable). */
data class Seller(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val url: String = "",
    val notes: String = "",
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    /** Short display form of the profile link, e.g. "facebook.com". */
    val host: String
        get() = url.substringAfter("://").substringBefore('/').removePrefix("www.").removePrefix("m.")
}

internal fun Seller.toJson(): JSONObject = JSONObject().put("id", id).put("name", name).put("url", url)
    .put("notes", notes).put("fav", favorite).put("at", createdAt)

internal fun sellerFromJson(json: JSONObject) = Seller(
    id = json.getString("id"),
    name = json.optString("name"),
    url = json.optString("url"),
    notes = json.optString("notes"),
    favorite = json.optBoolean("fav"),
    createdAt = json.optLong("at"),
)

interface SellerRepository {
    suspend fun load(): List<Seller>
    suspend fun save(sellers: List<Seller>)
}

class JsonFileSellerRepository(file: File) : SellerRepository {
    private val store = JsonListStore(file, Seller::toJson, ::sellerFromJson)
    override suspend fun load() = store.load()
    override suspend fun save(sellers: List<Seller>) = store.save(sellers)
}
