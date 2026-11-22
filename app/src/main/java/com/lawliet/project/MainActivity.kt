package com.lawliet.project

import android.content.Context
import android.content.Intent
import android.graphics.Color as AColor
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT),
        )
        setContent { AppTheme { Root() } }
    }
}

@Composable
fun Root(vm: AppVM = viewModel()) {
    var splash by rememberSaveable { mutableStateOf(true) }
    var authPage by rememberSaveable { mutableStateOf("landing") }
    Box(Modifier.fillMaxSize().background(Paper)) {
        when {
            splash -> LoadingScreen { splash = false }
            vm.user == null -> when (authPage) {
                "login" -> AuthScreen(vm) { authPage = "landing" }
                "buy" -> BuyAccessScreen(vm, { authPage = "landing" }) { authPage = "login" }
                else -> LandingScreen({ authPage = "buy" }) { authPage = "login" }
            }
            vm.lawblox -> vm.lb.playing.let { p -> if (p != null) LbGameScreen(vm, p) else LawBloxShell(vm) }
            else -> MainShell(vm)
        }
    }
    // ikon status bar menyesuaikan tema (gelap saat LawBlox)
    val view = LocalView.current
    val dark = LbTheme.on
    SideEffect {
        val act = view.context.findActivity() ?: return@SideEffect
        WindowCompat.getInsetsController(act.window, view).apply {
            isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
        }
    }
}

fun Context.openUrl(url: String) = runCatching {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}.onFailure { Toast.makeText(this, "Tidak bisa membuka link", Toast.LENGTH_SHORT).show() }

private val TAB_NAMES = listOf("Home", "History", "Whatsapp", "Tools", "Account")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(vm: AppVM) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sub by rememberSaveable { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf<String?>(null) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val isFounder = vm.user?.role == Role.FOUNDER
    LaunchedEffect(Unit) { while (true) { vm.refreshUser(); delay(60_000) } }
    LaunchedEffect(Unit) { vm.refreshSenders(false) }

    BackHandler(sub != null || tab != 0 || drawer.isOpen) {
        when {
            drawer.isOpen -> scope.launch { drawer.close() }
            sub != null -> sub = null
            else -> tab = 0
        }
    }
    fun openTab(i: Int) { tab = i; sub = null; vm.log("Membuka navigasi ${TAB_NAMES[i]}") }
    fun openSub(s: String, label: String) { sub = s; vm.log("Membuka $label") }
    val go: (String) -> Unit = { key ->
        when (key) {
            "sender" -> openSub("sender", "Sender Manager")
            "globalchat" -> openSub("globalchat", "Global Chat")
            "tools" -> openTab(3)
            "whatsapp" -> openTab(2)
            "telegram" -> { vm.log("Membuka Telegram Chat"); ctx.openUrl(TELEGRAM_URL) }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Paper) {
                Spacer(Modifier.height(24.dp))
                Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.logo), null, Modifier.size(40.dp).clip(CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Title("Lawliet Project")
                }
                val base: List<Triple<String, ImageVector, () -> Unit>> = listOf(
                    Triple("Dashboard", Lucide.House) { openTab(0) },
                    Triple("Beli Akun", Lucide.ShoppingCart) { openSub("buy", "Beli Akun") },
                    Triple("Up Role", Lucide.CircleArrowUp) { openSub("uprole", "Up Role") },
                    Triple("Change Password", Lucide.KeyRound) { dialog = "pw" },
                    Triple("Info Aplikasi", Lucide.Info) { dialog = "info" },
                    Triple("Ganti Username", Lucide.Pencil) { dialog = "name" },
                    Triple("Update APK", Lucide.Download) { vm.log("Membuka Update APK"); ctx.openUrl(APK_URL) },
                )
                val founder: List<Triple<String, ImageVector, () -> Unit>> = if (!isFounder) emptyList() else listOf(
                    Triple("Add Sender Global", Lucide.Globe) { openSub("gsender", "Add Sender Global") },
                    Triple("Add Role", Lucide.UserPlus) { openSub("addrole", "Add Role") },
                )
                val entries = base + founder
                entries.forEach { (label, icon, action) ->
                    NavigationDrawerItem(
                        label = { Text(label, fontSize = 17.sp) },
                        icon = { Icon(icon, null, Modifier.size(22.dp)) },
                        selected = false,
                        onClick = { scope.launch { drawer.close() }; action() },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            containerColor = Paper,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Lawliet Project", fontFamily = Orbitron, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton({ scope.launch { drawer.open() } }) { Icon(Lucide.Menu, "Menu") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper, titleContentColor = Ink, navigationIconContentColor = Ink),
                )
            },
            bottomBar = { LiquidNav(tab) { openTab(it) } },
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (sub) {
                    "sender" -> SenderManagerScreen(vm, global = false)
                    "gsender" -> if (isFounder) SenderManagerScreen(vm, global = true)
                    "addrole" -> if (isFounder) FounderAddRoleScreen(vm)
                    "globalchat" -> GlobalChatScreen(vm)
                    "buy" -> BuyAccountForm(vm, onLogin = null)
                    "uprole" -> UpRoleScreen(vm)
                    else -> when (tab) {
                        0 -> HomeScreen(vm, go)
                        1 -> HistoryScreen(vm)
                        2 -> WhatsappScreen(vm)
                        3 -> ToolsScreen(go)
                        else -> AccountScreen(vm)
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
            title = { Text("Info Aplikasi", fontFamily = Orbitron) },
            text = { Text("Lawliet Project\nVersi 1.0.0\nPlatform WhatsApp tools & komunitas.", fontSize = 16.sp) },
            containerColor = Paper,
        )
    }
}

@Composable
fun PasswordDialog(vm: AppVM, onClose: () -> Unit) {
    var old by remember { mutableStateOf("") }; var new by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }; val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onClose, containerColor = Paper,
        title = { Text("Change Password", fontFamily = Orbitron) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field(old, { old = it }, "Password lama", password = true)
                Field(new, { new = it }, "Password baru", password = true)
                err?.let { Text(it, color = androidx.compose.ui.graphics.Color.Red, fontSize = 14.sp) }
            }
        },
        confirmButton = {
            TextButton({ scope.launch {
                err = vm.changePassword(old, new)
                if (err == null) { Toast.makeText(ctx, "Password diganti", Toast.LENGTH_SHORT).show(); onClose() }
            } }) { Text("Simpan", color = Ink) }
        },
        dismissButton = { TextButton(onClose) { Text("Batal", color = Muted) } },
    )
}

@Composable
fun UsernameDialog(vm: AppVM, onClose: () -> Unit) {
    var name by remember { mutableStateOf(vm.user?.username ?: "") }
    var err by remember { mutableStateOf<String?>(null) }; val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onClose, containerColor = Paper,
        title = { Text("Ganti Username", fontFamily = Orbitron) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field(name, { name = it.trim() }, "Username baru")
                err?.let { Text(it, color = androidx.compose.ui.graphics.Color.Red, fontSize = 14.sp) }
            }
        },
        confirmButton = {
            TextButton({ scope.launch {
                err = vm.changeUsername(name)
                if (err == null) { Toast.makeText(ctx, "Username diganti", Toast.LENGTH_SHORT).show(); onClose() }
            } }) { Text("Simpan", color = Ink) }
        },
        dismissButton = { TextButton(onClose) { Text("Batal", color = Muted) } },
    )
}
