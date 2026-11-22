package com.lawliet.project

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

data class Order(val id: String, val url: String?)

class AppVM(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("lawliet", Context.MODE_PRIVATE)
    private var token: String? = prefs.getString("token", null)

    var user by mutableStateOf(prefs.getString("user", null)?.let { runCatching { parseUser(JSONObject(it)) }.getOrNull() })
        private set
    val history = mutableStateListOf<HistoryItem>()
    val senders = mutableStateListOf<Sender>()
    val globalSenders = mutableStateListOf<Sender>() // hanya terisi untuk Founder
    val chat = mutableStateListOf<ChatMsg>()
    private var chatId = 0L

    // ---------- LawBlox (tampilan kedua bertema Roblox) ----------
    val lb = LbStore(prefs)
    var lawblox by mutableStateOf(prefs.getBoolean("lawblox", false))
        private set
    private fun syncTheme() { LbTheme.on = lawblox && user != null }
    fun updateLawblox(on: Boolean) {
        lawblox = on; prefs.edit().putBoolean("lawblox", on).apply(); syncTheme()
        log(if (on) "Beralih ke tampilan LawBlox" else "Kembali ke tampilan original")
    }

    init {
        runCatching {
            val a = JSONArray(prefs.getString("history", "[]"))
            for (i in 0 until a.length()) a.getJSONObject(i).let { history += HistoryItem(it.getString("t"), it.getLong("s")) }
            readSenders("senders", senders); readSenders("gsenders", globalSenders)
        }
        chat += ChatMsg(chatId++, "Moderator Bot", "Global Chat aktif. Jaga bahasa ya, pesan melanggar akan dihapus otomatis.", system = true)
        syncTheme()
    }

    private fun readSenders(key: String, into: MutableList<Sender>) {
        val s = JSONArray(prefs.getString(key, "[]"))
        for (i in 0 until s.length()) s.getJSONObject(i).let { into += Sender(it.getString("id"), it.getString("n"), it.getBoolean("c")) }
    }
    private fun writeSenders(list: List<Sender>): String {
        val s = JSONArray(); list.forEach { s.put(JSONObject().put("id", it.id).put("n", it.number).put("c", it.connected)) }
        return s.toString()
    }
    private fun parseUser(o: JSONObject) = User(
        o.getString("username"),
        runCatching { Role.valueOf(o.optString("role", "MEMBER")) }.getOrDefault(Role.MEMBER),
        o.optInt("activeUsers", 1),
        o.optString("expired", "31/12/2099"),
    )
    private fun saveUser(u: User) {
        user = u; syncTheme()
        prefs.edit().putString("user", JSONObject().put("username", u.username).put("role", u.role.name)
            .put("activeUsers", u.activeUsers).put("expired", u.expired).toString()).apply()
    }
    private fun saveLists() {
        val h = JSONArray(); history.take(100).forEach { h.put(JSONObject().put("t", it.text).put("s", it.time)) }
        prefs.edit().putString("history", h.toString()).putString("senders", writeSenders(senders))
            .putString("gsenders", writeSenders(globalSenders)).apply()
    }

    fun log(text: String) { history.add(0, HistoryItem(text, System.currentTimeMillis())); saveLists() }

    /** POST ke backend. Kalau sesi habis (code AUTH) otomatis logout. */
    private suspend fun call(path: String, body: JSONObject = JSONObject()): JSONObject {
        val r = Api.post(path, body, token)
        if (r.optString("code") == "AUTH") logout()
        return r
    }

    // ---------- Auth ----------
    suspend fun login(u: String, p: String): String? {
        if (u.isBlank() || p.isBlank()) return "Isi username dan password"
        return try {
            if (BuildConfig.DEMO) {
                if (u == "founder" && p == "founder") saveUser(User(u, Role.FOUNDER)) // akun demo lokal
                else {
                    if (prefs.getString("pw_$u", null) != p) return "Username atau password salah"
                    saveUser(User(u, runCatching { Role.valueOf(prefs.getString("role_$u", "MEMBER")!!) }.getOrDefault(Role.MEMBER)))
                }
            } else {
                val r = Api.post("/auth/login", JSONObject().put("username", u).put("password", p))
                if (!r.optBoolean("ok")) return r.optString("message", "Gagal login")
                token = r.optString("token"); prefs.edit().putString("token", token).apply()
                saveUser(parseUser(r.getJSONObject("user")))
                senders.clear(); globalSenders.clear()
                refreshSenders(false)
                if (user?.role == Role.FOUNDER) refreshSenders(true)
            }
            log("Login berhasil"); null
        } catch (e: Exception) { "Gagal terhubung: ${e.message}" }
    }

    fun logout() {
        prefs.edit().remove("user").remove("token").apply(); token = null; user = null; lb.playing = null; syncTheme()
        senders.clear(); globalSenders.clear(); saveLists()
    }

    suspend fun refreshUser() {
        if (BuildConfig.DEMO || token == null) return
        runCatching {
            val r = call("/me")
            if (r.optBoolean("ok")) saveUser(parseUser(r.getJSONObject("user")))
        }
    }

    suspend fun changeUsername(n: String): String? {
        val u = user ?: return "Belum login"
        if (n.length < 3) return "Username minimal 3 karakter"
        return try {
            if (!BuildConfig.DEMO) {
                val r = call("/account/username", JSONObject().put("username", n))
                if (!r.optBoolean("ok")) return r.optString("message", "Gagal")
                r.optString("token").takeIf { it.isNotEmpty() }?.let { token = it; prefs.edit().putString("token", it).apply() }
            } else {
                val pw = prefs.getString("pw_${u.username}", "") ?: ""
                prefs.edit().remove("pw_${u.username}").putString("pw_$n", pw).putString("role_$n", u.role.name).apply()
            }
            saveUser(u.copy(username = n)); log("Mengganti username menjadi $n"); null
        } catch (e: Exception) { "Gagal terhubung: ${e.message}" }
    }

    suspend fun changePassword(old: String, new: String): String? {
        val u = user ?: return "Belum login"
        if (new.length < 6) return "Password baru minimal 6 karakter"
        return try {
            if (BuildConfig.DEMO) {
                if (prefs.getString("pw_${u.username}", null) != old) return "Password lama salah"
                prefs.edit().putString("pw_${u.username}", new).apply()
            } else {
                val r = call("/account/password", JSONObject().put("old", old).put("new", new))
                if (!r.optBoolean("ok")) return r.optString("message", "Gagal")
            }
            log("Mengganti password"); null
        } catch (e: Exception) { "Gagal terhubung: ${e.message}" }
    }

    // ---------- Beli akun / Up role (Sayabayar) ----------
    suspend fun createOrder(username: String, password: String, role: Role): Result<Order> = runCatching {
        if (username.length < 3) error("Username minimal 3 karakter")
        if (password.length < 6) error("Password minimal 6 karakter")
        if (BuildConfig.DEMO) {
            if (prefs.contains("pw_$username") || username == "founder") error("Username sudah dipakai")
            prefs.edit().putString("pw_$username", password).putString("role_$username", role.name).apply()
            return@runCatching Order("demo", null)
        }
        val r = Api.post("/order/create", JSONObject().put("username", username).put("password", password).put("role", role.name))
        if (!r.optBoolean("ok")) error(r.optString("message", "Gagal membuat pesanan"))
        Order(r.getString("orderId"), r.optString("paymentUrl"))
    }

    suspend fun createUpgrade(role: Role): Result<Order> = runCatching {
        if (BuildConfig.DEMO) {
            user?.let { saveUser(it.copy(role = role)) }
            return@runCatching Order("demo", null)
        }
        val r = call("/order/upgrade", JSONObject().put("role", role.name))
        if (!r.optBoolean("ok")) error(r.optString("message", "Gagal membuat pesanan"))
        Order(r.getString("orderId"), r.optString("paymentUrl"))
    }

    /** pending | paid | expired | cancelled | failed */
    suspend fun orderStatus(id: String): String {
        if (id == "demo") return "paid"
        return runCatching {
            val r = Api.post("/order/status", JSONObject().put("orderId", id))
            if (r.optBoolean("ok")) r.optString("status", "pending") else "pending"
        }.getOrDefault("pending")
    }

    // ---------- Founder: buat akun gratis ----------
    suspend fun founderCreateUser(username: String, password: String, role: Role): String? {
        if (user?.role != Role.FOUNDER) return "Khusus Founder"
        if (username.length < 3) return "Username minimal 3 karakter"
        if (password.length < 6) return "Password minimal 6 karakter"
        return try {
            if (BuildConfig.DEMO) {
                if (prefs.contains("pw_$username")) return "Username sudah dipakai"
                prefs.edit().putString("pw_$username", password).putString("role_$username", role.name).apply()
            } else {
                val r = call("/founder/users/create", JSONObject().put("username", username).put("password", password).put("role", role.name))
                if (!r.optBoolean("ok")) return r.optString("message", "Gagal")
            }
            log("Membuat akun $username (${role.label}) gratis"); null
        } catch (e: Exception) { "Gagal terhubung: ${e.message}" }
    }

    // ---------- Sender (Baileys pairing). global = sender global khusus Founder ----------
    private fun listFor(global: Boolean) = if (global) globalSenders else senders
    private fun prefix(global: Boolean) = if (global) "/founder/sender" else "/sender"

    suspend fun refreshSenders(global: Boolean) {
        if (BuildConfig.DEMO || token == null) return
        if (global && user?.role != Role.FOUNDER) return
        runCatching {
            val r = call("${prefix(global)}/list"); if (!r.optBoolean("ok")) return
            val a = r.getJSONArray("senders"); val target = listFor(global)
            val fresh = (0 until a.length()).map { a.getJSONObject(it).let { o -> Sender(o.getString("id"), o.getString("number"), o.optBoolean("connected")) } }
            if (fresh != target.toList()) { target.clear(); target.addAll(fresh); saveLists() }
        }
    }

    /** Minta kode pairing untuk nomor. Return kode (8 karakter) atau throw. */
    suspend fun requestPairing(number: String, global: Boolean): String {
        if (BuildConfig.DEMO) {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            return (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        }
        val r = call("${prefix(global)}/pair", JSONObject().put("number", number))
        if (!r.optBoolean("ok")) error(r.optString("message", "Gagal membuat kode pairing"))
        return r.getString("code")
    }

    suspend fun addSender(number: String, global: Boolean) {
        if (BuildConfig.DEMO) {
            val l = listFor(global); l.removeAll { it.number == number }
            l.add(0, Sender(System.currentTimeMillis().toString(), number, true))
            saveLists()
        } else refreshSenders(global)
        log("Menambah sender${if (global) " global" else ""} $number")
    }

    suspend fun deleteSender(s: Sender, global: Boolean) {
        if (!BuildConfig.DEMO) runCatching { call("${prefix(global)}/delete", JSONObject().put("id", s.id)) }
        listFor(global).remove(s); saveLists(); log("Menghapus sender${if (global) " global" else ""} ${s.number}")
    }

    // ---------- Confess (logika kirim ada di backend) ----------
    suspend fun sendConfess(group: Boolean, target: String, type: String, globalSender: Boolean): String? {
        if (!group && validatePhone(target) != null) return validatePhone(target)
        if (group && !target.startsWith("https://chat.whatsapp.com/")) return "URL grup harus berupa https://chat.whatsapp.com/..."
        if (BuildConfig.DEMO && !globalSender && senders.none { it.connected }) return "Belum ada sender privat. Tambah di Sender Manager."
        return try {
            if (!BuildConfig.DEMO) {
                val r = call("/confess/send", JSONObject().put("group", group).put("target", target)
                    .put("type", type).put("senderGlobal", globalSender))
                if (!r.optBoolean("ok")) return r.optString("message", "Gagal mengirim")
            }
            log("Mengirim $type ke $target"); null
        } catch (e: Exception) { "Gagal terhubung: ${e.message}" }
    }

    // ---------- Global Chat ----------
    fun sendChat(text: String? = null, image: android.graphics.Bitmap? = null, sticker: Boolean = false) {
        val me = user?.username ?: "Saya"
        if (text != null && Moderator.violates(text)) {
            chat += ChatMsg(chatId++, "Moderator Bot", "Pesan dari $me dihapus karena mengandung kata yang melanggar.", system = true)
            log("Pesan Global Chat diblokir moderator"); return
        }
        chat += ChatMsg(chatId++, me, text, image, sticker, mine = true)
        if (sticker) log("Membuat stiker di Global Chat") else if (image != null) log("Mengunggah foto di Global Chat")
    }
}
