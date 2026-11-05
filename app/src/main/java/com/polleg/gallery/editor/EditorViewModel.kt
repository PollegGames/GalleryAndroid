package com.polleg.gallery.editor

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    var source by mutableStateOf<Bitmap?>(null); private set
    var photo by mutableStateOf(PhotoEdits())
    var video by mutableStateOf(VideoEdits())
    var durationMs by mutableStateOf(0L); private set
    var loading by mutableStateOf(true); private set
    var saving by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var savedUri by mutableStateOf<Uri?>(null); private set
    private var sourceUri: Uri? = null
    private var isVideo = false
    private var exportJob: Job? = null

    fun load(uri: Uri, videoInput: Boolean) {
        if (sourceUri != null) return
        sourceUri = uri
        isVideo = videoInput
        viewModelScope.launch {
            try {
                if (videoInput) {
                    durationMs = withContext(Dispatchers.IO) {
                        MediaMetadataRetriever().use { retriever ->
                            retriever.setDataSource(getApplication(), uri)
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        }
                    }
                    check(durationMs > 0) { "Durée de la vidéo indisponible." }
                    video = VideoEdits(endMs = durationMs)
                } else {
                    source = withContext(Dispatchers.IO) { MediaEditor.decode(getApplication(), uri, 1200) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "Impossible de lire ce média." }
            finally { loading = false }
        }
    }

    fun reset() {
        photo = PhotoEdits()
        video = VideoEdits(endMs = durationMs)
        error = null
    }

    fun save() {
        val uri = sourceUri ?: return
        if (saving || loading || (isVideo && !video.isValid(durationMs)) || (!isVideo && source == null)) return
        saving = true
        error = null
        val photoEdits = photo
        val videoEdits = video
        exportJob = viewModelScope.launch {
            try {
                savedUri = if (isVideo) MediaEditor.saveVideo(getApplication(), uri, videoEdits)
                    else MediaEditor.savePhoto(getApplication(), uri, photoEdits)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                error = "Échec de l’enregistrement. Vérifiez l’espace disponible ou essayez un autre format. ${failure.message.orEmpty()}"
            } finally { saving = false }
        }
    }
}
