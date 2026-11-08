package com.rubixscript.flipscout.domain

import java.net.URLEncoder

enum class SearchTarget { EBAY, EBAY_SOLD, MARKETPLACE }

/**
 * Builds a search URL for [target]. These are plain https links, so Android opens the eBay or Facebook
 * app when installed (and the browser otherwise). EBAY_SOLD shows completed sales, i.e. what items really sell for.
 */
fun searchUrl(target: SearchTarget, query: String): String {
    val q = URLEncoder.encode(query.trim(), "UTF-8")
    return when (target) {
        SearchTarget.EBAY -> "https://www.ebay.com/sch/i.php?_nkw=$q"
        SearchTarget.EBAY_SOLD -> "https://www.ebay.com/sch/i.php?_nkw=$q&LH_Sold=1&LH_Complete=1"
        SearchTarget.MARKETPLACE -> "https://www.facebook.com/marketplace/search/?query=$q"
    }
}
