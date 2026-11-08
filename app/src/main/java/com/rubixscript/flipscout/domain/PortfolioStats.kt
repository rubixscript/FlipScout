package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Status

/** Headline numbers shown on the dashboard. */
data class PortfolioStats(
    val profit: Double = 0.0,
    val inStock: Double = 0.0,
    val soldCount: Int = 0,
    val avgRoi: Double = 0.0,
)

fun List<Item>.portfolioStats(): PortfolioStats {
    val sold = filter { it.status == Status.SOLD }
    return PortfolioStats(
        profit = sold.sumOf { it.profit },
        inStock = filter { it.status == Status.BOUGHT || it.status == Status.LISTED }.sumOf { it.buyPrice + it.shipping },
        soldCount = sold.size,
        avgRoi = sold.filter { it.buyPrice > 0 }.map { it.roi }.average().takeIf { !it.isNaN() } ?: 0.0,
    )
}
