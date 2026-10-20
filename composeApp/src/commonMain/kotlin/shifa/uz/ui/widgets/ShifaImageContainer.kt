package shifa.uz.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaDimens

/** Figma image-comtainer: white square with [url] fitted inside. */
@Composable
fun ShifaImageContainer(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(ShifaDimens.ImageContainerSize).background(ShifaColors.Bg.Surface)) {
        url?.let {
            KamelImage(
                resource = asyncPainterResource(it),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
