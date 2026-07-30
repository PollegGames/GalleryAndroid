package com.polleg.gallery.gallery.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaDateTest {
    @Test
    fun `uses the newest value between taken and added dates`() {
        val date = MediaDate(
            takenAtMillis = 1_000L,
            addedAtMillis = 3_000L,
            modifiedAtMillis = 9_000L,
        )

        assertEquals(3_000L, date.mostRecentMillis())
    }

    @Test
    fun `ignores modification while taken or added date exists`() {
        val date = MediaDate(
            takenAtMillis = 2_000L,
            addedAtMillis = null,
            modifiedAtMillis = 9_000L,
        )

        assertEquals(2_000L, date.mostRecentMillis())
    }

    @Test
    fun `uses modification only as a fallback`() {
        val date = MediaDate(
            takenAtMillis = 0L,
            addedAtMillis = null,
            modifiedAtMillis = 7_000L,
        )

        assertEquals(7_000L, date.mostRecentMillis())
    }
}
