package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Status

/** How one seller's flips are doing, shown on their row in the Sellers tab. */
data class SellerStats(
    val itemsBought: Int = 0,
    val soldCount: Int = 0,
    val spent: Double = 0.0,
    val profit: Double = 0.0,
)

/** Aggregates the items linked to [sellerId]. Watching items don't count as purchases. */
fun sellerStats(sellerId: String, items: List<Item>): SellerStats {
    val mine = items.filter { it.sellerId == sellerId }
    val bought = mine.filter { it.status != Status.WATCHING }
    val sold = bought.filter { it.status == Status.SOLD }
    return SellerStats(
        itemsBought = bought.size,
        soldCount = sold.size,
        spent = bought.sumOf { it.buyPrice + it.shipping },
        profit = sold.sumOf { it.profit },
    )
}
