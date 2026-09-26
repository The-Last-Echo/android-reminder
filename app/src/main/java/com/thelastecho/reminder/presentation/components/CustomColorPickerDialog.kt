package com.thelastecho.reminder.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thelastecho.reminder.R
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun CustomColorPickerDialog(
    initialColor: Int,
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit
) {
    val hsv = remember(initialColor) { FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor, it) } }
    var hue by remember(initialColor) { mutableStateOf(hsv[0]) }
    var saturation by remember(initialColor) { mutableStateOf(hsv[1]) }
    var brightness by remember(initialColor) { mutableStateOf(hsv[2]) }
    val selectedColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Canvas(
                    Modifier.size(224.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { pos ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = pos.x - center.x
                                val dy = pos.y - center.y
                                hue = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f)
                                saturation = (sqrt(dx * dx + dy * dy) / (min(size.width, size.height) / 2f)).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = change.position.x - center.x
                                val dy = change.position.y - center.y
                                hue = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f)
                                saturation = (sqrt(dx * dx + dy * dy) / (min(size.width, size.height) / 2f)).coerceIn(0f, 1f)
                                change.consume()
                            }
                        }
                ) {
                    val diameter = min(size.width, size.height)
                    val radius = diameter / 2f
                    drawCircle(
                        brush = Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)),
                        radius = radius,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color.White, Color.Transparent), center = Offset(size.width / 2f, size.height / 2f), radius = radius),
                        radius = radius,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                    val angle = Math.toRadians(hue.toDouble()).toFloat()
                    val markerRadius = radius * saturation
                    val markerCenter = Offset(
                        size.width / 2f + kotlin.math.cos(angle) * markerRadius,
                        size.height / 2f + kotlin.math.sin(angle) * markerRadius
                    )
                    drawCircle(Color.White, radius = 9.dp.toPx(), center = markerCenter)
                    drawCircle(selectedColor, radius = 6.dp.toPx(), center = markerCenter)
                }
                Surface(color = selectedColor, shape = androidx.compose.foundation.shape.CircleShape, modifier = Modifier.size(44.dp)) {}
                Text(stringResource(R.string.brightness), style = MaterialTheme.typography.labelLarge)
                Slider(value = brightness, onValueChange = { brightness = it }, valueRange = 0.05f..1f)
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))) }) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
