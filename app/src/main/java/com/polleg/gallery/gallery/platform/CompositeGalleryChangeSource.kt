package com.polleg.gallery.gallery.platform

import com.polleg.gallery.gallery.application.GalleryChangeSource
import com.polleg.gallery.gallery.domain.GalleryEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

class CompositeGalleryChangeSource(
    private vararg val sources: GalleryChangeSource,
) : GalleryChangeSource {
    override val events: Flow<GalleryEvent> = merge(*sources.map { it.events }.toTypedArray())
}
