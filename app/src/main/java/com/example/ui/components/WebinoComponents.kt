package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.delay
import com.example.ui.theme.GrapePurple
import com.example.ui.theme.OrchidPink
import com.example.ui.theme.SunsetOrange
import com.example.ui.theme.RosePink
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.JalaliCalendar
import com.example.domain.model.AppLanguage
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.IndigoDeep
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WebinoThemeTokens

@Composable
fun WebinoAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = WebinoThemeTokens.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        tokens.canvasTop,
                        tokens.canvasMid,
                        tokens.canvasBottom,
                    )
                )
            )
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tokens.ambientVioletOrb, Color.Transparent),
                        center = Offset(size.width * 0.16f, size.height * 0.10f),
                        radius = size.width * 0.65f,
                    ),
                    radius = size.width * 0.65f,
                    center = Offset(size.width * 0.16f, size.height * 0.10f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tokens.ambientCyanOrb, Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.24f),
                        radius = size.width * 0.58f,
                    ),
                    radius = size.width * 0.58f,
                    center = Offset(size.width * 0.88f, size.height * 0.24f),
                )
                if (!tokens.isDark) {
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0x66FF9A8B), Color(0x66FF6AA8)),
                            start = Offset(-size.width * 0.2f, -size.width * 0.1f),
                            end = Offset(size.width * 0.3f, size.width * 0.4f),
                        ),
                        radius = size.width * 0.34f,
                        center = Offset(-size.width * 0.04f, size.height * 0.03f),
                    )
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0x55B28BFF), Color(0x556C8CFF)),
                            start = Offset(size.width * 0.7f, size.height * 0.4f),
                            end = Offset(size.width * 1.1f, size.height * 0.6f),
                        ),
                        radius = size.width * 0.30f,
                        center = Offset(size.width * 1.02f, size.height * 0.52f),
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tokens.ambientMintOrb, Color.Transparent),
                        center = Offset(size.width * 0.35f, size.height * 0.84f),
                        radius = size.width * 0.60f,
                    ),
                    radius = size.width * 0.60f,
                    center = Offset(size.width * 0.35f, size.height * 0.84f),
                )
            },
        content = content,
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    containerColor: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = WebinoThemeTokens.colors
    val shape = RoundedCornerShape(26.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f),
        label = "glassCardScale",
    )

    val resolvedFill = containerColor ?: tokens.glassContainerStrong
    val fillBrush = when {
        containerColor != null -> Brush.linearGradient(listOf(containerColor, containerColor))
        tokens.isDark -> Brush.linearGradient(listOf(tokens.glassContainerStrong, tokens.glassContainer))
        else -> Brush.linearGradient(listOf(Color(0xD1FFFFFF), Color(0x8CFFFFFF)))
    }
    val borderBrush = if (borderColor != null) {
        Brush.linearGradient(listOf(borderColor.copy(alpha = 0.72f), borderColor.copy(alpha = 0.28f)))
    } else {
        Brush.linearGradient(listOf(tokens.glassStrokeTop, tokens.glassStrokeBottom))
    }

    val clickableMod = if (onClick != null) {
        Modifier
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick,
            )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (tokens.isDark) 4.dp else 14.dp,
                shape = shape,
                ambientColor = tokens.cardShadowTint,
                spotColor = tokens.cardShadowTint,
            )
            .then(clickableMod),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.2.dp, borderBrush),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .background(fillBrush)
                .padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun GradientPrimaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "gradient_primary_btn",
) {
    val tokens = WebinoThemeTokens.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 520f),
        label = "btnScale",
    )
    val shape = RoundedCornerShape(18.dp)
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(ElectricViolet, IndigoDeep, SkyBlue))
    } else {
        val disabledColor = if (tokens.isDark) Color(0xFF28345E) else Color(0xFFCBD5E1)
        Brush.horizontalGradient(listOf(disabledColor, disabledColor))
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .height(54.dp)
            .minimumInteractiveComponentSize()
            .scale(scale)
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = shape,
                ambientColor = ElectricViolet.copy(alpha = 0.35f),
                spotColor = SkyBlue.copy(alpha = 0.35f),
            )
            .testTag(testTag),
        contentPadding = PaddingValues(0.dp),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(brush, shape)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = if (enabled) 0.32f else 0.1f),
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun WebinoSecondaryButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val tokens = WebinoThemeTokens.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = tokens.glassContainerStrong,
            contentColor = accent,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun WebinoScreenHeader(
    title: String,
    subtitle: String? = null,
    iconType: Webino3DIconType? = null,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "بازگشت",
    actions: @Composable RowScope.() -> Unit = {},
) {
    val tokens = WebinoThemeTokens.colors
    Surface(
        color = tokens.glassContainerStrong,
        border = BorderStroke(1.dp, tokens.navPillBorder.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        shadowElevation = if (tokens.isDark) 2.dp else 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = tokens.subtleSurface,
                    border = BorderStroke(1.dp, tokens.navPillBorder),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = backContentDescription,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
            }
            if (iconType != null) {
                Webino3DGlassIcon(type = iconType, size = 38.dp)
                Spacer(Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

data class FloatingNavItemData(
    val label: String,
    val testTag: String,
    val iconType: Webino3DIconType,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun Modifier.softEnter(delayMs: Int = 0): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        progress.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 28.dp.toPx()
    }
}

@Composable
fun WebinoFloatingGlassBottomBar(
    items: List<FloatingNavItemData>,
    modifier: Modifier = Modifier,
) {
    val tokens = WebinoThemeTokens.colors
    val barShape = RoundedCornerShape(38.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = if (tokens.isDark) 14.dp else 22.dp,
                    shape = barShape,
                    ambientColor = GrapePurple.copy(alpha = 0.40f),
                    spotColor = GrapePurple.copy(alpha = 0.40f),
                )
                .clip(barShape)
                .background(
                    if (tokens.isDark) {
                        Brush.linearGradient(listOf(tokens.navPillContainer, tokens.navPillContainer))
                    } else {
                        Brush.linearGradient(listOf(Color(0xD6FFFFFF), Color(0x99FFFFFF)))
                    }
                )
                .border(
                    width = 1.3.dp,
                    brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.95f), tokens.navPillBorder)),
                    shape = barShape,
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val iconSize by animateDpAsState(
                        targetValue = if (item.selected) 46.dp else 30.dp,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
                        label = "navIconSize",
                    )
                    val labelColor by animateColorAsState(
                        targetValue = if (item.selected) GrapePurple else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(220),
                        label = "navLabelColor",
                    )
                    Surface(
                        onClick = item.onClick,
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag(item.testTag),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .then(
                                        if (item.selected) {
                                            Modifier
                                                .shadow(
                                                    elevation = 10.dp,
                                                    shape = CircleShape,
                                                    ambientColor = GrapePurple.copy(alpha = 0.55f),
                                                    spotColor = GrapePurple.copy(alpha = 0.55f),
                                                )
                                                .background(Color.White.copy(alpha = 0.92f), CircleShape)
                                                .border(2.dp, Color.White, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Webino3DGlassIcon(
                                    type = item.iconType,
                                    size = iconSize,
                                    elevated = item.selected,
                                    contentDescription = item.label,
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (item.selected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = labelColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    color: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = WebinoThemeTokens.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = if (tokens.isDark) 0.18f else 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = if (tokens.isDark) 0.42f else 0.30f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    testTag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .padding(vertical = 8.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(ElectricViolet.copy(alpha = 0.16f), SkyBlue.copy(alpha = 0.14f))
                        ),
                        RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ElectricViolet,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    icon3DType: Webino3DIconType = Webino3DIconType.PROJECTS,
) {
    GlassCard(contentPadding = PaddingValues(28.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Webino3DGlassIcon(
                type = icon3DType,
                size = 64.dp,
                elevated = true,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionText != null && onAction != null) {
                Spacer(Modifier.height(18.dp))
                GradientPrimaryButton(
                    text = actionText,
                    icon = icon,
                    onClick = onAction,
                )
            }
        }
    }
}

@Composable
fun SkeletonBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = alpha)),
    )
}

@Composable
fun ColorSwatchRow(
    selectedHex: String,
    onSelectHex: (String) -> Unit,
    presets: List<String> = listOf(
        "#7C3AED", "#0EA5E9", "#06B6D4", "#10B981", "#F43F5E", "#F97316", "#EC4899", "#0B1026",
    ),
) {
    val tokens = WebinoThemeTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        presets.forEach { hex ->
            val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(ElectricViolet)
            val isSelected = selectedHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(if (isSelected) 6.dp else 1.dp, CircleShape)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.5.dp,
                        color = if (isSelected) {
                            if (tokens.isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        } else {
                            Color.White.copy(alpha = 0.65f)
                        },
                        shape = CircleShape,
                    )
                    .clickable { onSelectHex(hex) },
            )
        }
    }
}

fun localizeNumber(value: Number, language: AppLanguage, usePersianDigits: Boolean): String =
    JalaliCalendar.localizeDigits(value.toString(), usePersianDigits && language == AppLanguage.FA)

fun localizeNumber(value: String, language: AppLanguage, usePersianDigits: Boolean): String =
    JalaliCalendar.localizeDigits(value, usePersianDigits && language == AppLanguage.FA)

fun statusColorFor(statusName: String): Color = when (statusName) {
    "SUCCEEDED" -> SuccessEmerald
    "FAILED", "CANCELLED" -> ErrorCoral
    "RUNNING", "QUEUED" -> WarningAmber
    else -> CyanAccent
}
