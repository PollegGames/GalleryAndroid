package com.polleg.gallery.gallery.ui

object GallerySelection {
    data class ClickResult(
        val selectedUris: Set<String>,
        val openMedia: Boolean,
    )

    data class BackResult(
        val clearSelection: Boolean,
        val navigateBack: Boolean,
    )

    data class MutationResult(
        val selectedUris: Set<String>,
        val refreshMedia: Boolean,
        val refreshFolders: Boolean,
    )

    fun onLongClick(selectedUris: Set<String>, contentUri: String): Set<String> =
        selectedUris + contentUri

    fun onClick(selectedUris: Set<String>, contentUri: String): ClickResult {
        if (selectedUris.isEmpty()) return ClickResult(selectedUris, openMedia = true)
        return ClickResult(
            selectedUris = selectedUris.toMutableSet().apply {
                if (!add(contentUri)) remove(contentUri)
            },
            openMedia = false,
        )
    }

    fun onBack(selectedUris: Set<String>, hasNavigationHistory: Boolean): BackResult =
        BackResult(
            clearSelection = selectedUris.isNotEmpty(),
            navigateBack = selectedUris.isEmpty() && hasNavigationHistory,
        )

    fun onMutationCompleted(selectedUris: Set<String>, succeeded: Boolean): MutationResult =
        MutationResult(
            selectedUris = if (succeeded) emptySet() else selectedUris,
            refreshMedia = succeeded,
            refreshFolders = succeeded,
        )
}
