package com.polleg.gallery.gallery.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentGallerySystemTest {
    private val assembler = MediaPageAssembler()

    @Test
    fun `opening gallery returns the ten newest images and videos across phone and sd card`() {
        val phoneItems = (1L..7L).map { index ->
            media(
                id = index,
                volume = "external_primary",
                kind = if (index % 2L == 0L) MediaKind.Video else MediaKind.Image,
                timestamp = index * 1_000L,
            )
        }
        val sdItems = (8L..14L).map { index ->
            media(
                id = index,
                volume = "1234-ABCD",
                kind = if (index % 2L == 0L) MediaKind.Image else MediaKind.Video,
                timestamp = index * 1_000L,
            )
        }
        val allItems = phoneItems + sdItems

        val page = assembler.assemble(
            candidateBatches = listOf(
                allItems.sortedByDescending { it.dates.takenAtMillis },
                allItems.sortedByDescending { it.dates.addedAtMillis },
                emptyList(),
            ),
            desiredCount = 10,
        )

        assertEquals((14L downTo 5L).toList(), page.items.map(MediaItem::mediaStoreId))
        assertTrue(page.items.any { it.volumeName == "external_primary" })
        assertTrue(page.items.any { it.volumeName == "1234-ABCD" })
        assertTrue(page.items.any { it.kind == MediaKind.Image })
        assertTrue(page.items.any { it.kind == MediaKind.Video })
        assertTrue(page.hasMore)
    }

    private fun media(
        id: Long,
        volume: String,
        kind: MediaKind,
        timestamp: Long,
    ): MediaItem = MediaItem(
        id = "$volume:$id",
        mediaStoreId = id,
        contentUri = "content://media/$volume/$id",
        displayName = "$id",
        mimeType = if (kind == MediaKind.Video) "video/mp4" else "image/jpeg",
        kind = kind,
        volumeName = volume,
        relativePath = "DCIM/",
        dates = MediaDate(
            takenAtMillis = timestamp,
            addedAtMillis = timestamp - 100L,
            modifiedAtMillis = timestamp + 50_000L,
        ),
        durationMillis = if (kind == MediaKind.Video) 10_000L else null,
        width = 1_080,
        height = 1_920,
    )
}
