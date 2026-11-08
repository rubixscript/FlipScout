package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Status

/** Ways the item list can be ordered; NEWEST matches the stored (prepend) order. */
enum class ItemSort { NEWEST, OLDEST, PROFIT, ROI, PRICE }

/** Everything the Home list can narrow and order by at once. Empty sets/lists mean "no filter". */
data class ItemQuery(
    val text: String = "",
    val status: Status? = null,
    val categories: Set<String> = emptySet(),
    val platforms: Set<String> = emptySet(),
    val sort: ItemSort = ItemSort.NEWEST,
) {
    val isFiltering: Boolean
        get() = text.isNotBlank() || status != null || categories.isNotEmpty() || platforms.isNotEmpty()
}

/** Text search over title, notes, category and tags + status/category/platform filters + ordering. */
fun List<Item>.query(query: ItemQuery): List<Item> {
    val needle = query.text.trim().lowercase()
    val matched = filter { item ->
        (query.status == null || item.status == query.status) &&
            (query.categories.isEmpty() || item.category in query.categories) &&
            (query.platforms.isEmpty() || item.platform in query.platforms) &&
            (needle.isBlank() || needle in item.title.lowercase() || needle in item.notes.lowercase() ||
                needle in item.category.lowercase() || item.tags.any { needle in it.lowercase() })
    }
    return when (query.sort) {
        ItemSort.NEWEST -> matched.sortedByDescending { it.createdAt }
        ItemSort.OLDEST -> matched.sortedBy { it.createdAt }
        ItemSort.PROFIT -> matched.sortedByDescending { it.profit }
        ItemSort.ROI -> matched.sortedByDescending { it.roi }
        ItemSort.PRICE -> matched.sortedByDescending { it.sellPrice }
    }
}

/** Distinct non-blank categories across the items, alphabetical — feeds the filter sheet and editor suggestions. */
fun List<Item>.distinctCategories(): List<String> = map { it.category }.filter { it.isNotBlank() }.distinct().sorted()

/** Distinct non-blank platforms across the items, alphabetical. */
fun List<Item>.distinctPlatforms(): List<String> = map { it.platform }.filter { it.isNotBlank() }.distinct().sorted()
