package com.aros.app.feature.sos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aros.app.designsystem.haptics.rememberArOsHaptics
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.sos.domain.SosContact
import com.aros.app.feature.sos.domain.SosMode
import com.aros.app.feature.sos.domain.SosState

@Composable
fun SosCountdownScreen(
    state: SosState,
    onCancel: () -> Unit,
    onCallNow: () -> Unit
) {
    when (state) {
        is SosState.Countdown -> {
            SosCountdownContent(
                remainingSeconds = state.remainingSeconds,
                totalSeconds = state.totalSeconds,
                targetContact = state.targetContact,
                mode = state.mode,
                onCancel = onCancel,
                onCallNow = onCallNow
            )
        }
        is SosState.Dialing -> {
            SosDialingContent(
                targetContact = state.targetContact,
                mode = state.mode,
                method = state.method,
                onCancel = onCancel
            )
        }
        is SosState.Finished -> {
            SosFinishedContent(
                targetContact = state.targetContact,
                mode = state.mode,
                reason = state.reason,
                onDismiss = onCancel
            )
        }
        SosState.Idle -> {
            // No-op
        }
    }
}

@Composable
fun SosCountdownScreen(
    remainingSeconds: Int,
    totalSeconds: Int,
    targetContact: SosContact,
    mode: SosMode,
    onCancel: () -> Unit,
    onCallNow: () -> Unit
) {
    SosCountdownContent(
        remainingSeconds = remainingSeconds,
        totalSeconds = totalSeconds,
        targetContact = targetContact,
        mode = mode,
        onCancel = onCancel,
        onCallNow = onCallNow
    )
}

@Composable
private fun SosCountdownContent(
    remainingSeconds: Int,
    totalSeconds: Int,
    targetContact: SosContact,
    mode: SosMode,
    onCancel: () -> Unit,
    onCallNow: () -> Unit
) {
    val haptics = rememberArOsHaptics()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(remainingSeconds) {
        haptics.sosPulse()
    }

    val isLive = mode == SosMode.LIVE
    val accentColor = if (isLive) ArOsTheme.colors.sosCritical else ArOsTheme.colors.warning

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ArOsTheme.colors.background,
                        if (isLive) Color(0xFF2B0E14) else Color(0xFF241B0A),
                        ArOsTheme.colors.background
                    )
                )
            )
            .padding(ArOsTheme.spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Mode Banner Header
            Box(
                modifier = Modifier
                    .padding(top = 28.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLive) "EMERGENCY SOS (LIVE MODE)" else "SAFETY TEST (TEST MODE)",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        style = ArOsTheme.typography.caption.copy(letterSpacing = 1.sp)
                    )
                }
            }

            // Countdown Circle Widget
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(scale.value)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(accentColor.copy(alpha = 0.35f), Color.Transparent)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .background(ArOsTheme.colors.surfaceRaised.copy(alpha = 0.85f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$remainingSeconds",
                                fontSize = 84.sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
                            )
                            Text(
                                text = "SECONDS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Initiating Call To",
                    style = ArOsTheme.typography.footnote,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = targetContact.name,
                    style = ArOsTheme.typography.title1,
                    color = Color.White
                )
                Text(
                    text = targetContact.number,
                    style = ArOsTheme.typography.numericTitle,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            // Bottom Action Controls - SAFETY CRITICAL: 64dp Hit Target for Cancel Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Large CANCEL button
                Button(
                    onClick = {
                        haptics.heavy()
                        onCancel()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("sos_cancel_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CANCEL SOS",
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Immediate Call Now button
                Button(
                    onClick = {
                        haptics.heavy()
                        onCallNow()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sos_call_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call Now",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Call Contact Immediately",
                        color = Color.White,
                        style = ArOsTheme.typography.callout
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Press CANCEL anytime to immediately abort.",
                    style = ArOsTheme.typography.caption,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SosDialingContent(
    targetContact: SosContact,
    mode: SosMode,
    method: String,
    onCancel: () -> Unit
) {
    val haptics = rememberArOsHaptics()
    val isLive = mode == SosMode.LIVE
    val accentColor = if (isLive) ArOsTheme.colors.sosCritical else ArOsTheme.colors.accent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArOsTheme.colors.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(accentColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isLive) "DIALING EMERGENCY CONTACT" else "TEST CALL SIMULATION",
                style = ArOsTheme.typography.caption.copy(letterSpacing = 1.sp),
                color = accentColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = targetContact.name,
                style = ArOsTheme.typography.title1,
                color = Color.White
            )

            Text(
                text = targetContact.number,
                style = ArOsTheme.typography.numericTitle,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = method,
                style = ArOsTheme.typography.footnote,
                color = ArOsTheme.colors.contentSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    haptics.click()
                    onCancel()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Dismiss / End Sequence",
                    color = Color.White,
                    style = ArOsTheme.typography.callout
                )
            }
        }
    }
}

@Composable
private fun SosFinishedContent(
    targetContact: SosContact?,
    mode: SosMode,
    reason: String,
    onDismiss: () -> Unit
) {
    val haptics = rememberArOsHaptics()
    val isLive = mode == SosMode.LIVE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArOsTheme.colors.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(ArOsTheme.colors.success.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ArOsTheme.colors.success,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isLive) "EMERGENCY CALL DISPATCHED" else "SAFE TEST COMPLETED",
                style = ArOsTheme.typography.caption.copy(letterSpacing = 1.sp),
                color = ArOsTheme.colors.success
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (targetContact != null) {
                Text(
                    text = targetContact.name,
                    style = ArOsTheme.typography.title1,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = reason,
                style = ArOsTheme.typography.footnote,
                color = ArOsTheme.colors.contentSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = {
                    haptics.click()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ArOsTheme.colors.accent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Return to AR/OS",
                    color = ArOsTheme.colors.contentOnAccent,
                    style = ArOsTheme.typography.callout
                )
            }
        }
    }
}
