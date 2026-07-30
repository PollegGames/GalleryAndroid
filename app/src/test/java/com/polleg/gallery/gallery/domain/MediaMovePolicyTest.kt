package com.polleg.gallery.gallery.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaMovePolicyTest {
    @Test
    fun `images and videos can share DCIM or Pictures destinations`() {
        val selection = listOf(
            media(MediaKind.Image, "external_primary", "DCIM/Camera/"),
            media(MediaKind.Video, "external_primary", "Movies/"),
        )

        assertTrue(
            MediaMovePolicy.canMoveTo(
                selection,
                FolderId.of("external_primary", "DCIM/Vacances/"),
            ),
        )
        assertTrue(
            MediaMovePolicy.canMoveTo(
                selection,
                FolderId.of("1234-ABCD", "Pictures/Vacances/"),
            ),
        )
        assertFalse(
            MediaMovePolicy.canMoveTo(
                selection,
                FolderId.of("external_primary", "Movies/Vacances/"),
            ),
        )
    }

    @Test
    fun `videos can move to Movies but images cannot`() {
        val destination = FolderId.of("external_primary", "Movies/Archives/")

        assertTrue(
            MediaMovePolicy.canMoveTo(
                listOf(media(MediaKind.Video, "external_primary", "DCIM/")),
                destination,
            ),
        )
        assertFalse(
            MediaMovePolicy.canMoveTo(
                listOf(media(MediaKind.Image, "external_primary", "DCIM/")),
                destination,
            ),
        )
        assertTrue(
            MediaMovePolicy.defaultTopLevelDirectories(
                listOf(media(MediaKind.Video, "external_primary", "DCIM/")),
            ).contains("Movies"),
        )
        assertFalse(
            MediaMovePolicy.defaultTopLevelDirectories(
                listOf(media(MediaKind.Image, "external_primary", "DCIM/")),
            ).contains("Movies"),
        )
    }

    @Test
    fun `WhatsApp and cross-volume sources use transactional transfer`() {
        val phonePictures = FolderId.of("external_primary", "Pictures/")
        val sdPictures = FolderId.of("1234-ABCD", "Pictures/")
        val whatsapp = media(
            kind = MediaKind.Image,
            volume = "external_primary",
            path = "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/",
        )
        val camera = media(MediaKind.Image, "external_primary", "DCIM/Camera/")

        assertTrue(MediaMovePolicy.requiresTransfer(whatsapp, phonePictures))
        assertTrue(MediaMovePolicy.requiresTransfer(camera, sdPictures))
        assertFalse(MediaMovePolicy.requiresTransfer(camera, phonePictures))
    }

    private fun media(
        kind: MediaKind,
        volume: String,
        path: String,
    ) = MediaItem(
        id = "$volume:$path:${kind.name}",
        mediaStoreId = 1,
        contentUri = "content://media/$volume/${kind.name.lowercase()}/1",
        displayName = if (kind == MediaKind.Image) "image.jpg" else "video.mp4",
        mimeType = if (kind == MediaKind.Image) "image/jpeg" else "video/mp4",
        kind = kind,
        volumeName = volume,
        relativePath = path,
        dates = MediaDate(null, 1, null),
        durationMillis = null,
        width = 100,
        height = 100,
    )
}
