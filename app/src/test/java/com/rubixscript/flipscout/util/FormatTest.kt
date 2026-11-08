package com.rubixscript.flipscout.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun moneyFormatsSign() {
        assertEquals("$5.00", money(5.0))
        assertEquals("-$2.50", money(-2.5))
    }

    @Test fun amountParsingToleratesCommaAndGarbage() {
        assertEquals(12.5, "12,5".toAmount(), 0.001)
        assertEquals(0.0, "abc".toAmount(), 0.001)
    }

    @Test fun fieldTextRoundTrips() {
        assertEquals("", 0.0.toFieldText())
        assertEquals("20", 20.0.toFieldText())
        assertEquals("19.99", 19.99.toFieldText())
    }

    @Test fun formatDateUsesLocalDay() {
        val text = formatDate(0)
        assert(Regex("""^\d{1,2} [A-Za-z]{3} \d{4}$""").matches(text)) // e.g. "1 Jan 1970"
    }
}
