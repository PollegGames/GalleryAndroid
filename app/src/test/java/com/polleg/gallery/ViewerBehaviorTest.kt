package com.polleg.gallery

import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerBehaviorTest {
    @Test
    fun `zoom scale stays between one and five`() {
        assertEquals(1f, ZoomBounds.clampScale(0.4f), 0f)
        assertEquals(2.5f, ZoomBounds.clampScale(2.5f), 0f)
        assertEquals(5f, ZoomBounds.clampScale(8f), 0f)
    }

    @Test
    fun `zoom offset is reset at base scale`() {
        assertEquals(
            Offset.Zero,
            ZoomBounds.clampOffset(
                offset = Offset(400f, -300f),
                scale = 1f,
                viewportSize = IntSize(1_000, 600),
            ),
        )
    }

    @Test
    fun `zoom offset cannot leave the scaled viewport`() {
        assertEquals(
            Offset(500f, -300f),
            ZoomBounds.clampOffset(
                offset = Offset(4_000f, -3_000f),
                scale = 2f,
                viewportSize = IntSize(1_000, 600),
            ),
        )
    }

    @Test
    fun `external viewer accepts only image view intents`() {
        assertTrue(ExternalImageIntentPolicy.accepts(Intent.ACTION_VIEW, "image/jpeg"))
        assertTrue(ExternalImageIntentPolicy.accepts(Intent.ACTION_VIEW, "image/heic"))
        assertFalse(ExternalImageIntentPolicy.accepts(Intent.ACTION_VIEW, "video/mp4"))
        assertFalse(ExternalImageIntentPolicy.accepts(Intent.ACTION_SEND, "image/png"))
        assertFalse(ExternalImageIntentPolicy.accepts(Intent.ACTION_VIEW, null))
    }
}
