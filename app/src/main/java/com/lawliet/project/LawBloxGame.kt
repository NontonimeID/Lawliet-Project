package com.lawliet.project

import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

// ====================== Dunia & fisika ======================
class Obstacle(val x: Float, val z: Float, val w: Float, val d: Float, val h: Float, val color: Color)

class Bot(val name: String, var x: Float, var z: Float) {
    var heading = 0f; var tx = x; var tz = z; var wait = 0f; var phase = 0f
    var moving = false; var walkT = 0f
    var say: String? = null; var sayUntil = 0f
    val look = lookFor(name)
}

private val RAINBOW = listOf(0xFFE53935, 0xFF1E88E5, 0xFF43A047, 0xFFFDD835, 0xFF8E24AA, 0xFFFB8C00, 0xFF00ACC1).map { Color(it) }

fun genObstacles(seed: Int, kind: Int): List<Obstacle> {
    val r = Random(seed); val out = ArrayList<Obstacle>()
    when (kind) {
        0 -> for (i in 0 until 14) { // lintasan spiral naik
            val a = i * 0.5f; val rad = 9f + i * 1.2f
            out += Obstacle(cos(a) * rad, sin(a) * rad, 5f, 5f, 1.5f + i * 1.2f, RAINBOW[i % RAINBOW.size])
        }
        1 -> repeat(14) {
            out += Obstacle(r.nextFloat() * 64 - 32, r.nextFloat() * 64 - 32, 3 + r.nextFloat() * 4, 3 + r.nextFloat() * 4, 1 + r.nextFloat() * 5, RAINBOW[r.nextInt(RAINBOW.size)])
        }
        2 -> for (gx in -2..2) for (gz in -2..2) {
            if (gx == 0 && gz == 0) continue
            out += Obstacle(gx * 16f, gz * 16f, 8f, 8f, 5f + r.nextInt(10), RAINBOW[(gx + gz + 10) % RAINBOW.size])
        }
        else -> {
            for (i in 0 until 18) { val a = i * 0.5f; out += Obstacle(cos(a) * 8f, sin(a) * 8f, 4f, 4f, 1.2f + i * 1.2f, RAINBOW[i % RAINBOW.size]) }
            out += Obstacle(0f, 0f, 5f, 5f, 24f, Color(0xFF9E9E9E))
        }
    }
    return out.filter { !(abs(it.x) < it.w / 2 + 3.5f && abs(it.z) < it.d / 2 + 3.5f) } // area spawn kosong
}

private val BOT_NAMES = listOf("Rizky_99", "NoobMaster", "AyamGoreng", "SiPalingPro", "kucing_oren", "Dimas123", "Cahaya", "BangJago", "Putri_Dev", "Zaky", "Mochi", "TukangLompat")
private val CHAT_LINES = listOf(
    "halo semuanya!", "mapnya keren banget", "ada yang mau lomba sampai atas?", "wkwkwk jatuh lagi",
    "lompat dong", "siapa yang bisa sampai puncak?", "boleh add friend ga?", "gg", "ayo ikut aku", "lagi cari spot foto",
)
private const val RAD = 0.9f

class GameWorld(val exp: Experience, val me: String) {
    val obstacles = genObstacles(exp.id, exp.kind)
    val cam = Cam().apply { pitch = 0.38f; dist = 15f; ty = 3f }
    val myLook = lookFor(me)
    var px = 0f; var py = 0f; var pz = 0f; var vy = 0f
    var heading = 0f; var grounded = true; var phase = 0f; var swing = 0f
    var time = 0f
    var saySelf: String? = null; var saySelfUntil = 0f
    val bots: List<Bot>
    private var rx = 0f; private var rz = 0f

