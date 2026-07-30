package com.polleg.gallery.gallery.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderMonogramTest {
    @Test
    fun `creates short and distinct monograms when possible`() {
        val monograms = FolderMonogram.assign(
            listOf("Camera", "Captures", "WhatsApp Images", "WhatsApp Video"),
        )

        assertEquals(monograms.size, monograms.toSet().size)
        assertTrue(monograms.all { it.length in 1..2 })
    }
}
