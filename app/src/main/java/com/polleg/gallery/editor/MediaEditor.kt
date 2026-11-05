package com.polleg.gallery.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

object MediaEditor {
    // Bound decoding memory, including panoramas. ImageDecoder applies EXIF orientation.
    fun decode(context: Context, uri: Uri, maxEdge: Int): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val scale = minOf(1f, maxEdge.toFloat() / maxOf(info.size.width, info.size.height))
            decoder.setTargetSize(
                (info.size.width * scale).roundToInt().coerceAtLeast(1),
                (info.size.height * scale).roundToInt().coerceAtLeast(1),
            )
        }

    fun render(source: Bitmap, edits: PhotoEdits): Bitmap {
        val matrix = Matrix().apply {
            postScale(if (edits.mirror) -1f else 1f, 1f)
            postRotate(edits.rotation.toFloat())
        }
        val oriented = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        val bounds = cropBounds(oriented.width, oriented.height, edits)
        val result = Bitmap.createBitmap(bounds.width, bounds.height, Bitmap.Config.ARGB_8888)
        result.setHasAlpha(source.hasAlpha())
        Canvas(result).drawBitmap(oriented, -bounds.left.toFloat(), -bounds.top.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix(edits))
        })
        if (oriented !== source) oriented.recycle()
        return result
    }

    private fun colorMatrix(edits: PhotoEdits): ColorMatrix {
        val color = ColorMatrix().apply { setSaturation(edits.saturation) }
        val style = when (edits.style) {
            PhotoStyle.Natural -> ColorMatrix()
            PhotoStyle.Mono -> ColorMatrix().apply { setSaturation(0f) }
            PhotoStyle.Warm -> ColorMatrix(floatArrayOf(1.08f,0f,0f,0f,8f, 0f,1f,0f,0f,2f, 0f,0f,.9f,0f,0f, 0f,0f,0f,1f,0f))
            PhotoStyle.Cool -> ColorMatrix(floatArrayOf(.9f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1.08f,0f,8f, 0f,0f,0f,1f,0f))
            PhotoStyle.Sepia -> ColorMatrix(floatArrayOf(.393f,.769f,.189f,0f,0f, .349f,.686f,.168f,0f,0f, .272f,.534f,.131f,0f,0f, 0f,0f,0f,1f,0f))
        }
        color.postConcat(style)
        val c = edits.contrast
        val offset = 128f * (1f - c) + edits.brightness * 255f
        color.postConcat(ColorMatrix(floatArrayOf(c,0f,0f,0f,offset, 0f,c,0f,0f,offset, 0f,0f,c,0f,offset, 0f,0f,0f,1f,0f)))
        return color
    }

    suspend fun savePhoto(context: Context, uri: Uri, edits: PhotoEdits): Uri = withContext(Dispatchers.IO) {
        val source = decode(context, uri, 4096)
        try {
            val output = render(source, edits)
            try {
                coroutineContext.ensureActive()
                val alpha = output.hasAlpha()
                publish(context, false, if (alpha) "image/png" else "image/jpeg", if (alpha) "png" else "jpg") { stream ->
                    check(output.compress(if (alpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 95, stream))
                }
            } finally { output.recycle() }
        } finally { source.recycle() }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    suspend fun saveVideo(context: Context, uri: Uri, edits: VideoEdits): Uri {
        val file = File.createTempFile("gallery-edit-", ".mp4", context.cacheDir)
        try {
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val transformer = Transformer.Builder(context)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                                if (continuation.isActive) continuation.resumeWithException(exportException)
                            }
                        }).build()
                    val input = MediaItem.Builder().setUri(uri).setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder().setStartPositionMs(edits.startMs)
                            .setEndPositionMs(edits.endMs).build(),
                    ).build()
                    val effects = if (edits.rotation == 0) emptyList() else listOf(
                        ScaleAndRotateTransformation.Builder().setRotationDegrees(edits.rotation.toFloat()).build(),
                    )
                    val edited = EditedMediaItem.Builder(input).setRemoveAudio(edits.muted)
                        .setEffects(Effects(emptyList(), effects)).build()
                    continuation.invokeOnCancellation { transformer.cancel() }
                    try { transformer.start(edited, file.absolutePath) }
                    catch (error: Exception) { if (continuation.isActive) continuation.resumeWithException(error) }
                }
            }
            coroutineContext.ensureActive()
            return withContext(Dispatchers.IO) {
                publish(context, true, "video/mp4", "mp4") { output -> file.inputStream().use { it.copyTo(output) } }
            }
        } finally { file.delete() }
    }

    // Pending rows keep incomplete exports out of the gallery. Originals are never written.
    private suspend fun publish(context: Context, video: Boolean, mime: String, extension: String, write: (OutputStream) -> Unit): Uri =
        withContext(NonCancellable + Dispatchers.IO) {
            val resolver = context.contentResolver
            val collection = if (video) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "Galerie_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$extension")
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, if (video) "Movies/Galerie" else "Pictures/Galerie")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val destination = checkNotNull(resolver.insert(collection, values)) { "Impossible de créer la copie." }
            try {
                checkNotNull(resolver.openOutputStream(destination)).use(write)
                check(resolver.update(destination, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null) == 1)
                destination
            } catch (error: Throwable) {
                resolver.delete(destination, null, null)
                throw error
            }
        }
}
