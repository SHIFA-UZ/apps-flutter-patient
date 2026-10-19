package shifa.uz.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaRadius
import shifa.uz.ui.theme.ShifaSize
import shifa.uz.ui.theme.ShifaSpacing
import shifa.uz.ui.theme.ShifaTextStyles
import shifa.uz.utils.encodePng

/** One finger-down to finger-up, in the pad's pixels. */
typealias SignatureStroke = List<Offset>

/**
 * Figma input-field as the signature box (1919:23186): [label] until something is drawn. The caller keeps the
 * [strokes]; [signaturePng] turns them into the image to send.
 */
@Composable
fun ShifaSignaturePad(
    strokes: List<SignatureStroke>,
    onStrokesChange: (List<SignatureStroke>) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val current by rememberUpdatedState(strokes)
    val change by rememberUpdatedState(onStrokesChange)
    Box(
        modifier
            .fillMaxWidth()
            .height(ShifaDimens.SignaturePadHeight)
            .background(ShifaColors.Bg.Surface, ShifaRadius.ControlMd)
            .border(ShifaSize.S2, ShifaColors.Border.Default, ShifaRadius.ControlMd),
    ) {
        if (strokes.isEmpty()) {
            Text(
                label,
                Modifier.padding(horizontal = ShifaSpacing.Inset.Sm, vertical = ShifaSpacing.Inset.Xs),
                style = ShifaTextStyles.caption(),
                color = ShifaColors.Text.Tertiary,
            )
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // The gesture keeps its own copy: [current] only updates on the next composition.
                    var drawn = emptyList<SignatureStroke>()
                    detectDragGestures(
                        onDragStart = {
                            drawn = current + listOf(listOf(it))
                            change(drawn)
                        },
                        onDrag = { input, _ ->
                            drawn = drawn.dropLast(1) + listOf(drawn.last() + input.position)
                            change(drawn)
                        },
                    )
                },
        ) { drawStrokes(strokes) }
    }
}

private fun DrawScope.drawStrokes(strokes: List<SignatureStroke>) {
    val style = Stroke(ShifaDimens.SignatureStroke.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    strokes.forEach { stroke ->
        val path = Path()
        path.moveTo(stroke.first().x, stroke.first().y)
        // A tap is a dot: a line to itself still draws the round cap.
        stroke.drop(1).ifEmpty { listOf(stroke.first()) }.forEach { path.lineTo(it.x, it.y) }
        drawPath(path, ShifaColors.Text.Primary, style = style)
    }
}

/** The [strokes] on white at the pad's [size], as a PNG (06 §10: plain PNG bytes, base64 is the caller's). */
fun signaturePng(
    strokes: List<SignatureStroke>,
    size: IntSize,
    density: Density,
): ByteArray {
    val image = ImageBitmap(size.width, size.height)
    CanvasDrawScope().draw(
        density,
        LayoutDirection.Ltr,
        Canvas(image),
        Size(size.width.toFloat(), size.height.toFloat()),
    ) {
        drawRect(ShifaColors.Bg.Surface)
        drawStrokes(strokes)
    }
    return image.encodePng()
}
