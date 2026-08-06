package com.polleg.gallery.gallery.application

import com.polleg.gallery.gallery.domain.FolderId
import com.polleg.gallery.gallery.domain.MediaDate
import com.polleg.gallery.gallery.domain.MediaItem
import com.polleg.gallery.gallery.domain.MediaKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaMutationCommandsTest {
    @Test
    fun `move updates relative path when every item is on the destination volume`() = runTest {
        val repository = RecordingMutationRepository()
        val handler = MoveMediaHandler(repository)

        val result = handler.handle(
            MoveMediaCommand(
                media = listOf(media("external_primary", 1), media("external_primary", 2)),
                destination = FolderId.of("external_primary", "Pictures/Vacances/"),
            ),
        )

        assertEquals(MoveMediaResult(movedCount = 2, failedCount = 0), result)
        assertEquals(
            listOf(
                "content://media/external_primary/1" to "Pictures/Vacances/",
                "content://media/external_primary/2" to "Pictures/Vacances/",
            ),
            repository.moves,
        )
    }

    @Test
    fun `move accepts a destination on another storage volume`() = runTest {
        val repository = RecordingMutationRepository()
        val handler = MoveMediaHandler(repository)

        val result = handler.handle(
            MoveMediaCommand(
                media = listOf(media("external_primary", 1)),
                destination = FolderId.of("1234-ABCD", "Pictures/"),
            ),
        )

        assertEquals(1, result.movedCount)
        assertEquals(
            listOf("content://media/external_primary/1" to "Pictures/"),
            repository.moves,
        )
    }

    @Test
    fun `move port exposes a management operation and no copy command`() = runTest {
        val repository: MediaMutationRepository = RecordingMutationRepository()

        MoveMediaHandler(repository).handle(
            MoveMediaCommand(
                media = listOf(media("external_primary", 1)),
                destination = FolderId.of("external_primary", "Pictures/"),
            ),
        )

        // Any volume transfer is an implementation detail of the move transaction.
        // The application layer still exposes only move and delete operations.
        assertEquals(1, (repository as RecordingMutationRepository).moves.size)
    }

    @Test
    fun `move reports partial results`() = runTest {
        val repository = RecordingMutationRepository(failingUris = setOf("content://media/external_primary/2"))

        val result = MoveMediaHandler(repository).handle(
            MoveMediaCommand(
                media = listOf(media("external_primary", 1), media("external_primary", 2)),
                destination = FolderId.of("external_primary", "Pictures/"),
            ),
        )

        assertEquals(MoveMediaResult(movedCount = 1, failedCount = 1), result)
    }

    @Test
    fun `move rejects a destination that MediaStore cannot use for images`() = runTest {
        val repository = RecordingMutationRepository()

        val failure = runCatching {
            MoveMediaHandler(repository).handle(
                MoveMediaCommand(
                    media = listOf(media("external_primary", 1)),
                    destination = FolderId.of("external_primary", "Android/media/com.whatsapp/"),
                ),
            )
        }.exceptionOrNull()

        assertTrue(failure is InvalidMoveException)
        assertTrue(repository.moves.isEmpty())
    }

    private class RecordingMutationRepository(
        private val failingUris: Set<String> = emptySet(),
    ) : MediaMutationRepository {
        val moves = mutableListOf<Pair<String, String>>()

        override suspend fun delete(contentUri: String): Boolean = true

        override suspend fun move(media: MediaItem, destination: FolderId): Boolean {
            moves += media.contentUri to destination.relativePath
            return media.contentUri !in failingUris
        }
    }

    private fun media(volume: String, id: Long) = MediaItem(
        id = "$volume:$id",
        mediaStoreId = id,
        contentUri = "content://media/$volume/$id",
        displayName = "$id.jpg",
        mimeType = "image/jpeg",
        kind = MediaKind.Image,
        volumeName = volume,
        relativePath = "DCIM/",
        dates = MediaDate(null, id, null),
        durationMillis = null,
        width = 100,
        height = 100,
    )
}
