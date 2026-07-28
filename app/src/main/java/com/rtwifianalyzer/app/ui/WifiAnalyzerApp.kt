@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
package com.rtwifianalyzer.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.TrafficStats
import com.rtwifianalyzer.app.BuildConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtwifianalyzer.app.domain.*
import com.rtwifianalyzer.app.util.copy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private enum class Destination(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    GENERAL("General", Icons.Default.Wifi), SCAN("Wi‑Fi scan", Icons.Default.Radar), GRAPH("Signal graph", Icons.Default.BarChart), DEVICES("Devices", Icons.Default.Devices), SAVED_WIFI("Saved Wi‑Fi", Icons.Default.Lock), P2P("Wi‑Fi Direct", Icons.Default.Share), TRAFFIC("Traffic", Icons.Default.Speed), SIM("SIM & cellular", Icons.Default.SimCard), SETTINGS("Settings", Icons.Default.Settings)
}

@Composable fun WifiAnalyzerApp(vm: AnalyzerViewModel, theme: AppTheme, scanWithPermission: () -> Unit, setTheme: (AppTheme) -> Unit) {
    var destination by rememberSaveable { mutableStateOf(Destination.GENERAL) }; val drawer = rememberDrawerState(DrawerValue.Closed); val scope = androidx.compose.runtime.rememberCoroutineScope()
    val state by vm.state.collectAsState(); val privileged by vm.privilegedAccess.collectAsState(); val shizuku by vm.shizukuRunning.collectAsState()
    ModalNavigationDrawer(drawerState = drawer, drawerContent = { ModalDrawerSheet(Modifier.width(280.dp)) {
        Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surface))).padding(24.dp)) { Icon(Icons.Default.Wifi, null, tint = Color.White, modifier = Modifier.size(38.dp)); Text("WiFi Analyzer", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold); Text(if (privileged) "Privileged access available" else "Smart network insights", color = Color.White.copy(.8f)) }
        Spacer(Modifier.height(10.dp)); Destination.entries.forEach { item -> NavigationDrawerItem(label = { Text(item.title) }, icon = { Icon(item.icon, null) }, selected = item == destination, onClick = { destination = item; scope.launch { drawer.close() } }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)) }
    } }) {
        Scaffold(topBar = { TopAppBar(title = { Text(destination.title, fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton({ scope.launch { drawer.open() } }) { Icon(Icons.Default.Menu, "Open navigation") } }, actions = { if (privileged) AssistChip(onClick = {}, label = { Text(if (shizuku) "SHIZUKU" else "ROOT") }, leadingIcon = { Icon(Icons.Default.Verified, null) }) }) }) { padding ->
            Box(Modifier.padding(padding).fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)))) { AnimatedContent(destination, label = "screen") { screen -> when (screen) {
                Destination.GENERAL -> GeneralScreen(state, vm::refreshConnection)
                Destination.SCAN -> ScanScreen(state, scanWithPermission)
                Destination.GRAPH -> GraphScreen(state, scanWithPermission)
                Destination.DEVICES -> DevicesScreen(vm, state)
                Destination.SAVED_WIFI -> SavedWifiScreen(state, privileged, shizuku, vm::checkPrivileges, vm::savedWifiNetworks)
                Destination.P2P -> P2pScreen()
                Destination.TRAFFIC -> TrafficScreen()
                Destination.SIM -> SimScreen()
                Destination.SETTINGS -> SettingsScreen(theme, setTheme)
            } } }
        }
    }
}

@Composable private fun ScreenList(content: @Composable ColumnScope.() -> Unit) = Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
@Composable private fun SectionCard(title: String, body: @Composable ColumnScope.() -> Unit) { Card(shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), content = { Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(10.dp)); body() }) } }
@Composable private fun CopyValue(label: String, value: String) { val clipboard = LocalClipboardManager.current; Row(Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { clipboard.copy(value) }).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.width(112.dp), color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, fontSize = 13.sp); IconButton({ clipboard.copy(value) }, Modifier.size(30.dp)) { Icon(Icons.Default.ContentCopy, "Copy $label", Modifier.size(16.dp)) } } }