    init {
        val r = Random(exp.id * 31)
        val n = min(exp.maxPlayers - 1, 4 + r.nextInt(4))
        bots = BOT_NAMES.shuffled(r).take(n).map {
            val a = r.nextFloat() * 6.28f; val d = 6f + r.nextFloat() * 10f
            Bot(it, cos(a) * d, sin(a) * d)
        }
        bots.forEach { b -> resolve(b.x, b.z, 0f); b.x = rx; b.z = rz }
    }

    private fun resolve(nx0: Float, nz0: Float, y: Float) {
        var nx = nx0; var nz = nz0
        for (o in obstacles) {
            if (y >= o.h - 0.35f) continue
            val hx = o.w / 2 + RAD; val hz = o.d / 2 + RAD
            val dx = nx - o.x; val dz = nz - o.z
            if (abs(dx) < hx && abs(dz) < hz) {
                if (hx - abs(dx) < hz - abs(dz)) nx = o.x + (if (dx >= 0) hx else -hx)
                else nz = o.z + (if (dz >= 0) hz else -hz)
            }
        }
        rx = nx.coerceIn(-48f, 48f); rz = nz.coerceIn(-48f, 48f)
    }

    fun groundAt(x: Float, z: Float, fromY: Float): Float {
        var best = 0f
        for (o in obstacles) if (o.h > best && o.h <= fromY + 0.5f && abs(x - o.x) < o.w / 2 + 0.25f && abs(z - o.z) < o.d / 2 + 0.25f) best = o.h
        return best
    }

    fun respawn() { px = 0f; pz = 0f; py = 0f; vy = 0f; grounded = true; heading = 0f }

    fun step(dt: Float, jx: Float, jy: Float, jump: Boolean) {
        time += dt
        val len = hypot(jx, jy)
        val yaw = cam.yaw
        if (len > 0.08f) {
            val dirx = cos(yaw) * jx + sin(yaw) * jy
            val dirz = -sin(yaw) * jx + cos(yaw) * jy
            val dl = hypot(dirx, dirz).coerceAtLeast(1e-4f)
            val nx = dirx / dl; val nz = dirz / dl
            val sp = 12f * min(len, 1f)
            resolve(px + nx * sp * dt, pz + nz * sp * dt, py); px = rx; pz = rz
            heading = lerpAngle(heading, atan2(nx, nz), 16f * dt)
            phase += sp * dt * 0.75f
            swing = sin(phase) * 0.85f
        } else swing *= (1f - 10f * dt).coerceAtLeast(0f)

        val g = groundAt(px, pz, py)
        if (jump && grounded) { vy = 24f; grounded = false }
        if (vy <= 0f && py <= g + 0.05f) { py = g; vy = 0f; grounded = true }
        else {
            vy -= 60f * dt; py += vy * dt; grounded = false
            if (py < g && vy <= 0f) { py = g; vy = 0f; grounded = true }
        }
        cam.tx = px; cam.tz = pz
        cam.ty += ((py + 3f) - cam.ty) * min(1f, 8f * dt)
        if (saySelf != null && time > saySelfUntil) saySelf = null

        for (b in bots) {
            if (b.say != null && time > b.sayUntil) b.say = null
            if (b.wait > 0f) { b.wait -= dt; b.moving = false }
            else {
                val dx = b.tx - b.x; val dz = b.tz - b.z; val d = hypot(dx, dz)
                b.walkT += dt
                if (d < 1f || b.walkT > 5f) {
                    b.wait = 1f + Random.nextFloat() * 3f; b.walkT = 0f
                    b.tx = (Random.nextFloat() * 2 - 1) * 28f; b.tz = (Random.nextFloat() * 2 - 1) * 28f
                } else {
                    val s = 5f; b.moving = true
                    resolve(b.x + dx / d * s * dt, b.z + dz / d * s * dt, 0f); b.x = rx; b.z = rz
                    b.heading = lerpAngle(b.heading, atan2(dx, dz), 10f * dt)
                    b.phase += s * dt * 0.75f
                }
            }
        }
    }
}

