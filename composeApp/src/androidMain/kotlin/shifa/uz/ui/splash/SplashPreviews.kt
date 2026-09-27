package shifa.uz.ui.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import shifa.uz.ui.widgets.PreviewTheme

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun SplashStartPreview() = PreviewTheme { SplashScreenContent(progress = 0f) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun SplashMiddlePreview() = PreviewTheme { SplashScreenContent(progress = 0.5f) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun SplashEndPreview() = PreviewTheme { SplashScreenContent(progress = 1f) }
