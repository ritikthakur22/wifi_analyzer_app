package com.rtwifianalyzer.app.util

/** Small offline seed table. Unknown prefixes remain explicitly labelled instead of guessed. */
object OuiLookup {
    private val vendors = mapOf(
        "00:1A:11" to "Google", "F4:F5:D8" to "Google", "3C:5A:B4" to "Google",
        "FC:FB:FB" to "Apple", "AC:BC:32" to "Apple", "28:CF:E9" to "Apple",
        "00:50:F2" to "Microsoft", "00:1D:0F" to "TP-Link", "50:C7:BF" to "TP-Link",
        "CC:2D:E0" to "Xiaomi", "18:D6:C7" to "Xiaomi", "00:1E:58" to "D-Link",
        "00:24:A8" to "Cisco", "F8:1A:67" to "TP-Link"
    )
    fun manufacturer(mac: String): String = vendors[mac.uppercase().take(8)] ?: "Unknown manufacturer"
}
