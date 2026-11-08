package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Status
import org.junit.Assert.assertEquals
import org.junit.Test

class SellerStatsTest {

    private val items = listOf(
        Item(id = "a", sellerId = "s1", status = Status.SOLD, buyPrice = 20.0, sellPrice = 50.0, shipping = 5.0),
        Item(id = "b", sellerId = "s1", status = Status.BOUGHT, buyPrice = 30.0),
        Item(id = "c", sellerId = "s1", status = Status.WATCHING, buyPrice = 99.0), // not bought yet
        Item(id = "d", sellerId = "s2", status = Status.SOLD, buyPrice = 10.0, sellPrice = 40.0),
        Item(id = "e", sellerId = "", status = Status.BOUGHT, buyPrice = 1.0), // no seller
    )

    @Test fun countsOnlyPurchasesPastWatching() {
        val stats = sellerStats("s1", items)
        assertEquals(2, stats.itemsBought)
        assertEquals(1, stats.soldCount)
    }

    @Test fun spentIncludesShippingOfPurchases() {
        assertEquals(55.0, sellerStats("s1", items).spent, 0.001)
    }

    @Test fun profitSumsOnlySoldItems() {
        assertEquals(25.0, sellerStats("s1", items).profit, 0.001)
        assertEquals(30.0, sellerStats("s2", items).profit, 0.001)
    }

    @Test fun unknownSellerHasNoStats() {
        assertEquals(SellerStats(), sellerStats("nope", items))
    }
}
