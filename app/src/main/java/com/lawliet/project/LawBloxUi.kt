package com.lawliet.project

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

val LbBlue = Color(0xFF335FFF)
val LbGreen = Color(0xFF00B06F)
val LbCard = Color(0xFF393B3D)

/** Tekstur stud ala baseplate Roblox. */
fun Modifier.studs(base: Color, cell: Dp = 18.dp): Modifier = this.drawBehind {
    drawRect(base)
    val c = cell.toPx()
    val hi = lerp(base, Color.White, 0.12f); val lo = lerp(base, Color.Black, 0.28f)
    var y = c / 2f
    while (y < size.height + c) {
        var x = c / 2f
        while (x < size.width + c) {
            drawCircle(lo, c * 0.30f, Offset(x, y + c * 0.06f))
            drawCircle(hi, c * 0.30f, Offset(x, y))
            x += c
        }
        y += c
    }
}

// ====================== Avatar 3D ======================
@Composable
fun Avatar3D(name: String, modifier: Modifier = Modifier) {
    val cam = remember { Cam().apply { pitch = 0.12f; dist = 8.4f; ty = 2.6f } }
    val look = remember(name) { lookFor(name) }
    var spin by remember { mutableFloatStateOf(0f) }
    var tick by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) withFrameNanos { now ->
            val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceAtMost(0.05f)
            last = now
            if (!dragging) spin += dt * 0.7f
            tick++
        }
    }
    Canvas(
        modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(onDragStart = { dragging = true }, onDragEnd = { dragging = false }, onDragCancel = { dragging = false }) { c, dx ->
                c.consume(); spin -= dx * 0.012f
            }
        }
    ) {
        val t = tick
        cam.yaw = PI.toFloat() + spin
        cam.update(size.width, size.height)
        val items = ArrayList<Item>(12)
        addCharacter(items, cam, 0f, 0f, 0f, 0f, sin(t * 0.05f) * 0.2f, false, look, 0f)
        items.sortByDescending { it.key }
        for (it in items) it.draw(this)
    }
}

// ====================== Komponen kecil ======================
@Composable
fun AvatarDot(name: String, size: Dp = 48.dp, online: Boolean? = null) {
    Box(Modifier.size(size)) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(lookFor(name).shirt), contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.4f).sp)
        }
        if (online != null) Box(
            Modifier.align(Alignment.BottomEnd).size(size / 3.2f).clip(CircleShape)
                .background(if (online) LbGreen else Color(0xFF8A8C8F)).border(2.dp, Paper, CircleShape)
        )
    }
}

@Composable
fun ExpThumb(e: Experience, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp))) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(shade(Color(e.sky), 0.85f), lerp(Color(e.sky), Color.White, 0.5f)))))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.38f).studs(Color(e.ground), 12.dp))
        Box(
            Modifier.align(Alignment.Center).offset(y = (-8).dp).fillMaxWidth(0.28f).aspectRatio(1f)
                .clip(RoundedCornerShape(4.dp)).studs(Color(e.accent), 10.dp)
        )
    }
}

@Composable
private fun Metric(icon: ImageVector, text: String) {
    Icon(icon, null, Modifier.size(13.dp), tint = Muted); Spacer(Modifier.width(3.dp))
    Text(text, color = Muted, fontSize = 12.sp); Spacer(Modifier.width(8.dp))
}

@Composable
fun ExpCard(e: Experience, onOpen: () -> Unit) {
    Column(Modifier.width(150.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpen)) {
        ExpThumb(e, Modifier.fillMaxWidth().aspectRatio(1f))
        Spacer(Modifier.height(6.dp))
        Text(e.title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Metric(Lucide.ThumbsUp, "${e.likePct}%"); Metric(Lucide.Users, compact(e.playing.toLong()))
        }
    }
}

@Composable
private fun SectionTitle(text: String) =
    Text(text, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))

