package com.polleg.gallery.gallery.platform

import com.polleg.gallery.gallery.domain.MediaDate
import com.polleg.gallery.gallery.domain.MediaItem
import com.polleg.gallery.gallery.domain.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSharePolicyTest {
    @Test
    fun `single media keeps its exact mime type`() {
        assertEquals(
            "video/mp4",
            MediaSharePolicy.mimeType(listOf(media(MediaKind.Video, "video/mp4"))),
        )
    }

    @Test
    fun `several images use image wildcard`() {
        assertEquals(
            "image/*",
            MediaSharePolicy.mimeType(
                listOf(
                    media(MediaKind.Image, "image/jpeg"),
                    media(MediaKind.Image, "image/png"),
                ),
            ),
        )
    }

    @Test
    fun `mixed selection uses generic mime type`() {
        assertEquals(
            "*/*",
            MediaSharePolicy.mimeType(
                listOf(
                    media(MediaKind.Image, "image/jpeg"),
                    media(MediaKind.Video, "video/mp4"),
                ),
            ),
        )
    }

    private fun media(kind: MediaKind, mimeType: String) = MediaItem(
        id = "$kind:$mimeType",
        mediaStoreId = 1L,
        contentUri = "content://media/$kind/1",
        displayName = "media",
        mimeType = mimeType,
        kind = kind,
        volumeName = "external_primary",
        relativePath = "DCIM/",
        dates = MediaDate(null, null, null),
        durationMillis = null,
        width = null,
        height = null,
    )
}
