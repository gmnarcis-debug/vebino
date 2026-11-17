package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AuroraTeal
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CoralRed
import com.example.ui.theme.GrapePurple
import com.example.ui.theme.GrapeSoft
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.OrchidDeep
import com.example.ui.theme.OrchidPink
import com.example.ui.theme.RosePink
import com.example.ui.theme.SunriseAmber
import com.example.ui.theme.SunsetOrange
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.IndigoDeep
import com.example.ui.theme.MintGreen
import com.example.ui.theme.PinkGlow
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SoftAmber
import com.example.ui.theme.SoftOrange
import kotlin.math.cos
import kotlin.math.sin

enum class Webino3DIconType {
    HOME,
    PROJECTS,
    BUILD,
    PREVIEW,
    CUSTOMIZE,
    OUTPUTS,
    SIGNING,
    TEMPLATES,
    SETTINGS,
    HELP,
    HISTORY,
    ABOUT,
}

private data class IconPalette3D(
    val frontStart: Color,
    val frontEnd: Color,
    val backPlate: Color,
    val shadowGlow: Color,
)

private fun pal(start: Color, end: Color, back: Color) =
    IconPalette3D(frontStart = start, frontEnd = end, backPlate = back, shadowGlow = end)

private fun paletteFor(type: Webino3DIconType): IconPalette3D = when (type) {
    Webino3DIconType.HOME -> pal(GrapePurple, OceanBlue, AuroraTeal)
    Webino3DIconType.PROJECTS -> pal(SunsetOrange, RosePink, OrchidPink)
    Webino3DIconType.BUILD -> pal(RosePink, GrapePurple, SunsetOrange)
    Webino3DIconType.PREVIEW -> pal(AuroraTeal, OceanBlue, GrapeSoft)
    Webino3DIconType.CUSTOMIZE -> pal(OrchidPink, OrchidDeep, GrapeSoft)
    Webino3DIconType.OUTPUTS -> pal(AuroraTeal, OceanBlue, CyanAccent)
    Webino3DIconType.SIGNING -> pal(SunriseAmber, CoralRed, RosePink)
    Webino3DIconType.TEMPLATES -> pal(GrapeSoft, OrchidDeep, OrchidPink)
    Webino3DIconType.SETTINGS -> pal(OceanBlue, GrapePurple, AuroraTeal)
    Webino3DIconType.HELP -> pal(GrapeSoft, OceanBlue, AuroraTeal)
    Webino3DIconType.HISTORY -> pal(SunsetOrange, OrchidDeep, RosePink)
    Webino3DIconType.ABOUT -> pal(OceanBlue, GrapePurple, OrchidPink)
}