// ====================== Render ======================
private fun DrawScope.tag(cam: Cam, tm: TextMeasurer, pos: V3, name: String, color: Color, say: String?) {
    val p = cam.project(pos) ?: return
    if ((pos - cam.pos).len() > 70f) return
    val st = TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    val lay = tm.measure(name, st)
    val x = p.x - lay.size.width / 2f; val y = p.y - lay.size.height
    drawText(tm.measure(name, st.copy(color = Color.Black)), topLeft = Offset(x + 1.5f, y + 1.5f))
    drawText(lay, topLeft = Offset(x, y))
    if (say != null) {
        val bl = tm.measure(say, TextStyle(color = Color(0xFF202020), fontSize = 12.sp), constraints = Constraints(maxWidth = 170.dp.roundToPx()))
        val w = bl.size.width + 18f; val h = bl.size.height + 12f
        val bx = p.x - w / 2f; val by = y - h - 6f
        drawRoundRect(Color.White, Offset(bx, by), Size(w, h), CornerRadius(14f))
        drawText(bl, topLeft = Offset(bx + 9f, by + 6f))
    }
}

fun DrawScope.renderWorld(w: GameWorld, tm: TextMeasurer, showNames: Boolean) {
    val cam = w.cam
    cam.update(size.width, size.height)
    val skyTop = shade(Color(w.exp.sky), 0.85f)
    val skyBot = lerp(Color(w.exp.sky), Color.White, 0.55f)
    val horizon = (cam.cy - cam.focal * tan(cam.pitch)).coerceIn(1f, size.height)
    drawRect(Brush.verticalGradient(listOf(skyTop, skyBot), startY = 0f, endY = horizon))
    val ground = Color(w.exp.ground)
    poly(cam, arrayOf(V3(-60f, 0f, -60f), V3(60f, 0f, -60f), V3(60f, 0f, 60f), V3(-60f, 0f, 60f)), ground, seam = false)

    // stud di baseplate
    val step = 2f; val gx0 = floor(w.px / step).toInt(); val gz0 = floor(w.pz / step).toInt()
    for (ix in -10..10) for (iz in -10..10) {
        val sx = (gx0 + ix) * step + 1f; val sz = (gz0 + iz) * step + 1f
        if (abs(sx) > 58f || abs(sz) > 58f) continue
        drawStud(cam, V3(sx, 0f, sz), 0.55f, ground)
    }

    val items = ArrayList<Item>(80)
    for (o in w.obstacles) {
        val verts = Array(8) {
            V3(o.x + if (it and 1 != 0) o.w / 2 else -o.w / 2, if (it and 2 != 0) o.h else 0f, o.z + if (it and 4 != 0) o.d / 2 else -o.d / 2)
        }
        val c = V3(o.x, o.h / 2, o.z)
        val big = max(o.w, o.d)
        val key = (c - cam.pos).len() + if (big > 3f) 0.7f * big / 2f else 0f
        items.add(Item(key) {
            drawBoxWorld(cam, verts, o.color)
            if (cam.pos.y > o.h && (c - cam.pos).len() < 45f) {
                var sx = -o.w / 2 + 0.5f
                while (sx < o.w / 2) {
                    var sz = -o.d / 2 + 0.5f
                    while (sz < o.d / 2) { drawStud(cam, V3(o.x + sx, o.h, o.z + sz), 0.3f, o.color); sz += 1f }
                    sx += 1f
                }
            }
        })
    }
    addCharacter(items, cam, w.px, w.py, w.pz, w.heading, w.swing, !w.grounded, w.myLook, w.groundAt(w.px, w.pz, w.py))
    for (b in w.bots) addCharacter(items, cam, b.x, 0f, b.z, b.heading, if (b.moving) sin(b.phase) * 0.85f else 0f, false, b.look, 0f)
    items.sortByDescending { it.key }
    for (it in items) it.draw(this)

    if (showNames) {
        tag(cam, tm, V3(w.px, w.py + 6.1f, w.pz), w.me, nameColor(w.me), w.saySelf)
        for (b in w.bots) tag(cam, tm, V3(b.x, 6.1f, b.z), b.name, nameColor(b.name), b.say)
    }
}

