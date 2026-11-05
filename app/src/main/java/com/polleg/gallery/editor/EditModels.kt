package com.polleg.gallery.editor

import kotlin.math.roundToInt

// Coordinates are relative to the oriented image, before color adjustments.
data class PhotoEdits(
    val rotation: Int = 0,
    val mirror: Boolean = false,
    val ratio: Float? = null,
    val zoom: Float = 1f,
    val horizontal: Float = 0.5f,
    val vertical: Float = 0.5f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val style: PhotoStyle = PhotoStyle.Natural,
)

enum class PhotoStyle(val label: String) {
    Natural("Naturel"), Mono("Noir et blanc"), Warm("Chaud"), Cool("Froid"), Sepia("Sépia"),
}

data class CropBounds(val left: Int, val top: Int, val width: Int, val height: Int)

fun cropBounds(width: Int, height: Int, edits: PhotoEdits): CropBounds {
    require(width > 0 && height > 0)
    val ratio = edits.ratio ?: (width.toFloat() / height)
    require(ratio.isFinite() && ratio > 0)
    val baseWidth = minOf(width.toFloat(), height * ratio)
    val baseHeight = baseWidth / ratio
    val zoom = edits.zoom.coerceIn(1f, 4f)
    val cropWidth = (baseWidth / zoom).roundToInt().coerceIn(1, width)
    val cropHeight = (baseHeight / zoom).roundToInt().coerceIn(1, height)
    return CropBounds(
        ((width - cropWidth) * edits.horizontal.coerceIn(0f, 1f)).roundToInt(),
        ((height - cropHeight) * edits.vertical.coerceIn(0f, 1f)).roundToInt(),
        cropWidth, cropHeight,
    )
}

data class VideoEdits(
    val startMs: Long = 0,
    val endMs: Long = 0,
    val muted: Boolean = false,
    val rotation: Int = 0,
) {
    fun isValid(durationMs: Long): Boolean =
        startMs >= 0 && endMs <= durationMs && endMs > startMs
}
