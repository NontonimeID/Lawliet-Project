package com.lawliet.project

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------- Sender Manager (privat) / Add Sender Global (Founder) ----------------
@Composable
fun SenderManagerScreen(vm: AppVM, global: Boolean = false) {
    val list = if (global) vm.globalSenders else vm.senders
    var addDialog by remember { mutableStateOf(false) }
    var pairNumber by remember { mutableStateOf<String?>(null) }   // nomor yang sedang di-pairing
    val scope = rememberCoroutineScope()
    LaunchedEffect(global) { while (true) { vm.refreshSenders(global); delay(4000) } }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Title(if (global) "Add Sender Global" else "Sender Manager")
                if (global) Text("Nomor sender global hanya terlihat oleh Founder. Role lain memakainya tanpa melihat nomor.", color = Muted, fontSize = 14.sp)
            }
            if (list.isEmpty()) item { Text("Belum ada sender. Tekan + untuk menambah.", color = Muted) }
            items(list, key = { it.id }) { s ->
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(Soft), contentAlignment = Alignment.Center) {
                            Icon(if (global) Lucide.Globe else Lucide.Smartphone, null, Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("+${s.number}", fontSize = 18.sp)
                            Text(if (s.connected) "Terhubung" else "Menunggu pairing", fontSize = 14.sp, color = if (s.connected) WaGreen else Muted)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ pairNumber = s.number }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Icon(Lucide.RefreshCw, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Pairing ulang", color = Ink)
                        }
                        OutlinedButton({ scope.launch { vm.deleteSender(s, global) } }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Icon(Lucide.Trash2, null, Modifier.size(16.dp), tint = androidx.compose.ui.graphics.Color.Red)
                            Spacer(Modifier.width(6.dp)); Text("Hapus", color = androidx.compose.ui.graphics.Color.Red)
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { addDialog = true }, containerColor = Ink, contentColor = Paper, shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) { Icon(Lucide.Plus, "Tambah sender") }
    }
    if (addDialog) AddSenderDialog({ addDialog = false }) { pairNumber = it; addDialog = false }
    pairNumber?.let { num -> PairingDialog(vm, num, global) { pairNumber = null } }
}

@Composable
private fun AddSenderDialog(onClose: () -> Unit, onNext: (String) -> Unit) {
    var n by remember { mutableStateOf("") }
    val err = if (n.isEmpty()) null else validatePhone(n)
    AlertDialog(
        onDismissRequest = onClose, containerColor = Paper,
        title = { Text("Tambah Sender", fontFamily = Orbitron) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Masukkan nomor WhatsApp dengan kode negara, hanya angka. Contoh: 6281234567890 (bukan 081234567890).", fontSize = 14.sp, color = Muted)
                Field(n, { n = it.filter(Char::isDigit) }, "Nomor WhatsApp", keyboard = KeyboardType.Number)
                err?.let { Text(it, color = androidx.compose.ui.graphics.Color.Red, fontSize = 13.sp) }
            }
        },
        confirmButton = { TextButton({ onNext(n) }, enabled = n.isNotEmpty() && err == null) { Text("Lanjut", color = Ink) } },
        dismissButton = { TextButton(onClose) { Text("Batal", color = Muted) } },
    )
}

