package com.rtwifianalyzer.app.data

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import com.rtwifianalyzer.app.domain.AccessPoint
import com.rtwifianalyzer.app.domain.NetworkDevice
import com.rtwifianalyzer.app.domain.WifiSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class WifiRepository @Inject constructor(private val wifi: WifiManager, @ApplicationContext private val context: Context) {
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
        AccessPoint(result.SSID.ifBlank { "[Hidden]" }, result.BSSID, result.level, result.frequency, result.capabilities ?: "", result.timestamp / 1000)
    }.sortedByDescending { it.rssi } }.getOrDefault(emptyList())
    fun requestScan(): Boolean = runCatching { wifi.startScan() }.getOrDefault(false)
    suspend fun publicIp(): String = withContext(Dispatchers.IO) { runCatching { URL("https://api.ipify.org").readText().trim() }.getOrDefault("Unavailable") }
    suspend fun arpDevices(): List<NetworkDevice> = withContext(Dispatchers.IO) {
        runCatching { File("/proc/net/arp").readLines().drop(1).mapNotNull { line ->
            val p = line.trim().split(Regex("\\s+")); if (p.size >= 4 && p[3] != "00:00:00:00:00:00") NetworkDevice(p[0], p[3], runCatching { InetAddress.getByName(p[0]).canonicalHostName }.getOrDefault("Unknown device")) else null
        }}.getOrDefault(emptyList())
    }
}
