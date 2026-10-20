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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtwifianalyzer.app.R
import com.rtwifianalyzer.app.domain.*
import com.rtwifianalyzer.app.ui.components.ParabolicSpectrumChart
import com.rtwifianalyzer.app.util.ChannelEvaluator
import com.rtwifianalyzer.app.util.copy
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private enum class Destination(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    GENERAL("General", Icons.Default.Wifi),
    SCAN("Wi‑Fi scan", Icons.Default.Radar),
    GRAPH("Spectrum graph", Icons.Default.BarChart),
    CHANNELS("Channel rating", Icons.Default.Star),
    DEVICES("Devices", Icons.Default.Devices),
    SAVED_WIFI("Saved Wi‑Fi", Icons.Default.Lock),
    P2P("Wi‑Fi Direct", Icons.Default.Share),
    TRAFFIC("Traffic", Icons.Default.Speed),
    SIM("SIM & cellular", Icons.Default.SimCard),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable fun WifiAnalyzerApp(vm: AnalyzerViewModel, theme: AppTheme, scanWithPermission: () -> Unit, setTheme: (AppTheme) -> Unit) {
    var destination by rememberSaveable { mutableStateOf(Destination.GENERAL) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsState()
    val privileged by vm.privilegedAccess.collectAsState()
    val shizuku by vm.shizukuRunning.collectAsState()

    ModalNavigationDrawer(drawerState = drawer, drawerContent = {
        ModalDrawerSheet(Modifier.width(280.dp), drawerContainerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "WiFi Analyzer Logo",
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("WiFi Analyzer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(if (privileged) "Privileged engine active" else "Standard mode", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Destination.entries.forEach { item ->
                NavigationDrawerItem(
                    label = { Text(item.title, fontSize = 14.sp) },
                    icon = { Icon(item.icon, null, modifier = Modifier.size(20.dp)) },
                    selected = item == destination,
                    onClick = { destination = item; scope.launch { drawer.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    }) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(destination.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton({ scope.launch { drawer.open() } }) {
                            Icon(Icons.Default.Menu, "Navigation")
                        }
                    },
                    actions = {
                        if (privileged) {
                            AssistChip(
                                onClick = {},
                                label = { Text(if (shizuku) "SHIZUKU" else "ROOT", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Verified, null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                AnimatedContent(destination, label = "screen") { screen ->
                    when (screen) {
                        Destination.GENERAL -> GeneralScreen(state, vm::refreshConnection, vm::runDiagnostics)
                        Destination.SCAN -> ScanScreen(state, scanWithPermission)
                        Destination.GRAPH -> GraphScreen(state, scanWithPermission)
                        Destination.CHANNELS -> ChannelRatingScreen(state, scanWithPermission)
                        Destination.DEVICES -> DevicesScreen(vm, state)
                        Destination.SAVED_WIFI -> SavedWifiScreen(state, privileged, shizuku, vm::checkPrivileges, vm::savedWifiNetworks)
                        Destination.P2P -> P2pScreen()
                        Destination.TRAFFIC -> TrafficScreen()
                        Destination.SIM -> SimScreen()
                        Destination.SETTINGS -> SettingsScreen(theme, setTheme)
                    }
                }
            }
        }
    }
}

// taste-skill: Quiet, flat surfaces with 1px border. No loud card shadows or gradients.
@Composable private fun ScreenList(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)

@Composable private fun SectionCard(title: String, body: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            body()
        }
    }
}

@Composable private fun CopyValue(label: String, value: String) {
    val clipboard = LocalClipboardManager.current
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { clipboard.copy(value) }).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.width(116.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(value, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Normal)
        IconButton({ clipboard.copy(value) }, Modifier.size(24.dp)) {
            Icon(Icons.Default.ContentCopy, "Copy $label", Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable private fun GeneralScreen(state: AnalyzerState, refresh: () -> Unit, runDiagnostics: () -> Unit) {
    val w = state.wifi
    LaunchedEffect(Unit) { while (true) { refresh(); delay(2_000) } }
    ScreenList {
        SectionCard("Connected Network") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, null, tint = signalColor(w.rssi), modifier = Modifier.size(36.dp))
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(w.ssid, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${w.quality} · ${w.rssi} dBm · ${w.band}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                IconButton(refresh) { Icon(Icons.Default.Refresh, "Refresh") }
            }
            Spacer(Modifier.height(10.dp))
            SignalLine(state.history, w.rssi)
        }
        SectionCard("Network Latency & Reachability") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Jitter & Ping Diagnostics",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(runDiagnostics, enabled = !state.diagnosticsLoading) {
                    Icon(Icons.Default.Speed, "Run Ping Test", tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (state.diagnosticsLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp))
            }
            if (state.diagnostics.isEmpty()) {
                Text(
                    "Tap speed icon to test reachability and latency to Gateway, DNS, and Web.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.diagnostics.forEach { diag ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(diag.target, Modifier.weight(1f), fontSize = 13.sp)
                        Text(diag.ip, Modifier.width(110.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (diag.isReachable) {
                            Text(
                                "${diag.latencyMs} ms",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if ((diag.latencyMs ?: 999) < 40) Color(0xFF198038) else Color(0xFFF1C21B)
                            )
                        } else {
                            Text("Timeout", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        SectionCard("Wireless Link") {
            CopyValue("BSSID", w.bssid)
            CopyValue("Signal", "${w.rssi} dBm (${w.quality})")
            CopyValue("Frequency", "${w.frequency} MHz · ${w.band}")
            CopyValue("Channel", channel(w.frequency).toString())
            CopyValue("Link speed", "${w.linkSpeed} Mbps")
        }
        SectionCard("IP & Routing") {
            CopyValue("IPv4", w.ipV4)
            CopyValue("IPv6", w.ipV6)
            CopyValue("Public IP", if (state.publicIpLoading) "Loading…" else w.publicIp)
            CopyValue("Gateway", w.gateway)
            CopyValue("Netmask", w.netmask)
            CopyValue("DHCP server", w.dhcp)
            CopyValue("DNS 1", w.dns1)
            CopyValue("DNS 2", w.dns2)
            CopyValue("Device MAC", w.deviceMac)
        }
    }
}

@Composable private fun SignalLine(values: List<Int>, latest: Int) {
    val color = signalColor(latest)
    Column {
        Text("LIVE RSSI (LAST 60S)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(Modifier.fillMaxWidth().height(60.dp).padding(top = 6.dp)) {
            if (values.size > 1) {
                values.zipWithNext().forEachIndexed { i, (a, b) ->
                    val x1 = i * size.width / (values.size - 1)
                    val y1 = ((-a - 30) / 70f).coerceIn(0f, 1f) * size.height
                    val x2 = (i + 1) * size.width / (values.size - 1)
                    val y2 = ((-b - 30) / 70f).coerceIn(0f, 1f) * size.height
                    drawLine(color, Offset(x1, y1), Offset(x2, y2), 2.5f)
                }
            }
        }
    }
}

private fun estimateDistance(frequencyMHz: Int, rssi: Int): String {
    if (frequencyMHz <= 0 || rssi >= 0) return "—"
    val exp = (27.55 - (20 * log10(frequencyMHz.toDouble())) + abs(rssi)) / 20.0
    val meters = 10.0.pow(exp)
    return if (meters < 1.0) String.format("%.1f m", meters)
    else if (meters < 100.0) String.format("%.1f m", meters)
    else String.format("%d m", meters.toInt())
}

@Composable private fun ScanScreen(state: AnalyzerState, scan: () -> Unit) {
    var band by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<AccessPoint?>(null) }
    val filtered = state.points.filter { 
        (band == "All" || it.band == band) &&
        (searchQuery.isBlank() || it.ssid.contains(searchQuery, ignoreCase = true) || it.bssid.contains(searchQuery, ignoreCase = true) || it.manufacturer.contains(searchQuery, ignoreCase = true))
    }
    LaunchedEffect(Unit) { scan() }
    ScreenList {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("All", "2.4 GHz", "5 GHz", "6 GHz").forEach {
                FilterChip(
                    selected = band == it,
                    onClick = { band = it },
                    label = { Text(it, fontSize = 12.sp) },
                    shape = RoundedCornerShape(6.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(scan) { Icon(Icons.Default.Refresh, "Scan") }
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter networks by SSID, BSSID or vendor...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton({ searchQuery = "" }) { Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp)) }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        if (filtered.isEmpty()) {
            SectionCard("No networks found") { Text("Tap scan or clear search filter.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            filtered.forEach { p ->
                Card(
                    onClick = { selected = p },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(signalColor(p.rssi), RoundedCornerShape(4.dp)))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.ssid, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${p.bssid} · Ch ${p.channel} (${p.channelWidthDisplay}) · ${p.frequency} MHz", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${p.rssi} dBm", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = signalColor(p.rssi))
                            Text("${p.signalPercent}% · ${p.security}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    selected?.let { p ->
        ModalBottomSheet({ selected = null }) {
            Column(Modifier.padding(20.dp).padding(bottom = 28.dp)) {
                Text(p.ssid, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                CopyValue("BSSID", p.bssid)
                CopyValue("Manufacturer", p.manufacturer)
                CopyValue("Signal", "${p.rssi} dBm (${p.signalPercent}%)")
                CopyValue("Estimated Distance", estimateDistance(p.frequency, p.rssi))
                CopyValue("Band / Channel", "${p.band} / Ch ${p.channel}")
                CopyValue("Channel Width", p.channelWidthDisplay)
                CopyValue("Frequency", "${p.frequency} MHz")
                CopyValue("Security", p.security)
                CopyValue("Capabilities", p.capabilities)
                CopyValue("Scan Timestamp", DateFormat.getDateTimeInstance().format(Date(p.timestamp)))
            }
        }
    }
}

@Composable private fun GraphScreen(state: AnalyzerState, scan: () -> Unit) {
    var band by remember { mutableStateOf("2.4 GHz") }
    val entries = state.points.filter { it.band == band }
    ScreenList {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(band == "2.4 GHz", { band = "2.4 GHz" }, { Text("2.4 GHz", fontSize = 12.sp) }, shape = RoundedCornerShape(6.dp))
            FilterChip(band == "5 GHz", { band = "5 GHz" }, { Text("5 GHz", fontSize = 12.sp) }, shape = RoundedCornerShape(6.dp))
            Spacer(Modifier.weight(1f))
            IconButton(scan) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        SectionCard("Parabolic Spectrum Overlap") {
            if (entries.isEmpty()) {
                Text("No $band networks detected. Tap refresh.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            } else {
                ParabolicSpectrumChart(band = band, accessPoints = entries)
            }
        }
        SectionCard("Signal Comparison (RSSI)") {
            if (entries.isEmpty()) {
                Text("No data.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            } else {
                entries.forEach { p ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.ssid, Modifier.width(100.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                        LinearProgressIndicator(
                            progress = { ((p.rssi + 100) / 60f).coerceIn(0f, 1f) },
                            color = signalColor(p.rssi),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                        )
                        Text(" ${p.rssi}", Modifier.width(44.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable private fun ChannelRatingScreen(state: AnalyzerState, scan: () -> Unit) {
    var band by remember { mutableStateOf("2.4 GHz") }
    val ratings = remember(state.points, band) {
        if (band == "2.4 GHz") ChannelEvaluator.evaluate24GHz(state.points)
        else ChannelEvaluator.evaluate5GHz(state.points)
    }
    val bestChannel = ratings.firstOrNull { it.isRecommended }

    ScreenList {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(band == "2.4 GHz", { band = "2.4 GHz" }, { Text("2.4 GHz", fontSize = 12.sp) }, shape = RoundedCornerShape(6.dp))
            FilterChip(band == "5 GHz", { band = "5 GHz" }, { Text("5 GHz", fontSize = 12.sp) }, shape = RoundedCornerShape(6.dp))
            Spacer(Modifier.weight(1f))
            IconButton(scan) { Icon(Icons.Default.Refresh, "Evaluate") }
        }

        bestChannel?.let { best ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Recommended Channel: ${best.channel}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Score: ${best.score}/10 · Lowest co-channel interference", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        SectionCard("All Channels Breakdown") {
            ratings.forEach { r ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ch ${r.channel}", Modifier.width(54.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        repeat(10) { i ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .padding(horizontal = 1.dp)
                                    .background(
                                        if (i < r.score) channelScoreColor(r.score) else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${r.score}/10", Modifier.width(36.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("(${r.apCount} APs)", Modifier.width(54.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun channelScoreColor(score: Int) = when {
    score >= 8 -> Color(0xFF198038)
    score >= 5 -> Color(0xFFF1C21B)
    else -> Color(0xFFDA1E28)
}

@Composable private fun DeviceTypeIcon(device: NetworkDevice) {
    val identity = "${device.name} ${device.manufacturer}".lowercase()
    val icon = when {
        listOf("router", "gateway", "tp-link", "d-link", "netgear", "cisco").any { identity.contains(it) } -> Icons.Default.Router
        listOf("macbook", "laptop", "windows", "dell", "lenovo", "thinkpad").any { identity.contains(it) } -> Icons.Default.LaptopMac
        listOf("iphone", "android", "pixel", "galaxy", "xiaomi", "phone").any { identity.contains(it) } -> Icons.Default.PhoneAndroid
        else -> Icons.Default.Devices
    }
    Icon(icon, "Device type", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
}

@Composable private fun DevicesScreen(vm: AnalyzerViewModel, state: AnalyzerState) {
    val devices by vm.devices.collectAsState()
    LaunchedEffect(Unit) { vm.devices() }
    ScreenList {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LAN Devices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Button(vm::devices, shape = RoundedCornerShape(6.dp)) {
                Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Discover", fontSize = 13.sp)
            }
        }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        if (devices.isEmpty()) {
            SectionCard("No devices found") { Text("Tap Discover to probe your Wi‑Fi subnet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            devices.forEach { d ->
                SectionCard(if (d.name == "Unknown device") "Network device" else d.name) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DeviceTypeIcon(d)
                        Spacer(Modifier.width(8.dp))
                        Text(d.ip, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                    CopyValue("MAC", d.mac)
                    if (d.manufacturer != "Unknown manufacturer") CopyValue("Vendor", d.manufacturer)
                    CopyValue("Status", if (d.online) "Connected" else "Offline")
                    CopyValue("Last seen", DateFormat.getTimeInstance().format(Date(d.lastSeen)))
                }
            }
        }
    }
}

@Composable private fun SavedWifiScreen(state: AnalyzerState, privileged: Boolean, shizukuRunning: Boolean, checkPrivileges: () -> Unit, loadNetworks: () -> Unit) {
    ScreenList {
        SectionCard("Saved Wi‑Fi Credentials") {
            Text("Requires Root or Shizuku to read protected system Wi-Fi configs.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Button(onClick = loadNetworks, enabled = privileged, shape = RoundedCornerShape(6.dp)) {
                Text("Read saved networks", fontSize = 13.sp)
            }
        }
        state.savedWifi.forEach { network ->
            SectionCard(network.ssid) {
                CopyValue("SSID", network.ssid)
                CopyValue("Password", network.password ?: "Unavailable")
            }
        }
    }
}

@Composable private fun P2pScreen() {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(WifiP2pManager::class.java) }
    var peers by remember { mutableStateOf<List<WifiP2pDevice>>(emptyList()) }
    var status by remember { mutableStateOf(if (manager == null) "Wi‑Fi Direct unavailable." else "Ready to discover peers.") }
    val channel = remember(manager) { manager?.initialize(context, context.mainLooper) { status = "Channel lost." } }

    DisposableEffect(context, manager, channel) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, android.Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) return
                        manager?.requestPeers(channel) { peers = it.deviceList.toList() }
                    }
                }
            }
        }
        val filter = IntentFilter().apply { addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) }
        context.registerReceiver(receiver, filter)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    ScreenList {
        SectionCard("Wi‑Fi Direct (P2P)") {
            Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { manager?.discoverPeers(channel, null) },
                enabled = manager != null && channel != null,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Discover Peers", fontSize = 13.sp)
            }
        }
        if (peers.isNotEmpty()) {
            SectionCard("Discovered Peers (${peers.size})") {
                peers.forEach { peer ->
                    CopyValue("Name", peer.deviceName.ifBlank { "Unknown peer" })
                    CopyValue("MAC", peer.deviceAddress)
                }
            }
        }
    }
}

@Composable private fun TrafficScreen() {
    var lastRx by remember { mutableLongStateOf(TrafficStats.getTotalRxBytes()) }
    var lastTx by remember { mutableLongStateOf(TrafficStats.getTotalTxBytes()) }
    var rx by remember { mutableLongStateOf(0) }
    var tx by remember { mutableLongStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            val currentRx = TrafficStats.getTotalRxBytes()
            val currentTx = TrafficStats.getTotalTxBytes()
            rx = (currentRx - lastRx).coerceAtLeast(0)
            tx = (currentTx - lastTx).coerceAtLeast(0)
            lastRx = currentRx
            lastTx = currentTx
        }
    }

    ScreenList {
        SectionCard("Global Device Traffic") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Text("Download", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatRate(rx), fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Upload, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                    Text("Upload", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatRate(tx), fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable private fun SimScreen() {
    val context = LocalContext.current
    val telephony = remember { context.getSystemService(TelephonyManager::class.java) }
    val state = runCatching { simState(telephony?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN) }.getOrDefault("Unavailable")
    val operator = runCatching { telephony?.networkOperatorName.orEmpty().ifBlank { "Unknown" } }.getOrDefault("Unavailable")
    val type = runCatching { if (telephony == null) "Unavailable" else networkTypeName(telephony.dataNetworkType) }.getOrDefault("Unavailable")

    ScreenList {
        SectionCard("Cellular & SIM") {
            CopyValue("SIM State", state)
            CopyValue("Carrier", operator)
            CopyValue("Network Type", type)
        }
    }
}

@Composable private fun SettingsScreen(theme: AppTheme, setTheme: (AppTheme) -> Unit) {
    val context = LocalContext.current
    ScreenList {
        SectionCard("Appearance") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppTheme.entries.forEach { t ->
                    FilterChip(
                        selected = theme == t,
                        onClick = { setTheme(t) },
                        label = { Text(t.name.lowercase().replaceFirstChar { it.titlecase() }, fontSize = 12.sp) },
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            }
        }
        SectionCard("About") {
            CopyValue("App Version", BuildConfig.VERSION_NAME)
            Text("Engineered with Jetpack Compose & Material 3.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

private fun channel(frequency: Int) = when {
    frequency in 2412..2484 -> (frequency - 2407) / 5
    frequency >= 5000 -> (frequency - 5000) / 5
    else -> 0
}

private fun signalColor(rssi: Int) = when {
    rssi >= -50 -> Color(0xFF198038)
    rssi >= -65 -> Color(0xFF0F62FE)
    rssi >= -75 -> Color(0xFFF1C21B)
    else -> Color(0xFFDA1E28)
}

private fun formatRate(bytes: Long): String {
    val bits = bytes * 8
    return if (bits > 1_000_000) String.format("%.2f Mbps", bits / 1_000_000.0)
    else String.format("%.1f Kbps", bits / 1_000.0)
}

private fun simState(state: Int) = when (state) {
    TelephonyManager.SIM_STATE_READY -> "READY"
    TelephonyManager.SIM_STATE_ABSENT -> "ABSENT"
    TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN required"
    TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK required"
    else -> "Unknown"
}

private fun networkTypeName(type: Int) = when (type) {
    TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
    TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
    TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
    TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
    else -> "Unknown"
}