@Composable private fun GeneralScreen(state: AnalyzerState, refresh: () -> Unit) { val w = state.wifi; LaunchedEffect(Unit) { while (true) { refresh(); delay(1_000) } }; ScreenList {
    SectionCard("Connected network") { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Wifi, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp)); Column(Modifier.padding(start = 14.dp).weight(1f)) { Text(w.ssid, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("${w.quality} • ${w.rssi} dBm • ${w.band}") }; IconButton(refresh) { Icon(Icons.Default.Refresh, "Refresh") } }; SignalLine(state.history, w.rssi) }
    SectionCard("Wireless link") { CopyValue("BSSID", w.bssid); CopyValue("Signal", "${w.rssi} dBm (${w.quality})"); CopyValue("Frequency", "${w.frequency} MHz • ${w.band}"); CopyValue("Channel", channel(w.frequency).toString()); CopyValue("Link speed", "${w.linkSpeed} Mbps") }
    SectionCard("IP & routing") { CopyValue("IPv4", w.ipV4); CopyValue("IPv6", w.ipV6); CopyValue("Public IP", if (state.publicIpLoading) "Loading…" else w.publicIp); CopyValue("Gateway", w.gateway); CopyValue("Netmask", w.netmask); CopyValue("DHCP server", w.dhcp); CopyValue("DNS 1", w.dns1); CopyValue("DNS 2", w.dns2); CopyValue("Device Wi‑Fi MAC", w.deviceMac) }
} }
@Composable private fun SignalLine(values: List<Int>, latest: Int) { val color = when { latest >= -50 -> Color(0xFF16A34A); latest >= -60 -> Color(0xFF0EA5E9); latest >= -70 -> Color(0xFFF59E0B); else -> Color(0xFFEF4444) }; Column { Text("Live RSSI · last 60 seconds", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Canvas(Modifier.fillMaxWidth().height(90.dp).padding(top = 8.dp)) { if (values.size > 1) values.zipWithNext().forEachIndexed { i, (a,b) -> drawLine(color, Offset(i * size.width / (values.size - 1), ((-a - 30) / 70f).coerceIn(0f,1f)*size.height), Offset((i+1) * size.width / (values.size-1), ((-b - 30)/70f).coerceIn(0f,1f)*size.height), 5f) } } } }

@Composable private fun ScanScreen(state: AnalyzerState, scan: () -> Unit) { var band by remember { mutableStateOf("All") }; var selected by remember { mutableStateOf<AccessPoint?>(null) }; val filtered = state.points.filter { band == "All" || it.band == band }; LaunchedEffect(Unit) { scan() }; ScreenList {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { listOf("All", "2.4 GHz", "5 GHz", "6 GHz").forEach { FilterChip(selected = band == it, onClick = { band = it }, label = { Text(it) }) }; Spacer(Modifier.weight(1f)); FilledTonalIconButton(scan) { Icon(Icons.Default.Refresh, "Scan") } }
    if (state.loading) { LinearProgressIndicator(Modifier.fillMaxWidth()) }; state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (filtered.isEmpty()) SectionCard("No networks yet") { Text("Tap Scan. Android requires location permission and may throttle frequent scans.") } else filtered.forEach { p -> Card(onClick = { selected = p }, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Wifi, null, tint = signalColor(p.rssi)); Column(Modifier.padding(start=14.dp).weight(1f)) { Text(p.ssid, fontWeight = FontWeight.Bold); Text("${p.bssid} • ${p.frequency} MHz • Ch ${p.channel}", maxLines=1, overflow=TextOverflow.Ellipsis) }; Column(horizontalAlignment = Alignment.End) { Text("${p.rssi} dBm", fontWeight = FontWeight.Bold); Text(p.security, fontSize=12.sp) } } } }
}; selected?.let { p -> ModalBottomSheet({ selected = null }) { Column(Modifier.padding(24.dp).padding(bottom=28.dp)) { Text(p.ssid, style=MaterialTheme.typography.headlineSmall); Spacer(Modifier.height(12.dp)); CopyValue("BSSID",p.bssid); CopyValue("Manufacturer",p.manufacturer); CopyValue("Signal", "${p.rssi} dBm"); CopyValue("Band/channel", "${p.band} / ${p.channel}"); CopyValue("Security",p.security); CopyValue("Capabilities",p.capabilities); CopyValue("Scan timestamp", DateFormat.getDateTimeInstance().format(Date(p.timestamp))) } } }
}

