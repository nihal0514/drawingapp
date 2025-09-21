import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class DrawPath(
    val points: List<Offset>,
    val color: Color = Color.Black,
    val strokeWidth: Float = 5f
)

enum class Tool {
    DRAW, ERASE, RULER, SET_SQUARE_45, SET_SQUARE_30_60, PROTRACTOR
}

data class RulerState(
    val start: Offset = Offset.Zero,
    val end: Offset = Offset.Zero,
    val isPlaced: Boolean = false,
    val isVisible: Boolean = false
)

data class SetSquareState(
    val center: Offset = Offset.Zero,
    val rotation: Float = 0f,
    val isPlaced: Boolean = false,
    val isVisible: Boolean = false,
    val type: Tool = Tool.SET_SQUARE_45
)

data class ProtractorState(
    val center: Offset = Offset.Zero,
    val radius: Float = 100f,
    val ray1Angle: Float = 0f,
    val ray2Angle: Float = 0f,
    val isPlaced: Boolean = false,
    val isVisible: Boolean = false,
    val isDraggingRay1: Boolean = false,
    val isDraggingRay2: Boolean = false
)
