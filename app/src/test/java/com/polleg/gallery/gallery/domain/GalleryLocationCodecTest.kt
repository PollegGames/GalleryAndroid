package com.polleg.gallery.gallery.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryLocationCodecTest {
    @Test
    fun `round trips a storage root folder`() {
        val location = GalleryLocation.Folder(
            FolderId.of("external_primary", ""),
        )

        assertEquals(
            location,
            GalleryLocationCodec.decode(GalleryLocationCodec.encode(location)),
        )
    }

    @Test
    fun `round trips a nested folder with spaces`() {
        val location = GalleryLocation.Folder(
            FolderId.of("1234-ABCD", "My Pictures/Family 2026/"),
        )

        assertEquals(
            location,
            GalleryLocationCodec.decode(GalleryLocationCodec.encode(location)),
        )
    }
}
