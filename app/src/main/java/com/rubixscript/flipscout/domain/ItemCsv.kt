package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Status
import java.util.UUID

/**
 * CSV export/import for items (RFC 4180: quotes wrap fields containing commas, quotes or newlines).
 * The `id` column lets a re-imported file merge back onto the same items instead of duplicating them.
 */
private val CSV_HEADER = listOf(
    "id", "title", "status", "buy", "sell", "fees", "shipping", "quantity",
    "category", "platform", "tags", "sellerId", "soldAt", "createdAt", "link", "notes",
)

fun itemsToCsv(items: List<Item>): String = buildString {
    appendLine(CSV_HEADER.joinToString(","))
    items.forEach { item ->
        appendLine(
            listOf(
                item.id, item.title, item.status.name,
                item.buyPrice, item.sellPrice, item.fees, item.shipping, item.quantity,
                item.category, item.platform, item.tags.joinToString(";"), item.sellerId,
                item.soldAt, item.createdAt, item.link, item.notes,
            ).joinToString(",") { escapeCsvField(it.toString()) },
        )
    }
}

private fun escapeCsvField(field: String) =
    if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${field.replace("\"", "\"\"")}\"" else field

/** Parses CSV text into rows of fields, honouring quoted fields, `""` escapes and multiline notes. */
internal fun parseCsv(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var field = StringBuilder()
    var row = mutableListOf<String>()
    var inQuotes = false
    var i = 0
    fun endField() { row.add(field.toString()); field = StringBuilder() }
    fun endRow() { endField(); if (row.any { it.isNotBlank() }) rows.add(row); row = mutableListOf() }
    while (i < text.length) {
        val c = text[i]
        when {
            inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
            inQuotes && c == '"' -> inQuotes = false
            inQuotes -> field.append(c)
            c == '"' -> inQuotes = true
            c == ',' -> endField()
            c == '\n' -> endRow()
            c == '\r' -> {} // Windows line endings: wait for the \n
            else -> field.append(c)
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) endRow()
    return rows
}

/** Reads items from CSV produced by [itemsToCsv] (or a hand-made file with the same columns). */
fun csvToItems(csv: String): List<Item> {
    val rows = parseCsv(csv)
    if (rows.size < 2) return emptyList()
    val header = rows.first().map { it.trim().lowercase() }
    fun cell(row: List<String>, name: String): String? = header.indexOf(name).takeIf { it >= 0 }?.let { row.getOrNull(it) }
    return rows.drop(1).mapNotNull { row ->
        val title = cell(row, "title")
        if (title == null && cell(row, "id") == null) return@mapNotNull null
        Item(
            id = cell(row, "id")?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            title = title.orEmpty(),
            buyPrice = cell(row, "buy")?.toDoubleOrNull() ?: 0.0,
            sellPrice = cell(row, "sell")?.toDoubleOrNull() ?: 0.0,
            fees = cell(row, "fees")?.toDoubleOrNull() ?: 0.0,
            shipping = cell(row, "shipping")?.toDoubleOrNull() ?: 0.0,
            quantity = cell(row, "quantity")?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
            status = cell(row, "status")?.let { runCatching { Status.valueOf(it) }.getOrNull() } ?: Status.WATCHING,
            category = cell(row, "category").orEmpty(),
            platform = cell(row, "platform").orEmpty(),
            tags = cell(row, "tags")?.split(';')?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
            sellerId = cell(row, "sellerid").orEmpty(),
            link = cell(row, "link").orEmpty(),
            notes = cell(row, "notes").orEmpty(),
            createdAt = cell(row, "createdat")?.toLongOrNull() ?: System.currentTimeMillis(),
            soldAt = cell(row, "soldat")?.toLongOrNull() ?: 0L,
        )
    }
}
