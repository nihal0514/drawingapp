import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// Simple ViewModel
class SimpleDrawingViewModel : ViewModel() {
    var paths by mutableStateOf<List<DrawPath>>(emptyList())
        private set

    var currentTool by mutableStateOf(Tool.DRAW)
        private set

    var currentColor by mutableStateOf(Color.Black)
        private set

    var rulerState by mutableStateOf(RulerState())
        private set

    var setSquareState by mutableStateOf(SetSquareState())
        private set

    var protractorState by mutableStateOf(ProtractorState())
        private set

    private var currentPath = mutableListOf<Offset>()

    fun startDrawing(point: Offset) {
        when (currentTool) {
            Tool.DRAW, Tool.ERASE -> {
                currentPath.clear()
                currentPath.add(point)
            }
            Tool.RULER -> {
                if (!rulerState.isPlaced) {
                    rulerState = rulerState.copy(
                        start = point,
                        end = point,
                        isVisible = true,
                        isPlaced = false
                    )
                }
            }
            Tool.SET_SQUARE_45, Tool.SET_SQUARE_30_60 -> {
                if (!setSquareState.isPlaced) {
                    setSquareState = setSquareState.copy(
                        center = point,
                        isVisible = true,
                        isPlaced = false,
                        type = currentTool
                    )
                }
            }
            Tool.PROTRACTOR -> {
                if (!protractorState.isPlaced) {
                    protractorState = protractorState.copy(
                        center = point,
                        isVisible = true,
                        isPlaced = false
                    )
                } else {
                    // Check if clicking on rays to start dragging
                    val ray1End = Offset(
                        protractorState.center.x + protractorState.radius * cos(protractorState.ray1Angle),
                        protractorState.center.y + protractorState.radius * sin(protractorState.ray1Angle)
                    )
                    val ray2End = Offset(
                        protractorState.center.x + protractorState.radius * cos(protractorState.ray2Angle),
                        protractorState.center.y + protractorState.radius * sin(protractorState.ray2Angle)
                    )

                    val distToRay1 = sqrt((point.x - ray1End.x).pow(2) + (point.y - ray1End.y).pow(2))
                    val distToRay2 = sqrt((point.x - ray2End.x).pow(2) + (point.y - ray2End.y).pow(2))

                    if (distToRay1 < 30) {
                        protractorState = protractorState.copy(isDraggingRay1 = true)
                    } else if (distToRay2 < 30) {
                        protractorState = protractorState.copy(isDraggingRay2 = true)
                    }
                }
            }
        }
    }

    fun addPoint(point: Offset) {
        when (currentTool) {
            Tool.DRAW, Tool.ERASE -> {
                currentPath.add(point)
            }
            Tool.RULER -> {
                if (rulerState.isVisible && !rulerState.isPlaced) {
                    rulerState = rulerState.copy(end = point)
                }
            }
            Tool.SET_SQUARE_45, Tool.SET_SQUARE_30_60 -> {
                if (setSquareState.isVisible && !setSquareState.isPlaced) {
                    // Calculate rotation based on drag direction
                    val dx = point.x - setSquareState.center.x
                    val dy = point.y - setSquareState.center.y
                    val rotation = atan2(dy, dx)
                    setSquareState = setSquareState.copy(rotation = rotation)
                }
            }
            Tool.PROTRACTOR -> {
                if (protractorState.isVisible) {
                    if (protractorState.isDraggingRay1) {
                        val dx = point.x - protractorState.center.x
                        val dy = point.y - protractorState.center.y
                        val angle = atan2(dy, dx)
                        val snappedAngle = snapAngle(angle)
                        protractorState = protractorState.copy(ray1Angle = snappedAngle)
                    } else if (protractorState.isDraggingRay2) {
                        val dx = point.x - protractorState.center.x
                        val dy = point.y - protractorState.center.y
                        val angle = atan2(dy, dx)
                        val snappedAngle = snapAngle(angle)
                        protractorState = protractorState.copy(ray2Angle = snappedAngle)
                    }
                }
            }
        }
    }

    fun finishDrawing() {
        when (currentTool) {
            Tool.DRAW, Tool.ERASE -> {
                if (currentPath.size > 1) {
                    val newPath = DrawPath(
                        points = currentPath.toList(),
                        color = if (currentTool == Tool.DRAW) currentColor else Color.White,
                        strokeWidth = if (currentTool == Tool.DRAW) 5f else 20f
                    )
                    paths = paths + newPath
                }
                currentPath.clear()
            }
            Tool.RULER -> {
                if (rulerState.isVisible && !rulerState.isPlaced) {
                    rulerState = rulerState.copy(isPlaced = true)
                }
            }
            Tool.SET_SQUARE_45, Tool.SET_SQUARE_30_60 -> {
                if (setSquareState.isVisible && !setSquareState.isPlaced) {
                    setSquareState = setSquareState.copy(isPlaced = true)
                }
            }
            Tool.PROTRACTOR -> {
                if (protractorState.isVisible) {
                    if (!protractorState.isPlaced) {
                        protractorState = protractorState.copy(isPlaced = true)
                    } else {
                        protractorState = protractorState.copy(
                            isDraggingRay1 = false,
                            isDraggingRay2 = false
                        )
                    }
                }
            }
        }
    }

