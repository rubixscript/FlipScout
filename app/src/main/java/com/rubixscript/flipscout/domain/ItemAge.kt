package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Status

private const val DAY_MS = 86_400_000L

/** Days sitting in the app; items without a creation date (old imports) count as brand new. */
fun Item.ageDays(now: Long): Int =
    if (createdAt <= 0L) 0 else ((now - createdAt) / DAY_MS).toInt().coerceAtLeast(0)

/** Bought or listed stock that has not sold for [thresholdDays] or more: worth a nudge to reprice or relist. */
fun Item.isStale(now: Long, thresholdDays: Int = 14): Boolean =
    (status == Status.BOUGHT || status == Status.LISTED) && ageDays(now) >= thresholdDays
