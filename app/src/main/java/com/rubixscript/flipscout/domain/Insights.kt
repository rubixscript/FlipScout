package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Status
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

/** Profit banked in one calendar month. [month] uses the device timezone, like the rest of the app. */
data class MonthProfit(val month: YearMonth, val profit: Double) {
    /** Short month name for chart labels, e.g. "Jan". */
    val label: String get() = month.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
}

/** Total sold profit per month, ending with the current month. Empty months are included as zero bars. */
fun List<Item>.monthlyProfit(months: Int = 6): List<MonthProfit> {
    val sold = filter { it.status == Status.SOLD }
    val zone = ZoneId.systemDefault()
    val now = YearMonth.now(zone)
    return (months - 1 downTo 0).map { back ->
        val month = now.minusMonths(back.toLong())
        MonthProfit(month, sold.filter { it.soldMonth(zone) == month }.sumOf { it.profit })
    }
}

/** Sold items use [Item.soldAt]; older data without a sold date falls back to when the item was added. */
private fun Item.soldMonth(zone: ZoneId): YearMonth =
    YearMonth.from(Instant.ofEpochMilli(if (soldAt > 0) soldAt else createdAt).atZone(zone))

/** One row of a "profit by X" breakdown, biggest profit first. */
data class GroupProfit(val label: String, val profit: Double, val count: Int)

/** Sold profit grouped by category, best first. [fallbackLabel] is used for uncategorised items. */
fun List<Item>.profitByCategory(fallbackLabel: String): List<GroupProfit> =
    profitBy({ it.category.ifBlank { fallbackLabel } })

/** Sold profit grouped by platform, best first. [fallbackLabel] is used for items without a platform. */
fun List<Item>.profitByPlatform(fallbackLabel: String): List<GroupProfit> =
    profitBy({ it.platform.ifBlank { fallbackLabel } })

private fun List<Item>.profitBy(key: (Item) -> String): List<GroupProfit> =
    filter { it.status == Status.SOLD }
        .groupBy(key)
        .map { (label, group) -> GroupProfit(label, group.sumOf { it.profit }, group.size) }
        .sortedWith(compareByDescending<GroupProfit> { it.profit }.thenBy { it.label })
