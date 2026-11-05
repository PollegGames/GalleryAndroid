package com.polleg.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.polleg.gallery.editor.*
import com.polleg.gallery.gallery.domain.MediaItem
import com.polleg.gallery.gallery.domain.MediaKind
import com.polleg.gallery.gallery.ui.theme.GalleryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class EditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.data ?: run { finish(); return }
        val video = intent.getBooleanExtra("video", false)
        enableEdgeToEdge()
        setContent {
            GalleryTheme {
                val model: EditorViewModel = viewModel()
                LaunchedEffect(uri) { model.load(uri, video) }
                DisposableEffect(model.saving) {
                    if (model.saving) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    onDispose { window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
                }
                LaunchedEffect(model.savedUri) {
                    if (model.savedUri != null) {
                        Toast.makeText(this@EditorActivity, "Copie enregistrée dans ${if (video) "Movies" else "Pictures"}/Galerie", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
                EditorScreen(model, uri, video, ::finish)
            }
        }
    }

    companion object {
        fun intent(context: Context, item: MediaItem): Intent = Intent(context, EditorActivity::class.java)
            .setData(Uri.parse(item.contentUri))
            .putExtra("video", item.kind == MediaKind.Video)
    }
}

@Composable
private fun EditorScreen(model: EditorViewModel, uri: Uri, video: Boolean, close: () -> Unit) {
    var confirmClose by remember { mutableStateOf(false) }
    val dirty = if (video) model.video != VideoEdits(endMs = model.durationMs) else model.photo != PhotoEdits()
    val requestClose = { if (dirty || model.saving) confirmClose = true else close() }
    BackHandler { requestClose() }
    if (confirmClose) {
        AlertDialog(
            onDismissRequest = { confirmClose = false },
            title = { Text(if (model.saving) "Enregistrement en cours" else "Quitter l’éditeur ?") },
            text = { Text(if (model.saving) "Gardez cet écran ouvert jusqu’à la fin de l’enregistrement." else "Les modifications non enregistrées seront perdues.") },
            confirmButton = { TextButton(onClick = { confirmClose = false; if (!model.saving) close() }) { Text(if (model.saving) "Continuer" else "Quitter") } },
            dismissButton = { if (!model.saving) TextButton(onClick = { confirmClose = false }) { Text("Rester") } },
        )
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = requestClose) { Text("Fermer") }
                Text(if (video) "Modifier la vidéo" else "Modifier la photo", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = model::reset, enabled = !model.saving && !model.loading) { Text("Réinitialiser") }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (model.loading) {
                    CircularProgressIndicator()
                } else if (video && model.durationMs > 0) {
                    VideoEditorPreview(uri, model.video, model.saving)
                    Text("Extrait : ${editTime(model.video.startMs)} → ${editTime(model.video.endMs)}")
                    RangeSlider(
                        value = model.video.startMs.toFloat()..model.video.endMs.toFloat(),
                        onValueChange = { range ->
                            if (range.endInclusive - range.start >= minOf(100f, model.durationMs.toFloat())) {
                                model.video = model.video.copy(startMs = range.start.toLong(), endMs = range.endInclusive.toLong())
                            }
                        },
                        valueRange = 0f..model.durationMs.toFloat(), enabled = !model.saving,
                    )
                    Text("Déplacez les deux poignées pour choisir le début et la fin.", style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = model.video.muted, onCheckedChange = { model.video = model.video.copy(muted = it) }, enabled = !model.saving)
                        Text("Supprimer le son")
                    }
                    TextButton(onClick = { model.video = model.video.copy(rotation = (model.video.rotation + 90) % 360) }, enabled = !model.saving) {
                        Text("Rotation : ${model.video.rotation}°")
                    }
                    Text("Copie MP4 dans Movies/Galerie. L’original est conservé.", style = MaterialTheme.typography.bodySmall)
                } else if (model.source != null) {
                    PhotoControls(model)
                }
                model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            if (model.saving) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Enregistrement de la copie…", Modifier.padding(12.dp))
            }
            Button(
                onClick = model::save,
                enabled = !model.saving && !model.loading && (if (video) model.video.isValid(model.durationMs) else model.source != null),
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            ) { Text("Enregistrer une copie") }
        }
    }
}

