package com.rubixscript.flipscout.domain

private const val MAX_KEYWORDS = 200
private const val MAX_LENGTH = 80

/** Keys whose value merely wraps keywords/categories (they don't name a category themselves). */
private val CONTAINER_KEYS = setOf("keywords", "queries", "searches", "terms", "categories", "groups", "items", "results")
private val QUERY_KEYS = setOf("query", "keyword", "term", "name")
private val CATEGORY_KEYS = setOf("category", "group")

/** A keyword suggested by the AI, optionally filed under a category; [category] is blank when uncategorised. */
data class ParsedKeyword(val query: String, val category: String = "")

/** The prompt the user pastes into ChatGPT / Gemini to get keyword ideas back as JSON, sorted into categories. */
fun keywordsPrompt(topic: String): String = buildString {
    appendLine("You are an expert reseller who flips items on eBay and Facebook Marketplace.")
    appendLine("Suggest search keywords for finding underpriced items that resell for a profit.")
    if (topic.isNotBlank()) appendLine("Focus on: ${topic.trim()}")
    appendLine()
    appendLine("Rules:")
    appendLine("- Sort them into 4 to 6 categories by product type, with about 6 keywords per category.")
    appendLine("- Be specific: brand + model or product type (e.g. \"sony walkman wm-ex\"), not generic words.")
    appendLine("- Prefer items that are easy to ship and have steady demand.")
    appendLine("- 2 to 5 words per keyword, lowercase, no duplicates.")
    appendLine()
    appendLine("Reply with ONLY valid JSON in exactly this shape, with no explanation and no code fences:")
    appendLine("{\"categories\": {\"Category name\": [\"keyword one\", \"keyword two\"], \"Another category\": [\"keyword three\"]}}")
}

/**
 * Extracts keywords (and their categories) from whatever the AI replied. Tolerates markdown code fences, surrounding
 * text, and shapes like {"categories":{"Audio":["a"]}}, {"Audio":["a"]}, {"keywords":["a"]}, ["a"],
 * [{"query":"a","category":"Audio"}]. Returns an empty list if nothing usable was found.
 */
fun parseKeywords(text: String): List<ParsedKeyword> {
    val start = text.indexOfFirst { it == '[' || it == '{' }
    if (start < 0) return emptyList()
    val root = readFirstJsonValue(text.substring(start)) ?: return emptyList()

    val found = LinkedHashMap<String, ParsedKeyword>() // keyed by lowercase query; first occurrence wins
    fun add(raw: String, category: String) {
        val q = raw.trim()
        if (q.isNotEmpty() && q.length <= MAX_LENGTH) found.putIfAbsent(q.lowercase(), ParsedKeyword(q, category.trim()))
    }
    fun collect(value: Any?, category: String) {
        when (value) {
            is String -> add(value, category)
            is List<*> -> value.forEach { collect(it, category) }
            is Map<*, *> -> {
                val own = value.entries.firstOrNull { (it.key as String).lowercase() in CATEGORY_KEYS && it.value is String }
                    ?.value as String? ?: category
                value.forEach { (k, v) ->
                    val key = k as String
                    when (v) {
                        is String -> if (key.lowercase() in QUERY_KEYS) add(v, own)
                        is List<*>, is Map<*, *> -> collect(v, if (key.lowercase() in CONTAINER_KEYS) own else key)
                    }
                }
            }
        }
    }
    collect(root, "")
    return found.values.take(MAX_KEYWORDS)
}
