package io.github.showys.mimoandroid

import org.junit.Assert.assertEquals
import org.junit.Test

class PrefsTest {
    @Test
    fun `补协议与结尾斜杠`() {
        assertEquals("http://127.0.0.1:7681/", Prefs.normalizeUrl("127.0.0.1:7681"))
        assertEquals("https://mimo.local/", Prefs.normalizeUrl("https://mimo.local"))
        assertEquals("", Prefs.normalizeUrl("   "))
    }
}
