package com.lawliet.project

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------- Landing: grid kotak hitam di background putih ----------------
@Composable
fun LandingScreen(onBuy: () -> Unit, onLogin: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Paper)) {
        Box(Modifier.fillMaxSize().drawBehind {
            val step = 36.dp.toPx(); val w = 1.dp.toPx()
            var x = 0f
            while (x <= size.width) { drawLine(Ink, Offset(x, 0f), Offset(x, size.height), w); x += step }
            var y = 0f
            while (y <= size.height) { drawLine(Ink, Offset(0f, y), Offset(size.width, y), w); y += step }
            // pudar di tengah supaya teks mudah dibaca
            drawRect(Brush.radialGradient(
                listOf(Paper, Paper, Paper.copy(alpha = 0f)),
                center = Offset(size.width / 2, size.height / 2), radius = size.minDimension * 0.85f,
            ))
        })
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(32.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Lawliet Project", fontFamily = Orbitron, fontSize = 30.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            InkButton("Beli Akses", Lucide.ShoppingCart, Modifier.fillMaxWidth().height(54.dp), onClick = onBuy)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onLogin, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Ink), colors = ButtonDefaults.outlinedButtonColors(containerColor = Paper),
            ) { Icon(Lucide.LogIn, null, Modifier.size(18.dp), tint = Ink); Spacer(Modifier.width(8.dp)); Text("Masuk", color = Ink, fontSize = 16.sp) }
        }
    }
}

// ---------------- Beli akses (sebelum login) ----------------
@Composable
fun BuyAccessScreen(vm: AppVM, onBack: () -> Unit, onLogin: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Lucide.ArrowLeft, "Kembali") }
            Text("Beli Akses", fontFamily = Orbitron, fontSize = 18.sp)
        }
        BuyAccountForm(vm, onLogin)
    }
}

/** Form beli akun: username + password + pilih role -> invoice Sayabayar -> tunggu webhook. */
@Composable
fun BuyAccountForm(vm: AppVM, onLogin: (() -> Unit)?) {
    var u by remember { mutableStateOf("") }; var p by remember { mutableStateOf("") }; var p2 by remember { mutableStateOf("") }
    var roleIdx by remember { mutableIntStateOf(0) }
    var err by remember { mutableStateOf<String?>(null) }; var busy by remember { mutableStateOf(false) }
    var order by remember { mutableStateOf<Order?>(null) }; var status by remember { mutableStateOf("pending") }
    val scope = rememberCoroutineScope(); val ctx = LocalContext.current

    LaunchedEffect(order) {
        val o = order ?: return@LaunchedEffect
        var tries = 0
        while (tries < 600) { status = vm.orderStatus(o.id); if (status != "pending") break; delay(3000); tries++ }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val o = order
        if (o != null) {
            AppCard(Modifier.fillMaxWidth()) {
                when (status) {
                    "paid" -> {
                        Icon(Lucide.ShieldCheck, null, Modifier.size(32.dp), tint = WaGreen)
                        Spacer(Modifier.height(8.dp))
                        Title("Pembayaran diterima")
                        Text("Akun $u (${ROLE_PRICES[roleIdx].role.label}) sudah aktif. Silakan masuk.", color = Muted, fontSize = 15.sp)
                        if (onLogin != null) { Spacer(Modifier.height(12.dp)); InkButton("Masuk", Lucide.LogIn, Modifier.fillMaxWidth(), onClick = onLogin) }
                    }
                    "pending" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = Ink, strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)); Title("Menunggu pembayaran")
                        }
                        Text("Selesaikan pembayaran di halaman Sayabayar. Akun aktif otomatis setelah terkonfirmasi.", color = Muted, fontSize = 15.sp)
                        if (!o.url.isNullOrEmpty()) { Spacer(Modifier.height(12.dp)); InkButton("Buka halaman pembayaran", Lucide.ExternalLink, Modifier.fillMaxWidth()) { ctx.openUrl(o.url) } }
                    }
                    else -> {
                        Title("Pesanan ${if (status == "expired") "kedaluwarsa" else "gagal"}")
                        Text("Silakan buat pesanan baru.", color = Muted, fontSize = 15.sp)
                        Spacer(Modifier.height(12.dp)); InkButton("Buat ulang", null, Modifier.fillMaxWidth()) { order = null; status = "pending" }
                    }
                }
            }
        } else {
            AppCard(Modifier.fillMaxWidth()) {
                Title("Buat Akun"); Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(u, { u = it.trim() }, "Username")
                    Field(p, { p = it }, "Password", password = true)
                    Field(p2, { p2 = it }, "Konfirmasi password", password = true)
                }
            }
            AppCard(Modifier.fillMaxWidth()) {
                Title("Pilih Role"); Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ROLE_PRICES.forEachIndexed { i, r ->
                        val on = i == roleIdx
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (on) Ink else Soft)
                                .clickable { roleIdx = i }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(r.role.label, Modifier.weight(1f), color = if (on) Paper else Ink, fontSize = 17.sp)
                            Text("${rupiah(r.price)} • Permanen", color = if (on) Paper else Muted, fontSize = 14.sp)
                        }
                    }
                }
            }
            err?.let { Text(it, color = androidx.compose.ui.graphics.Color.Red, fontSize = 14.sp) }
            InkButton("Bayar ${rupiah(ROLE_PRICES[roleIdx].price)}", Lucide.ShoppingCart, Modifier.fillMaxWidth(), enabled = !busy) {
                if (p != p2) { err = "Konfirmasi password tidak sama"; return@InkButton }
                busy = true; err = null
                scope.launch {
                    vm.createOrder(u, p, ROLE_PRICES[roleIdx].role)
                        .onSuccess { o2 -> order = o2; status = "pending"; if (!o2.url.isNullOrEmpty()) ctx.openUrl(o2.url) }
                        .onFailure { err = it.message }
                    busy = false
                }
            }
        }
    }
}

