package de.remk0.flickpick.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatRating(r: Double?): String = r?.let { String.format(Locale.US, "%.1f", it) } ?: "–"

/** 1234567 -> "1.2M votes", 36512 -> "36.5k votes", 870 -> "870 votes". */
fun formatVotes(v: Int?): String {
    if (v == null) return "No votes"
    val n = when {
        v >= 1_000_000 -> trim(v / 1_000_000.0) + "M"
        v >= 1_000 -> trim(v / 1_000.0) + "k"
        else -> v.toString()
    }
    return if (v == 1) "1 vote" else "$n votes"
}

fun formatVotesShort(v: Int): String = when {
    v >= 1_000_000 -> trim(v / 1_000_000.0) + "M"
    v >= 1_000 -> trim(v / 1_000.0) + "k"
    else -> v.toString()
}

private fun trim(d: Double): String {
    val s = String.format(Locale.US, "%.1f", d)
    return if (s.endsWith(".0")) s.dropLast(2) else s
}

private val generatedFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)

fun formatGeneratedAt(iso: String, zone: ZoneId = ZoneId.systemDefault()): String =
    runCatching { generatedFormatter.format(Instant.parse(iso).atZone(zone)) }.getOrDefault(iso)
