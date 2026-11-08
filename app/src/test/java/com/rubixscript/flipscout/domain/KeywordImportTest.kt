package com.rubixscript.flipscout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordImportTest {
    private fun k(q: String, g: String = "") = ParsedKeyword(q, g)

    @Test fun parsesPlainKeywordsObjectAsUngrouped() {
        assertEquals(listOf(k("nintendo switch"), k("sony walkman")),
            parseKeywords("""{"keywords": ["nintendo switch", "sony walkman"]}"""))
    }

    @Test fun parsesCategoriesFormatFromThePrompt() {
        val reply = """{"categories": {"Retro gaming": ["gameboy color", "snes console"], "Audio": ["sony walkman"]}}"""
        assertEquals(
            listOf(k("gameboy color", "Retro gaming"), k("snes console", "Retro gaming"), k("sony walkman", "Audio")),
            parseKeywords(reply),
        )
    }

    @Test fun parsesBareCategoryMapAndFencedReplyWithChatter() {
        val reply = "Sure! Here you go:\n```json\n{\"Tools\": [\"dewalt drill\"]}\n```\nGood luck!"
        assertEquals(listOf(k("dewalt drill", "Tools")), parseKeywords(reply))
    }

    @Test fun parsesObjectsWithCategoryField() {
        assertEquals(listOf(k("ps5 controller", "Gaming")),
            parseKeywords("""[{"query": "ps5 controller", "category": "Gaming", "note": "ignored"}]"""))
    }

    @Test fun parsesBareArray() {
        assertEquals(listOf(k("ps5 controller"), k("Gameboy Color")), parseKeywords("""["ps5 controller", "Gameboy Color"]"""))
    }

    @Test fun dedupesIgnoringCaseKeepingFirstAndSkipsBlanks() {
        assertEquals(listOf(k("Lego Set", "A")), parseKeywords("""{"A": ["Lego Set"], "B": ["lego set", "  ", ""]}"""))
    }

    @Test fun invalidInputGivesEmpty() {
        assertTrue(parseKeywords("no json here").isEmpty())
        assertTrue(parseKeywords("{ broken").isEmpty())
    }

    @Test fun promptAsksForCategorisedJsonAndIncludesTopic() {
        val p = keywordsPrompt("retro gaming")
        assertTrue(p.contains("Focus on: retro gaming"))
        assertTrue(p.contains("{\"categories\""))
    }
}
