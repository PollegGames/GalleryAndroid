package com.polleg.gallery.gallery.domain

import java.text.Normalizer
import java.util.Locale

object FolderMonogram {
    fun assign(labels: List<String>): List<String> {
        val used = mutableSetOf<String>()
        return labels.map { label ->
            candidates(label)
                .firstOrNull(used::add)
                ?: numberedFallback(label, used)
        }
    }

    private fun candidates(label: String): List<String> {
        val words = label.asciiWords()
        val compact = words.joinToString(separator = "")
        if (compact.isEmpty()) return listOf("?")

        return buildList {
            if (words.size > 1) add(words.take(2).joinToString("") { it.take(1) })
            add(compact.take(1))
            add(compact.take(2))
            compact.drop(1).forEach { character -> add("${compact.first()}$character") }
        }.map { it.uppercase(Locale.ROOT) }.distinct()
    }

    private fun numberedFallback(label: String, used: MutableSet<String>): String {
        val first = label.asciiWords()
            .joinToString(separator = "")
            .firstOrNull()
            ?.uppercaseChar()
            ?: '?'

        for (suffix in 2..9) {
            val candidate = "$first$suffix"
            if (used.add(candidate)) return candidate
        }

        return first.toString()
    }

    private fun String.asciiWords(): List<String> =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .split(Regex("[^A-Za-z0-9]+"))
            .filter(String::isNotBlank)
}