@Composable private fun GraphScreen(state: AnalyzerState, scan: () -> Unit) { var band by remember { mutableStateOf("2.4 GHz") }; val entries = state.points.filter { it.band == band }; ScreenList { Row { FilterChip(band=="2.4 GHz", { band="2.4 GHz" }, { Text("2.4 GHz") }); Spacer(Modifier.width(8.dp)); FilterChip(band=="5 GHz", { band="5 GHz" }, { Text("5 GHz") }); Spacer(Modifier.weight(1f)); FilledTonalIconButton(scan) { Icon(Icons.Default.Refresh, "Refresh signal data") } }; SectionCard("Signal comparison") { if (entries.isEmpty()) Text("No $band networks. Scan to populate the chart.") else entries.forEach { p -> Row(Modifier.padding(vertical=7.dp), verticalAlignment=Alignment.CenterVertically) { Text(p.ssid, Modifier.width(110.dp), maxLines=1, overflow=TextOverflow.Ellipsis); LinearProgressIndicator(progress = { ((p.rssi + 100)/60f).coerceIn(0f,1f) }, color=signalColor(p.rssi), modifier=Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(8.dp))); Text(" ${p.rssi}", Modifier.width(42.dp)) } } } } }

@Composable private fun DeviceTypeIcon(device: NetworkDevice) { val identity = "${device.name} ${device.manufacturer}".lowercase(); val icon = when { listOf("router", "gateway", "tp-link", "d-link", "netgear", "cisco").any { identity.contains(it) } -> Icons.Default.Router; listOf("macbook", "laptop", "windows", "dell", "lenovo", "thinkpad").any { identity.contains(it) } -> Icons.Default.LaptopMac; listOf("iphone", "android", "pixel", "galaxy", "xiaomi", "phone").any { identity.contains(it) } -> Icons.Default.PhoneAndroid; else -> Icons.Default.Devices }; Icon(icon, "Detected device type", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) }
@Composable private fun DevicesScreen(vm: AnalyzerViewModel, state: AnalyzerState) { val devices by vm.devices.collectAsState(); LaunchedEffect(Unit) { vm.devices() }; ScreenList { Row(verticalAlignment=Alignment.CenterVertically) { Text("Devices seen on your LAN", Modifier.weight(1f), fontWeight=FontWeight.SemiBold); Button(vm::devices) { Icon(Icons.Default.Search,null); Spacer(Modifier.width(6.dp)); Text("Discover") } }; if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth()); state.message?.let { Text(it, color=MaterialTheme.colorScheme.error) }; if (devices.isEmpty()) SectionCard("No devices found") { Text("Tap Discover to probe your Wi‑Fi subnet and list devices that respond.") } else devices.forEach { d -> SectionCard(d.name) { Row(verticalAlignment = Alignment.CenterVertically) { DeviceTypeIcon(d); Spacer(Modifier.width(10.dp)); Text(if (d.name == "Unknown device") "Network device" else d.name, fontWeight = FontWeight.Medium) }; CopyValue("IP address",d.ip); CopyValue("MAC address",d.mac); if (d.manufacturer != "Unknown manufacturer") CopyValue("Manufacturer",d.manufacturer); CopyValue("Status",if(d.online) "Connected" else "Disconnected"); CopyValue("Last seen",DateFormat.getDateTimeInstance().format(Date(d.lastSeen))) } } } }

