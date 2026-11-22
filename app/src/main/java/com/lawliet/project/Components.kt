package com.lawliet.project

import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.composables.icons.lucide.*

@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val m = modifier.clip(RoundedCornerShape(20.dp)).background(Paper)
        .border(1.dp, Line, RoundedCornerShape(20.dp))
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Column(m.padding(16.dp), content = content)
}

@Composable
fun Title(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier, fontFamily = Orbitron, fontSize = 15.sp, color = Ink)

@Composable
fun InkButton(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick, modifier.height(48.dp), enabled = enabled, shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, fontSize = 16.sp)
    }
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
          password: Boolean = false, keyboard: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text) {
    OutlinedTextField(
        value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = if (password) androidx.compose.ui.text.input.PasswordVisualTransformation()
        else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = if (password) androidx.compose.ui.text.input.KeyboardType.Password else keyboard),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink, unfocusedBorderColor = Line, focusedLabelColor = Ink, cursorColor = Ink),
    )
}

/** Switch dua/lebih opsi dengan ikon (Confess User/Group, Sender Privat/Global). */
@Composable
fun SegSwitch(options: List<Pair<String, ImageVector>>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Soft).padding(4.dp)) {
        options.forEachIndexed { i, (label, icon) ->
            val on = i == selected
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) Ink else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = if (on) Paper else Ink)
                Spacer(Modifier.width(6.dp))
                Text(label, color = if (on) Paper else Ink, fontSize = 15.sp)
            }
        }
    }
}

/** Video dari res/raw. Jika file belum ada, tampil placeholder hitam. onDuration dipanggil (ms). */
@OptIn(UnstableApi::class)
@Composable
fun RawVideo(name: String, modifier: Modifier = Modifier, loop: Boolean = true, onEnded: (() -> Unit)? = null, onDuration: ((Long) -> Unit)? = null) {
    val ctx = LocalContext.current
    val id = remember(name) { ctx.resources.getIdentifier(name, "raw", ctx.packageName) }
    if (id == 0) {
        LaunchedEffect(Unit) { onDuration?.invoke(4000); kotlinx.coroutines.delay(4000); onEnded?.invoke() }
        Box(modifier.background(Ink), contentAlignment = Alignment.Center) {
            Text("raw/$name.mp4 belum ada", color = Paper, textAlign = TextAlign.Center, fontSize = 13.sp)
        }
        return
    }
    val player = remember(id) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(id)))
            repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            volume = if (loop) 0f else 1f
            playWhenReady = true; prepare()
        }
    }
    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && player.duration > 0) onDuration?.invoke(player.duration)
                if (state == Player.STATE_ENDED) onEnded?.invoke()
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }
    AndroidView(modifier = modifier, factory = {
        PlayerView(it).apply { useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM; this.player = player }
    })
}

/** Bottom navigation "liquid": blob putih memanjang saat berpindah, Whatsapp = lingkaran hijau. */
@Composable
fun LiquidNav(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        "Home" to Lucide.House, "History" to Lucide.History, "Whatsapp" to Lucide.MessageCircle,
        "Tools" to Lucide.Wrench, "Account" to Lucide.User,
    )
    Box(Modifier.fillMaxWidth().background(Paper).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(68.dp).shadow(14.dp, RoundedCornerShape(34.dp))
                .clip(RoundedCornerShape(34.dp)).background(Ink)
        ) {
            val w = maxWidth / 5
            val fast by animateDpAsState(w * selected, spring(0.8f, Spring.StiffnessMedium), label = "fast")
            val slow by animateDpAsState(w * selected, spring(0.9f, Spring.StiffnessVeryLow), label = "slow")
            val left = minOf(fast, slow)
            val right = maxOf(fast, slow) + w
            val blob by animateFloatAsState(if (selected == 2) 0f else 1f, label = "blob")
            Box(
                Modifier.offset(x = left + 6.dp, y = 8.dp).width(right - left - 12.dp).height(52.dp)
                    .alpha(blob).background(Paper, RoundedCornerShape(26.dp))
            )
            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { i, (label, icon) ->
                    val on = i == selected
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null
                        ) { onSelect(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (i == 2) {
                            val s by animateDpAsState(if (on) 52.dp else 46.dp, label = "wa")
                            Box(Modifier.size(s).background(WaGreen, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(icon, label, Modifier.size(24.dp), tint = Color.White)
                            }
                        } else {
                            val tint = if (on) Ink else Color.White
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, label, Modifier.size(22.dp), tint = tint)
                                Text(label, color = tint, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
