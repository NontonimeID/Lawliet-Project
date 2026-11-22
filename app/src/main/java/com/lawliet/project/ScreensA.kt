package com.lawliet.project

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import coil.compose.AsyncImage
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ---------------- Loading ----------------
/** Video fullscreen; loading bar ditumpuk di dalam video (bukan terpisah), plus tombol Skip. */
@Composable
fun LoadingScreen(onDone: () -> Unit) {
    var dur by remember { mutableLongStateOf(0L) }
    var progress by remember { mutableFloatStateOf(0f) }
    var finished by remember { mutableStateOf(false) }
    fun finish() { if (!finished) { finished = true; onDone() } }
    LaunchedEffect(dur) {
        if (dur > 0) {
            val start = withFrameNanos { it }
            while (progress < 1f) {
                val t = withFrameNanos { it }
                progress = ((t - start) / 1_000_000f / dur).coerceIn(0f, 1f)
            }
            delay(600); finish() // cadangan jika event selesai video tidak terkirim
        }
    }
    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
        RawVideo("loading_screen", Modifier.fillMaxSize(), loop = false, onEnded = { finish() }, onDuration = { dur = it })
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 28.dp)) {
            LinearProgressIndicator(
                progress = { progress }, Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = androidx.compose.ui.graphics.Color.White, trackColor = androidx.compose.ui.graphics.Color(0x55FFFFFF),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            Spacer(Modifier.height(8.dp))
            Text("${(progress * 100).toInt()}%", Modifier.fillMaxWidth(), color = androidx.compose.ui.graphics.Color.White,
                fontFamily = Orbitron, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        TextButton(
            onClick = { finish() }, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp),
            colors = ButtonDefaults.textButtonColors(
                containerColor = androidx.compose.ui.graphics.Color(0x88000000), contentColor = androidx.compose.ui.graphics.Color.White),
        ) { Text("Skip"); Spacer(Modifier.width(6.dp)); Icon(Lucide.SkipForward, null, Modifier.size(16.dp)) }
    }
}

// ---------------- Login ----------------
@Composable
fun AuthScreen(vm: AppVM, onBack: () -> Unit) {
    var u by remember { mutableStateOf("") }; var p by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }; var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    androidx.activity.compose.BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        IconButton(onBack, Modifier.padding(8.dp)) { Icon(Lucide.ArrowLeft, "Kembali") }
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Lawliet Project", fontFamily = Orbitron, fontSize = 26.sp)
            Spacer(Modifier.height(6.dp))
            Text("Masuk ke akunmu", color = Muted, fontSize = 16.sp)
            Spacer(Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(u, { u = it.trim() }, "Username")
                Field(p, { p = it }, "Password", password = true)
                err?.let { Text(it, color = androidx.compose.ui.graphics.Color.Red, fontSize = 14.sp) }
                InkButton("Masuk", Lucide.LogIn, Modifier.fillMaxWidth(), enabled = !busy) {
                    busy = true
                    scope.launch { err = vm.login(u, p); busy = false }
                }
            }
        }
    }
}

