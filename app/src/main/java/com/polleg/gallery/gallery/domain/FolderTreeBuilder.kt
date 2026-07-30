package com.polleg.gallery.gallery.domain

class FolderTreeBuilder {
    fun build(
        records: List<FolderPathRecord>,
        storageVolumes: List<StorageVolume>,
    ): List<MediaFolder> {
        val volumesByName = storageVolumes.associateBy(StorageVolume::mediaStoreName)
        val roots = linkedMapOf<String, MutableFolder>()

        records.forEach { record ->
            val storage = volumesByName[record.volumeName] ?: fallbackVolume(record.volumeName)
            val root = roots.getOrPut(record.volumeName) {
                MutableFolder(
                    id = FolderId.of(record.volumeName, ""),
                    name = storage.displayName,
                    storage = storage,
                )
            }

            val segments = FolderId.of(record.volumeName, record.relativePath)
                .relativePath
                .split('/')
                .filter(String::isNotBlank)

            val target = segments.fold(root) { parent, segment ->
                parent.children.getOrPut(segment) {
                    MutableFolder(
                        id = parent.id.child(segment),
                        name = segment,
                        storage = null,
                    )
                }
            }
            target.directMediaCount += 1
        }

        return roots.values
            .map(MutableFolder::freeze)
            .filter { it.totalMediaCount > 0 }
            .sortedWith(
                compareBy<MediaFolder> { it.storage?.kind != StorageKind.Phone }
                    .thenBy { it.name.lowercase() },
            )
    }

    private fun fallbackVolume(name: String): StorageVolume =
        StorageVolume(
            mediaStoreName = name,
            displayName = if (name == "external_primary") "Téléphone" else "Carte SD",
            kind = if (name == "external_primary") StorageKind.Phone else StorageKind.SdCard,
            availability = StorageAvailability.Available,
        )

    private class MutableFolder(
        val id: FolderId,
        val name: String,
        val storage: StorageVolume?,
        var directMediaCount: Int = 0,
        val children: MutableMap<String, MutableFolder> = linkedMapOf(),
    ) {
        fun freeze(): MediaFolder {
            val immutableChildren = children.values
                .map(MutableFolder::freeze)
                .filter { it.totalMediaCount > 0 }
                .sortedBy { it.name.lowercase() }
            val total = directMediaCount + immutableChildren.sumOf(MediaFolder::totalMediaCount)

            return MediaFolder(
                id = id,
                name = name,
                directMediaCount = directMediaCount,
                totalMediaCount = total,
                children = immutableChildren,
                storage = storage,
            )
        }
    }
}

fun List<MediaFolder>.findFolder(id: FolderId): MediaFolder? {
    forEach { folder ->
        if (folder.id == id) return folder
        folder.children.findFolder(id)?.let { return it }
    }
    return null
}

fun List<MediaFolder>.flattenFolders(): List<MediaFolder> = buildList {
    fun addRecursively(folder: MediaFolder) {
        add(folder)
        folder.children.forEach(::addRecursively)
    }
    this@flattenFolders.forEach(::addRecursively)
}
