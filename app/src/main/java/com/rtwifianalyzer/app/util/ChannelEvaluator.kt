package com.rtwifianalyzer.app.util

import com.rtwifianalyzer.app.domain.AccessPoint

data class ChannelRating(
    val channel: Int,
    val frequency: Int,
    val score: Int, // 1 to 10
    val apCount: Int,
    val isRecommended: Boolean = false
)

object ChannelEvaluator {
    val CHANNELS_24 = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
    val RECOMMENDED_24 = listOf(1, 6, 11)
    val CHANNELS_5G = listOf(36, 40, 44, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144, 149, 153, 157, 161, 165)

    fun evaluate24GHz(points: List<AccessPoint>): List<ChannelRating> {
        val points24 = points.filter { it.band == "2.4 GHz" && it.channel in CHANNELS_24 }
        val ratings = CHANNELS_24.map { ch ->
            var penalty = 0
            var count = 0
            points24.forEach { ap ->
                val dist = kotlin.math.abs(ap.channel - ch)
                if (dist <= 4) { // 2.4 GHz channel overlap range (+/- 22 MHz)
                    count++
                    val weight = when (dist) {
                        0 -> 4  // co-channel
                        1 -> 3  // adjacent channel
                        2 -> 2
                        3 -> 1
                        else -> 1
                    }
                    val signalFactor = when {
                        ap.rssi >= -60 -> 3
                        ap.rssi >= -75 -> 2
                        else -> 1
                    }
                    penalty += weight * signalFactor
                }
            }
            val score = (10 - penalty).coerceIn(1, 10)
            ChannelRating(channel = ch, frequency = 2407 + ch * 5, score = score, apCount = count)
        }
        
        // Find best recommended from standard non-overlapping channels (1, 6, 11)
        val bestChannel = ratings.filter { it.channel in RECOMMENDED_24 }.maxByOrNull { it.score }?.channel ?: 1
        return ratings.map { it.copy(isRecommended = it.channel == bestChannel) }
    }

    fun evaluate5GHz(points: List<AccessPoint>): List<ChannelRating> {
        val points5 = points.filter { it.band == "5 GHz" }
        val ratings = CHANNELS_5G.map { ch ->
            val matching = points5.filter { it.channel == ch }
            val count = matching.size
            var penalty = 0
            matching.forEach { ap ->
                penalty += when {
                    ap.rssi >= -60 -> 4
                    ap.rssi >= -75 -> 2
                    else -> 1
                }
            }
            val score = (10 - penalty).coerceIn(1, 10)
            ChannelRating(channel = ch, frequency = 5000 + ch * 5, score = score, apCount = count)
        }
        val best = ratings.maxByOrNull { it.score }?.channel ?: 36
        return ratings.map { it.copy(isRecommended = it.channel == best) }
    }
}
