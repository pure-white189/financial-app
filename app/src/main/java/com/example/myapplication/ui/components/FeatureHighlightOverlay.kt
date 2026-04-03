package com.example.myapplication.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R

@Composable
fun FeatureHighlightOverlay(
    targetRect: Rect?,
    title: String,
    description: String,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    isLastStep: Boolean = false,
    cornerRadius: Dp = 16.dp,
) {
    if (targetRect == null) return

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 关键修复：开启离屏缓冲，保证 BlendMode.Clear 正常工作
            .graphicsLayer { alpha = 0.99f }
            .drawBehind {
                // 1. 画一层半透明黑底
                drawRect(Color.Black.copy(alpha = 0.7f))

                // 2. 挖空目标区域
                drawRoundRect(
                    color = Color.Black,
                    topLeft = targetRect.topLeft,
                    size = targetRect.size,
                    cornerRadius = CornerRadius(
                        x = with(density) { cornerRadius.toPx() },
                        y = with(density) { cornerRadius.toPx() },
                    ),
                    blendMode = BlendMode.Clear,
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { }
            }
    ) {
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
        val maxCardHeightPx = 250f
        val desiredY = if (targetRect.top > maxCardHeightPx) {
            (targetRect.top - maxCardHeightPx).coerceAtLeast(0f)
        } else {
            targetRect.bottom + 20f
        }
        val safeY = desiredY.coerceAtMost((screenHeightPx - maxCardHeightPx).coerceAtLeast(0f))
        val cardYOffset = with(density) { safeY.toDp() }

        Card(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .offset(y = cardYOffset),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    IconButton(onClick = onSkip) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.common_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.onboarding_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onNext) {
                        Text(if (isLastStep) stringResource(R.string.onboarding_done) else stringResource(R.string.onboarding_next))
                    }
                }
            }
        }
    }
}