// ====================== Kontrol ======================
@Composable
fun Joystick(modifier: Modifier = Modifier, onMove: (Float, Float) -> Unit) {
    var knob by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier.size(150.dp).clip(CircleShape).background(Color(0x44FFFFFF)).border(2.dp, Color(0x88FFFFFF), CircleShape)
            .pointerInput(Unit) {
                val r = this.size.width / 2f
                fun upd(p: Offset) {
                    var d = p - Offset(r, r)
                    val l = d.getDistance()
                    if (l > r) d = d * (r / l)
                    knob = d; onMove(d.x / r, -d.y / r)
                }
                awaitEachGesture {
                    val down = awaitFirstDown(); upd(down.position)
                    do {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id }
                        if (ch != null) { upd(ch.position); ch.consume() }
                    } while (ev.changes.any { it.pressed && it.id == down.id })
                    knob = Offset.Zero; onMove(0f, 0f)
                }
            },
    ) {
        Box(
            Modifier.offset { IntOffset(knob.x.roundToInt(), knob.y.roundToInt()) }.align(Alignment.Center)
                .size(62.dp).clip(CircleShape).background(Color(0xCCFFFFFF))
        )
    }
}

@Composable
fun JumpButton(modifier: Modifier = Modifier, onHold: (Boolean) -> Unit) {
    Box(
        modifier.size(86.dp).clip(CircleShape).background(Color(0x55FFFFFF)).border(2.dp, Color(0x99FFFFFF), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(); onHold(true)
                    do { val ev = awaitPointerEvent(); ev.changes.forEach { it.consume() } } while (ev.changes.any { it.pressed })
                    onHold(false)
                }
            },
        contentAlignment = Alignment.Center,
    ) { Icon(Lucide.ArrowUp, "Lompat", Modifier.size(40.dp), tint = Color.White) }
}

@Composable
private fun RoundBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, active: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (active) Color(0xAA335FFF) else Color(0x99000000))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(24.dp), tint = Color.White) }
}

@Composable
fun LbChatPanel(msgs: List<LbChat>, onSend: (String) -> Unit, modifier: Modifier = Modifier) {
    val state = rememberLazyListState()
    var text by remember { mutableStateOf("") }
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) state.animateScrollToItem(msgs.lastIndex) }
    fun send() { onSend(text); text = "" }
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x88000000)).padding(8.dp)) {
        LazyColumn(state = state, modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(msgs) { m ->
                Text(
                    buildAnnotatedString {
                        if (m.system) withStyle(SpanStyle(color = m.color)) { append("[${m.name}] ${m.text}") }
                        else {
                            withStyle(SpanStyle(color = m.color, fontWeight = FontWeight.Bold)) { append("[${m.name}] ") }
                            withStyle(SpanStyle(color = Color.White)) { append(m.text) }
                        }
                    },
                    fontSize = 13.sp, lineHeight = 17.sp,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                text, { text = it }, Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Color(0x33FFFFFF)).padding(horizontal = 12.dp, vertical = 8.dp),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp), singleLine = true, cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send() }),
                decorationBox = { inner -> if (text.isEmpty()) Text("Ketik pesan...", color = Color(0xAAFFFFFF), fontSize = 14.sp); inner() },
            )
            IconButton({ send() }, Modifier.size(36.dp)) { Icon(Lucide.Send, "Kirim", tint = Color.White, modifier = Modifier.size(20.dp)) }
        }
    }
}