// ====================== Header & Nav ======================
@Composable
fun LbHeader(query: String, onQuery: (String) -> Unit, onMenu: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Paper).statusBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onMenu) { Icon(Lucide.Menu, "Menu", tint = Ink) }
        Row(
            Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(20.dp)).background(Soft).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Lucide.Search, null, Modifier.size(18.dp), tint = Muted); Spacer(Modifier.width(8.dp))
            BasicTextField(
                query, onQuery, Modifier.weight(1f), singleLine = true,
                textStyle = TextStyle(color = Ink, fontSize = 15.sp), cursorBrush = SolidColor(Ink),
                decorationBox = { inner -> if (query.isEmpty()) Text("Cari experience atau user", color = Muted, fontSize = 15.sp); inner() },
            )
            if (query.isNotEmpty()) Icon(Lucide.X, "Hapus", Modifier.size(18.dp).clickable { onQuery("") }, tint = Muted)
        }
        Spacer(Modifier.width(6.dp))
    }
}

@Composable
fun LbNav(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf("Home" to Lucide.House, "Trending" to Lucide.TrendingUp, "Whatsapp" to Lucide.MessageCircle, "Friends" to Lucide.Users, "Tools" to Lucide.Wrench)
    Row(Modifier.fillMaxWidth().background(Color(0xFF1B1D1F)).navigationBarsPadding().height(60.dp)) {
        items.forEachIndexed { i, (label, icon) ->
            val on = i == selected
            val tint = if (i == 2) WaGreen else if (on) Color.White else Color(0xFF9A9CA0)
            Column(
                Modifier.weight(1f).fillMaxHeight()
                    .drawBehind { if (on) drawRect(LbBlue, size = Size(size.width, 3.dp.toPx())) }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Icon(icon, label, Modifier.size(24.dp), tint = tint)
                Text(label, color = tint, fontSize = 10.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

// ====================== Home ======================
@Composable
fun LbHome(vm: AppVM, onOpen: (Experience) -> Unit, onFriends: () -> Unit) {
    val u = vm.user ?: return
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(16.dp)).studs(Color(0xFF2B3A55), 16.dp).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar3D(u.username, Modifier.width(130.dp).height(190.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Halo,", color = Color(0xFFD0D6E2), fontSize = 14.sp)
                    Text(u.username, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(u.role.label, Modifier.clip(RoundedCornerShape(50)).background(LbBlue).padding(horizontal = 10.dp, vertical = 3.dp), color = Color.White, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("${vm.lb.friends.size} Teman • ${vm.lb.following.size} Mengikuti", color = Color(0xFFD0D6E2), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Geser avatar untuk memutar", color = Color(0xFF9AA3B5), fontSize = 11.sp)
                }
            }
        }
        item {
            SectionTitle("Teman")
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Column(Modifier.clickable(onClick = onFriends), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(LbCard), contentAlignment = Alignment.Center) { Icon(Lucide.UserPlus, "Tambah", tint = Color.White) }
                        Text("Tambah", color = Muted, fontSize = 12.sp)
                    }
                }
                items(vm.lb.friends.toList()) { f ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AvatarDot(f, 56.dp, online = !friendStatus(f).startsWith("Offline"))
                        Text(f, color = Ink, fontSize = 12.sp, maxLines = 1, modifier = Modifier.width(60.dp), overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        item {
            SectionTitle("Rekomendasi untukmu")
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(LB_EXPERIENCES) { e -> ExpCard(e) { onOpen(e) } }
            }
        }
        item {
            SectionTitle("Sedang populer")
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(LB_EXPERIENCES.sortedByDescending { it.playing }) { e -> ExpCard(e) { onOpen(e) } }
            }
        }
        item {
            OutlinedButton(
                { vm.updateLawblox(false) }, Modifier.fillMaxWidth().padding(16.dp).height(48.dp), shape = RoundedCornerShape(12.dp),
            ) { Text("Kembali ke tampilan original", color = Ink) }
        }
    }
}

// ====================== Trending ======================
@Composable
fun LbTrending(vm: AppVM, onOpen: (Experience) -> Unit) {
    var seg by remember { mutableIntStateOf(0) }
    var sort by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        SegSwitch(listOf("Experience" to Lucide.Play, "User" to Lucide.Users), seg) { seg = it }
        Spacer(Modifier.height(8.dp))
        SegSwitch(
            if (seg == 0) listOf("Dimainkan" to Lucide.Users, "Disukai" to Lucide.ThumbsUp) else listOf("Followers" to Lucide.Users, "Likes" to Lucide.ThumbsUp),
            sort,
        ) { sort = it }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (seg == 0) {
                val list = if (sort == 0) LB_EXPERIENCES.sortedByDescending { it.playing }
                else LB_EXPERIENCES.sortedWith(compareByDescending<Experience> { it.likePct }.thenByDescending { it.visits })
                items(list.size) { i ->
                    val e = list[i]
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Soft).clickable { onOpen(e) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("#${i + 1}", color = if (i < 3) Color(0xFFFFC107) else Muted, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.width(34.dp))
                        ExpThumb(e, Modifier.size(60.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("oleh ${e.creator}", color = Muted, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (sort == 0) Metric(Lucide.Users, "${compact(e.playing.toLong())} bermain") else Metric(Lucide.ThumbsUp, "${e.likePct}% suka")
                                Metric(Lucide.Play, "${compact(e.visits)} kunjungan")
                            }
                        }
                    }
                }
            } else {
                val list = if (sort == 0) LB_USERS.sortedByDescending { it.followers } else LB_USERS.sortedByDescending { it.likes }
                items(list.size) { i ->
                    val u = list[i]
                    val following = u.name in vm.lb.following
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Soft).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("#${i + 1}", color = if (i < 3) Color(0xFFFFC107) else Muted, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.width(34.dp))
                        AvatarDot(u.name, 46.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Metric(Lucide.Users, compact((u.followers + if (following) 1 else 0).toLong()))
                                Metric(Lucide.ThumbsUp, compact(u.likes.toLong()))
                            }
                        }
                        Button(
                            { vm.lb.toggleFollow(u.name); vm.log("${if (following) "Berhenti mengikuti" else "Mengikuti"} ${u.name}") },
                            colors = ButtonDefaults.buttonColors(containerColor = if (following) LbCard else LbBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp),
                        ) { Text(if (following) "Mengikuti" else "Ikuti", fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

// ====================== Friends ======================
@Composable
fun LbFriends(vm: AppVM) {
    var q by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val me = vm.user?.username ?: ""
    fun add(name: String) {
        val err = vm.lb.addFriend(name, me)
        if (err == null) { vm.log("Menambah teman $name"); msg = "$name ditambahkan sebagai teman" to true; q = "" } else msg = err to false
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Tambah Teman", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Field(q, { q = it.trim() }, "Username", Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Button({ add(q) }, Modifier.height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = LbBlue, contentColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                    Icon(Lucide.UserPlus, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Add")
                }
            }
            msg?.let { Text(it.first, color = if (it.second) LbGreen else Color(0xFFFF6B6B), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
        }
        item { Text("Teman (${vm.lb.friends.size})", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
        if (vm.lb.friends.isEmpty()) item { Text("Belum ada teman. Tambahkan lewat username di atas.", color = Muted, fontSize = 14.sp) }
        items(vm.lb.friends.toList()) { f ->
            val st = friendStatus(f)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Soft).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AvatarDot(f, 46.dp, online = !st.startsWith("Offline"))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(f, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(st, color = if (st.startsWith("Offline")) Muted else LbGreen, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton({ vm.lb.removeFriend(f); vm.log("Menghapus teman $f") }) { Icon(Lucide.X, "Hapus", tint = Muted) }
            }
        }
        item { Text("Mungkin kamu kenal", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp)) }
        items(LB_USERS.filter { u -> u.name != me && vm.lb.friends.none { it.equals(u.name, true) } }.take(5)) { u ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Soft).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AvatarDot(u.name, 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(u.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(u.bio, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button({ add(u.name) }, colors = ButtonDefaults.buttonColors(containerColor = LbBlue, contentColor = Color.White), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Add", fontSize = 13.sp) }
            }
        }
    }
}

// ====================== Detail experience ======================
@Composable
private fun StatCell(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text(label, color = Muted, fontSize = 12.sp)
    }
}

@Composable
fun LbDetail(vm: AppVM, e: Experience, onBack: () -> Unit) {
    val liked = e.id in vm.lb.liked
    val following = e.creator in vm.lb.following
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box {
            ExpThumb(e, Modifier.fillMaxWidth().aspectRatio(16f / 10f))
            IconButton(onBack, Modifier.align(Alignment.TopStart).padding(8.dp).clip(CircleShape).background(Color(0x88000000))) {
                Icon(Lucide.ArrowLeft, "Kembali", tint = Color.White)
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(e.title, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarDot(e.creator, 32.dp); Spacer(Modifier.width(8.dp))
                Text("oleh ", color = Muted, fontSize = 14.sp)
                Text(e.creator, color = LbBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Button(
                { vm.log("Masuk experience ${e.title}"); vm.lb.playing = e }, Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LbGreen, contentColor = Color.White), shape = RoundedCornerShape(10.dp),
            ) { Icon(Lucide.Play, null, Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); Text("Play", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Soft).padding(vertical = 12.dp)) {
                StatCell("${e.likePct + if (liked) 1 else 0}%", "Suka", Modifier.weight(1f))
                StatCell(compact(e.playing.toLong()), "Bermain", Modifier.weight(1f))
                StatCell(compact(e.visits), "Kunjungan", Modifier.weight(1f))
                StatCell("${e.maxPlayers}", "Maks pemain", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    { vm.lb.toggleLike(e.id) }, Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (liked) LbBlue else LbCard, contentColor = Color.White), shape = RoundedCornerShape(10.dp),
                ) { Icon(Lucide.ThumbsUp, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(if (liked) "Disukai" else "Suka") }
                Button(
                    { vm.lb.toggleFollow(e.creator) }, Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (following) LbBlue else LbCard, contentColor = Color.White), shape = RoundedCornerShape(10.dp),
                ) { Icon(Lucide.UserPlus, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(if (following) "Mengikuti" else "Ikuti kreator") }
            }
            Text("Deskripsi", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text(e.desc, color = Muted, fontSize = 15.sp)
        }
    }
}

// ====================== Pencarian ======================
@Composable
fun LbSearch(vm: AppVM, query: String, onOpen: (Experience) -> Unit) {
    val q = query.trim()
    val me = vm.user?.username ?: ""
    val ctx = LocalContext.current
    val exps = LB_EXPERIENCES.filter { it.title.contains(q, true) || it.creator.contains(q, true) }
    val users = LB_USERS.filter { it.name.contains(q, true) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Experience (${exps.size})", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
        items(exps) { e ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Soft).clickable { onOpen(e) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ExpThumb(e, Modifier.size(56.dp)); Spacer(Modifier.width(10.dp))
                Column { Text(e.title, color = Ink, fontWeight = FontWeight.Bold); Text("oleh ${e.creator}", color = Muted, fontSize = 12.sp) }
            }
        }
        item { Text("User", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp)) }
        items(users) { u ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Soft).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AvatarDot(u.name, 44.dp); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text(u.name, color = Ink, fontWeight = FontWeight.Bold); Text("${compact(u.followers.toLong())} followers", color = Muted, fontSize = 12.sp) }
            }
        }
        if (q.length >= 3 && users.none { it.name.equals(q, true) } && !q.equals(me, true)) item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Soft).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AvatarDot(q, 44.dp); Spacer(Modifier.width(10.dp))
                Text("Tambah \"$q\" sebagai teman", color = Ink, modifier = Modifier.weight(1f))
                Button(
                    { Toast.makeText(ctx, vm.lb.addFriend(q, me) ?: "$q ditambahkan", Toast.LENGTH_SHORT).show() },
                    colors = ButtonDefaults.buttonColors(containerColor = LbBlue, contentColor = Color.White), shape = RoundedCornerShape(10.dp),
                ) { Text("Add") }
            }
        }
    }
}

// ====================== Shell ======================
private val LB_TABS = listOf("Home", "Trending", "Whatsapp", "Friends", "Tools")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawBloxShell(vm: AppVM) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sub by rememberSaveable { mutableStateOf<String?>(null) }
    var detailId by rememberSaveable { mutableStateOf<Int?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var dialog by remember { mutableStateOf<String?>(null) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { while (true) { vm.refreshUser(); delay(60_000) } }
    LaunchedEffect(Unit) { vm.refreshSenders(false) }
    val detail = LB_EXPERIENCES.firstOrNull { it.id == detailId }

    BackHandler(drawer.isOpen || detail != null || sub != null || query.isNotEmpty() || tab != 0) {
        when {
            drawer.isOpen -> scope.launch { drawer.close() }
            detail != null -> detailId = null
            sub != null -> sub = null
            query.isNotEmpty() -> query = ""
            else -> tab = 0
        }
    }
    fun openTab(i: Int) { tab = i; sub = null; detailId = null; query = ""; vm.log("Membuka ${LB_TABS[i]} (LawBlox)") }
    val go: (String) -> Unit = { key ->
        when (key) {
            "sender" -> { sub = "sender"; vm.log("Membuka Sender Manager") }
            "globalchat" -> { sub = "globalchat"; vm.log("Membuka Global Chat") }
            "tools" -> openTab(4)
            "whatsapp" -> openTab(2)
            "telegram" -> { vm.log("Membuka Telegram Chat"); ctx.openUrl(TELEGRAM_URL) }
        }
    }
    fun openSub(s: String, label: String) { sub = s; detailId = null; query = ""; vm.log("Membuka $label") }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Paper) {
                Spacer(Modifier.height(24.dp))
                Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AvatarDot(vm.user?.username ?: "?", 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(vm.user?.username ?: "", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("LawBlox", color = Muted, fontSize = 13.sp)
                    }
                }
                val entries: List<Triple<String, ImageVector, () -> Unit>> = listOf(
                    Triple("Akun", Lucide.User) { openSub("account", "Akun") },
                    Triple("Riwayat", Lucide.History) { openSub("history", "Riwayat") },
                    Triple("Up Role", Lucide.CircleArrowUp) { openSub("uprole", "Up Role") },
                    Triple("Change Password", Lucide.KeyRound) { dialog = "pw" },
                    Triple("Ganti Username", Lucide.Pencil) { dialog = "name" },
                    Triple("Info Aplikasi", Lucide.Info) { dialog = "info" },
                    Triple("Tampilan Original", Lucide.House) { vm.updateLawblox(false) },
                )
                entries.forEach { (label, icon, action) ->
                    NavigationDrawerItem(
                        label = { Text(label, fontSize = 17.sp) }, icon = { Icon(icon, null, Modifier.size(22.dp)) }, selected = false,
                        onClick = { scope.launch { drawer.close() }; action() }, modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            containerColor = Paper,
            topBar = { LbHeader(query, { query = it }) { scope.launch { drawer.open() } } },
            bottomBar = { LbNav(tab) { openTab(it) } },
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when {
                    detail != null -> LbDetail(vm, detail) { detailId = null }
                    query.isNotBlank() -> LbSearch(vm, query) { detailId = it.id }
                    sub != null -> when (sub) {
                        "sender" -> SenderManagerScreen(vm, global = false)
                        "globalchat" -> GlobalChatScreen(vm)
                        "account" -> AccountScreen(vm)
                        "history" -> HistoryScreen(vm)
                        "uprole" -> UpRoleScreen(vm)
                        else -> {}
                    }
                    else -> when (tab) {
                        0 -> LbHome(vm, { detailId = it.id }) { openTab(3) }
                        1 -> LbTrending(vm) { detailId = it.id }
                        2 -> WhatsappScreen(vm)
                        3 -> LbFriends(vm)
                        else -> ToolsScreen(go)
                    }
                }
            }
        }
    }

    when (dialog) {
        "pw" -> PasswordDialog(vm) { dialog = null }
        "name" -> UsernameDialog(vm) { dialog = null }
        "info" -> AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton({ dialog = null }) { Text("Tutup", color = Ink) } },
            title = { Text("Info Aplikasi") },
            text = { Text("Lawliet Project - LawBlox\nVersi 1.0.0\nTampilan bertema Roblox dengan dunia 3D.", fontSize = 16.sp) },
            containerColor = Paper,
        )
    }
}
