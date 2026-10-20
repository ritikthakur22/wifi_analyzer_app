package com.rtwifianalyzer.app.domain

import com.rtwifianalyzer.app.util.OuiLookup

data class WifiSnapshot(
    val ssid: String = "Not connected", val bssid: String = "—", val rssi: Int = -100,
    val frequency: Int = 0, val linkSpeed: Int = 0, val ipV4: String = "—", val ipV6: String = "—",
    val deviceMac: String = "—", val gateway: String = "—", val netmask: String = "—",
    val dns1: String = "—", val dns2: String = "—", val dhcp: String = "—", val publicIp: String = "—"
) {
    val quality: String get() = when { rssi >= -50 -> "Excellent"; rssi >= -60 -> "Good"; rssi >= -70 -> "Fair"; else -> "Poor" }
    val band: String get() = when { frequency in 5925..7125 -> "6 GHz"; frequency >= 4900 -> "5 GHz"; frequency > 0 -> "2.4 GHz"; else -> "—" }
}

data class AccessPoint(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val capabilities: String,
    val timestamp: Long,
    val channelWidth: Int = 0
) {
    val band: String get() = if (frequency in 5925..7125) "6 GHz" else if (frequency >= 4900) "5 GHz" else "2.4 GHz"
    val channel: Int get() = when {
        frequency in 2412..2484 -> if (frequency == 2484) 14 else (frequency - 2407) / 5
        frequency in 5170..5835 -> (frequency - 5000) / 5
        frequency in 5925..7125 -> (frequency - 5950) / 5
        frequency >= 5000 -> (frequency - 5000) / 5
        else -> 0
    }
    val channelWidthDisplay: String get() = when (channelWidth) {
        1 -> "40 MHz"
        2 -> "80 MHz"
        3 -> "160 MHz"
        4 -> "80+80 MHz"
        5 -> "320 MHz"
        else -> "20 MHz"
    }
    val signalPercent: Int get() = (((rssi + 100).coerceIn(0, 70)) * 100) / 70
    val security: String get() = when {
        capabilities.contains("SAE") -> "WPA3"
        capabilities.contains("WPA2") || capabilities.contains("PSK") -> "WPA2"
        capabilities.contains("WEP") -> "WEP"
        else -> "Open"
    }
    val manufacturer: String get() = OuiLookup.manufacturer(bssid)
}

data class PingDiagnostic(
    val target: String,
    val ip: String,
    val latencyMs: Long? = null,
    val isReachable: Boolean = false,
    val error: String? = null
)

data class NetworkDevice(val ip: String, val mac: String, val name: String = "Unknown device", val manufacturer: String = "Unknown", val online: Boolean = true, val lastSeen: Long = System.currentTimeMillis())
data class SavedWifiNetwork(val ssid: String, val password: String?)
enum class AppTheme { SYSTEM, LIGHT, DARK }