// ---------------- Up Role (bayar selisih) ----------------
@Composable
fun UpRoleScreen(vm: AppVM) {
    val cur = vm.user?.role ?: Role.MEMBER
    val curPrice = ROLE_PRICES.firstOrNull { it.role == cur }?.price ?: 0
    val list = ROLE_PRICES.filter { it.role.ordinal > cur.ordinal }
    var order by remember { mutableStateOf<Order?>(null) }; var status by remember { mutableStateOf("pending") }
    var err by remember { mutableStateOf<String?>(null) }; var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope(); val ctx = LocalContext.current
    LaunchedEffect(order) {
        val o = order ?: return@LaunchedEffect
        var tries = 0
        while (tries < 600) { status = vm.orderStatus(o.id); if (status != "pending") break; delay(3000); tries++ }
        if (status == "paid") { vm.refreshUser(); vm.log("Up role berhasil") }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Title("Up Role") }
        item { Text("Role sekarang: ${cur.label}. Kamu bayar selisih harga.", color = Muted, fontSize = 15.sp) }
        if (order != null) item {
            AppCard(Modifier.fillMaxWidth()) {
                when (status) {
                    "paid" -> { Title("Berhasil"); Text("Role kamu sudah diperbarui.", color = Muted, fontSize = 15.sp) }
                    "pending" -> {
                        Title("Menunggu pembayaran")
                        order?.url?.takeIf { it.isNotEmpty() }?.let { url -> Spacer(Modifier.height(10.dp)); InkButton("Buka halaman pembayaran", Lucide.ExternalLink) { ctx.openUrl(url) } }
                    }
                    else -> { Title("Pesanan ${if (status == "expired") "kedaluwarsa" else "gagal"}"); Spacer(Modifier.height(8.dp)); InkButton("Tutup", null) { order = null; status = "pending" } }
                }
            }
        }
        err?.let { item { Text(it, color = androidx.compose.ui.graphics.Color.Red, fontSize = 14.sp) } }
        if (list.isEmpty()) item { Text("Tidak ada role yang lebih tinggi untuk dibeli.", color = Muted) }
        items(list) { r ->
            AppCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.role.label, fontFamily = Orbitron, fontSize = 17.sp)
                        Text("${rupiah(r.price - curPrice)} • Permanen", color = Muted, fontSize = 15.sp)
                    }
                    InkButton("Upgrade", Lucide.CircleArrowUp, enabled = !busy && (order == null || status != "pending")) {
                        busy = true; err = null
                        scope.launch {
                            vm.createUpgrade(r.role)
                                .onSuccess { o2 -> order = o2; status = "pending"; if (!o2.url.isNullOrEmpty()) ctx.openUrl(o2.url) }
                                .onFailure { err = it.message }
                            busy = false
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Founder: Add Role (gratis) ----------------
@Composable
fun FounderAddRoleScreen(vm: AppVM) {
    if (vm.user?.role != Role.FOUNDER) return // khusus Founder
    var u by remember { mutableStateOf("") }; var p by remember { mutableStateOf("") }
    var roleIdx by remember { mutableIntStateOf(0) }
    var err by remember { mutableStateOf<String?>(null) }; var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope(); val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Title("Add Role")
        Text("Buat akun baru tanpa pembayaran.", color = Muted, fontSize = 15.sp)
        AppCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field(u, { u = it.trim() }, "Username")
                Field(p, { p = it }, "Password", password = true)
            }
        }
        AppCard(Modifier.fillMaxWidth()) {
            Title("Role"); Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ROLE_PRICES.forEachIndexed { i, r ->
                    val on = i == roleIdx
                    Text(r.role.label, Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (on) Ink else Soft)
                        .clickable { roleIdx = i }.padding(14.dp), color = if (on) Paper else Ink, fontSize = 17.sp)
                }
            }
        }
        err?.let { Text(it, color = androidx.compose.ui.graphics.Color.Red, fontSize = 14.sp) }
        InkButton("Buat Akun (Gratis)", Lucide.UserPlus, Modifier.fillMaxWidth(), enabled = !busy) {
            busy = true
            scope.launch {
                err = vm.founderCreateUser(u, p, ROLE_PRICES[roleIdx].role)
                if (err == null) { Toast.makeText(ctx, "Akun $u dibuat", Toast.LENGTH_SHORT).show(); u = ""; p = "" }
                busy = false
            }
        }
    }
}