@Composable
private fun PairingDialog(vm: AppVM, number: String, global: Boolean, onClose: () -> Unit) {
    var code by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(number) {
        code = null; err = null
        runCatching { vm.requestPairing(number, global) }.onSuccess { code = it }.onFailure { err = it.message }
    }
    AlertDialog(
        onDismissRequest = onClose, containerColor = Paper,
        title = { Text("Kode Pairing", fontFamily = Orbitron) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("+$number", color = Muted)
                Spacer(Modifier.height(12.dp))
                when {
                    err != null -> Text(err!!, color = androidx.compose.ui.graphics.Color.Red)
                    code == null -> CircularProgressIndicator(color = Ink)
                    else -> Text(code!!.chunked(4).joinToString("-"), fontFamily = Orbitron, fontSize = 28.sp,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Soft).padding(horizontal = 16.dp, vertical = 12.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Buka WhatsApp > Perangkat tertaut > Tautkan perangkat > Tautkan dengan nomor telepon, lalu masukkan kode di atas.", fontSize = 14.sp, color = Muted)
            }
        },
        confirmButton = {
            TextButton({ scope.launch { if (code != null) vm.addSender(number, global); onClose() } }) {
                Text(if (code != null) "Selesai" else "Tutup", color = Ink)
            }
        },
    )
}

// ---------------- Whatsapp Confess ----------------
@Composable
fun WhatsappScreen(vm: AppVM) {
    var mode by remember { mutableIntStateOf(0) }       // 0 user, 1 group
    var target by remember { mutableStateOf("") }
    var typeIdx by remember { mutableIntStateOf(0) }
    var senderIdx by remember { mutableIntStateOf(0) }  // 0 privat, 1 global
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope(); val ctx = LocalContext.current
    val types = if (mode == 0) listOf("Confess", "Sorry", "Kenalan") else listOf("Confess Group", "Sorry Group", "Kenalan Group")

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { RawVideo("banner2", Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(20.dp))) }
        item {
            SegSwitch(listOf("Confess User" to Lucide.Smartphone, "Confess Group" to Lucide.Globe), mode) {
                mode = it; typeIdx = 0; target = ""
            }
        }
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Title(if (mode == 0) "Masukkan Nomor Target" else "Masukkan Url Group"); Spacer(Modifier.height(10.dp))
                if (mode == 0) Field(target, { target = it.filter(Char::isDigit) }, "Contoh: 6281234567890", keyboard = KeyboardType.Number)
                else Field(target, { target = it.trim() }, "https://chat.whatsapp.com/...", keyboard = KeyboardType.Uri)
            }
        }
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Title("Jenis Confess"); Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(types.size) { i ->
                        val on = i == typeIdx
                        Row(
                            Modifier.clip(RoundedCornerShape(14.dp)).background(if (on) Ink else Soft)
                                .clickable { typeIdx = i }.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Lucide.ShieldCheck, null, Modifier.size(18.dp), tint = if (on) Paper else Ink)
                            Spacer(Modifier.width(6.dp))
                            Text(types[i], color = if (on) Paper else Ink, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Title("Pilih Sender"); Spacer(Modifier.height(10.dp))
                SegSwitch(listOf("Sender Privat" to Lucide.Lock, "Sender Global" to Lucide.Globe), senderIdx) { senderIdx = it }
            }
        }
        if (mode == 1) item {
            AppCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Info, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Title("Catatan")
                }
                Spacer(Modifier.height(8.dp))
                Text("Pengaturan grup harus:", fontSize = 15.sp)
                Text("1. Nyalakan \"Siapapun boleh bergabung grup ini pakai url\".", fontSize = 15.sp, color = Muted)
                Text("2. Matikan \"Tinjau anggota\" (pending).", fontSize = 15.sp, color = Muted)
                Text("3. Matikan \"Izinkan user menambah anggota\".", fontSize = 15.sp, color = Muted)
            }
        }
        item {
            InkButton("Kirim ${types[typeIdx]}", Lucide.Send, Modifier.fillMaxWidth(), enabled = !busy) {
                busy = true
                scope.launch {
                    val err = vm.sendConfess(mode == 1, target, types[typeIdx], senderIdx == 1)
                    Toast.makeText(ctx, err ?: "Berhasil dikirim", Toast.LENGTH_SHORT).show()
                    busy = false
                }
            }
        }
    }
}

// ---------------- Global Chat ----------------
@Composable
fun GlobalChatScreen(vm: AppVM) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    val state = rememberLazyListState()
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { decodeUri(ctx, it)?.let { b -> vm.sendChat(image = b) } }
    }
    val sticker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { decodeUri(ctx, it)?.let { b -> vm.sendChat(image = makeSticker(b), sticker = true) } }
    }
    LaunchedEffect(vm.chat.size) { if (vm.chat.isNotEmpty()) state.animateScrollToItem(vm.chat.lastIndex) }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.MessagesSquare, null, Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); Title("Global Chat")
        }
        LazyColumn(Modifier.weight(1f), state = state, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.chat, key = { it.id }) { m ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (m.mine) Alignment.End else Alignment.Start) {
                    if (!m.mine) Row(verticalAlignment = Alignment.CenterVertically) {
                        if (m.system) { Icon(Lucide.Bot, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
                        Text(m.user, fontSize = 12.sp, color = Muted)
                    }
                    when {
                        m.image != null && m.sticker -> Image(m.image.asImageBitmap(), "stiker", Modifier.size(150.dp))
                        else -> Column(
                            Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(16.dp))
                                .background(if (m.mine) Ink else Soft).padding(10.dp)
                        ) {
                            if (m.image != null) {
                                Image(m.image.asImageBitmap(), "foto", Modifier.clip(RoundedCornerShape(10.dp)).fillMaxWidth())
                                if (m.text != null) Spacer(Modifier.height(6.dp))
                            }
                            if (m.text != null) Text(m.text, color = if (m.mine) Paper else Ink, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Lucide.Image, "Upload foto") }
            IconButton({ sticker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Lucide.Sticker, "Buat stiker") }
            OutlinedTextField(
                text, { text = it }, Modifier.weight(1f), placeholder = { Text("Tulis pesan...") }, singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, unfocusedBorderColor = Line, cursorColor = Ink),
            )
            IconButton({ if (text.isNotBlank()) { vm.sendChat(text.trim()); text = "" } }) { Icon(Lucide.SendHorizontal, "Kirim") }
        }
    }
}
