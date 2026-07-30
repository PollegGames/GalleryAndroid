package com.polleg.gallery.gallery.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GallerySelectionTest {
    @Test
    fun `long press starts selection and clicks toggle several media`() {
        var selection = GallerySelection.onLongClick(emptySet(), "content://media/1")
        selection = GallerySelection.onClick(selection, "content://media/2").selectedUris
        selection = GallerySelection.onClick(selection, "content://media/1").selectedUris

        assertEquals(setOf("content://media/2"), selection)
    }

    @Test
    fun `ordinary click still asks to open media outside selection`() {
        val result = GallerySelection.onClick(emptySet(), "content://media/1")

        assertTrue(result.openMedia)
        assertTrue(result.selectedUris.isEmpty())
    }

    @Test
    fun `back clears selection before navigation`() {
        val result = GallerySelection.onBack(
            selectedUris = setOf("content://media/1"),
            hasNavigationHistory = true,
        )

        assertTrue(result.clearSelection)
        assertFalse(result.navigateBack)
    }

    @Test
    fun `accepted deletion clears selection and requests a full refresh`() {
        val result = GallerySelection.onMutationCompleted(
            selectedUris = setOf("content://media/1", "content://media/2"),
            succeeded = true,
        )

        assertTrue(result.selectedUris.isEmpty())
        assertTrue(result.refreshMedia)
        assertTrue(result.refreshFolders)
    }
}