// ====================== Menu pengaturan ======================
@Composable
private fun LbMenu(
    vm: AppVM, world: GameWorld, showNames: Boolean, onNames: (Boolean) -> Unit, showBoard: Boolean, onBoard: (Boolean) -> Unit,
    sens: Float, onSens: (Float) -> Unit, onContinue: () -> Unit, onRespawn: () -> Unit, onLeave: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val panel = Color(0xFF232527)
    Column(
        Modifier.width(470.dp).heightIn(max = 330.dp).clip(RoundedCornerShape(14.dp)).background(panel)
            .pointerInput(Unit) { detectTapGestures { } }.padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("LawBlox", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            listOf("Pemain", "Pengaturan").forEachIndexed { i, t ->
                Text(
                    t, Modifier.clip(RoundedCornerShape(8.dp)).background(if (tab == i) Color(0xFF335FFF) else Color(0xFF393B3D))
                        .clickable { tab = i }.padding(horizontal = 14.dp, vertical = 7.dp),
                    color = Color.White, fontSize = 14.sp,
                )
                Spacer(Modifier.width(8.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.weight(1f, fill = true).fillMaxWidth()) {
            if (tab == 0) {
                val all = listOf(world.me) + world.bots.map { it.name }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(all) { n ->
                        val isMe = n == world.me
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF2E3032)).padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(lookFor(n).shirt))
                            Spacer(Modifier.width(10.dp))
                            Text(n, color = nameColor(n), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (isMe) Text("  (kamu)", color = Color(0xFFB0B0B0), fontSize = 13.sp)
                            Spacer(Modifier.weight(1f))
                            if (!isMe) {
                                if (vm.lb.friends.any { it.equals(n, true) }) Text("Teman", color = Color(0xFF00B06F), fontSize = 13.sp)
                                else IconButton({
                                    vm.lb.addFriend(n, world.me)?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
                                        ?: run { vm.log("Menambah teman $n"); Toast.makeText(ctx, "$n ditambahkan sebagai teman", Toast.LENGTH_SHORT).show() }
                                }, Modifier.size(32.dp)) { Icon(Lucide.UserPlus, "Tambah teman", tint = Color.White, modifier = Modifier.size(20.dp)) }
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sensitivitas kamera", color = Color.White, fontSize = 14.sp)
                    Slider(sens, onSens, valueRange = 0.4f..2.5f, colors = SliderDefaults.colors(thumbColor = Color(0xFF335FFF), activeTrackColor = Color(0xFF335FFF)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nama pemain", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(showNames, onNames)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Daftar pemain (pojok kanan atas)", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(showBoard, onBoard)
                    }
                    Text("Seret layar untuk memutar kamera, cubit untuk zoom.", color = Color(0xFFB0B0B0), fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuBtn("Leave", Lucide.LogOut, Color(0xFFD13F3F), Modifier.weight(1f), onLeave)
            MenuBtn("Respawn", Lucide.RotateCcw, Color(0xFF4A4C4F), Modifier.weight(1f), onRespawn)
            MenuBtn("Continue", Lucide.Play, Color(0xFF00B06F), Modifier.weight(1f), onContinue)
        }
    }
}

@Composable
private fun MenuBtn(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(44.dp).clip(RoundedCornerShape(10.dp)).background(bg).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = Color.White); Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

// ====================== Layar game ======================
@Composable
fun LbGameScreen(vm: AppVM, exp: Experience) {
    val ctx = LocalContext.current
    val me = vm.user?.username ?: "Player"
    val world = remember(exp.id) { GameWorld(exp, me) }
    var joined by remember(exp.id) { mutableStateOf(false) }
    var tick by remember { mutableLongStateOf(0L) }
    var jx by remember { mutableFloatStateOf(0f) }
    var jy by remember { mutableFloatStateOf(0f) }
    var jumpHeld by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var chatOpen by remember { mutableStateOf(true) }
    var showNames by remember { mutableStateOf(true) }
    var showBoard by remember { mutableStateOf(true) }
    var sens by remember { mutableFloatStateOf(1f) }
    val msgs = remember(exp.id) { mutableStateListOf<LbChat>() }
    val tm = rememberTextMeasurer()

    // landscape + layar penuh selama di dalam experience
    DisposableEffect(Unit) {
        val act = ctx.findActivity()
        act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val ctl = act?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        ctl?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ctl?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ctl?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    BackHandler { menu = !menu }

    LaunchedEffect(exp.id) { delay(1800); joined = true }
    LaunchedEffect(joined) {
        if (!joined) return@LaunchedEffect
        msgs += LbChat("Sistem", "Kamu bergabung ke ${exp.title}. ${world.bots.size} pemain lain ada di server ini.", Color(0xFFFFE082), true)
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                world.step(dt, jx, jy, jumpHeld)
                tick++
            }
        }
    }
    LaunchedEffect(joined) {
        if (!joined) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(3500, 9000))
            val b = world.bots.randomOrNull() ?: continue
            val t = CHAT_LINES.random()
            msgs += LbChat(b.name, t, nameColor(b.name)); b.say = t; b.sayUntil = world.time + 5f
        }
    }

    fun send(raw: String) {
        val t = raw.trim(); if (t.isEmpty()) return
        val shown = if (Moderator.violates(t)) "#".repeat(t.length) else t // disaring seperti Roblox
        msgs += LbChat(me, shown, nameColor(me)); world.saySelf = shown; world.saySelfUntil = world.time + 5f
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (!joined) {
            Column(
                Modifier.fillMaxSize().studs(Color(0xFF1B1D1F)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Text(exp.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                Text("oleh ${exp.creator}", color = Color(0xFFB8B8B8), fontSize = 14.sp)
                Spacer(Modifier.height(18.dp))
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(10.dp))
                Text("Bergabung ke server...", color = Color.White, fontSize = 14.sp)
            }
        } else {
            Canvas(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        world.cam.yaw -= pan.x * 0.007f * sens
                        world.cam.pitch = (world.cam.pitch + pan.y * 0.005f * sens).coerceIn(0.05f, 1.35f)
                        world.cam.dist = (world.cam.dist / zoom).coerceIn(6f, 30f)
                    }
                }
            ) {
                @Suppress("UNUSED_VARIABLE") val t = tick // invalidasi tiap frame
                renderWorld(world, tm, showNames)
            }

            Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoundBtn(Lucide.Settings, "Settings") { menu = true }
                    RoundBtn(Lucide.MessageSquare, "Chat", chatOpen) { chatOpen = !chatOpen }
                }
                if (chatOpen) LbChatPanel(msgs, { send(it) }, Modifier.padding(top = 8.dp).width(300.dp).height(180.dp))
            }

            if (showBoard) {
                Column(
                    Modifier.align(Alignment.TopEnd).padding(12.dp).width(150.dp).clip(RoundedCornerShape(8.dp))
                        .background(Color(0x88000000)).padding(8.dp),
                ) {
                    Text("Pemain (${world.bots.size + 1})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    (listOf(world.me) + world.bots.map { it.name }).take(8).forEach {
                        Text(it, color = nameColor(it), fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }

            Joystick(Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 28.dp)) { x, y -> jx = x; jy = y }
            JumpButton(Modifier.align(Alignment.BottomEnd).padding(end = 48.dp, bottom = 36.dp)) { jumpHeld = it }
        }

        if (menu) {
            Box(
                Modifier.fillMaxSize().background(Color(0xAA000000)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { menu = false },
                contentAlignment = Alignment.Center,
            ) {
                LbMenu(
                    vm, world, showNames, { showNames = it }, showBoard, { showBoard = it }, sens, { sens = it },
                    onContinue = { menu = false },
                    onRespawn = { world.respawn(); menu = false },
                    onLeave = { vm.log("Keluar dari ${exp.title}"); vm.lb.playing = null },
                )
            }
        }
    }
}
