package com.rockproject.mt6789.ui.components
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rockproject.mt6789.model.DotStatus
import com.rockproject.mt6789.ui.theme.RockTheme

@Composable
fun RockNavbar(
    title: String,
    statusText: String,
    dotStatus: DotStatus,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateBootSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = RockTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 6.dp)
    ) {
        // Top navigation row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (showBackButton) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBackClick() }
                        .padding(vertical = 6.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Back",
                        tint = colors.blue,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Back",
                        color = colors.blue,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            // Right action buttons (Boot Settings + Theme Toggle)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (onNavigateBootSettings != null) {
                    IconButton(
                        onClick = onNavigateBootSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceRaised)
                            .border(0.5.dp, colors.border, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Apply on boot settings",
                            tint = colors.label,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceRaised)
                        .border(0.5.dp, colors.border, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = "Toggle theme",
                        tint = colors.label,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Large One UI Title
        Text(
            text = title,
            color = colors.label,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.8).sp,
            lineHeight = 36.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Status row under title (left-aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(colors.surfaceRaised)
                    .border(0.5.dp, colors.border, RoundedCornerShape(100.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dotColor = when (dotStatus) {
                    DotStatus.OK -> colors.green
                    DotStatus.WARN -> colors.orange
                    DotStatus.BAD -> colors.red
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    color = colors.labelSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = RockTheme.colors
    val shape = RoundedCornerShape(26.dp)
    val isDark = colors.isDark
    
    // True iOS Liquid Glass translucent material over solid background
    val glassBg = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.75f) else Color(0xFFFFFFFF).copy(alpha = 0.88f)
    val glassBorder = if (isDark) Color(0x40FFFFFF) else Color(0x26000000)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "glassCardScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation = 10.dp, shape = shape, spotColor = if (isDark) Color.Black.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.25f), ambientColor = Color.Transparent)
            .clip(shape)
            .background(glassBg)
            .border(1.dp, glassBorder, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple()
                    ) { onClick() }
                } else Modifier
            )
            .padding(18.dp),
        content = content
    )
}

@Composable
fun CardWithBadge(
    badgeText: String,
    badgeIcon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = RockTheme.colors
    val shape = RoundedCornerShape(26.dp)
    val isDark = colors.isDark
    val glassBg = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.75f) else Color(0xFFFFFFFF).copy(alpha = 0.88f)
    val glassBorder = if (isDark) Color(0x40FFFFFF) else Color(0x26000000)

    Box(modifier = modifier.fillMaxWidth()) {
        // Card Body with offset top padding to accommodate badge
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp)
                .shadow(elevation = 10.dp, shape = shape, spotColor = if (isDark) Color.Black.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.25f), ambientColor = Color.Transparent)
                .clip(shape)
                .background(glassBg)
                .border(1.dp, glassBorder, shape)
                .padding(top = 22.dp, start = 18.dp, end = 18.dp, bottom = 18.dp),
            content = content
        )

        // Glossy mint badge anchored at top-left
        Row(
            modifier = Modifier
                .padding(start = 16.dp)
                .shadow(4.dp, RoundedCornerShape(100.dp))
                .clip(RoundedCornerShape(100.dp))
                .background(colors.mint)
                .border(0.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                .padding(horizontal = 14.dp, vertical = 6.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = colors.mintInk,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = badgeText,
                color = colors.mintInk,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

// MediaTek Parallelogram Logo shape
private val ParallelogramShape = GenericShape { size, _ ->
    moveTo(size.width * 0.12f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width * 0.88f, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
fun MediaTekLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(68.dp)
            .height(26.dp)
            .clip(ParallelogramShape)
            .background(Color(0xFFFFA626)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "MediaTek",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-0.4).sp
        )
    }
}

@Composable
fun ArmLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(26.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "arm",
            color = RockTheme.colors.blue,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.8).sp
        )
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    isActive: Boolean = false,
    showDivider: Boolean = true
) {
    val colors = RockTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = colors.labelSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = value,
                color = if (isActive) colors.mint else colors.label,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = colors.separator,
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
fun MenuListRow(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    val colors = RockTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    val interactionSource = remember { MutableInteractionSource() }
                    it.iosPressEffect(interactionSource)
                      .clickable(
                          interactionSource = interactionSource,
                          indication = ripple()
                      ) { onClick() }
                }
                .padding(vertical = 16.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colors.label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = colors.labelTertiary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.labelTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = colors.separator,
                thickness = 0.5.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSliderField(
    label: String,
    value: Int,
    rangeMin: Int,
    rangeMax: Int,
    step: Int,
    unit: String,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val colors = RockTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Label and value row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = colors.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$value $unit",
                color = colors.blue,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tactile One UI style Slider with solid straight track
        Slider(
            value = value.toFloat(),
            onValueChange = {
                val stepFloat = step.toFloat()
                val snappedValue = kotlin.math.round(it / stepFloat).toInt() * step
                onValueChange(snappedValue)
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = rangeMin.toFloat()..rangeMax.toFloat(),
            steps = 0,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = colors.blue,
                inactiveTrackColor = colors.surfaceHigh
            ),
            thumb = {
                // Sleek capsule thumb with dot indicator
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 24.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8C96AF))
                    )
                }
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(14.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = colors.blue,
                        inactiveTrackColor = colors.surfaceHigh
                    ),
                    drawStopIndicator = null
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Range text underneath
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "$rangeMin–$rangeMax $unit",
                color = colors.labelTertiary,
                fontSize = 12.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun PillButton(
    text: String,
    isPrimary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = RockTheme.colors
    val bgColor = if (isPrimary) colors.blue else colors.surfaceHigh
    val textColor = if (isPrimary) Color.White else colors.labelSecondary

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(100.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = textColor,
            disabledContainerColor = bgColor.copy(alpha = 0.4f),
            disabledContentColor = textColor.copy(alpha = 0.4f)
        ),
        modifier = modifier.height(52.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun OcActionButtons(
    isRestoreVisible: Boolean,
    isApplyVisible: Boolean,
    canApply: Boolean,
    onRestore: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val anyVisible = isRestoreVisible || isApplyVisible

    AnimatedVisibility(
        visible = anyVisible,
        enter = expandVertically(
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMediumLow
            ),
            expandFrom = Alignment.Top
        ) + fadeIn(
            animationSpec = tween(durationMillis = 260, easing = LinearOutSlowInEasing)
        ),
        exit = shrinkVertically(
            animationSpec = spring(
                dampingRatio = 0.95f,
                stiffness = Spring.StiffnessMediumLow
            ),
            shrinkTowards = Alignment.Top
        ) + fadeOut(
            animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        val restoreTargetWeight = if (isRestoreVisible) 1f else 0.0001f
        val applyTargetWeight = if (isApplyVisible) 1f else 0.0001f

        val restoreWeight by animateFloatAsState(
            targetValue = restoreTargetWeight,
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "restoreWeight"
        )
        val applyWeight by animateFloatAsState(
            targetValue = applyTargetWeight,
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "applyWeight"
        )

        val restoreAlpha by animateFloatAsState(
            targetValue = if (isRestoreVisible) 1f else 0f,
            animationSpec = tween(
                durationMillis = 220,
                easing = if (isRestoreVisible) LinearOutSlowInEasing else FastOutLinearInEasing
            ),
            label = "restoreAlpha"
        )
        val applyAlpha by animateFloatAsState(
            targetValue = if (isApplyVisible) 1f else 0f,
            animationSpec = tween(
                durationMillis = 220,
                easing = if (isApplyVisible) LinearOutSlowInEasing else FastOutLinearInEasing
            ),
            label = "applyAlpha"
        )

        val bothVisible = isRestoreVisible && isApplyVisible
        val animatedSpacing by animateDpAsState(
            targetValue = if (bothVisible) 10.dp else 0.dp,
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "actionSpacing"
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(animatedSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (restoreWeight > 0.002f || isRestoreVisible) {
                    Box(
                        modifier = Modifier
                            .weight(restoreWeight.coerceAtLeast(0.0001f))
                            .graphicsLayer {
                                alpha = restoreAlpha
                                val s = (0.92f + 0.08f * restoreAlpha).coerceIn(0f, 1f)
                                scaleX = s
                                scaleY = s
                            }
                    ) {
                        PillButton(
                            text = "Restore stock",
                            isPrimary = false,
                            enabled = canApply,
                            onClick = onRestore,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (applyWeight > 0.002f || isApplyVisible) {
                    Box(
                        modifier = Modifier
                            .weight(applyWeight.coerceAtLeast(0.0001f))
                            .graphicsLayer {
                                alpha = applyAlpha
                                val s = (0.92f + 0.08f * applyAlpha).coerceIn(0f, 1f)
                                scaleX = s
                                scaleY = s
                            }
                    ) {
                        PillButton(
                            text = "Apply",
                            isPrimary = true,
                            enabled = canApply,
                            onClick = onApply,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ControlFieldItem(
    label: String,
    currentValue: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    val colors = RockTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Label row: title on left, current value in blue mono on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = colors.label,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = currentValue,
                color = colors.blue,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Tactile One UI trigger button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surfaceRaised)
                .border(0.5.dp, colors.border, RoundedCornerShape(18.dp))
                .let {
                    val interactionSource = remember { MutableInteractionSource() }
                    it.iosPressEffect(interactionSource)
                      .clickable(
                          interactionSource = interactionSource,
                          indication = ripple()
                      ) { onClick() }
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentValue,
                color = colors.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.labelTertiary,
                modifier = Modifier.size(22.dp)
            )
        }

        if (showDivider) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = colors.separator, thickness = 0.5.dp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> BottomSheetPicker(
    title: String,
    items: List<T>,
    selectedItem: T?,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = RockTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceRaised,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.borderLight)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = title,
                color = colors.labelTertiary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
            ) {
                items(items) { item ->
                    val isSelected = item == selectedItem
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let {
                                val interactionSource = remember { MutableInteractionSource() }
                                it.iosPressEffect(interactionSource)
                                  .clickable(
                                      interactionSource = interactionSource,
                                      indication = ripple()
                                  ) {
                                      onItemSelected(item)
                                      onDismiss()
                                  }
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = itemLabel(item),
                            color = if (isSelected) colors.blue else colors.label,
                            fontSize = 17.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = colors.blue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = colors.separator, thickness = 0.5.dp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surfaceHigh,
                    contentColor = colors.blue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp)
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun FloatingToast(
    message: String?,
    modifier: Modifier = Modifier
) {
    val colors = RockTheme.colors
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        if (message != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(colors.surfaceTranslucent)
                    .border(0.5.dp, colors.borderLight, RoundedCornerShape(100.dp))
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message,
                    color = colors.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


fun Modifier.iosPressEffect(
    interactionSource: MutableInteractionSource,
    scale: Float = 0.97f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        ),
        label = "press_scale"
    )
    graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
    }
}