    fun getCurrentPath(): List<Offset> = currentPath.toList()

    fun selectTool(tool: Tool) {
        currentTool = tool
        // Hide all tools when switching
        if (tool != Tool.RULER) {
            rulerState = rulerState.copy(isVisible = false, isPlaced = false)
        }
        if (tool != Tool.SET_SQUARE_45 && tool != Tool.SET_SQUARE_30_60) {
            setSquareState = setSquareState.copy(isVisible = false, isPlaced = false)
        }
        if (tool != Tool.PROTRACTOR) {
            protractorState = protractorState.copy(isVisible = false, isPlaced = false)
        }
    }

    fun selectColor(color: Color) {
        currentColor = color
    }

    fun drawAlongRuler(point: Offset) {
        if (rulerState.isPlaced && currentTool == Tool.RULER) {
            val closestPoint = getClosestPointOnLine(rulerState.start, rulerState.end, point)
            val newPath = DrawPath(
                points = listOf(closestPoint, closestPoint),
                color = currentColor,
                strokeWidth = 5f
            )
            paths = paths + newPath
        }
    }

    fun drawAlongSetSquare(point: Offset) {
        if (setSquareState.isPlaced && (currentTool == Tool.SET_SQUARE_45 || currentTool == Tool.SET_SQUARE_30_60)) {
            val edges = getSetSquareEdges()
            var closestPoint = point
            var minDistance = Float.MAX_VALUE

            // Find closest edge
            edges.forEach { (start, end) ->
                val pointOnLine = getClosestPointOnLine(start, end, point)
                val distance = sqrt((point.x - pointOnLine.x).pow(2) + (point.y - pointOnLine.y).pow(2))
                if (distance < minDistance && distance < 30f) { // 30f is snap threshold
                    minDistance = distance
                    closestPoint = pointOnLine
                }
            }

            if (minDistance < 30f) {
                val newPath = DrawPath(
                    points = listOf(closestPoint, closestPoint),
                    color = currentColor,
                    strokeWidth = 5f
                )
                paths = paths + newPath
            }
        }
    }

    fun clear() {
        paths = emptyList()
        currentPath.clear()
        rulerState = RulerState()
        setSquareState = SetSquareState()
        protractorState = ProtractorState()
    }

    private fun getClosestPointOnLine(lineStart: Offset, lineEnd: Offset, point: Offset): Offset {
        val A = point.x - lineStart.x
        val B = point.y - lineStart.y
        val C = lineEnd.x - lineStart.x
        val D = lineEnd.y - lineStart.y

        val dot = A * C + B * D
        val lenSq = C * C + D * D

        if (lenSq == 0f) return lineStart

        val param = dot / lenSq
        val clampedParam = param.coerceIn(0f, 1f)

        return Offset(
            lineStart.x + clampedParam * C,
            lineStart.y + clampedParam * D
        )
    }

    private fun getSetSquareEdges(): List<Pair<Offset, Offset>> {
        if (!setSquareState.isPlaced) return emptyList()

        val size = 150f
        val center = setSquareState.center
        val rotation = setSquareState.rotation

        return when (setSquareState.type) {
            Tool.SET_SQUARE_45 -> {
                // 45-45-90 triangle
                val p1 = rotatePoint(Offset(center.x - size/2, center.y + size/2), center, rotation)
                val p2 = rotatePoint(Offset(center.x + size/2, center.y + size/2), center, rotation)
                val p3 = rotatePoint(Offset(center.x, center.y - size/2), center, rotation)

                listOf(
                    p1 to p2,  // base
                    p2 to p3,  // right edge
                    p3 to p1   // left edge
                )
            }
            Tool.SET_SQUARE_30_60 -> {
                // 30-60-90 triangle
                val p1 = rotatePoint(Offset(center.x - size/2, center.y + size/4), center, rotation)
                val p2 = rotatePoint(Offset(center.x + size/2, center.y + size/4), center, rotation)
                val p3 = rotatePoint(Offset(center.x, center.y - size * 0.866f/2), center, rotation) // height = size * sqrt(3)/2

                listOf(
                    p1 to p2,  // base
                    p2 to p3,  // right edge
                    p3 to p1   // left edge
                )
            }
            else -> emptyList()
        }
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

    private fun snapAngle(angle: Float): Float {
        val degrees = Math.toDegrees(angle.toDouble()).toFloat()
        val commonAngles = listOf(0f, 30f, 45f, 60f, 90f, 120f, 135f, 150f, 180f, 210f, 225f, 240f, 270f, 300f, 315f, 330f)

        // Find closest common angle
        val closest = commonAngles.minByOrNull { abs(it - degrees) }
        return if (closest != null && abs(closest - degrees) < 5f) {
            Math.toRadians(closest.toDouble()).toFloat()
        } else {
            // Snap to nearest degree
            Math.toRadians(degrees.roundToInt().toDouble()).toFloat()
        }
    }

    fun getAngleBetweenRays(): Float {
        val angle1 = Math.toDegrees(protractorState.ray1Angle.toDouble()).toFloat()
        val angle2 = Math.toDegrees(protractorState.ray2Angle.toDouble()).toFloat()
        var diff = abs(angle2 - angle1)
        if (diff > 180) diff = 360 - diff
        return diff
    }
}