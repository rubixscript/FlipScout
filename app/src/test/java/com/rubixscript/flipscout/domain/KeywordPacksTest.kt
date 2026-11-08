package com.rubixscript.flipscout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordPacksTest {
    private val a = KeywordPack("Toys", listOf("lego set", "hot wheels"))
    private val b = KeywordPack("Tools", listOf("dewalt drill", "lego set"))
    private val c = KeywordPack("Home", listOf("pyrex"))

    @Test fun builtInPacksAreWellFormed() {
        assertTrue(KEYWORD_PACKS.size >= 10)
        assertEquals(KEYWORD_PACKS.size, KEYWORD_PACKS.map { it.name }.toSet().size)
        KEYWORD_PACKS.forEach { pack ->
            assertTrue(pack.keywords.size >= 6)
            assertEquals(pack.keywords.size, pack.keywords.map { it.lowercase() }.toSet().size)
            assertTrue(pack.keywords.all { it == it.trim() && it.length <= 40 })
        }
    }

    @Test fun userCategoriesRankFirstKeepingOrderOtherwise() {
        assertEquals(listOf("Home", "Toys", "Tools"), rankPacks(listOf(a, b, c), listOf("home")).map { it.name })
    }

    @Test fun noInterestsKeepsOriginalOrder() {
        assertEquals(listOf("Toys", "Tools", "Home"), rankPacks(listOf(a, b, c), emptyList()).map { it.name })
    }

    @Test fun matchesPartialCategoryNames() {
        assertTrue(rankPacks(KEYWORD_PACKS, listOf("Jewelry")).first().name.startsWith("Jewelry"))
    }

    @Test fun forUserFlagFollowsSameRule() {
        assertTrue(isForUser(c, listOf("Home")))
        assertFalse(isForUser(a, listOf("Home")))
        assertFalse(isForUser(a, emptyList()))
    }

    @Test fun skipsExistingAndRepeatedKeywordsCaseInsensitively() {
        val added = keywordsToAdd(listOf(a, b), existing = listOf("Hot Wheels"))
        assertEquals(listOf("lego set", "dewalt drill"), added.map { it.query })
        assertEquals(listOf("Toys", "Tools"), added.map { it.category })
    }
}
