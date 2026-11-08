package com.rubixscript.flipscout.domain

/** What could be recovered from text shared by the Facebook Marketplace / eBay apps. */
data class SharedListing(val url: String = "", val title: String = "", val price: Double = 0.0)

private val urlRegex = Regex("""https?://\S+""")
private val priceRegex = Regex("""(?:US\s?|USD\s?)?\$\s?(\d{1,3}(?:,\d{3})+|\d+)(\.\d{1,2})?""")

// Boilerplate the share sheets wrap around the actual listing title.
private val noiseRegexes = listOf(
    Regex("""(?i)\b(check out|take a look at|have a look at) this (listing|item|post)( for sale)?\b[:!.]*"""),
    Regex("""(?i)\bcheck this out\b[:!.]*"""),
    Regex("""(?i)\bon (facebook )?marketplace\b"""),
    Regex("""(?i)\bon ebay\b"""),
    Regex("""(?i)[|\-–·]\s*(facebook marketplace|marketplace|ebay)\s*$"""),
)
private val edgeSeparators = Regex("""^[\s|\-–·:,!]+|[\s|\-–·:,!]+$""")

/**
 * Best-effort extraction of link, title and asking price from shared text. Listing pages themselves
 * need a login, so only what the share sheet put in the text is available; missing parts stay empty/0.
 */
fun parseSharedListing(text: String): SharedListing {
    val url = urlRegex.find(text)?.value?.trimEnd('.', ',', ')', ';').orEmpty()
    var rest = if (url.isEmpty()) text else text.replace(urlRegex, " ")

    val priceMatch = priceRegex.find(rest)
    val price = priceMatch?.let { (it.groupValues[1].replace(",", "") + it.groupValues[2]).toDoubleOrNull() } ?: 0.0
    if (priceMatch != null) rest = rest.replaceFirst(priceMatch.value, " ")

    rest = rest.replace('\n', ' ')
    noiseRegexes.forEach { rest = rest.replace(it, " ") }
    val title = rest.replace(Regex("""\s+"""), " ").replace(edgeSeparators, "")

    return SharedListing(url = url, title = title, price = price)
}
