package com.rubixscript.flipscout.domain

import com.rubixscript.flipscout.data.Item
import com.rubixscript.flipscout.data.Status
import org.junit.Assert.assertEquals
import org.junit.Test

class ItemQueryTest {

    private val items = listOf(
        Item(id = "1", title = "Nintendo Switch", notes = "joycon drift", tags = listOf("gaming"), category = "Electronics",
            platform = "eBay", status = Status.SOLD, buyPrice = 100.0, sellPrice = 180.0, createdAt = 30),
        Item(id = "2", title = "Vintage denim jacket", category = "Clothing", platform = "Depop",
            status = Status.WATCHING, buyPrice = 10.0, createdAt = 20),
        Item(id = "3", title = "Stereo receiver", tags = listOf("audio", "vintage"), category = "Electronics",
            status = Status.BOUGHT, buyPrice = 50.0, sellPrice = 120.0, createdAt = 10),
    )

    @Test fun textMatchesTitleNotesCategoryAndTags() {
        assertEquals(listOf("1"), items.query(ItemQuery(text = "switch")).map { it.id })
        assertEquals(listOf("1"), items.query(ItemQuery(text = "DRIFT")).map { it.id })
        assertEquals(listOf("1", "3"), items.query(ItemQuery(text = "electronics")).map { it.id })
        assertEquals(listOf("3"), items.query(ItemQuery(text = "audio")).map { it.id })
        assertEquals(3, items.query(ItemQuery(text = " ")).size) // blank search matches everything
    }

    @Test fun filtersByStatusCategoryAndPlatformTogether() {
        assertEquals(listOf("1"), items.query(ItemQuery(status = Status.SOLD)).map { it.id })
        assertEquals(listOf("1", "3"), items.query(ItemQuery(categories = setOf("Electronics"))).map { it.id })
        assertEquals(listOf("2"), items.query(ItemQuery(platforms = setOf("Depop"))).map { it.id })
        assertEquals(
            listOf("1"),
            items.query(ItemQuery(categories = setOf("Electronics"), platforms = setOf("eBay"))).map { it.id },
        )
    }

    @Test fun emptyFiltersKeepEverything() {
        assertEquals(3, items.query(ItemQuery()).size)
    }

    @Test fun sortsByAgeProfitRoiAndPrice() {
        assertEquals(listOf("1", "2", "3"), items.query(ItemQuery(sort = ItemSort.NEWEST)).map { it.id })
        assertEquals(listOf("3", "2", "1"), items.query(ItemQuery(sort = ItemSort.OLDEST)).map { it.id })
        // Profit: 80 (sold), 60 (projected receiver), 0 (watching jacket).
        assertEquals(listOf("1", "3", "2"), items.query(ItemQuery(sort = ItemSort.PROFIT)).map { it.id })
        // ROI: 140% (receiver, projected), 80%, 0%.
        assertEquals(listOf("3", "1", "2"), items.query(ItemQuery(sort = ItemSort.ROI)).map { it.id })
        // Sell price: 180, 120, 0.
        assertEquals(listOf("1", "3", "2"), items.query(ItemQuery(sort = ItemSort.PRICE)).map { it.id })
    }

    @Test fun distinctCategoriesAndPlatformsSkipBlanksAndSort() {
        assertEquals(listOf("Clothing", "Electronics"), items.distinctCategories())
        assertEquals(listOf("Depop", "eBay"), items.distinctPlatforms())
    }

    @Test fun isFilteringKnowsWhenSomethingIsSet() {
        assert(!ItemQuery().isFiltering)
        assert(ItemQuery(text = "x").isFiltering)
        assert(ItemQuery(status = Status.SOLD).isFiltering)
        assert(ItemQuery(categories = setOf("A")).isFiltering)
    }
}
