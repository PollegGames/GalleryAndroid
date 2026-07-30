package com.polleg.gallery.gallery.data.preferences

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import com.polleg.gallery.gallery.application.GalleryPreferencesRepository
import com.polleg.gallery.gallery.domain.GalleryPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class JsonGalleryPreferencesRepository(
    context: Context,
    applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : GalleryPreferencesRepository {
    private val dataStore: DataStore<StoredGalleryPreferences> = DataStoreFactory.create(
        serializer = StoredGalleryPreferencesSerializer,
        scope = applicationScope,
        produceFile = { context.dataStoreFile("gallery_preferences.json") },
    )

    override val preferences: Flow<GalleryPreferences> = dataStore.data.map { stored ->
        GalleryPreferences(
            pinnedFolderKeys = stored.pinnedFolderKeys.toSet(),
            navigationCollapsed = stored.navigationCollapsed,
        )
    }

    override suspend fun togglePinnedFolder(folderKey: String) {
        dataStore.updateData { current ->
            val updated = current.pinnedFolderKeys.toMutableSet().apply {
                if (!add(folderKey)) remove(folderKey)
            }
            current.copy(pinnedFolderKeys = updated.sorted())
        }
    }

    override suspend fun setNavigationCollapsed(collapsed: Boolean) {
        dataStore.updateData { current ->
            current.copy(navigationCollapsed = collapsed)
        }
    }
}

@Serializable
private data class StoredGalleryPreferences(
    val pinnedFolderKeys: List<String> = emptyList(),
    val navigationCollapsed: Boolean = false,
)

private object StoredGalleryPreferencesSerializer : Serializer<StoredGalleryPreferences> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    override val defaultValue: StoredGalleryPreferences = StoredGalleryPreferences()

    override suspend fun readFrom(input: InputStream): StoredGalleryPreferences =
        try {
            val bytes = input.readBytes()
            if (bytes.isEmpty()) defaultValue
            else json.decodeFromString(bytes.decodeToString())
        } catch (error: SerializationException) {
            throw CorruptionException("Les préférences de galerie sont illisibles.", error)
        }

    override suspend fun writeTo(
        t: StoredGalleryPreferences,
        output: OutputStream,
    ) {
        output.write(json.encodeToString(t).encodeToByteArray())
    }
}
