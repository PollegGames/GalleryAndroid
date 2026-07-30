package com.polleg.gallery.gallery.domain

/**
 * Rules imposed by MediaStore for destinations created by a regular third-party
 * application. Images can be written below DCIM or Pictures; videos can also be
 * written below Movies.
 */
object MediaMovePolicy {
    fun defaultTopLevelDirectories(media: List<MediaItem>): List<String> {
        if (media.isEmpty()) return emptyList()
        return buildList {
            add("DCIM")
            add("Pictures")
            if (media.all { it.kind == MediaKind.Video }) add("Movies")
        }
    }

    fun canMoveTo(
        media: List<MediaItem>,
        destination: FolderId,
    ): Boolean {
        if (media.isEmpty() || destination.relativePath.isBlank()) return false

        val topLevelDirectory = destination.relativePath
            .substringBefore('/')
            .lowercase()

        return defaultTopLevelDirectories(media)
            .any { it.equals(topLevelDirectory, ignoreCase = true) }
    }

    fun containsCompatibleDestination(
        folder: MediaFolder,
        media: List<MediaItem>,
    ): Boolean =
        canMoveTo(media, folder.id) ||
            folder.children.any { containsCompatibleDestination(it, media) }

    /**
     * MediaStore cannot change VOLUME_NAME. It can also refuse to move media out
     * of another application's Android/media directory, even after write consent.
     * Both cases therefore need a transactional transfer followed by deletion.
     */
    fun requiresTransfer(
        media: MediaItem,
        destination: FolderId,
    ): Boolean =
        media.volumeName != destination.volumeName ||
            media.relativePath.startsWith("Android/", ignoreCase = true)
}
