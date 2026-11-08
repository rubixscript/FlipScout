package com.rubixscript.flipscout.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ItemTest {
    @Test fun profitAndRoi() {
        val i = Item(buyPrice = 20.0, sellPrice = 50.0, fees = 5.0)
        assertEquals(25.0, i.profit, 0.001)
        assertEquals(125.0, i.roi, 0.001)
    }

    @Test fun shippingReducesProfitAndRaisesCostBasis() {
        val i = Item(buyPrice = 20.0, sellPrice = 50.0, fees = 5.0, shipping = 10.0)
        assertEquals(15.0, i.profit, 0.001)
        assertEquals(50.0, i.roi, 0.001) // 15 / (20 + 10)
    }

    @Test fun roiIsZeroWithoutAnyCost() {
        assertEquals(0.0, Item(sellPrice = 10.0).roi, 0.001)
        assertEquals(0.0, Item(sellPrice = 10.0, fees = 2.0).roi, 0.001) // fees are not part of the cost basis
    }

    @Test fun quantityIsInformationalNotArithmetic() {
        // Prices are totals for the purchase; a count of units never changes the math.
        val one = Item(buyPrice = 20.0, sellPrice = 50.0, quantity = 1)
        val three = one.copy(quantity = 3)
        assertEquals(one.profit, three.profit, 0.001)
        assertEquals(one.roi, three.roi, 0.001)
    }

    @Test fun statusPipelineAdvancesUntilSold() {
        assertEquals(Status.BOUGHT, Status.WATCHING.next)
        assertEquals(Status.SOLD, Status.LISTED.next)
        assertNull(Status.SOLD.next)
    }
}
