package com.wheretoeat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A tampered feed must not be able to make the app open anything but a normal https web page. */
class FeedSafetyTest {
    private fun feedWith(url: String, image: String) = """
        {"updated":"2026-10-08T00:00:00+00:00","banks":{},"cards":{},
         "promos":[{"id":"x","bank":"Metrobank","title":"50% OFF at Test","cards":[],
                    "url":"$url","image":"$image","branches":[]}]}
    """.trimIndent()

    private fun first(url: String, image: String = "https://img.example/a.png") = parseFeed(feedWith(url, image)).promos.single()

    @Test fun keepsHttpsLinks() {
        assertEquals("https://www.metrobank.com.ph/promos/x", first("https://www.metrobank.com.ph/promos/x").url)
    }

    @Test fun dropsOtherSchemes() {
        listOf(
            "intent://scan/#Intent;scheme=zxing;end",
            "javascript:alert(1)",
            "file:///sdcard/secret.txt",
            "content://com.android.contacts/contacts",
            "http://insecure.example/promo",
            "market://details?id=evil",
            "https:///no-host",
        ).forEach { assertEquals("blocked: $it", "", first(it).url) }
    }

    @Test fun dropsUnsafeImages() {
        assertNull(first("https://ok.example", image = "file:///data/data/com.wheretoeat/x").image)
        assertNull(first("https://ok.example", image = "http://img.example/a.png").image)
        assertEquals("https://img.example/a.png", first("https://ok.example").image)
    }
}