@Composable private fun SavedWifiScreen(state: AnalyzerState, privileged: Boolean, shizukuRunning: Boolean, checkPrivileges: () -> Unit, loadNetworks: () -> Unit) { ScreenList { SectionCard("Saved Wi‑Fi networks") { Text("Android keeps saved Wi‑Fi passwords in protected system storage. They can only be read on a rooted device or with Shizuku.", color=MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(12.dp)); Button(onClick = loadNetworks, enabled = privileged) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Load saved networks") }; if (!privileged) { if (shizukuRunning) { Button(onClick = { try { rikka.shizuku.Shizuku.requestPermission(100); checkPrivileges() } catch (e: Exception) {} }) { Text("Grant Shizuku Permission") } } else { Text("Root or Shizuku access is not available.", color=MaterialTheme.colorScheme.error, fontSize=12.sp) } } }; if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth()); state.savedWifiMessage?.let { Text(it, color=MaterialTheme.colorScheme.error) }; state.savedWifi.forEach { network -> SectionCard(network.ssid) { CopyValue("SSID", network.ssid); CopyValue("Password", network.password ?: "Saved, but not readable on this Android build") } } } }
@Composable private fun P2pScreen() {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(WifiP2pManager::class.java) }
    var peers by remember { mutableStateOf<List<WifiP2pDevice>>(emptyList()) }
    var groupInfo by remember { mutableStateOf("No active Wi‑Fi Direct group.") }
    var status by remember { mutableStateOf(if (manager == null) "Wi‑Fi Direct is not available on this device." else "Ready to discover peers.") }
    val channel = remember(manager) { manager?.initialize(context, context.mainLooper) { status = "Wi‑Fi Direct channel was lost. Reopen this screen to try again." } }
    val peerListener = remember(manager, channel) { WifiP2pManager.PeerListListener { list -> peers = list.deviceList.sortedBy { it.deviceName ?: it.deviceAddress } } }
    val connectionListener = remember { WifiP2pManager.ConnectionInfoListener { info: WifiP2pInfo -> groupInfo = if (info.groupFormed) "Connected • ${if (info.isGroupOwner) "You are the group owner" else "Joined a group"} • Owner: ${info.groupOwnerAddress?.hostAddress ?: "Unknown"}" else "No active Wi‑Fi Direct group." } }
    DisposableEffect(manager, channel) {
        if (manager == null || channel == null) return@DisposableEffect onDispose { }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                try {
                    when (intent.action) {
                        WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> manager.requestPeers(channel, peerListener)
                        WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> manager.requestConnectionInfo(channel, connectionListener)
                        WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> if (intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, WifiP2pManager.WIFI_P2P_STATE_DISABLED) != WifiP2pManager.WIFI_P2P_STATE_ENABLED) status = "Turn on Wi‑Fi to use Wi‑Fi Direct."
                    }
                } catch (_: SecurityException) { status = "Allow Nearby devices and Location permissions to use Wi‑Fi Direct." }
            }
        }
        val filter = IntentFilter().apply { addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION); addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION); addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION) }
        if (Build.VERSION.SDK_INT >= 33) ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED) else context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    fun discoverPeers() {
        if (manager == null || channel == null) return
        val required = if (Build.VERSION.SDK_INT >= 33) arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.NEARBY_WIFI_DEVICES) else arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (required.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }) { status = "Allow Nearby devices and Location permissions, then try again."; return }
        status = "Discovering nearby Wi‑Fi Direct devices…"
        try { manager.discoverPeers(channel, object : WifiP2pManager.ActionListener { override fun onSuccess() { status = "Discovery started. Waiting for nearby peers…"; manager.requestPeers(channel, peerListener) }; override fun onFailure(reason: Int) { status = "Peer discovery failed (Android error $reason)." } }) } catch (_: SecurityException) { status = "Allow Nearby devices and Location permissions to use Wi‑Fi Direct." }
    }
    ScreenList { SectionCard("Wi‑Fi Direct peers") { Text("Discover compatible devices near you without joining a Wi‑Fi network."); Spacer(Modifier.height(10.dp)); Button(onClick = ::discoverPeers, enabled = manager != null && channel != null) { Icon(Icons.Default.Radar, null); Spacer(Modifier.width(6.dp)); Text("Discover peers") }; Text(status, fontSize=12.sp, color=MaterialTheme.colorScheme.onSurfaceVariant); if (peers.isNotEmpty()) { Spacer(Modifier.height(12.dp)); peers.forEach { peer -> Text(peer.deviceName?.takeIf { it.isNotBlank() } ?: peer.deviceAddress, fontWeight=FontWeight.Medium); Text(peer.deviceAddress, fontSize=12.sp, color=MaterialTheme.colorScheme.onSurfaceVariant) } } }; SectionCard("Group information") { Text(groupInfo) } }
}

