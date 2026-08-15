package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CosmicBackground
import com.example.ui.theme.CosmicCard
import com.example.ui.theme.CosmicCyan
import com.example.ui.theme.CosmicGold
import com.example.ui.theme.CosmicOrange
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import com.example.util.ErrorSanitizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sign

enum class MessageType {
    ERROR,
    SUCCESS,
    INFO
}

/**
 * A highly polished, compact, gesture-dismissible floating message banner.
 * Supports smooth 4-directional drag-to-dismiss (swipe right, left, up, down),
 * spring bounce-back, auto-dismiss timer, and proper navigation bar insets.
 */
@Composable
fun FloatingAppMessageBanner(
    message: String,
    type: MessageType,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissDurationMillis: Long = 4500L
) {
    val cleanMessage = remember(message, type) {
        if (type == MessageType.ERROR) {
            ErrorSanitizer.sanitize(message)
        } else {
            message.trim()
        }
    }

    if (cleanMessage.isBlank()) return

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Drag offset animatable
    val animOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var isDragging by remember { mutableStateOf(false) }

    // Threshold in pixels for dismissing (approx 70dp)
    val dismissThresholdPx = with(density) { 70.dp.toPx() }

    // Auto dismiss countdown
    LaunchedEffect(cleanMessage, isDragging) {
        if (!isDragging) {
            delay(autoDismissDurationMillis)
            // Animate fade and slide down before dismissing
            animOffset.animateTo(
                targetValue = Offset(0f, 200f),
                animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing)
            )
            onDismiss()
        }
    }

    // Dynamic alpha & scale calculation based on drag distance
    val currentDistance = hypot(animOffset.value.x, animOffset.value.y)
    val progress = (currentDistance / (dismissThresholdPx * 1.5f)).coerceIn(0f, 1f)
    val contentAlpha = (1f - progress * 0.7f).coerceIn(0.2f, 1f)
    val contentScale = (1f - progress * 0.12f).coerceIn(0.88f, 1f)

    // Visual styling based on MessageType
    val (cardBgBrush, borderColor, iconColor, iconVector, badgeTitle) = when (type) {
        MessageType.ERROR -> Quintuple(
            Brush.verticalGradient(listOf(Color(0xFF2E1014), Color(0xFF1E0A0C))),
            Color(0xFFFF5252).copy(alpha = 0.75f),
            Color(0xFFFF5252),
            Icons.Default.ErrorOutline,
            "تنبيه"
        )
        MessageType.SUCCESS -> Quintuple(
            Brush.verticalGradient(listOf(Color(0xFF0F2B1D), Color(0xFF081C12))),
            GlowGreen.copy(alpha = 0.75f),
            GlowGreen,
            Icons.Default.CheckCircle,
            "نجاح"
        )
        MessageType.INFO -> Quintuple(
            Brush.verticalGradient(listOf(Color(0xFF122438), Color(0xFF0B1724))),
            CosmicCyan.copy(alpha = 0.75f),
            CosmicCyan,
            Icons.Default.Info,
            "إشعار"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.2.dp, borderColor),
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .offset {
                    IntOffset(
                        animOffset.value.x.roundToInt(),
                        animOffset.value.y.roundToInt()
                    )
                }
                .alpha(contentAlpha)
                .scale(contentScale)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(18.dp),
                    spotColor = borderColor,
                    ambientColor = borderColor
                )
                .pointerInput(cleanMessage) {
                    val velocityTracker = VelocityTracker()
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            velocityTracker.resetTracking()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            coroutineScope.launch {
                                animOffset.snapTo(
                                    Offset(
                                        animOffset.value.x + dragAmount.x,
                                        animOffset.value.y + dragAmount.y
                                    )
                                )
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            val velocity = velocityTracker.calculateVelocity()
                            val totalDist = hypot(animOffset.value.x, animOffset.value.y)
                            val speed = hypot(velocity.x, velocity.y)

                            if (totalDist > dismissThresholdPx || speed > 1000f) {
                                // Animate outward in the direction of swipe
                                val exitDirX = if (abs(animOffset.value.x) > 10f) animOffset.value.x.sign else 0f
                                val exitDirY = if (abs(animOffset.value.y) > 10f) animOffset.value.y.sign else 1f
                                val targetX = if (exitDirX != 0f) exitDirX * 800f else animOffset.value.x * 2f
                                val targetY = if (exitDirY != 0f) exitDirY * 600f else animOffset.value.y * 2f

                                coroutineScope.launch {
                                    animOffset.animateTo(
                                        targetValue = Offset(targetX, targetY),
                                        animationSpec = tween(180, easing = FastOutLinearInEasing)
                                    )
                                    onDismiss()
                                }
                            } else {
                                // Snap back smoothly to center
                                coroutineScope.launch {
                                    animOffset.animateTo(
                                        targetValue = Offset.Zero,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            coroutineScope.launch {
                                animOffset.animateTo(
                                    targetValue = Offset.Zero,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .background(cardBgBrush)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Status Icon with subtle background circle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = badgeTitle,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Message Text Content
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = cleanMessage,
                            color = TextLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Quick Dismiss "X" Touch Action
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                animOffset.animateTo(
                                    targetValue = Offset(0f, 150f),
                                    animationSpec = tween(150, easing = FastOutLinearInEasing)
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق الرسالة",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
