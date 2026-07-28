package com.rtwifianalyzer.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import com.rtwifianalyzer.app.domain.AccessPoint
import com.rtwifianalyzer.app.domain.NetworkDevice
import com.rtwifianalyzer.app.domain.SavedWifiNetwork
import com.rtwifianalyzer.app.domain.WifiSnapshot
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class WifiRepository @Inject constructor(private val wifi: WifiManager, @ApplicationContext private val context: Context) {
    fun isWifiConnected(): Boolean = runCatching {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        connectivity.getNetworkCapabilities(connectivity.activeNetwork)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }.getOrDefault(false)

    @Suppress("DEPRECATION") fun snapshot(): WifiSnapshot = runCatching {
        val i = wifi.connectionInfo
        if (i == null) {
            WifiSnapshot()
        } else {
        val dhcp = wifi.dhcpInfo
        fun le(value: Int): String = listOf(value and 255, value shr 8 and 255, value shr 16 and 255, value shr 24 and 255).joinToString(".")
        fun <T> java.util.Enumeration<T>.asList(): List<T> = buildList { while (hasMoreElements()) add(nextElement()) }
        val ips = NetworkInterface.getNetworkInterfaces()?.asList().orEmpty().flatMap { ni -> ni.inetAddresses.asList().filter { !it.isLoopbackAddress } }
        val ipv4 = ips.firstOrNull { it.hostAddress?.contains(':') == false }?.hostAddress ?: "—"
        val ipv6 = ips.firstOrNull { it.hostAddress?.contains(':') == true }?.hostAddress?.substringBefore('%') ?: "—"
        WifiSnapshot(i.ssid?.removeSurrounding("\"") ?: "Not connected", i.bssid ?: "—", i.rssi, i.frequency, i.linkSpeed, ipv4, ipv6, i.macAddress ?: "Unavailable", le(dhcp.gateway), le(dhcp.netmask), le(dhcp.dns1), le(dhcp.dns2), le(dhcp.serverAddress))
        }
    }.getOrDefault(WifiSnapshot())
    fun scan(): List<AccessPoint> = runCatching { wifi.scanResults.orEmpty().map { result ->
        val ssid = result.SSID.takeIf { !it.isNullOrBlank() && it != "<unknown ssid>" } ?: "[Hidden]"
        AccessPoint(ssid, result.BSSID, result.level, result.frequency, result.capabilities ?: "", result.timestamp / 1000)
    }.sortedByDescending { it.rssi } }.getOrDefault(emptyList())
    fun requestScan(): Boolean = runCatching { wifi.startScan() }.getOrDefault(false)
    suspend fun publicIp(): String = withContext(Dispatchers.IO) { runCatching { URL("https://api.ipify.org").readText().trim() }.getOrDefault("Unavailable") }
    suspend fun arpDevices(): List<NetworkDevice> = withContext(Dispatchers.IO) {
        val isRoot = runCatching { Shell.getShell().isRoot }.getOrDefault(false)
        val hasShizuku = runCatching { rikka.shizuku.Shizuku.pingBinder() && rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
        val cmd = "ip neigh"
        val outputLines = runCatching {
            if (isRoot) Shell.cmd(cmd).exec().out
            else if (hasShizuku) rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null).inputStream.bufferedReader().readLines()
            else File("/proc/net/arp").readLines().drop(1)
        }.getOrDefault(emptyList())

        if (isRoot || hasShizuku) {
            outputLines.mapNotNull { line ->
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size >= 5 && parts.contains("lladdr")) {
                    val ip = parts[0]
                    val macIdx = parts.indexOf("lladdr") + 1
                    if (macIdx < parts.size) {
                        val mac = parts[macIdx]
                        if (mac != "00:00:00:00:00:00") {
                            NetworkDevice(ip, mac, runCatching { InetAddress.getByName(ip).canonicalHostName }.getOrDefault("Unknown device"))
                        } else null
                    } else null
                } else null
            }
        } else {
            outputLines.mapNotNull { line ->
                val p = line.trim().split(Regex("\\s+")); if (p.size >= 4 && p[3] != "00:00:00:00:00:00") NetworkDevice(p[0], p[3], runCatching { InetAddress.getByName(p[0]).canonicalHostName }.getOrDefault("Unknown device")) else null
            }
        }
    }

    suspend fun savedWifiNetworks(): Pair<List<SavedWifiNetwork>, String?> = withContext(Dispatchers.IO) {
        val isRoot = runCatching { Shell.getShell().isRoot }.getOrDefault(false)
        val hasShizuku = runCatching { rikka.shizuku.Shizuku.pingBinder() && rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
        if (!isRoot && !hasShizuku) {
            return@withContext emptyList<SavedWifiNetwork>() to "Saved network passwords require Root or Shizuku access on Android."
        }
        val output = runCatching {
            val cmd = "for f in /data/misc/apexdata/com.android.wifi/WifiConfigStore.xml /data/misc/wifi/WifiConfigStore.xml /data/misc/wifi/wpa_supplicant.conf; do [ -r \$f ] && cat \$f; done"
            if (isRoot) Shell.cmd(cmd).exec().out.joinToString("\n")
            else rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null).inputStream.bufferedReader().use { it.readText() }
        }.getOrDefault("")
        val networks = parseSavedNetworks(output)
        networks to if (networks.isEmpty()) "No readable saved Wi‑Fi configuration was found on this Android build." else null
    }

    private fun parseSavedNetworks(config: String): List<SavedWifiNetwork> {
        fun decode(value: String) = value.replace("&quot;", "\"").replace("&amp;", "&").trim().removeSurrounding("\"")
        fun value(block: String, name: String) = Regex("<string name=\\\"$name\\\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL).find(block)?.groupValues?.get(1)?.let(::decode)
        val xml = Regex("<Network>(.*?)</Network>", RegexOption.DOT_MATCHES_ALL).findAll(config).mapNotNull { match ->
            val block = match.groupValues[1]
            value(block, "SSID")?.takeIf { it.isNotBlank() }?.let { SavedWifiNetwork(it, value(block, "PreSharedKey")?.takeUnless { key -> key.isBlank() || key == "*" || key.equals("null", true) }) }
        }
        val supplicant = Regex("network=\\s*\\{(.*?)\\}", RegexOption.DOT_MATCHES_ALL).findAll(config).mapNotNull { match ->
            val block = match.groupValues[1]
            val ssid = Regex("ssid=\\\"(.*?)\\\"").find(block)?.groupValues?.get(1)
            val password = Regex("psk=\\\"(.*?)\\\"").find(block)?.groupValues?.get(1)
            ssid?.takeIf { it.isNotBlank() }?.let { SavedWifiNetwork(it, password) }
        }
        return (xml + supplicant).distinctBy { it.ssid }.sortedBy { it.ssid.lowercase() }.toList()
    }
}
