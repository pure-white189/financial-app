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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
    // Skip if rect is null or invalid (off-screen or zero-size)
    val screenHeightPx = LocalConfiguration.current.screenHeightDp * LocalDensity.current.density
    val screenWidthPx = LocalConfiguration.current.screenWidthDp * LocalDensity.current.density

    val isRectValid = targetRect != null &&
            targetRect.width > 0f &&
            targetRect.height > 10f &&  // 改这里，高度小于10px不挖空
            targetRect.top >= 0f &&
            targetRect.bottom <= screenHeightPx * 1.1f &&
            targetRect.left >= 0f &&
            targetRect.right <= screenWidthPx * 1.1f

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 0.99f }
            .drawBehind {
                // Dark overlay
                drawRect(Color.Black.copy(alpha = 0.65f))
                // Cut out highlight only if rect is valid
                if (isRectValid) {
                    val padding = 8.dp.toPx()
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(
                            targetRect.left - padding,
                            targetRect.top - padding
                        ),
                        size = Size(
                            targetRect.width + padding * 2,
                            targetRect.height + padding * 2
                        ),
                        cornerRadius = CornerRadius(
                            x = with(density) { cornerRadius.toPx() } + padding,
                            y = with(density) { cornerRadius.toPx() } + padding,
                        ),
                        blendMode = BlendMode.Clear,
                    )
                }
            }
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        // Calculate card position
        // Place card below target if there's space, otherwise above
        val screenHeightDp = configuration.screenHeightDp.dp
        val cardHorizontalPadding = 24.dp
        val cardTopMargin = 16.dp

        val targetBottomDp = if (isRectValid) {
            with(density) { targetRect.bottom.toDp() }
        } else {
            screenHeightDp * 0.5f
        }

        val targetTopDp = if (isRectValid) {
            with(density) { targetRect.top.toDp() }
        } else {
            screenHeightDp * 0.4f
        }

        // Determine if card should go below or above the target
        val spaceBelow = screenHeightDp - targetBottomDp
        val cardGoesBelow = spaceBelow >= 200.dp || targetTopDp < 200.dp

        val cardOffsetY = if (cardGoesBelow) {
            targetBottomDp + cardTopMargin
        } else {
            // Place above - we use Alignment.Top with offset,
            // so we just position from top of screen
            (targetTopDp - cardTopMargin - 220.dp).coerceAtLeast(8.dp)
        }

        Card(
            modifier = Modifier
                .padding(horizontal = cardHorizontalPadding)
                .fillMaxWidth()
                .offset(y = cardOffsetY)
                // Let card height be determined by content
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onSkip,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Skip",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkip) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onNext) {
                        Text(
                            text = if (isLastStep)
                                stringResource(R.string.onboarding_done)
                            else
                                stringResource(R.string.onboarding_next)
                        )
                    }
                }
                if (!isRectValid) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "↓",
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

