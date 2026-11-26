package com.qust.schedule.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    // ---------------- compareVersions ----------------

    @Test
    fun `identical versions return zero`() {
        assertEquals(0, UpdateChecker.compareVersions("1.1.0", "1.1.0"))
        assertEquals(0, UpdateChecker.compareVersions("v1.1.0", "1.1.0"))
        assertEquals(0, UpdateChecker.compareVersions("V2.0", "v2.0.0"))
    }

    @Test
    fun `newer remote version returns positive`() {
        assertTrue(UpdateChecker.compareVersions("v1.2.0", "1.1.0") > 0)
        assertTrue(UpdateChecker.compareVersions("v2.0.0", "1.9.9") > 0)
        assertTrue(UpdateChecker.compareVersions("1.10.0", "1.9.0") > 0)
        assertTrue(UpdateChecker.compareVersions("1.1.1", "1.1") > 0)
    }

    @Test
    fun `older remote version returns negative`() {
        assertTrue(UpdateChecker.compareVersions("v1.0.9", "1.1.0") < 0)
        assertTrue(UpdateChecker.compareVersions("1.1", "1.1.1") < 0)
    }

    @Test
    fun `numeric segments compared as numbers not strings`() {
        // "10" 若按字符串比较会小于 "9"，必须按数值比较
        assertTrue(UpdateChecker.compareVersions("1.10.0", "1.9.0") > 0)
        assertTrue(UpdateChecker.compareVersions("v10.0.0", "v9.9.9") > 0)
    }

    @Test
    fun `pre-release suffix treated as base version`() {
        assertEquals(0, UpdateChecker.compareVersions("1.2.0-beta", "1.2.0"))
        assertTrue(UpdateChecker.compareVersions("1.2.0-beta", "1.1.0") > 0)
    }

    @Test
    fun `whitespace and missing segments tolerated`() {
        assertEquals(0, UpdateChecker.compareVersions(" v1.1.0 ", "1.1"))
        assertTrue(UpdateChecker.compareVersions("", "0.0.1") < 0)
    }

    // ---------------- extractTagFromUrl ----------------

    @Test
    fun `extracts tag from absolute redirect location`() {
        assertEquals(
            "v1.2.0",
            UpdateChecker.extractTagFromUrl("https://github.com/lazyfreeman/QUST-Schedule/releases/tag/v1.2.0")
        )
    }

    @Test
    fun `extracts tag from relative redirect location`() {
        assertEquals(
            "v1.2.0",
            UpdateChecker.extractTagFromUrl("/lazyfreeman/QUST-Schedule/releases/tag/v1.2.0")
        )
    }

    @Test
    fun `extracts tag from mirror proxied url`() {
        assertEquals(
            "v2.0.1",
            UpdateChecker.extractTagFromUrl("https://ghfast.top/https://github.com/lazyfreeman/QUST-Schedule/releases/tag/v2.0.1")
        )
    }

    @Test
    fun `extracts tag embedded in html and stops at quote`() {
        assertEquals(
            "v1.3.0",
            UpdateChecker.extractTagFromUrl("<a href=\"/lazyfreeman/QUST-Schedule/releases/tag/v1.3.0\">latest</a>")
        )
    }

    @Test
    fun `returns null when no tag present`() {
        assertNull(UpdateChecker.extractTagFromUrl("https://github.com/lazyfreeman/QUST-Schedule/releases"))
        assertNull(UpdateChecker.extractTagFromUrl(""))
    }

    // ---------------- mirrorUrl ----------------

    @Test
    fun `mirror url prepends prefix to github asset url`() {
        val github = "https://github.com/lazyfreeman/QUST-Schedule/releases/download/v1.2.0/QUST-Schedule-v1.2.0.apk"
        assertEquals(
            "https://gh-proxy.com/$github",
            UpdateChecker.mirrorUrl(github, "https://gh-proxy.com/")
        )
        assertEquals(
            UpdateChecker.MIRROR_PREFIXES.first() + github,
            UpdateChecker.mirrorUrl(github)
        )
    }
}
