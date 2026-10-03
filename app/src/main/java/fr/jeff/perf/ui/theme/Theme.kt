package fr.jeff.perf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Une seule couleur d'accent, le reste en neutres.
private val Accent = Color(0xFF2F5FD0)
private val AccentSombre = Color(0xFF9DB6FF)

private val Clair = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EAFB),
    onPrimaryContainer = Color(0xFF0F2A6B),
    secondaryContainer = Color(0xFFE3EAFB),
    onSecondaryContainer = Color(0xFF0F2A6B),
    background = Color(0xFFFBFBFC),
    surface = Color(0xFFFBFBFC),
    surfaceContainer = Color(0xFFF2F3F5),
    surfaceContainerHigh = Color(0xFFECEDF0),
    surfaceVariant = Color(0xFFE6E7EB),
    onSurfaceVariant = Color(0xFF5B5F68),
    outline = Color(0xFFB9BCC4),
    outlineVariant = Color(0xFFE0E2E6),
)

private val Sombre = darkColorScheme(
    primary = AccentSombre,
    onPrimary = Color(0xFF0F2A6B),
    primaryContainer = Color(0xFF233A73),
    onPrimaryContainer = Color(0xFFDDE5FF),
    secondaryContainer = Color(0xFF233A73),
    onSecondaryContainer = Color(0xFFDDE5FF),
    background = Color(0xFF131416),
    surface = Color(0xFF131416),
    surfaceContainer = Color(0xFF1C1D20),
    surfaceContainerHigh = Color(0xFF26272B),
    surfaceVariant = Color(0xFF2E3034),
    onSurfaceVariant = Color(0xFFA8ACB5),
    outline = Color(0xFF5E626B),
    outlineVariant = Color(0xFF34363B),
)

@Composable
fun PerfTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Sombre else Clair,
        content = content,
    )
}
