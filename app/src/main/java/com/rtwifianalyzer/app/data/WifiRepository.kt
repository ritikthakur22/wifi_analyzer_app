package com.rtwifianalyzer.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.rtwifianalyzer.app.domain.AccessPoint
import com.rtwifianalyzer.app.domain.NetworkDevice
import com.rtwifianalyzer.app.domain.SavedWifiNetwork
import com.rtwifianalyzer.app.domain.WifiSnapshot
import com.rtwifianalyzer.app.util.OuiLookup
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.net.Inet4Address
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
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val wifiNetwork = connectivity.allNetworks.firstOrNull { network ->
            connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
        val addresses = wifiNetwork?.let(connectivity::getLinkProperties)?.linkAddresses.orEmpty()
        val ipv4 = addresses.firstOrNull { it.address is Inet4Address }?.address?.hostAddress ?: "—"
        // Link-local IPv6 addresses (fe80::/10) identify only a local interface and are not useful as a device IP.
        val ipv6 = addresses.firstOrNull { it.address !is Inet4Address && !it.address.isLinkLocalAddress && !it.address.isLoopbackAddress }
            ?.address?.hostAddress?.substringBefore('%') ?: "—"
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
        fun neighborLines(): List<String> = runCatching {
            if (isRoot) Shell.cmd(cmd).exec().out
            else if (hasShizuku) rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null).inputStream.bufferedReader().readLines()
            else File("/proc/net/arp").readLines().drop(1)
        }.getOrDefault(emptyList())

        // ARP is only populated after the phone has talked to a LAN host. Brief reachability
        // probes make the system learn neighbours before the table is read.
        warmArpCache()
        val outputLines = neighborLines()

        if (isRoot || hasShizuku) {
            outputLines.mapNotNull { line ->
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size >= 5 && parts.contains("lladdr")) {
                    val ip = parts[0]
                    val macIdx = parts.indexOf("lladdr") + 1
                    if (ip.isIpv4Address() && macIdx < parts.size) {
                        val mac = parts[macIdx]
                        if (mac != "00:00:00:00:00:00") {
                            device(ip, mac)
                        } else null
                    } else null
                } else null
            }
        } else {
            outputLines.mapNotNull { line ->
                val p = line.trim().split(Regex("\\s+")); if (p.size >= 4 && p[0].isIpv4Address() && p[3] != "00:00:00:00:00:00") device(p[0], p[3]) else null
            }
        }
    }.distinctBy { it.mac.uppercase() }

    private suspend fun warmArpCache() = coroutineScope {
        val dhcp = wifi.dhcpInfo ?: return@coroutineScope
        if (dhcp.gateway == 0) return@coroutineScope
        val gateway = ipToLong(intToIpv4(dhcp.gateway))
        val mask = ipToLong(intToIpv4(dhcp.netmask))
        val prefixLength = intToIpv4(dhcp.netmask).split('.').sumOf { it.toInt().countOneBits() }
        val hostCount = (1L shl (32 - prefixLength)) - 2
        // Avoid unexpectedly sweeping large corporate subnets; a /24 or smaller slice is
        // enough for normal home LAN discovery.
        if (prefixLength !in 24..30 || hostCount !in 1..254) return@coroutineScope
        val network = gateway and mask
        val localIp = ipToLong(intToIpv4(dhcp.ipAddress))
        (1..hostCount.toInt()).map { host -> network + host }
            .filter { it != gateway && it != localIp }
            .map { address -> async(Dispatchers.IO) { runCatching { InetAddress.getByName(longToIpv4(address)).isReachable(150) } } }
            .awaitAll()
    }

    private fun device(ip: String, mac: String): NetworkDevice {
        val manufacturer = OuiLookup.manufacturer(mac)
        val resolvedName = runCatching { InetAddress.getByName(ip).canonicalHostName }.getOrNull()
            ?.takeUnless { it == ip }
        return NetworkDevice(ip, mac, resolvedName ?: manufacturer.takeUnless { it == "Unknown manufacturer" } ?: "Unknown device", manufacturer)
    }

    private fun String.isIpv4Address() = runCatching { InetAddress.getByName(this) is Inet4Address }.getOrDefault(false)
    private fun intToIpv4(value: Int) = listOf(value and 255, value shr 8 and 255, value shr 16 and 255, value shr 24 and 255).joinToString(".")
    private fun ipToLong(value: String) = value.split('.').fold(0L) { result, octet -> (result shl 8) or octet.toLong() }
    private fun longToIpv4(value: Long) = listOf(24, 16, 8, 0).joinToString(".") { shift -> ((value shr shift) and 255).toString() }

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
