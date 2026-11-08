package com.rubixscript.flipscout.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

fun money(v: Double) = (if (v < 0) "-$" else "$") + "%.2f".format(abs(v))

private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

/** Local-time short date for sold dates, e.g. "14 Mar 2026". */
fun formatDate(millis: Long): String =
    dayFormat.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

/** Parses user-typed amounts, tolerating a decimal comma. Invalid input counts as 0. */
fun String.toAmount() = replace(',', '.').toDoubleOrNull() ?: 0.0

/** Inverse of [toAmount] for pre-filling a text field (blank for 0, no trailing ".0"). */
fun Double.toFieldText() = if (this == 0.0) "" else toString().removeSuffix(".0")
