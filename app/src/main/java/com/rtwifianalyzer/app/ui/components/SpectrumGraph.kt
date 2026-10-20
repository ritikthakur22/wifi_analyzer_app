package com.rtwifianalyzer.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtwifianalyzer.app.domain.AccessPoint

private val SpectrumColors = listOf(
    Color(0xFF0F62FE), Color(0xFF00D2B4), Color(0xFF8A3FFC),
    Color(0xFFFF832B), Color(0xFFFA4D56), Color(0xFF198038), Color(0xFF1192E8)
)

@Composable
fun ParabolicSpectrumChart(
    band: String,
    accessPoints: List<AccessPoint>,
    modifier: Modifier = Modifier
) {
    val is24 = band == "2.4 GHz"
    val channelRange = if (is24) (1..14).toList() else listOf(36, 40, 44, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144, 149, 153, 157, 161, 165)
    val widthDp = if (is24) 400.dp else 800.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Canvas(modifier = Modifier.width(widthDp).height(180.dp)) {
                val chartW = size.width
                val chartH = size.height - 20.dp.toPx()
                val minRssi = -100f
                val maxRssi = -30f
                val rangeRssi = maxRssi - minRssi

                listOf(-40, -60, -80, -100).forEach { level ->
                    val y = chartH - ((level - minRssi) / rangeRssi) * chartH
                    drawLine(Color.Gray.copy(alpha = 0.15f), Offset(0f, y), Offset(chartW, y), 1f)
                }

                if (is24) {
                    fun chToX(ch: Float) = ((ch - 1f) / 13f) * (chartW - 40f) + 20f

                    accessPoints.forEachIndexed { idx, ap ->
                        val color = SpectrumColors[idx % SpectrumColors.size]
                        val centerCh = ap.channel.toFloat().coerceIn(1f, 14f)
                        val peakY = chartH - (((ap.rssi.coerceIn(-100, -30) - minRssi) / rangeRssi) * chartH)
                        val leftX = chToX(centerCh - 2.2f).coerceAtLeast(0f)
                        val centerX = chToX(centerCh)
                        val rightX = chToX(centerCh + 2.2f).coerceAtMost(chartW)
                        val baselineY = chartH

                        val path = Path().apply {
                            moveTo(leftX, baselineY)
                            cubicTo(leftX + (centerX - leftX) * 0.45f, baselineY, centerX - (centerX - leftX) * 0.2f, peakY, centerX, peakY)
                            cubicTo(centerX + (rightX - centerX) * 0.2f, peakY, rightX - (rightX - centerX) * 0.45f, baselineY, rightX, baselineY)
                        }
                        drawPath(path, color.copy(alpha = 0.18f))
                        drawPath(path, color, style = Stroke(width = 2.5f))
                    }

                    (1..14).forEach { ch ->
                        val x = chToX(ch.toFloat())
                        drawLine(Color.Gray.copy(alpha = 0.35f), Offset(x, chartH), Offset(x, chartH + 5.dp.toPx()), 1.5f)
                    }
                } else {
                    val channels = channelRange
                    val step = (chartW - 40f) / (channels.size - 1).coerceAtLeast(1)

                    accessPoints.forEachIndexed { idx, ap ->
                        val color = SpectrumColors[idx % SpectrumColors.size]
                        val chIndex = channels.indexOf(ap.channel)
                        if (chIndex != -1) {
                            val centerX = 20f + chIndex * step
                            val peakY = chartH - (((ap.rssi.coerceIn(-100, -30) - minRssi) / rangeRssi) * chartH)
                            val leftX = (centerX - step * 0.9f).coerceAtLeast(0f)
                            val rightX = (centerX + step * 0.9f).coerceAtMost(chartW)
                            val baselineY = chartH

                            val path = Path().apply {
                                moveTo(leftX, baselineY)
                                cubicTo(leftX + (centerX - leftX) * 0.4f, baselineY, centerX - (centerX - leftX) * 0.2f, peakY, centerX, peakY)
                                cubicTo(centerX + (rightX - centerX) * 0.2f, peakY, rightX - (rightX - centerX) * 0.4f, baselineY, rightX, baselineY)
                            }
                            drawPath(path, color.copy(alpha = 0.18f))
                            drawPath(path, color, style = Stroke(width = 2.5f))
                        }
                    }

                    channels.forEachIndexed { i, _ ->
                        val x = 20f + i * step
                        drawLine(Color.Gray.copy(alpha = 0.35f), Offset(x, chartH), Offset(x, chartH + 5.dp.toPx()), 1.5f)
                    }
                }
            }
        }

        if (accessPoints.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accessPoints.take(8).forEachIndexed { idx, ap ->
                    val color = SpectrumColors[idx % SpectrumColors.size]
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${ap.ssid} (Ch ${ap.channel})",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
