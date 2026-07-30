package com.polleg.gallery.gallery.domain

class MediaPageAssembler {
    fun assemble(
        candidateBatches: List<List<MediaItem>>,
        desiredCount: Int,
    ): MediaPage {
        require(desiredCount > 0) { "The desired media count must be positive." }

        val sorted = candidateBatches
            .flatten()
            .associateBy(MediaItem::contentUri)
            .values
            .sortedWith(
                compareByDescending<MediaItem>(MediaItem::sortTimestampMillis)
                    .thenBy(MediaItem::volumeName)
                    .thenByDescending(MediaItem::mediaStoreId),
            )

        return MediaPage(
            items = sorted.take(desiredCount),
            hasMore = sorted.size > desiredCount,
        )
    }
}
