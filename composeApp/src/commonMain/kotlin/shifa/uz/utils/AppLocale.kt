package shifa.uz.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Points the platform locale at [code] for [content] (14 §9.3: CMP has no runtime locale switch).
 * Resources re-resolve because [LocalAppLocale] changing recomposes the whole tree.
 */
@Composable
expect fun ProvideAppLocale(
    code: String,
    content: @Composable () -> Unit,
)

internal val LocalAppLocale = staticCompositionLocalOf { "" }
