package com.polleg.gallery.gallery.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FolderTreeBuilderTest {
    @Test
    fun `builds recursive folders and rolls counts up to their parents`() {
        val volumes = listOf(
            StorageVolume(
                mediaStoreName = "external_primary",
                displayName = "Téléphone",
                kind = StorageKind.Phone,
                availability = StorageAvailability.Available,
            ),
            StorageVolume(
                mediaStoreName = "1234-ABCD",
                displayName = "Carte SD",
                kind = StorageKind.SdCard,
                availability = StorageAvailability.Available,
            ),
        )
        val records = listOf(
            FolderPathRecord("external_primary", "DCIM/Camera/"),
            FolderPathRecord("external_primary", "DCIM/Camera/"),
            FolderPathRecord("external_primary", "DCIM/Screenshots/"),
            FolderPathRecord("1234-ABCD", "Pictures/Wallpapers/"),
        )

        val roots = FolderTreeBuilder().build(records, volumes)

        assertEquals(2, roots.size)
        val phone = roots.first()
        assertEquals(3, phone.totalMediaCount)
        val dcim = phone.children.single()
        assertEquals(3, dcim.totalMediaCount)
        assertEquals(2, dcim.children.size)
        assertEquals(2, dcim.children.first { it.name == "Camera" }.directMediaCount)

        val sdCard = roots.last()
        assertEquals(1, sdCard.totalMediaCount)
        assertNotNull(
            roots.findFolder(FolderId.of("1234-ABCD", "Pictures/Wallpapers/")),
        )
    }
}
