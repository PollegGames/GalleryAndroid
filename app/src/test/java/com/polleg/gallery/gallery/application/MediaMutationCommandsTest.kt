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
    fun `move rejects a selection spanning phone and sd card`() = runTest {
        val repository = RecordingMutationRepository()
        val handler = MoveMediaHandler(repository)

        val failure = runCatching {
            handler.handle(
                MoveMediaCommand(
                    media = listOf(media("external_primary", 1), media("1234-ABCD", 2)),
                    destination = FolderId.of("external_primary", "Pictures/"),
                ),
            )
        }.exceptionOrNull()
        assertTrue(failure is InvalidMoveException)
        assertEquals(emptyList<Pair<String, String>>(), repository.moves)
    }

    @Test
    fun `move never invokes a copy operation`() = runTest {
        val repository: MediaMutationRepository = RecordingMutationRepository()

        MoveMediaHandler(repository).handle(
            MoveMediaCommand(
                media = listOf(media("external_primary", 1)),
                destination = FolderId.of("external_primary", "Pictures/"),
            ),
        )

        // MediaMutationRepository intentionally exposes update and delete only:
        // a stream-based copy cannot be requested by the command.
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

    private class RecordingMutationRepository(
        private val failingUris: Set<String> = emptySet(),
    ) : MediaMutationRepository {
        val moves = mutableListOf<Pair<String, String>>()

        override suspend fun delete(contentUri: String): Boolean = true

        override suspend fun move(contentUri: String, destinationRelativePath: String): Boolean {
            moves += contentUri to destinationRelativePath
            return contentUri !in failingUris
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
