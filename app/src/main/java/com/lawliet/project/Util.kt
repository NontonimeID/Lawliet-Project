package com.lawliet.project

import android.content.Context
import android.graphics.*
import android.net.Uri

/** Nomor WhatsApp: hanya digit, wajib kode negara (tidak boleh diawali 0), 9-15 digit. */
fun validatePhone(n: String): String? = when {
    n.isEmpty() -> "Nomor tidak boleh kosong"
    !n.all { it.isDigit() } -> "Hanya angka, tanpa +, spasi, atau -"
    n.startsWith("0") -> "Jangan diawali 0, pakai kode negara (contoh 6281234567890)"
    n.length < 9 || n.length > 15 -> "Panjang nomor tidak valid"
    else -> null
}

fun relTime(now: Long, t: Long): String {
    val s = ((now - t) / 1000).coerceAtLeast(0)
    return when {
        s < 60 -> "$s detik lalu"
        s < 3600 -> "${s / 60} menit lalu"
        s < 86400 -> "${s / 3600} jam lalu"
        else -> "${s / 86400} hari lalu"
    }
}

fun decodeUri(ctx: Context, uri: Uri, max: Int = 1024): Bitmap? = runCatching {
    val src = ImageDecoder.createSource(ctx.contentResolver, uri)
    ImageDecoder.decodeBitmap(src) { dec, info, _ ->
        dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val s = max.toFloat() / maxOf(info.size.width, info.size.height)
        if (s < 1f) dec.setTargetSize((info.size.width * s).toInt(), (info.size.height * s).toInt())
    }
}.getOrNull()

/** Foto -> stiker 512x512 (transparan, sudut membulat). */
fun makeSticker(src: Bitmap): Bitmap {
    val size = 512
    val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    val s = minOf(size.toFloat() / src.width, size.toFloat() / src.height)
    val w = src.width * s; val h = src.height * s
    val l = (size - w) / 2; val t = (size - h) / 2
    val r = RectF(l, t, l + w, t + h)
    val path = android.graphics.Path().apply { addRoundRect(r, 56f, 56f, android.graphics.Path.Direction.CW) }
    c.clipPath(path)
    c.drawBitmap(src, null, r, Paint(Paint.FILTER_BITMAP_FLAG))
    return out
}

/** Bot moderator sederhana: cek kata kasar per kata (dengan normalisasi leetspeak). */
object Moderator {
    private val bad = setOf(
        "anjing", "anjir", "bangsat", "babi", "kontol", "memek", "tolol", "goblok", "bajingan",
        "kampret", "ngentot", "jancok", "asu", "brengsek", "fuck", "shit", "bitch", "bastard",
    )
    fun violates(text: String): Boolean {
        val n = text.lowercase().map {
            when (it) { '0' -> 'o'; '1' -> 'i'; '3' -> 'e'; '4' -> 'a'; '5' -> 's'; '@' -> 'a'; '$' -> 's'; else -> it }
        }.joinToString("")
        return n.split(Regex("[^a-z]+")).any { it in bad }
    }
}
