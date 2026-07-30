package com.polleg.gallery.gallery.domain

import java.util.Base64

enum class MediaKind {
    Image,
    Video,
}

data class MediaDate(
    val takenAtMillis: Long?,
    val addedAtMillis: Long?,
    val modifiedAtMillis: Long?,
) {
    fun mostRecentMillis(): Long {
        val taken = takenAtMillis.validTimestamp()
        val added = addedAtMillis.validTimestamp()

        return when {
            taken != null || added != null -> maxOf(taken ?: 0L, added ?: 0L)
            else -> modifiedAtMillis.validTimestamp() ?: 0L
        }
    }

    private fun Long?.validTimestamp(): Long? = this?.takeIf { it > 0L }
}

data class MediaItem(
    val id: String,
    val mediaStoreId: Long,
    val contentUri: String,
    val displayName: String,
    val mimeType: String,
    val kind: MediaKind,
    val volumeName: String,
    val relativePath: String,
    val dates: MediaDate,
    val durationMillis: Long?,
    val width: Int?,
    val height: Int?,
) {
    val sortTimestampMillis: Long = dates.mostRecentMillis()
}

class FolderId private constructor(
    val volumeName: String,
    val relativePath: String,
) {
    val stableKey: String
        get() = "${volumeName.toBase64Url()}.${relativePath.toBase64Url()}"

    fun child(segment: String): FolderId {
        val childPath = buildString {
            append(relativePath)
            append(segment.trim('/'))
            append('/')
        }
        return of(volumeName, childPath)
    }

    fun isSameOrParentOf(other: FolderId): Boolean =
        volumeName == other.volumeName && other.relativePath.startsWith(relativePath)

    override fun equals(other: Any?): Boolean =
        other is FolderId &&
            volumeName == other.volumeName &&
            relativePath == other.relativePath

    override fun hashCode(): Int = 31 * volumeName.hashCode() + relativePath.hashCode()

    override fun toString(): String = stableKey

    companion object {
        fun of(volumeName: String, relativePath: String): FolderId {
            require(volumeName.isNotBlank()) { "A MediaStore volume name is required." }
            return FolderId(
                volumeName = volumeName,
                relativePath = relativePath.normalizedRelativePath(),
            )
        }

        fun fromStableKey(value: String): FolderId? {
            val separator = value.indexOf('.')
            if (separator <= 0) return null

            return runCatching {
                of(
                    volumeName = value.substring(0, separator).fromBase64Url(),
                    relativePath = value.substring(separator + 1).fromBase64Url(),
                )
            }.getOrNull()
        }
    }
}

sealed interface GalleryLocation {
    data object Recent : GalleryLocation
    data class Folder(val id: FolderId) : GalleryLocation
}

object GalleryLocationCodec {
    private const val RecentKey = "recent"
    private const val FolderPrefix = "folder:"

    fun encode(location: GalleryLocation): String = when (location) {
        GalleryLocation.Recent -> RecentKey
        is GalleryLocation.Folder -> FolderPrefix + location.id.stableKey
    }

    fun decode(value: String?): GalleryLocation {
        if (value == null || value == RecentKey) return GalleryLocation.Recent
        if (!value.startsWith(FolderPrefix)) return GalleryLocation.Recent

        return FolderId.fromStableKey(value.removePrefix(FolderPrefix))
            ?.let(GalleryLocation::Folder)
            ?: GalleryLocation.Recent
    }
}

enum class StorageKind {
    Phone,
    SdCard,
}

enum class StorageAvailability {
    Available,
    Unavailable,
}

data class StorageVolume(
    val mediaStoreName: String,
    val displayName: String,
    val kind: StorageKind,
    val availability: StorageAvailability,
)

data class FolderPathRecord(
    val volumeName: String,
    val relativePath: String,
)

data class MediaFolder(
    val id: FolderId,
    val name: String,
    val directMediaCount: Int,
    val totalMediaCount: Int,
    val children: List<MediaFolder>,
    val storage: StorageVolume?,
) {
    val hasChildren: Boolean = children.isNotEmpty()
    val isStorageRoot: Boolean = storage != null
}

data class MediaPage(
    val items: List<MediaItem>,
    val hasMore: Boolean,
)

data class GalleryPreferences(
    val pinnedFolderKeys: Set<String> = emptySet(),
    val navigationCollapsed: Boolean = false,
)

data class ScrollPosition(
    val itemIndex: Int = 0,
    val itemOffset: Int = 0,
    val anchorContentUri: String? = null,
    val restorationGeneration: Int = 0,
)

sealed interface GalleryEvent {
    data object MediaLibraryChanged : GalleryEvent
    data object StorageAvailabilityChanged : GalleryEvent
}

private fun String.normalizedRelativePath(): String {
    val segments = replace('\\', '/')
        .split('/')
        .filter { it.isNotBlank() && it != "." }

    require(segments.none { it == ".." }) { "A relative media path cannot contain '..'." }
    return if (segments.isEmpty()) "" else segments.joinToString(separator = "/", postfix = "/")
}

private fun String.toBase64Url(): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(Charsets.UTF_8))

private fun String.fromBase64Url(): String =
    String(Base64.getUrlDecoder().decode(this), Charsets.UTF_8)
