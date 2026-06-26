package com.finanzas.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BarChart(
    datos: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    maxBarras: Int = 6
) {
    val top = datos.take(maxBarras)
    val max = top.maxOfOrNull { it.second } ?: 1.0

    val colores = listOf(
        Color(0xFF6B27E8), Color(0xFFF44336), Color(0xFF2196F3),
        Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF9C27B0)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        top.forEachIndexed { i, (cat, total) ->
            val fraccion = (total / max).coerceIn(0.0, 1.0).toFloat()
            val color    = colores[i % colores.size]

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = cat,
                    modifier = Modifier.width(100.dp),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color    = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraccion)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text       = "S/ %.0f".format(total),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = color
                )
            }
        }
    }
}
