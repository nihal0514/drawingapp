# Drawing App

This Android application provides a basic drawing canvas with various tools like a ruler, set squares, and a protractor. Users can draw freehand lines, snap to tool guides, and measure angles.

## Architecture Overview

The application follows a Model-View-ViewModel (MVVM) architecture pattern, primarily utilizing Jetpack Compose for the UI.

### Key Classes:

*   **`SimpleDrawingScreen.kt`**: This file contains the main Composable function (`SimpleDrawingScreen`) responsible for rendering the entire UI, including the drawing canvas, toolbars, and informational messages. It observes the state from the `SimpleDrawingViewModel` and delegates user interactions to it.
*   **`SimpleDrawingViewModel.kt` (Assumed)**: This ViewModel is the core of the application's logic.
    *   **State Management**: It holds the application's state, such as the list of drawn paths (`paths`), the current path being drawn, the selected tool (`currentTool`), the current color (`currentColor`), and the states of the drawing tools (e.g., `rulerState`, `setSquareState`, `protractorState`). It likely exposes these as `StateFlow` or `LiveData` to be observed by the Composable UI.
    *   **User Interaction Handling**: It contains functions to handle user input from the `Canvas` (e.g., `startDrawing`, `addPoint`, `finishDrawing`) and tool selections (e.g., `selectTool`, `selectColor`, `clear`).
    *   **Tool Logic**: It manages the placement, rotation, and state of the drawing tools.
*   **`DrawPath.kt` (Assumed)**: A data class likely representing a single continuous drawn line. It would store a list of `Offset` points, the color, and the stroke width for that path.
*   **`Tool.kt` (Assumed)**: An enum or sealed class defining the available tools (e.g., `DRAW`, `ERASE`, `RULER`, `SET_SQUARE_45`, `PROTRACTOR`).
*   **Tool State Data Classes (e.g., `RulerState`, `SetSquareState`, `ProtractorState`) (Assumed)**: These data classes hold the specific properties for each drawing tool, such as:
    *   `RulerState`: `isVisible`, `start: Offset`, `end: Offset`, `isPlaced`.
    *   `SetSquareState`: `isVisible`, `center: Offset`, `rotation: Float`, `type: Tool`, `isPlaced`.
    *   `ProtractorState`: `isVisible`, `center: Offset`, `radius: Float`, `ray1Angle: Float`, `ray2Angle: Float`, `isPlaced`.

### State Flow:

1.  **User Interaction**: The user interacts with the UI (e.g., drags on the `Canvas`, clicks a tool button).
2.  **View (Composable)**: The `SimpleDrawingScreen` Composable captures these interactions and calls the corresponding methods in the `SimpleDrawingViewModel`.
3.  **ViewModel**:
    *   Updates its internal state based on the interaction (e.g., adds points to the current path, changes the selected tool, updates tool position).
    *   If snapping is involved, it performs calculations to adjust the drawing points based on active tool guides.
4.  **State Observation**: The `SimpleDrawingScreen` observes the state changes in the `SimpleDrawingViewModel` (likely via Compose `State` objects derived from `StateFlow` or `LiveData`).
5.  **UI Recomposition**: Jetpack Compose automatically recomposes the relevant parts of the UI that depend on the changed state, leading to an updated display on the screen.

### Rendering Loop:

The rendering is managed by Jetpack Compose's declarative UI system.

1.  The `SimpleDrawingScreen` Composable defines the layout and what to draw based on the current state from the `SimpleDrawingViewModel`.
2.  The `Canvas` Composable is the primary drawing surface.
3.  Inside the `Canvas`'s `onDraw` scope:
    *   A white background is drawn.
    *   All completed paths (`viewModel.paths`) are iterated and drawn using the `drawPath` extension function.
    *   The current path being drawn (`viewModel.getCurrentPath()`) is rendered if it contains enough points. The color and stroke width depend on whether the current tool is `DRAW` or `ERASE`.
    *   If visible, the drawing tools (ruler, set square, protractor) are rendered by their respective `drawRuler`, `drawSetSquare`, and `drawProtractor` extension functions on `DrawScope`. These functions use the state objects (e.g., `viewModel.rulerState`) to determine their position, orientation, and appearance.
4.  When the state in the `SimpleDrawingViewModel` changes (e.g., a new point is added, a tool is moved), Compose detects this and triggers a recomposition of the `Canvas` (and other affected Composables), causing it to redraw with the updated information.

## Snapping Strategy & Data Structures

