package com.polleg.gallery.editor

import org.junit.Assert.*
import org.junit.Test

class EditModelsTest {
    @Test fun `original crop preserves all pixels`() {
        assertEquals(CropBounds(0, 0, 4000, 3000), cropBounds(4000, 3000, PhotoEdits()))
    }
    @Test fun `square can move to either edge without escaping image`() {
        assertEquals(CropBounds(0, 0, 3000, 3000), cropBounds(4000, 3000, PhotoEdits(ratio = 1f, horizontal = 0f)))
        assertEquals(CropBounds(1000, 0, 3000, 3000), cropBounds(4000, 3000, PhotoEdits(ratio = 1f, horizontal = 1f)))
    }
    @Test fun `zoom and portrait ratio remain within tiny or panoramic images`() {
        for ((w, h) in listOf(1 to 1, 12000 to 80, 80 to 12000, 4032 to 3024)) {
            for (ratio in listOf(null, 1f, 9f / 16f, 16f / 9f)) {
                val crop = cropBounds(w, h, PhotoEdits(ratio = ratio, zoom = 4f, horizontal = 1f, vertical = 1f))
                assertTrue(crop.width > 0 && crop.height > 0)
                assertTrue(crop.left >= 0 && crop.top >= 0)
                assertTrue(crop.left + crop.width <= w && crop.top + crop.height <= h)
            }
        }
    }
    @Test fun `video requires a nonempty interval inside source`() {
        assertTrue(VideoEdits(100, 900).isValid(1000))
        assertFalse(VideoEdits(900, 100).isValid(1000))
        assertFalse(VideoEdits(0, 1001).isValid(1000))
        assertFalse(VideoEdits(-1, 900).isValid(1000))
        assertFalse(VideoEdits(100, 100).isValid(1000))
    }
}
