package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.SavedSearch
import com.rubixscript.flipscout.data.Seller
import com.rubixscript.flipscout.data.Status

/** Every mock record's id starts with this, so removing mock data never touches the user's own. */
const val MOCK_PREFIX = "mock-"

fun isMockId(id: String): Boolean = id.startsWith(MOCK_PREFIX)

private const val DAY_MS = 86_400_000L

fun mockSellers(now: Long): List<Seller> = listOf(
    Seller("${MOCK_PREFIX}seller-1", "Garage Sale Gary", "https://www.facebook.com/marketplace/profile/1",
        "Cheap tools, always haggles.", favorite = true, createdAt = now - 40 * DAY_MS),
    Seller("${MOCK_PREFIX}seller-2", "Thrift Queen", "https://www.ebay.com/usr/thriftqueen",
        "Brand-name clothes, fair prices.", createdAt = now - 25 * DAY_MS),
    Seller("${MOCK_PREFIX}seller-3", "Estate Sales Co", "https://estatesales.net/co",
        "Weekend estate lots.", createdAt = now - 10 * DAY_MS),
)

/** A varied portfolio: every status, several platforms/categories, sales over the last months, and some stale stock. */
fun mockItems(now: Long): List<Item> {
    fun item(
        n: Int, title: String, status: Status, buy: Double, sell: Double, ageDays: Int,
        soldDaysAgo: Int = 0, fees: Double = 0.0, shipping: Double = 0.0,
        category: String = "", platform: String = "", seller: Int = 0,
    ) = Item(
        id = "${MOCK_PREFIX}item-$n", title = title, status = status, buyPrice = buy, sellPrice = sell,
        fees = fees, shipping = shipping, category = category, platform = platform,
        sellerId = if (seller > 0) "${MOCK_PREFIX}seller-$seller" else "",
        createdAt = now - ageDays * DAY_MS,
        soldAt = if (status == Status.SOLD) now - soldDaysAgo * DAY_MS else 0L,
    )
    return listOf(
        item(1, "Vintage Levi's 501 jeans", Status.SOLD, 12.0, 58.0, 70, 55, 7.5, 6.0, "Clothing", "eBay", 2),
        item(2, "Nintendo Switch console", Status.SOLD, 120.0, 215.0, 60, 40, 21.0, 12.0, "Electronics", "Facebook Marketplace", 1),
        item(3, "Dewalt cordless drill", Status.SOLD, 25.0, 70.0, 45, 30, 8.0, 0.0, "Tools", "Facebook Marketplace", 1),
        item(4, "Lego Star Wars set 75192", Status.SOLD, 300.0, 520.0, 38, 20, 62.0, 15.0, "Toys", "eBay", 3),
        item(5, "Patagonia fleece jacket", Status.SOLD, 18.0, 64.0, 28, 9, 8.3, 7.0, "Clothing", "eBay", 2),
        item(6, "Sony WH-1000XM4 headphones", Status.SOLD, 80.0, 150.0, 18, 3, 19.5, 9.0, "Electronics", "eBay"),
        item(7, "Cast iron skillet set", Status.LISTED, 15.0, 55.0, 24, category = "Home", platform = "Facebook Marketplace", seller = 3),
        item(8, "Canon AE-1 film camera", Status.LISTED, 60.0, 160.0, 16, category = "Electronics", platform = "eBay", seller = 3),
        item(9, "Carhartt work jacket", Status.LISTED, 22.0, 75.0, 6, category = "Clothing", platform = "eBay", seller = 2),
        item(10, "KitchenAid stand mixer", Status.BOUGHT, 70.0, 180.0, 21, category = "Home", seller = 1),
        item(11, "Pokemon card binder", Status.BOUGHT, 40.0, 140.0, 4, category = "Collectibles", seller = 3),
        item(12, "Herman Miller office chair", Status.BOUGHT, 90.0, 320.0, 2, category = "Furniture"),
        item(13, "Retro Game Boy Color", Status.WATCHING, 45.0, 110.0, 3, category = "Electronics"),
        item(14, "Vintage Pyrex bowls", Status.WATCHING, 20.0, 65.0, 1, category = "Home"),
    )
}

/** Sample keyword library (pinned, grouped by category) plus a few recent searches. */
fun mockSearches(now: Long): List<SavedSearch> {
    fun pinned(n: Int, query: String, category: String) = SavedSearch(
        id = "${MOCK_PREFIX}search-$n", query = query, pinned = true, category = category, createdAt = now - n * DAY_MS)
    fun recent(n: Int, query: String) = SavedSearch(
        id = "${MOCK_PREFIX}search-$n", query = query, createdAt = now - n * 3_600_000L)
    return listOf(
        recent(101, "nintendo switch oled"),
        recent(102, "dewalt drill bare tool"),
        recent(103, "vintage pyrex"),
        pinned(1, "nintendo switch lot", "Electronics"),
        pinned(2, "sony wh-1000xm4", "Electronics"),
        pinned(3, "canon ae-1 camera", "Electronics"),
        pinned(4, "vintage levis 501", "Clothing"),
        pinned(5, "patagonia fleece", "Clothing"),
        pinned(6, "carhartt jacket", "Clothing"),
        pinned(7, "lego star wars sealed", "Toys"),
        pinned(8, "pokemon card lot", "Toys"),
        pinned(9, "kitchenaid stand mixer", "Home"),
        pinned(10, "cast iron skillet", "Home"),
    )
}