The snapping strategy appears to be implemented within the `SimpleDrawingViewModel` (though the exact logic isn't visible in `DrawingScreen.kt`). When a drawing tool (ruler, set square) is active and placed:

*   **Ruler Snapping**: When drawing, if a point is close to the line defined by the ruler's `start` and `end` points, the `ViewModel` likely adjusts the drawing point to lie exactly on the ruler's line.
*   **Set Square Snapping**: Similarly, when drawing near the edges of a placed set square, the `ViewModel` would adjust the drawing point to align with the relevant edge of the triangle.
*   **Protractor**: While the protractor itself doesn't directly snap drawing, it provides visual guides (rays) and angle measurements.

### Data Structures for Hit-Testing and Snapping (Inferred):

*   **Points and Lines**: The core data structures are `Offset` for points and pairs of `Offset`s (or mathematical line equations) for the edges of tools.
*   **Tool State Objects**: `RulerState`, `SetSquareState`, and `ProtractorState` store the geometric properties (center, start/end points, rotation, radius) necessary for snapping calculations.
*   **Distance Calculations**:
    *   For ruler snapping, the ViewModel would calculate the perpendicular distance from the current drawing point to the ruler's line segment.
    *   For set square snapping, it would calculate the distance from the drawing point to each of the three line segments forming the set square's edges.
*   **Thresholds**: A predefined snapping threshold (distance) is likely used. If the drawing point is within this threshold of a tool's guide, snapping occurs.

### Hit-Testing (for tool interaction and snapping):

*   **Tool Placement/Movement**:
    *   When dragging to place/move a tool, the `detectDragGestures` on the `Canvas` (or potentially on invisible handler areas around tools if they were more complex) would update the tool's state in the ViewModel (e.g., `center`, `start`, `end`, `rotation`).
    *   The `DrawingScreen.kt` shows `detectDragGestures` directly on the main canvas for drawing. It's implied that when a tool like the ruler is selected (`viewModel.currentTool == Tool.RULER`), these drag gestures are interpreted by the ViewModel to update the `rulerState` (e.g., placing `ruler.start` on `onDragStart` and `ruler.end` on subsequent `onDrag` events, or updating `center` and `rotation` for set squares).
*   **Protractor Ray Manipulation**: The `ProtractorState` includes `ray1Angle` and `ray2Angle`. The UI would need draggable handles (visualized by the yellow circles at `ray1End` and `ray2End`) to modify these angles. The hit-testing for these handles would involve checking if a touch point is within a certain radius of `ray1End` or `ray2End`.

No explicit spatial indexing structures (like Quadtrees or R-trees) are apparent from `DrawingScreen.kt`. Given the limited number of interactive tools typically active at once, direct distance calculations are likely sufficient for performance.

## Performance Notes

*   **What was measured (Speculative - based on typical Compose performance considerations):**
    *   **Recomposition Scope**: Care is generally taken in Compose to minimize the scope of recompositions. By using `State` objects and observing them, only the Composables that depend on a specific piece of state should recompose when that state changes. For example, changing the selected color should ideally only recompose the color picker buttons and parts of the canvas related to future drawing, not necessarily the entire toolbar.
    *   **Drawing Operations**: The drawing operations within the `Canvas` (e.g., `drawLine`, `drawCircle`, `drawPath`) are hardware-accelerated on modern Android devices.
    *   **Path Complexity**: Drawing very long or numerous complex paths could eventually impact performance. The current implementation redraws all completed paths and the current path on every relevant state change that affects the canvas.
    *   **Gesture Processing**: `detectDragGestures` is efficient for handling continuous input.

*   **Potential Trade-offs:**
    *   **Simplicity vs. Granular Recomposition**: The current structure with a main `Canvas` redrawing paths and tools is simple to understand. More complex scenarios might benefit from separate `Canvas` layers or more granular `derivedStateOf` usage to optimize recompositions further, but this adds complexity.
    *   **Immediate Mode Graphics**: Compose's drawing API is an immediate mode graphics API. This means that for every frame (or recomposition that affects the canvas), the drawing commands are re-issued. For very complex scenes, this could be less performant than a retained mode system, but it offers greater flexibility and fits well with Compose's declarative nature.
    *   **Snapping Calculation Cost**: If many complex snapping guides were active simultaneously, the cost of iterating through all of them and performing distance calculations for every point in a drag gesture could become noticeable. However, with only one major tool (ruler or set square) typically providing snapping guides at a time, this is unlikely to be an issue in the current design.
    *   **Eraser Implementation**: The eraser works by drawing white lines. This is simple but means erased strokes are still part of the `paths` list (just drawn in white). A more sophisticated eraser might modify the existing `DrawPath` objects or use blending modes, which could have different performance characteristics and complexity.

## Calibration Approach for Real-World Units

The current code in `DrawingScreen.kt` **does not appear to have an explicit calibration mechanism for real-world units** (like millimeters or inches).

*   **DPI Assumptions**:
    *   Sizes are defined in `dp` (density-independent pixels) for UI elements (buttons, padding) and `Float` values for canvas drawing coordinates (which are pixel-based within the canvas).
    *   The ruler and set square visual markings (e.g., `distance / 50f` for ruler marks, `size = 150f` for set square) are based on these pixel/canvas units.
    *   There's no direct conversion to physical units. The perceived size of a "50-unit" mark on the ruler will vary depending on the device's screen density (DPI).

*   **Potential Device-Based Calibration Flow (Not Implemented):**
    To achieve more accurate real-world unit representation, a calibration flow could be added:
    1.  **User Calibration Screen**: Ask the user to place a real-world object of known size (e.g., a credit card, a physical ruler) against the screen.
    2.  **On-Screen Guides**: Display adjustable on-screen guides that the user aligns with the edges of the physical object.
    3.  **Calculate Pixels Per Inch/CM**: From the known size of the object and the number of pixels it spans on the screen (as determined by the user's adjustments), the application could calculate a `pixelsPerMillimeter` or `pixelsPerInch` factor.
    4.  **Store Calibration Factor**: This factor would be stored in persistent storage (e.g., SharedPreferences).
    5.  **Apply Factor in Drawing**: When displaying measurements or drawing tools that represent real-world units, this calibration factor would be used to convert between pixel units and physical units. For example, the markings on the ruler could then represent actual centimeters or inches.

    This would make the tools more accurate for actual measurement but adds complexity to the application setup. For a "simple" drawing app, the current approach of using abstract canvas units is common.