@Composable
fun Webino3DGlassIcon(
    type: Webino3DIconType,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    elevated: Boolean = true,
    contentDescription: String? = null,
) {
    val palette = paletteFor(type)
    val animScale by animateFloatAsState(
        targetValue = if (elevated) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "icon3dScale",
    )
    val semanticsModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    Box(
        modifier = semanticsModifier
            .size(size)
            .scale(animScale),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val pad = w * 0.11f
            val cardW = w - pad * 2f
            val cardH = h - pad * 2f
            val corner = CornerRadius(cardW * 0.30f, cardW * 0.30f)

            // 1. Soft colored drop shadow aura underneath
            drawRoundRect(
                color = palette.shadowGlow.copy(alpha = if (elevated) 0.24f else 0.12f),
                topLeft = Offset(pad * 0.95f, pad * 1.48f),
                size = Size(cardW * 1.02f, cardH * 0.96f),
                cornerRadius = corner,
            )

            // 2. Tilted frosted translucent backing prism
            rotate(degrees = -10f, pivot = Offset(w * 0.52f, h * 0.52f)) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            palette.backPlate.copy(alpha = 0.52f),
                            palette.frontEnd.copy(alpha = 0.28f),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h),
                    ),
                    topLeft = Offset(pad * 1.1f, pad * 0.85f),
                    size = Size(cardW * 0.96f, cardH * 0.96f),
                    cornerRadius = corner,
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.45f),
                    topLeft = Offset(pad * 1.1f, pad * 0.85f),
                    size = Size(cardW * 0.96f, cardH * 0.96f),
                    cornerRadius = corner,
                    style = Stroke(width = (w * 0.022f).coerceAtLeast(1f)),
                )
            }

            // 3. Front luminous 3D glass card
            val frontLeft = pad * 0.92f
            val frontTop = pad * 1.05f
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(palette.frontStart, palette.frontEnd),
                    start = Offset(frontLeft, frontTop),
                    end = Offset(frontLeft + cardW, frontTop + cardH),
                ),
                topLeft = Offset(frontLeft, frontTop),
                size = Size(cardW, cardH),
                cornerRadius = corner,
            )

            // 4. Diagonal glass sheen highlight on top half
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.46f),
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                    start = Offset(frontLeft, frontTop),
                    end = Offset(frontLeft + cardW * 0.85f, frontTop + cardH * 0.72f),
                ),
                topLeft = Offset(frontLeft, frontTop),
                size = Size(cardW, cardH * 0.72f),
                cornerRadius = corner,
            )

            // 4b. Bottom caustic glow + specular highlight dot for real glass depth
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, palette.backPlate.copy(alpha = 0.38f)),
                    startY = frontTop + cardH * 0.55f,
                    endY = frontTop + cardH,
                ),
                topLeft = Offset(frontLeft, frontTop + cardH * 0.55f),
                size = Size(cardW, cardH * 0.45f),
                cornerRadius = corner,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.78f),
                radius = w * 0.045f,
                center = Offset(frontLeft + cardW * 0.20f, frontTop + cardH * 0.17f),
            )

            // 5. Specular rim stroke around the glass tile
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.24f),
                        palette.frontEnd.copy(alpha = 0.55f),
                    ),
                    start = Offset(frontLeft, frontTop),
                    end = Offset(frontLeft + cardW, frontTop + cardH),
                ),
                topLeft = Offset(frontLeft, frontTop),
                size = Size(cardW, cardH),
                cornerRadius = corner,
                style = Stroke(width = (w * 0.032f).coerceAtLeast(1.2f)),
            )

            // 6. Crisp layered vector glyph centered inside
            val glyphBoxLeft = frontLeft + cardW * 0.24f
            val glyphBoxTop = frontTop + cardH * 0.24f
            val glyphSize = cardW * 0.52f
            drawGlyphFor(
                type = type,
                left = glyphBoxLeft,
                top = glyphBoxTop,
                size = glyphSize,
            )
        }
    }
}

