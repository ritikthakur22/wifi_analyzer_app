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

data class AccessPoint(val ssid: String, val bssid: String, val rssi: Int, val frequency: Int, val capabilities: String, val timestamp: Long) {
    val band: String get() = if (frequency >= 5925) "6 GHz" else if (frequency >= 4900) "5 GHz" else "2.4 GHz"
    val channel: Int get() = when { frequency in 2412..2484 -> (frequency - 2407) / 5; frequency >= 5000 -> (frequency - 5000) / 5; else -> 0 }
    val security: String get() = when { capabilities.contains("SAE") -> "WPA3"; capabilities.contains("WPA2") || capabilities.contains("PSK") -> "WPA2"; capabilities.contains("WEP") -> "WEP"; else -> "Open" }
    val manufacturer: String get() = OuiLookup.manufacturer(bssid)
}

data class NetworkDevice(val ip: String, val mac: String, val name: String = "Unknown device", val manufacturer: String = "Unknown", val online: Boolean = true, val lastSeen: Long = System.currentTimeMillis())
data class SavedWifiNetwork(val ssid: String, val password: String?)
enum class AppTheme { SYSTEM, LIGHT, DARK }