@Composable
private fun PhotoControls(model: EditorViewModel) {
    val source = model.source ?: return
    val edits = model.photo
    var preview by remember(source) { mutableStateOf(source) }
    var compare by remember { mutableStateOf(false) }
    LaunchedEffect(source, edits) {
        delay(100)
        preview = withContext(Dispatchers.Default) { MediaEditor.render(source, edits) }
    }
    Image(
        bitmap = (if (compare) source else preview).asImageBitmap(),
        contentDescription = if (compare) "Photo originale" else "Aperçu de la retouche",
        modifier = Modifier.fillMaxWidth().height(270.dp), contentScale = ContentScale.Fit,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = compare, onCheckedChange = { compare = it })
        Text("Voir l’original")
    }
    val enabled = !model.saving
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        TextButton(onClick = { model.photo = edits.copy(rotation = (edits.rotation + 90) % 360) }, enabled = enabled) { Text("Rotation ${edits.rotation}°") }
        FilterChip(selected = edits.mirror, onClick = { model.photo = edits.copy(mirror = !edits.mirror) }, label = { Text("Miroir") }, enabled = enabled)
    }
    Text("Recadrage", style = MaterialTheme.typography.titleSmall)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Original" to null, "Carré" to 1f, "4:3" to 4f/3f, "3:4" to 3f/4f, "16:9" to 16f/9f, "9:16" to 9f/16f).forEach { (label, ratio) ->
            FilterChip(selected = edits.ratio == ratio, onClick = { model.photo = edits.copy(ratio = ratio) }, label = { Text(label) }, enabled = enabled)
        }
    }
    EditSlider("Zoom du recadrage", edits.zoom, 1f..4f, enabled) { model.photo = edits.copy(zoom = it) }
    EditSlider("Position horizontale", edits.horizontal, 0f..1f, enabled) { model.photo = edits.copy(horizontal = it) }
    EditSlider("Position verticale", edits.vertical, 0f..1f, enabled) { model.photo = edits.copy(vertical = it) }
    Text("Couleurs", style = MaterialTheme.typography.titleSmall)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PhotoStyle.entries.forEach { style ->
            FilterChip(selected = edits.style == style, onClick = { model.photo = edits.copy(style = style) }, label = { Text(style.label) }, enabled = enabled)
        }
    }
    EditSlider("Luminosité", edits.brightness, -.5f.. .5f, enabled) { model.photo = edits.copy(brightness = it) }
    EditSlider("Contraste", edits.contrast, .5f..1.5f, enabled) { model.photo = edits.copy(contrast = it) }
    EditSlider("Saturation", edits.saturation, 0f..2f, enabled) { model.photo = edits.copy(saturation = it) }
    Text("Copie dans Pictures/Galerie, jusqu’à 4096 pixels sur le côté le plus long selon la mémoire disponible. Les images animées deviennent une photo fixe. L’original est conservé.", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun EditSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean, change: (Float) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    Slider(value = value, onValueChange = change, valueRange = range, enabled = enabled)
}

@Composable
private fun VideoEditorPreview(uri: Uri, edits: VideoEdits, saving: Boolean) {
    var view by remember { mutableStateOf<VideoView?>(null) }
    var player by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var previewError by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    var active by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setMediaController(MediaController(context).also { it.setAnchorView(this) })
                setOnPreparedListener { prepared -> player = prepared; seekTo(edits.startMs.toInt()) }
                setOnErrorListener { _, _, _ -> previewError = true; true }
                setVideoURI(uri)
                view = this
            }
        },
        update = { it.rotation = -edits.rotation.toFloat() },
        modifier = Modifier.fillMaxWidth().height(250.dp),
    )
    if (previewError) Text("Aperçu indisponible pour ce format. Vous pouvez essayer d’enregistrer la copie.")
    LaunchedEffect(edits.startMs, edits.endMs) { view?.seekTo(edits.startMs.toInt()) }
    LaunchedEffect(player, edits.muted) { player?.setVolume(if (edits.muted) 0f else 1f, if (edits.muted) 0f else 1f) }
    LaunchedEffect(saving, active) { if (saving || !active) view?.pause() }
    LaunchedEffect(view, edits.startMs, edits.endMs, active, saving) {
        while (active && !saving) {
            val current = view
            if (current != null && current.isPlaying && (current.currentPosition >= edits.endMs || current.currentPosition < edits.startMs)) {
                current.pause()
                current.seekTo(edits.startMs.toInt())
            }
            delay(100)
        }
    }
    TextButton(enabled = !saving && active && !previewError, onClick = { view?.seekTo(edits.startMs.toInt()); view?.start() }) { Text("Lire l’extrait") }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) { active = false; view?.pause() }
            if (event == Lifecycle.Event.ON_RESUME) active = true
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); view?.stopPlayback(); player = null }
    }
}

private fun editTime(ms: Long): String = "%d:%02d.%01d".format(ms / 60000, ms / 1000 % 60, ms / 100 % 10)
