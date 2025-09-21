import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun SimpleDrawingScreen(viewModel: SimpleDrawingViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Drawing Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            viewModel.startDrawing(offset)
                        },
                        onDrag = { change, _ ->
                            viewModel.addPoint(change.position)
                        },
                        onDragEnd = {
                            viewModel.finishDrawing()
                        }
                    )
                }
        ) {
            // Background
            drawRect(
                color = Color.White,
                size = size
            )

            // Draw all completed paths
            viewModel.paths.forEach { path ->
                drawPath(path)
            }

            // Draw current path being drawn
            val currentPoints = viewModel.getCurrentPath()
            if (currentPoints.size > 1) {
                val currentColor = if (viewModel.currentTool == Tool.DRAW) viewModel.currentColor else Color.White
                val currentWidth = if (viewModel.currentTool == Tool.DRAW) 5f else 20f

                drawConnectedLines(
                    points = currentPoints,
                    color = currentColor,
                    strokeWidth = currentWidth
                )
            }

            // Draw tools
            if (viewModel.rulerState.isVisible) {
                drawRuler(viewModel.rulerState)
            }

            if (viewModel.setSquareState.isVisible) {
                drawSetSquare(viewModel.setSquareState)
            }

            if (viewModel.protractorState.isVisible) {
                drawProtractor(viewModel.protractorState)
            }
        }

        // Tool bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tool buttons row 1
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolButton("Draw", Icons.Default.Edit, Tool.DRAW, viewModel)
                ToolButton("Ruler", Icons.Default.Settings, Tool.RULER, viewModel)
                ToolButton("Erase", Icons.Default.Delete, Tool.ERASE, viewModel)
                Button(onClick = { viewModel.clear() }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }

            // Tool buttons row 2
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolButton("45° Square", Icons.Default.AccountBox, Tool.SET_SQUARE_45, viewModel)
                ToolButton("30-60° Square", Icons.Default.KeyboardArrowUp, Tool.SET_SQUARE_30_60, viewModel)
                ToolButton("Protractor", Icons.Default.PlayArrow, Tool.PROTRACTOR, viewModel)
            }

            // Color picker
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Colors:", color = Color.Black)

                val colors = listOf(
                    Color.Black, Color.Red, Color.Blue, Color.Green,
                    Color.Yellow, Color.Magenta, Color.Cyan
                )

                colors.forEach { color ->
                    Button(
                        onClick = { viewModel.selectColor(color) },
                        modifier = Modifier.size(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = color,
                            contentColor = if (color == Color.Black) Color.White else Color.Black
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        if (viewModel.currentColor == color) {
                            Text("✓", fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Instructions and measurements
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (viewModel.currentTool) {
                Tool.RULER -> {
                    InstructionCard(
                        if (viewModel.rulerState.isPlaced) {
                            "Ruler placed! Draw near the ruler line to snap to it"
                        } else {
                            "Drag to place ruler"
                        }
                    )
                }
                Tool.SET_SQUARE_45, Tool.SET_SQUARE_30_60 -> {
                    InstructionCard(
                        if (viewModel.setSquareState.isPlaced) {
                            "Set square placed! Draw near edges to snap to them"
                        } else {
                            "Drag to place and rotate set square"
                        }
                    )
                }
                Tool.PROTRACTOR -> {
                    Column {
                        InstructionCard(
                            if (viewModel.protractorState.isPlaced) {
                                "Drag ray endpoints to measure angles"
                            } else {
                                "Drag to place protractor"
                            }
                        )

                        if (viewModel.protractorState.isPlaced) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Green.copy(alpha = 0.8f)
                                )
                            ) {
                                Text(
                                    text = "Angle: ${viewModel.getAngleBetweenRays().roundToInt()}°",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ToolButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tool: Tool, viewModel: SimpleDrawingViewModel) {
    Button(
        onClick = { viewModel.selectTool(tool) },
        colors = if (viewModel.currentTool == tool) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        } else {
            ButtonDefaults.outlinedButtonColors()
        }
    ) {
        Icon(icon, contentDescription = text)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text)
    }
}

@Composable
fun InstructionCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.padding(12.dp)
        )
    }
}

// Drawing functions
fun DrawScope.drawPath(path: DrawPath) {
    drawConnectedLines(
        points = path.points,
        color = path.color,
        strokeWidth = path.strokeWidth
    )
}

fun DrawScope.drawConnectedLines(
    points: List<Offset>,
    color: Color,
    strokeWidth: Float
) {
    if (points.size < 2) return

    for (i in 0 until points.size - 1) {
        drawLine(
            color = color,
            start = points[i],
            end = points[i + 1],
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

fun DrawScope.drawRuler(ruler: RulerState) {
    if (!ruler.isVisible) return

    drawLine(
        color = Color.Blue,
        start = ruler.start,
        end = ruler.end,
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )

    drawCircle(color = Color.Blue, radius = 8f, center = ruler.start)
    drawCircle(color = Color.Blue, radius = 8f, center = ruler.end)

    val distance = sqrt(
        (ruler.end.x - ruler.start.x) * (ruler.end.x - ruler.start.x) +
                (ruler.end.y - ruler.start.y) * (ruler.end.y - ruler.start.y)
    )

    if (distance > 50f) {
        val numMarks = (distance / 50f).toInt().coerceAtMost(10)
        for (i in 1 until numMarks) {
            val t = i.toFloat() / numMarks
            val markX = ruler.start.x + t * (ruler.end.x - ruler.start.x)
            val markY = ruler.start.y + t * (ruler.end.y - ruler.start.y)
            drawCircle(
                color = Color.Blue.copy(alpha = 0.6f),
                radius = 3f,
                center = Offset(markX, markY)
            )
        }
    }
}

fun DrawScope.drawSetSquare(setSquare: SetSquareState) {
    if (!setSquare.isVisible) return

    val size = 150f
    val center = setSquare.center
    val rotation = setSquare.rotation

    val points = when (setSquare.type) {
        Tool.SET_SQUARE_45 -> {
            // 45-45-90 triangle
            listOf(
                rotatePoint(Offset(center.x - size/2, center.y + size/2), center, rotation),
                rotatePoint(Offset(center.x + size/2, center.y + size/2), center, rotation),
                rotatePoint(Offset(center.x, center.y - size/2), center, rotation)
            )
        }
        Tool.SET_SQUARE_30_60 -> {
            // 30-60-90 triangle
            listOf(
                rotatePoint(Offset(center.x - size/2, center.y + size/4), center, rotation),
                rotatePoint(Offset(center.x + size/2, center.y + size/4), center, rotation),
                rotatePoint(Offset(center.x, center.y - size * 0.866f/2), center, rotation)
            )
        }
        else -> emptyList()
    }

    // Draw triangle edges
    for (i in points.indices) {
        val start = points[i]
        val end = points[(i + 1) % points.size]
        drawLine(
            color = Color.Red,
            start = start,
            end = end,
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }

    // Draw corner markers
    points.forEach { point ->
        drawCircle(
            color = Color.Red,
            radius = 6f,
            center = point
        )
    }

    // Draw center point
    drawCircle(
        color = Color.Red.copy(alpha = 0.5f),
        radius = 4f,
        center = center
    )
}

fun DrawScope.drawProtractor(protractor: ProtractorState) {
    if (!protractor.isVisible) return

    val center = protractor.center
    val radius = protractor.radius

    // Draw main circle
    drawCircle(
        color = Color.Green,
        radius = radius,
        center = center,
        style = Stroke(width = 3f)
    )

    // Draw angle markers
    for (i in 0..360 step 10) {
        val angle = Math.toRadians(i.toDouble()).toFloat()
        val isMainMark = i % 30 == 0
        val innerRadius = if (isMainMark) radius - 15f else radius - 8f

        val start = Offset(
            center.x + innerRadius * cos(angle),
            center.y + innerRadius * sin(angle)
        )
        val end = Offset(
            center.x + radius * cos(angle),
            center.y + radius * sin(angle)
        )

        drawLine(
            color = Color.Green,
            start = start,
            end = end,
            strokeWidth = if (isMainMark) 2f else 1f
        )
    }

    // Draw rays
    val ray1End = Offset(
        center.x + radius * cos(protractor.ray1Angle),
        center.y + radius * sin(protractor.ray1Angle)
    )
    val ray2End = Offset(
        center.x + radius * cos(protractor.ray2Angle),
        center.y + radius * sin(protractor.ray2Angle)
    )

    drawLine(
        color = Color.Yellow,
        start = center,
        end = ray1End,
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )

    drawLine(
        color = Color.Yellow,
        start = center,
        end = ray2End,
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )

    // Draw ray endpoints
    drawCircle(color = Color.Yellow, radius = 10f, center = ray1End)
    drawCircle(color = Color.Yellow, radius = 10f, center = ray2End)

    // Draw center
    drawCircle(color = Color.Green, radius = 8f, center = center)
}

private fun rotatePoint(point: Offset, center: Offset, angle: Float): Offset {
    val cos = cos(angle)
    val sin = sin(angle)
    val dx = point.x - center.x
    val dy = point.y - center.y

    return Offset(
        center.x + dx * cos - dy * sin,
        center.y + dx * sin + dy * cos
    )
}