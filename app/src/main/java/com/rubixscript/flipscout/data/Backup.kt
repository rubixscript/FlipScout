package com.rubixscript.flipscout.data

import org.json.JSONArray
import org.json.JSONObject

/** Whole-app backup as a single JSON file: items + sellers, shareable and re-importable. */
object Backup {
    private const val VERSION = 1

    fun toJson(items: List<Item>, sellers: List<Seller>): String = JSONObject()
        .put("app", "FlipScout")
        .put("version", VERSION)
        .put("exportedAt", System.currentTimeMillis())
        .put("items", JSONArray(items.map { it.toJson() }))
        .put("sellers", JSONArray(sellers.map { it.toJson() }))
        .toString()

    data class Parsed(val items: List<Item>, val sellers: List<Seller>)

    /** Accepts anything [toJson] wrote; returns null when the text isn't a FlipScout backup. */
    fun parse(text: String): Parsed? = runCatching {
        val root = JSONObject(text.trim())
        val items = root.optJSONArray("items") ?: JSONArray()
        val sellers = root.optJSONArray("sellers") ?: JSONArray()
        Parsed(
            items = List(items.length()) { itemFromJson(items.getJSONObject(it)) },
            sellers = List(sellers.length()) { sellerFromJson(sellers.getJSONObject(it)) },
        )
    }.getOrNull()
}