// ---------------- Home ----------------
@Composable
fun HomeScreen(vm: AppVM, go: (String) -> Unit) {
    val u = vm.user ?: return
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { RawVideo("banner", Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(20.dp))) }
        item {
            AppCard(Modifier.fillMaxWidth(), onClick = { vm.setLawblox(true) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Play, null, Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Title("Tampilan LawBlox")
                        Text("Ganti ke tampilan bertema Roblox + dunia 3D", color = Muted, fontSize = 15.sp)
                    }
                }
            }
        }
        item { WelcomeCard(u, vm.senders.size) }
        item { PrayerCard() }
        item { QuickActions(go) }
        item { NewsSection() }
        item {
            AppCard(Modifier.fillMaxWidth(), onClick = { go("globalchat") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.MessagesSquare, null, Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Title("Global Chat")
                        Text("Ketuk untuk membuka global chat", color = Muted, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeCard(u: User, senderCount: Int) {
    AppCard(Modifier.fillMaxWidth()) {
        Text("Ahlan Wa Sahlan!", fontFamily = Orbitron, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
                Text(u.username.take(1).uppercase(), color = Paper, fontFamily = Orbitron, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(u.username, fontSize = 20.sp)
                Spacer(Modifier.height(4.dp))
                Text(u.role.label, Modifier.clip(RoundedCornerShape(50)).background(Soft).padding(horizontal = 10.dp, vertical = 2.dp), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat(Lucide.User, "Pengguna Aktif", u.activeUsers.toString(), Modifier.weight(1f))
            Stat(Lucide.Send, "Sender", senderCount.toString(), Modifier.weight(1f))
            Stat(Lucide.CalendarDays, "Expired", u.expired, Modifier.weight(1.2f))
        }
    }
}

@Composable
private fun Stat(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = Muted, fontSize = 13.sp)
        Text(value, fontSize = 16.sp)
    }
}

@Composable
fun PrayerCard() {
    val zone = ZoneId.of("Asia/Jakarta")
    var times by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    LaunchedEffect(Unit) {
        runCatching {
            val d = LocalDate.now(zone).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            val url = "https://api.aladhan.com/v1/timingsByCity/$d?city=Jakarta&country=Indonesia&method=20&timezonestring=Asia/Jakarta"
            val t = JSONObject(Api.get(url)).getJSONObject("data").getJSONObject("timings")
            times = listOf("Subuh" to "Fajr", "Dzuhur" to "Dhuhr", "Ashar" to "Asr", "Maghrib" to "Maghrib", "Isya" to "Isha")
                .map { (n, k) -> n to t.getString(k).take(5) }
        }
    }
    val now by produceState(LocalTime.now(zone)) { while (true) { delay(30_000); value = LocalTime.now(zone) } }
    val next = times?.firstOrNull { LocalTime.parse(it.second) > now }?.first ?: times?.firstOrNull()?.first
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.Clock, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
            Title("Jadwal Sholat"); Spacer(Modifier.weight(1f))
            Text("Jakarta (WIB)", color = Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        val list = times
        if (list == null) Text("Memuat jadwal...", color = Muted)
        else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            list.forEach { (n, t) ->
                val on = n == next
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (on) Ink else Soft).padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(n, color = if (on) Paper else Muted, fontSize = 12.sp, maxLines = 1)
                    Text(t, color = if (on) Paper else Ink, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun QuickActions(go: (String) -> Unit) {
    val items = listOf(
        Triple("Sender Manager", Lucide.Smartphone, "sender"), Triple("Tools Manager", Lucide.Wrench, "tools"),
        Triple("Whatsapp", Lucide.MessageCircle, "whatsapp"), Triple("Global Chat", Lucide.Globe, "globalchat"),
        Triple("Telegram Chat", Lucide.Send, "telegram"),
    )
    AppCard(Modifier.fillMaxWidth()) {
        Title("Quick Action"); Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { (label, icon, key) ->
                Column(
                    Modifier.width(96.dp).clip(RoundedCornerShape(16.dp)).background(Soft).clickable { go(key) }.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
                        Icon(icon, null, Modifier.size(20.dp), tint = Paper)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(label, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
                }
            }
        }
    }
}

@Composable
fun NewsSection() {
    var news by remember { mutableStateOf<List<NewsItem>?>(null) }
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { news = runCatching { Api.news() }.getOrDefault(emptyList()) }
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.Newspaper, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Title("Berita Terkini")
        }
        Spacer(Modifier.height(12.dp))
        val list = news
        when {
            list == null -> Text("Memuat berita...", color = Muted)
            list.isEmpty() -> Text("Berita tidak bisa dimuat. Cek koneksi internet.", color = Muted)
            else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(list) { n ->
                    Column(
                        Modifier.width(250.dp).clip(RoundedCornerShape(16.dp)).background(Soft).clickable { ctx.openUrl(n.link) },
                    ) {
                        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Line)) {
                            if (n.image != null) AsyncImage(n.image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        Column(Modifier.padding(12.dp)) {
                            Text(n.title, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
                            Spacer(Modifier.height(4.dp))
                            Text(n.desc, fontSize = 13.sp, color = Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- History / Tools / Account / Roles ----------------
@Composable
fun HistoryScreen(vm: AppVM) {
    val now by produceState(System.currentTimeMillis()) { while (true) { delay(1000); value = System.currentTimeMillis() } }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Title("History Navigation") }
        if (vm.history.isEmpty()) item { Text("Belum ada aktivitas.", color = Muted) }
        items(vm.history) { h ->
            AppCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.History, null, Modifier.size(20.dp)); Spacer(Modifier.width(12.dp))
                    Column { Text(h.text, fontSize = 16.sp); Text(relTime(now, h.time), color = Muted, fontSize = 13.sp) }
                }
            }
        }
    }
}

@Composable
fun ToolsScreen(go: (String) -> Unit) {
    val tools = listOf(
        Triple("Sender Manager", "Kelola sender WhatsApp & pairing", "sender"),
        Triple("Global Chat", "Ngobrol, upload foto, buat stiker", "globalchat"),
        Triple("Whatsapp Confess", "Confess, Sorry, Kenalan", "whatsapp"),
        Triple("Telegram Chat", "Buka grup Telegram", "telegram"),
    )
    val icons = listOf(Lucide.Smartphone, Lucide.MessagesSquare, Lucide.MessageCircle, Lucide.Send)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Title("Tools") }
        items(tools.size) { i ->
            val (n, d, k) = tools[i]
            AppCard(Modifier.fillMaxWidth(), onClick = { go(k) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icons[i], null, Modifier.size(26.dp)); Spacer(Modifier.width(14.dp))
                    Column { Text(n, fontSize = 18.sp); Text(d, color = Muted, fontSize = 14.sp) }
                }
            }
        }
    }
}

@Composable
fun AccountScreen(vm: AppVM) {
    val u = vm.user ?: return
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        WelcomeCard(u, vm.senders.size)
        InkButton("Logout", Lucide.LogOut, Modifier.fillMaxWidth()) { vm.logout() }
    }
}