private fun DrawScope.drawGlyphFor(
    type: Webino3DIconType,
    left: Float,
    top: Float,
    size: Float,
) {
    val shadowOffset = Offset(0f, size * 0.07f)
    val strokeW = (size * 0.115f).coerceAtLeast(1.6f)
    val cx = left + size / 2f
    val cy = top + size / 2f

    when (type) {
        Webino3DIconType.HOME -> {
            val path = Path().apply {
                moveTo(cx, top + size * 0.06f)
                lineTo(left + size * 0.94f, top + size * 0.48f)
                lineTo(left + size * 0.80f, top + size * 0.48f)
                lineTo(left + size * 0.80f, top + size * 0.92f)
                lineTo(left + size * 0.58f, top + size * 0.92f)
                lineTo(left + size * 0.58f, top + size * 0.64f)
                lineTo(left + size * 0.42f, top + size * 0.64f)
                lineTo(left + size * 0.42f, top + size * 0.92f)
                lineTo(left + size * 0.20f, top + size * 0.92f)
                lineTo(left + size * 0.20f, top + size * 0.48f)
                lineTo(left + size * 0.06f, top + size * 0.48f)
                close()
            }
            drawPath(path, Color.Black.copy(alpha = 0.22f))
            drawPath(path, Color.White)
        }

        Webino3DIconType.PROJECTS -> {
            val back = Path().apply {
                addRoundRect(
                    RoundRect(
                        Rect(left + size * 0.06f, top + size * 0.14f, left + size * 0.94f, top + size * 0.86f),
                        CornerRadius(size * 0.16f),
                    )
                )
            }
            val frontTab = Path().apply {
                addRoundRect(
                    RoundRect(
                        Rect(left + size * 0.06f, top + size * 0.32f, left + size * 0.94f, top + size * 0.88f),
                        CornerRadius(size * 0.16f),
                    )
                )
            }
            drawPath(back, Color.White.copy(alpha = 0.58f))
            drawPath(frontTab, Color.White)
            drawLine(
                color = SkyBlue.copy(alpha = 0.55f),
                start = Offset(left + size * 0.24f, top + size * 0.58f),
                end = Offset(left + size * 0.68f, top + size * 0.58f),
                strokeWidth = strokeW * 0.78f,
                cap = StrokeCap.Round,
            )
        }

        Webino3DIconType.BUILD -> {
            // Sharp crisp lightning / build prism
            val bolt = Path().apply {
                moveTo(left + size * 0.56f, top + size * 0.04f)
                lineTo(left + size * 0.18f, top + size * 0.56f)
                lineTo(left + size * 0.48f, top + size * 0.56f)
                lineTo(left + size * 0.40f, top + size * 0.96f)
                lineTo(left + size * 0.84f, top + size * 0.42f)
                lineTo(left + size * 0.53f, top + size * 0.42f)
                close()
            }
            drawPath(bolt, Color.White)
        }

        Webino3DIconType.PREVIEW -> {
            // Browser/phone card with play triangle
            drawRoundRect(
                color = Color.White.copy(alpha = 0.30f),
                topLeft = Offset(left + size * 0.06f, top + size * 0.10f),
                size = Size(size * 0.88f, size * 0.80f),
                cornerRadius = CornerRadius(size * 0.20f),
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(left + size * 0.06f, top + size * 0.10f),
                size = Size(size * 0.88f, size * 0.80f),
                cornerRadius = CornerRadius(size * 0.20f),
                style = Stroke(width = strokeW * 0.85f),
            )
            val play = Path().apply {
                moveTo(left + size * 0.40f, top + size * 0.32f)
                lineTo(left + size * 0.70f, top + size * 0.50f)
                lineTo(left + size * 0.40f, top + size * 0.68f)
                close()
            }
            drawPath(play, Color.White)
        }

        Webino3DIconType.CUSTOMIZE -> {
            // Stylized paint palette + diamond sparkle
            drawCircle(
                color = Color.White,
                radius = size * 0.38f,
                center = Offset(cx, cy),
                style = Stroke(width = strokeW),
            )
            drawCircle(color = Color.White, radius = size * 0.11f, center = Offset(cx - size * 0.13f, cy - size * 0.10f))
            drawCircle(color = Color.White, radius = size * 0.10f, center = Offset(cx + size * 0.14f, cy - size * 0.08f))
            drawCircle(color = Color.White, radius = size * 0.10f, center = Offset(cx + size * 0.03f, cy + size * 0.15f))
        }

        Webino3DIconType.OUTPUTS -> {
            // Package box / APK shield with crisp check
            val shield = Path().apply {
                moveTo(cx, top + size * 0.06f)
                lineTo(left + size * 0.88f, top + size * 0.22f)
                lineTo(left + size * 0.84f, top + size * 0.62f)
                cubicTo(
                    left + size * 0.78f, top + size * 0.82f,
                    cx, top + size * 0.94f,
                    cx, top + size * 0.94f,
                )
                cubicTo(
                    cx, top + size * 0.94f,
                    left + size * 0.22f, top + size * 0.82f,
                    left + size * 0.16f, top + size * 0.62f,
                )
                lineTo(left + size * 0.12f, top + size * 0.22f)
                close()
            }
            drawPath(shield, Color.White.copy(alpha = 0.28f))
            drawPath(shield, Color.White, style = Stroke(width = strokeW * 0.9f, join = StrokeJoin.Round))
            val check = Path().apply {
                moveTo(left + size * 0.32f, top + size * 0.50f)
                lineTo(left + size * 0.46f, top + size * 0.64f)
                lineTo(left + size * 0.69f, top + size * 0.37f)
            }
            drawPath(check, Color.White, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        Webino3DIconType.SIGNING -> {
            // 3D Key silhouette
            drawCircle(
                color = Color.White,
                radius = size * 0.21f,
                center = Offset(left + size * 0.31f, cy),
                style = Stroke(width = strokeW),
            )
            drawLine(
                color = Color.White,
                start = Offset(left + size * 0.50f, cy),
                end = Offset(left + size * 0.92f, cy),
                strokeWidth = strokeW,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White,
                start = Offset(left + size * 0.74f, cy),
                end = Offset(left + size * 0.74f, cy + size * 0.18f),
                strokeWidth = strokeW,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White,
                start = Offset(left + size * 0.90f, cy),
                end = Offset(left + size * 0.90f, cy + size * 0.14f),
                strokeWidth = strokeW,
                cap = StrokeCap.Round,
            )
        }

        Webino3DIconType.TEMPLATES -> {
            val gap = size * 0.10f
            val cell = (size - gap) * 0.44f
            val r = CornerRadius(cell * 0.28f)
            val x0 = left + size * 0.06f
            val y0 = top + size * 0.06f
            drawRoundRect(Color.White, Offset(x0, y0), Size(cell, cell), r)
            drawRoundRect(Color.White.copy(alpha = 0.68f), Offset(x0 + cell + gap, y0), Size(cell, cell), r)
            drawRoundRect(Color.White.copy(alpha = 0.68f), Offset(x0, y0 + cell + gap), Size(cell, cell), r)
            drawRoundRect(Color.White, Offset(x0 + cell + gap, y0 + cell + gap), Size(cell, cell), r)
        }

        Webino3DIconType.SETTINGS -> {
            drawCircle(
                color = Color.White,
                radius = size * 0.24f,
                center = Offset(cx, cy),
                style = Stroke(width = strokeW),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = size * 0.11f,
                center = Offset(cx, cy),
            )
            for (i in 0 until 6) {
                val angle = (i * 60f) * (Math.PI / 180.0)
                val inner = size * 0.28f
                val outer = size * 0.42f
                drawLine(
                    color = Color.White,
                    start = Offset(cx + (cos(angle) * inner).toFloat(), cy + (sin(angle) * inner).toFloat()),
                    end = Offset(cx + (cos(angle) * outer).toFloat(), cy + (sin(angle) * outer).toFloat()),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round,
                )
            }
        }

        Webino3DIconType.HELP -> {
            drawCircle(
                color = Color.White.copy(alpha = 0.28f),
                radius = size * 0.42f,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = Color.White,
                radius = size * 0.42f,
                center = Offset(cx, cy),
                style = Stroke(width = strokeW * 0.9f),
            )
            val question = Path().apply {
                moveTo(cx - size * 0.12f, cy - size * 0.12f)
                cubicTo(
                    cx - size * 0.12f, cy - size * 0.26f,
                    cx + size * 0.15f, cy - size * 0.26f,
                    cx + size * 0.14f, cy - size * 0.08f,
                )
                cubicTo(
                    cx + size * 0.14f, cy + size * 0.02f,
                    cx, cy + size * 0.04f,
                    cx, cy + size * 0.14f,
                )
            }
            drawPath(question, Color.White, style = Stroke(width = strokeW, cap = StrokeCap.Round))
            drawCircle(color = Color.White, radius = size * 0.06f, center = Offset(cx, cy + size * 0.26f))
        }

        Webino3DIconType.HISTORY -> {
            drawCircle(
                color = Color.White,
                radius = size * 0.39f,
                center = Offset(cx, cy),
                style = Stroke(width = strokeW * 0.95f),
            )
            drawLine(
                color = Color.White,
                start = Offset(cx, cy),
                end = Offset(cx, cy - size * 0.22f),
                strokeWidth = strokeW * 0.95f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White,
                start = Offset(cx, cy),
                end = Offset(cx + size * 0.17f, cy + size * 0.11f),
                strokeWidth = strokeW * 0.95f,
                cap = StrokeCap.Round,
            )
        }
        Webino3DIconType.ABOUT -> {
            drawCircle(
                color = Color.White.copy(alpha = 0.26f),
                radius = size * 0.42f,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = Color.White,
                radius = size * 0.42f,
                center = Offset(cx, cy),
                style = Stroke(width = strokeW * 0.9f),
            )
            drawCircle(color = Color.White, radius = size * 0.065f, center = Offset(cx, cy - size * 0.2f))
            drawLine(
                color = Color.White,
                start = Offset(cx, cy - size * 0.05f),
                end = Offset(cx, cy + size * 0.22f),
                strokeWidth = strokeW,
                cap = StrokeCap.Round,
            )
        }
    }
    // Reference shadowOffset cleanly to keep helper consistent
    @Suppress("UNUSED_EXPRESSION")
    shadowOffset
}
