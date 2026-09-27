package shifa.uz.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.lerp
import org.jetbrains.compose.resources.painterResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.img_logo
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens
import shifa.uz.ui.theme.ShifaSize

private const val SPLASH_MS = 1600 // 02 §8: animation length and minimum on-screen time

// ponytail: always ends on Login. AUTH-01 adds the session restore (4 s wait, 5 s fallback) here.
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(SPLASH_MS, easing = EaseInOutCubic))
        finish()
    }
    SplashScreenContent(progress.value)
}

/** Figma loading screen: [progress] 0 = Mobile 1 (`1614:12212`), 1 = Mobile 2 (`1614:12237`). */
@Composable
internal fun SplashScreenContent(progress: Float) {
    val tile = RoundedCornerShape(ShifaDimens.SplashTileRadius)
    Box(
        Modifier
            .fillMaxSize()
            .background(ShifaColors.Bg.Surface)
            .drawBehind {
                blob(lerp(ShifaDimens.SplashBlobAStart, ShifaDimens.SplashBlobAEnd, progress))
                blob(lerp(ShifaDimens.SplashBlobBStart, ShifaDimens.SplashBlobBEnd, progress))
            },
        contentAlignment = Alignment.Center,
    ) {
        // Figma also blurs the backdrop; skipped, blur froze low-end Android GPUs in Flutter.
        Box(
            Modifier
                .size(ShifaDimens.SplashTile)
                .clip(tile)
                .background(ShifaColors.Raw.GlassTile)
                .border(ShifaSize.S2, ShifaColors.Raw.GlassTileBorder.copy(alpha = progress), tile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(Res.drawable.img_logo),
                contentDescription = null,
                tint = lerp(ShifaColors.Icon.Inverse, ShifaColors.Raw.SplashLogo, progress),
            )
        }
    }
}

// Figma's blurred ellipse, rotated 45°. A radial gradient fades the edge over ±2σ instead of a real blur
// (Figma's blur value is 2σ).
private fun DrawScope.blob(offset: DpOffset) {
    val color = ShifaColors.Raw.SplashBlob
    val rx = ShifaDimens.SplashBlobWidth.toPx() / 2
    val fade = ShifaDimens.SplashBlobBlur.toPx()
    val radius = rx + fade
    val c = center + Offset(offset.x.toPx(), offset.y.toPx())
    rotate(45f, c) {
        scale(1f, ShifaDimens.SplashBlobHeight / ShifaDimens.SplashBlobWidth, c) {
            drawCircle(
                Brush.radialGradient(
                    (rx - fade) / radius to color,
                    rx / radius to color.copy(alpha = color.alpha / 2),
                    1f to color.copy(alpha = 0f),
                    center = c,
                    radius = radius,
                ),
                radius,
                c,
            )
        }
    }
}
