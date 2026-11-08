package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.util.money

enum class Platform(val displayName: String) {
    EBAY("eBay"),
    MARKETPLACE("Facebook Marketplace"),
}

/** Builds the prompt the user pastes into ChatGPT / Gemini (photos are attached there). */
fun listingPrompt(item: Item, platform: Platform): String = buildString {
    appendLine("You are an expert reseller. Write a high-converting ${platform.displayName} listing for the item in the attached photos.")
    appendLine()
    appendLine("Item: ${item.title.ifBlank { "(identify from photos)" }}")
    if (item.sellPrice > 0) appendLine("Target price: ${money(item.sellPrice)}")
    if (item.notes.isNotBlank()) appendLine("Condition/details: ${item.notes}")
    appendLine()
    appendLine("Return:")
    appendLine("1. Title (max 80 chars, keyword-rich)")
    appendLine("2. Description (condition, specs, what is included, honest flaws)")
    appendLine("3. Suggested category and 10 search keywords")
    appendLine("4. Price recommendation (quick-sale / fair / ambitious)")
}