@Composable private fun TrafficScreen() { var lastRx by remember { mutableLongStateOf(TrafficStats.getTotalRxBytes()) }; var lastTx by remember { mutableLongStateOf(TrafficStats.getTotalTxBytes()) }; var rx by remember { mutableLongStateOf(0) }; var tx by remember { mutableLongStateOf(0) }; LaunchedEffect(Unit) { while(true) { delay(1_000); val currentRx=TrafficStats.getTotalRxBytes(); val currentTx=TrafficStats.getTotalTxBytes(); rx=(currentRx-lastRx).coerceAtLeast(0); tx=(currentTx-lastTx).coerceAtLeast(0); lastRx=currentRx; lastTx=currentTx } }; ScreenList { SectionCard("Live device traffic") { Text("Device-wide traffic from Android TrafficStats", color=MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(18.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceEvenly) { TrafficValue(Icons.Default.Download, "Download", rx); TrafficValue(Icons.Default.Upload, "Upload", tx) } }; SectionCard("About this meter") { Text("Per-app traffic is not reliably available to regular apps on modern Android. Root-enabled builds can add deeper collection later.") } } }
@Composable private fun TrafficValue(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, bytes: Long) { Column(horizontalAlignment=Alignment.CenterHorizontally) { Icon(icon,null,tint=MaterialTheme.colorScheme.primary); Text(label); Text(formatRate(bytes), fontSize=20.sp, fontWeight=FontWeight.Bold) } }

@Composable private fun StatusValue(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium) } }
@Composable private fun SimScreen() { val context=LocalContext.current; val telephony = remember { context.getSystemService(TelephonyManager::class.java) }; val state = runCatching { simState(telephony?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN) }.getOrDefault("Unavailable"); val operator = runCatching { telephony?.networkOperatorName.orEmpty().ifBlank { "Unknown" } }.getOrDefault("Permission required"); val operatorCode = runCatching { telephony?.networkOperator.orEmpty().ifBlank { "Unknown" } }.getOrDefault("Permission required"); val type = runCatching { if (telephony == null) "Unavailable" else networkTypeName(telephony.dataNetworkType) }.getOrDefault("Permission required"); ScreenList { SectionCard("SIM status") { StatusValue("State", state); StatusValue("Operator", operator); StatusValue("MCC / MNC", operatorCode); StatusValue("Data network", type) }; SectionCard("Device identity") { Text("Some cellular values are restricted by Android. This screen never reads IMEI/MEID and remains safe when Phone permission is unavailable.", color=MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun SettingsScreen(theme: AppTheme, setTheme: (AppTheme) -> Unit) { val context=LocalContext.current; ScreenList { SectionCard("Appearance") { Text("Theme"); Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { AppTheme.entries.forEach { t -> FilterChip(theme==t, { setTheme(t) }, { Text(t.name.lowercase().replaceFirstChar { it.titlecase() }) }) } } }; SectionCard("Language") { Text("English (default)", fontWeight=FontWeight.Medium); Text("String resources are structured for translated locale packs.", color=MaterialTheme.colorScheme.onSurfaceVariant, fontSize=12.sp) }; SectionCard("Support") { TextButton({ context.startActivity(Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:developer@example.com?subject=WiFi%20Analyzer"))) }) { Icon(Icons.Default.Email,null); Spacer(Modifier.width(8.dp)); Text("Contact developer") }; TextButton({ context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com/privacy"))) }) { Text("Privacy policy & terms") } }; SectionCard("About") { StatusValue("Version", BuildConfig.VERSION_NAME); Text("Built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, Hilt and platform network APIs.", color=MaterialTheme.colorScheme.onSurfaceVariant) } } }

private fun channel(frequency: Int) = when { frequency in 2412..2484 -> (frequency - 2407) / 5; frequency >= 5000 -> (frequency - 5000) / 5; else -> 0 }
private fun signalColor(rssi: Int) = when { rssi >= -50 -> Color(0xFF16A34A); rssi >= -60 -> Color(0xFF0EA5E9); rssi >= -70 -> Color(0xFFF59E0B); else -> Color(0xFFEF4444) }
private fun formatRate(bytes: Long): String {
    val bits = bytes * 8
    return if (bits > 1_000_000) String.format("%.2f Mbps", bits / 1_000_000.0)
    else String.format("%.1f Kbps", bits / 1_000.0)
}
private fun simState(state: Int) = when(state) { TelephonyManager.SIM_STATE_READY -> "READY"; TelephonyManager.SIM_STATE_ABSENT -> "ABSENT"; TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN required"; TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK required"; else -> "Unknown" }
private fun networkTypeName(type: Int) = when(type) { TelephonyManager.NETWORK_TYPE_LTE -> "LTE"; TelephonyManager.NETWORK_TYPE_NR -> "5G NR"; TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"; TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"; TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"; TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"; else -> "Unknown" }
