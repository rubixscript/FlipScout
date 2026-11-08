package com.rubixscript.flipscout.domain

/** A ready-made set of reseller search keywords for one area of interest. The pack name becomes the keyword category. */
data class KeywordPack(val name: String, val keywords: List<String>)

val KEYWORD_PACKS: List<KeywordPack> = listOf(
    KeywordPack("Electronics", listOf(
        "sony walkman", "bose quietcomfort", "apple watch lot", "ipad mini", "jbl speaker",
        "garmin gps watch", "kindle paperwhite", "airpods pro")),
    KeywordPack("Video Games", listOf(
        "nintendo switch lot", "gameboy advance", "ps2 console bundle", "sega genesis games",
        "n64 controller", "xbox 360 lot", "retro game lot", "pokemon games lot")),
    KeywordPack("Cameras", listOf(
        "canon ae-1", "nikon fm2", "polaroid sx-70", "film camera lot", "gopro hero",
        "fujifilm instax", "olympus om-1", "camera lens vintage")),
    KeywordPack("Clothing", listOf(
        "vintage levis 501", "patagonia fleece", "carhartt jacket", "north face puffer", "vintage band tee",
        "pendleton wool", "ralph lauren polo lot", "lululemon lot")),
    KeywordPack("Shoes", listOf(
        "nike dunk low", "air jordan 1", "new balance 990", "adidas samba", "vintage nike",
        "red wing boots", "dr martens boots", "birkenstock")),
    KeywordPack("Jewelry & Watches", listOf(
        "sterling silver lot", "vintage seiko", "citizen eco-drive", "turquoise ring", "gold filled chain",
        "pandora charm lot", "costume jewelry lot", "vintage watch parts")),
    KeywordPack("Toys", listOf(
        "lego star wars sealed", "lego retired set", "vintage star wars figures", "hot wheels lot",
        "barbie vintage", "funko pop lot", "transformers g1", "he-man figures")),
    KeywordPack("Collectibles", listOf(
        "pokemon card lot", "baseball card lot", "magic the gathering lot", "vintage comic lot",
        "coin lot silver", "vinyl record lot", "stamp collection", "sports memorabilia")),
    KeywordPack("Home", listOf(
        "kitchenaid stand mixer", "cast iron skillet", "vintage pyrex", "le creuset", "dyson vacuum",
        "vitamix blender", "corelle lot", "yeti cooler")),
    KeywordPack("Furniture", listOf(
        "mid century modern", "herman miller chair", "ikea lack lot", "vintage dresser",
        "danish teak", "rattan chair", "solid wood desk", "floor lamp vintage")),
    KeywordPack("Tools", listOf(
        "dewalt drill bare tool", "milwaukee m18", "makita cordless", "snap-on wrench", "craftsman tool set",
        "stanley hand plane", "festool", "dremel kit")),
    KeywordPack("Sports", listOf(
        "titleist driver", "yeti tumbler lot", "fishing reel shimano", "hockey skates", "bike components shimano",
        "camping stove", "golf club set", "snowboard bindings")),
    KeywordPack("Music & Audio", listOf(
        "boss guitar pedal", "vintage turntable", "marantz receiver", "yamaha keyboard", "shure microphone",
        "fender squier", "vintage speakers", "cassette deck")),
    KeywordPack("Books & Media", listOf(
        "first edition book lot", "textbook lot", "vintage cookbook lot", "dvd box set lot",
        "blu-ray steelbook", "criterion collection", "manga lot", "board game sealed")),
)

private fun matchesInterest(pack: KeywordPack, interests: Collection<String>): Boolean {
    val name = pack.name.lowercase()
    return interests.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        .any { it == name || name.contains(it) || it.contains(name) }
}

/** Packs whose name matches one of the user's own item categories come first ("for you"); order is otherwise unchanged. */
fun rankPacks(packs: List<KeywordPack>, interests: Collection<String>): List<KeywordPack> =
    packs.filter { matchesInterest(it, interests) } + packs.filterNot { matchesInterest(it, interests) }

/** True when [pack] matches one of the user's own item categories (the rule [rankPacks] uses). */
fun isForUser(pack: KeywordPack, interests: Collection<String>): Boolean = matchesInterest(pack, interests)

/** The keywords from [packs] still worth adding: drops ones already in the library (case-insensitive) and repeats across packs. */
fun keywordsToAdd(packs: List<KeywordPack>, existing: Collection<String>): List<ParsedKeyword> {
    val seen = existing.map { it.trim().lowercase() }.toMutableSet()
    return packs.flatMap { pack -> pack.keywords.map { ParsedKeyword(it, pack.name) } }
        .filter { seen.add(it.query.lowercase()) }
}
