package com.rubixscript.flipscout.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class Status { WATCHING, BOUGHT, LISTED, SOLD;

    /** The next stage of the pipeline, or null once the item is sold. */
    val next: Status? get() = entries.getOrNull(ordinal + 1)
}

/**
 * One flip. All money fields are TOTALS for the purchase (enter what you actually paid),
 * [quantity] is an informational count of units in the lot.
 */
data class Item(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0, // target while unsold, actual once SOLD
    val fees: Double = 0.0,
    val shipping: Double = 0.0, // one-off cost for the whole purchase
    val quantity: Int = 1,
    val status: Status = Status.WATCHING,
    val category: String = "",
    val platform: String = "", // where it is listed / sold, e.g. "eBay"
    val tags: List<String> = emptyList(),
    val sellerId: String = "",
    val link: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val soldAt: Long = 0L, // epoch millis when it sold, 0 = never
    val photo: String = "", // file name inside the app photo folder, blank = none
) {
    /** Profit once sold, projected profit otherwise. */
    val profit: Double get() = sellPrice - buyPrice - fees - shipping
    val roi: Double get() = if (buyPrice + shipping > 0) profit / (buyPrice + shipping) * 100 else 0.0
}

internal fun Item.toJson(): JSONObject = JSONObject().put("id", id).put("title", title).put("buy", buyPrice)
    .put("sell", sellPrice).put("fees", fees).put("ship", shipping).put("qty", quantity)
    .put("status", status.name).put("cat", category).put("plat", platform)
    .put("tags", JSONArray(tags)).put("seller", sellerId)
    .put("link", link).put("notes", notes).put("at", createdAt).put("soldAt", soldAt).put("photo", photo)

internal fun itemFromJson(json: JSONObject): Item = json.toItem()

internal fun JSONObject.toItem() = Item(
    id = getString("id"),
    title = optString("title"),
    buyPrice = optDouble("buy"),
    sellPrice = optDouble("sell"),
    fees = optDouble("fees"),
    shipping = optDouble("ship"),
    quantity = optInt("qty", 1).coerceAtLeast(1),
    status = runCatching { Status.valueOf(optString("status")) }.getOrDefault(Status.WATCHING),
    category = optString("cat"),
    platform = optString("plat"),
    tags = runCatching {
        val arr = optJSONArray("tags") ?: return@runCatching emptyList()
        List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList()),
    sellerId = optString("seller"),
    link = optString("link"),
    notes = optString("notes"),
    createdAt = optLong("at"),
    soldAt = optLong("soldAt"),
    photo = optString("photo"),
)
