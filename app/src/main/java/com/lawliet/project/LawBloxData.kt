package com.lawliet.project

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Flag tema global: true = palet gelap ala Roblox (dipakai juga oleh layar Whatsapp/Tools lama). */
object LbTheme { var on by mutableStateOf(false) }

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

data class Experience(
    val id: Int, val title: String, val creator: String, val desc: String,
    val kind: Int,            // 0 obby spiral, 1 baseplate, 2 kota, 3 menara
    val sky: Long, val ground: Long, val accent: Long,
    val likePct: Int, val playing: Int, val visits: Long, val maxPlayers: Int,
)

data class LbUser(val name: String, val followers: Int, val likes: Int, val bio: String)

data class LbChat(val name: String, val text: String, val color: Color, val system: Boolean = false)

val LB_EXPERIENCES = listOf(
    Experience(1, "Lawliet Obby Tower", "Lawliet", "Lompati platform spiral setinggi mungkin. Jangan jatuh!", 0, 0xFF6EC6FF, 0xFF4CAF50, 0xFFE53935, 94, 18420, 5_320_000, 12),
    Experience(2, "Stud City", "BlokKota", "Jalan-jalan di kota penuh stud dan gedung warna-warni.", 2, 0xFFFFB86B, 0xFF607D8B, 0xFFFDD835, 91, 12980, 3_870_000, 16),
    Experience(3, "Baseplate Chill", "SantaiDev", "Baseplate polos untuk nongkrong bareng teman.", 1, 0xFF9BE7FF, 0xFF8D8D8D, 0xFF1E88E5, 88, 9210, 2_140_000, 20),
    Experience(4, "Sky Stairs", "TanggaLangit", "Naik tangga melingkar sampai puncak menara.", 3, 0xFF7C4DFF, 0xFF37474F, 0xFFAB47BC, 90, 7740, 1_960_000, 10),
    Experience(5, "Neon Parkour", "ParkourKu", "Parkour malam hari dengan platform neon.", 0, 0xFF1A1A40, 0xFF263238, 0xFF00E5FF, 87, 6120, 1_410_000, 12),
    Experience(6, "Pulau Kubus", "KubusKu", "Pulau blok kecil, cari spot terbaik untuk foto.", 1, 0xFF81D4FA, 0xFF66BB6A, 0xFFFF7043, 85, 4310, 980_000, 14),
    Experience(7, "Blok Arena", "ArenaKu", "Arena terbuka dengan banyak blok untuk dipanjat.", 1, 0xFFFFCC80, 0xFFA1887F, 0xFFEF5350, 83, 3560, 760_000, 16),
    Experience(8, "Escape Tower", "KaburDev", "Panjat menara dan kabur dari bawah!", 3, 0xFF455A64, 0xFF212121, 0xFFFFA726, 92, 3180, 720_000, 8),
    Experience(9, "Kota Pixel", "PixelKu", "Kota kecil bergaya pixel, banyak gedung untuk dipanjat.", 2, 0xFFB39DDB, 0xFF78909C, 0xFF26A69A, 80, 2740, 540_000, 16),
    Experience(10, "Lintasan Pelangi", "PelangiDev", "Lintasan platform pelangi, lompat satu per satu.", 0, 0xFFE1F5FE, 0xFF9CCC65, 0xFFEC407A, 89, 2210, 410_000, 12),
)

val LB_USERS = listOf(
    LbUser("Lawliet", 98_400, 412_000, "Pembuat Lawliet Obby Tower"),
    LbUser("BlokKota", 61_250, 233_000, "Arsitek kota stud"),
    LbUser("SantaiDev", 40_870, 150_000, "Santai tapi produktif"),
    LbUser("TanggaLangit", 35_300, 121_000, "Naik terus sampai langit"),
    LbUser("ParkourKu", 28_940, 98_000, "Parkour setiap hari"),
    LbUser("KubusKu", 21_100, 70_500, "Kubus itu seni"),
    LbUser("ArenaKu", 17_800, 61_200, "Siap tanding"),
    LbUser("KaburDev", 15_460, 52_300, "Jangan sampai ketangkap"),
    LbUser("PixelKu", 12_930, 40_100, "Pixel perfect"),
    LbUser("PelangiDev", 9_870, 33_400, "Semua warna ada"),
)

fun compact(n: Long): String = when {
    n >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", n / 1_000.0)
    else -> n.toString()
}

/** Status teman (simulasi lokal sampai backend LawBlox tersedia). */
fun friendStatus(name: String): String {
    val h = kotlin.math.abs(name.hashCode())
    return if (h % 3 == 0) "Offline" else "Online • ${LB_EXPERIENCES[h % LB_EXPERIENCES.size].title}"
}

/** Penyimpanan LawBlox: teman, follow, like. Disimpan di SharedPreferences. */
class LbStore(private val prefs: SharedPreferences) {
    val friends = mutableStateListOf<String>().also { it.addAll(prefs.getStringSet("lb_friends", emptySet())!!.sorted()) }
    val following = mutableStateListOf<String>().also { it.addAll(prefs.getStringSet("lb_following", emptySet())!!) }
    val liked = mutableStateListOf<Int>().also { l -> prefs.getStringSet("lb_liked", emptySet())!!.mapNotNull { it.toIntOrNull() }.forEach { l.add(it) } }
    var playing by mutableStateOf<Experience?>(null)

    private fun save() {
        prefs.edit().putStringSet("lb_friends", friends.toSet()).putStringSet("lb_following", following.toSet())
            .putStringSet("lb_liked", liked.map { it.toString() }.toSet()).apply()
    }

    /** Return pesan error atau null jika berhasil. */
    fun addFriend(name: String, me: String): String? {
        val n = name.trim()
        return when {
            n.length < 3 -> "Username minimal 3 karakter"
            n.equals(me, true) -> "Tidak bisa menambah diri sendiri"
            friends.any { it.equals(n, true) } -> "$n sudah jadi temanmu"
            else -> { friends.add(n); friends.sort(); save(); null }
        }
    }
    fun removeFriend(name: String) { friends.remove(name); save() }
    fun toggleFollow(name: String) { if (!following.remove(name)) following.add(name); save() }
    fun toggleLike(id: Int) { if (!liked.remove(id)) liked.add(id); save() }
}
