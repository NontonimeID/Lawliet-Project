package com.lawliet.project

import android.graphics.Bitmap

const val ADMIN_WA = "6281234567890"                 // GANTI: nomor admin untuk beli akun / up role
const val TELEGRAM_URL = "https://t.me/lawlietproject" // GANTI: link grup Telegram
const val APK_URL = "https://example.com/lawliet-project.apk" // GANTI: link download APK terbaru

enum class Role(val label: String) { MEMBER("Member"), VIP("VIP"), RESELLER("Reseller"), NEO("Neo"), OWNER("Owner"), FOUNDER("Founder") }

data class RoleInfo(val role: Role, val price: Int)

// Founder tidak dijual (hanya milik pembuat aplikasi), jadi tidak ada di daftar harga.
val ROLE_PRICES = listOf(
    RoleInfo(Role.MEMBER, 5000), RoleInfo(Role.VIP, 7000), RoleInfo(Role.RESELLER, 10000),
    RoleInfo(Role.NEO, 15000), RoleInfo(Role.OWNER, 50000),
)

fun rupiah(v: Int) = "Rp " + String.format(java.util.Locale.US, "%,d", v)

data class User(
    val username: String,
    val role: Role = Role.MEMBER,
    val activeUsers: Int = 1,
    val expired: String = "31/12/2099", // role permanen selalu tampil 31/12/2099
)

data class Sender(val id: String, val number: String, val connected: Boolean)
data class HistoryItem(val text: String, val time: Long)
data class NewsItem(val title: String, val desc: String, val image: String?, val link: String)
data class ChatMsg(
    val id: Long, val user: String, val text: String? = null, val image: Bitmap? = null,
    val sticker: Boolean = false, val mine: Boolean = false, val system: Boolean = false,
)
