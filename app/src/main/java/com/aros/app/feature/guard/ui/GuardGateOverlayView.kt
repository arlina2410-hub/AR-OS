package com.aros.app.feature.guard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.guard.domain.RecoveryResult
import com.aros.app.feature.guard.domain.VerifyResult
import kotlinx.coroutines.delay

@Composable
fun GuardGateOverlayView(
    isTestMode: Boolean,
    testRemainingSeconds: Int = 20,
    onVerifyPin: suspend (String) -> VerifyResult,
    onVerifyRecovery: suspend (String) -> RecoveryResult,
    onSuccessUnlock: () -> Unit,
    onCloseTest: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutRemainingSeconds by remember { mutableStateOf(0L) }
    var isChecking by remember { mutableStateOf(false) }
    var isRecoveryMode by remember { mutableStateOf(false) }
    var recoveryCodeInput by remember { mutableStateOf("") }

    // Lockout countdown timer
    LaunchedEffect(lockoutRemainingSeconds) {
        if (lockoutRemainingSeconds > 0) {
            delay(1000L)
            lockoutRemainingSeconds -= 1
            if (lockoutRemainingSeconds <= 0) {
                errorMessage = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D12)) // Fully opaque sovereign backdrop
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: Test mode banner or Guard Shield
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                if (isTestMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ArOsTheme.colors.warning.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, ArOsTheme.colors.warning.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = ArOsTheme.colors.warning, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "TEST MODE • Closes in ${testRemainingSeconds}s",
                                    color = ArOsTheme.colors.warning,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = onCloseTest,
                                colors = ButtonDefaults.buttonColors(containerColor = ArOsTheme.colors.warning),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Close Test", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(ArOsTheme.colors.accent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecoveryMode) Icons.Default.Key else Icons.Default.Lock,
                        contentDescription = null,
                        tint = ArOsTheme.colors.accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isRecoveryMode) "AR Guard Recovery" else "AR Guard Protected",
                    style = ArOsTheme.typography.title1,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isRecoveryMode) "Enter your 16-character recovery code" else "Enter your AR Guard PIN to access apps",
                    style = ArOsTheme.typography.footnote,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            // Middle: PIN Dots or Recovery Input & Error Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (lockoutRemainingSeconds > 0) {
                    Box(
                        modifier = Modifier
                            .background(ArOsTheme.colors.sosCritical.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, ArOsTheme.colors.sosCritical, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Too many wrong attempts. Try again in ${lockoutRemainingSeconds}s",
                            color = ArOsTheme.colors.sosCritical,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = ArOsTheme.colors.sosCritical,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (!isRecoveryMode) {
                    // PIN dots display (up to 12)
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        val dotCount = pinInput.length.coerceAtLeast(6)
                        for (i in 0 until dotCount) {
                            val filled = i < pinInput.length
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (filled) ArOsTheme.colors.accent else Color.White.copy(alpha = 0.2f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (filled) ArOsTheme.colors.accent else Color.White.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                } else {
                    // Recovery Code input field
                    OutlinedTextField(
                        value = recoveryCodeInput,
                        onValueChange = { if (it.length <= 19) recoveryCodeInput = it.uppercase() },
                        placeholder = { Text("XXXX-XXXX-XXXX-XXXX", color = Color.White.copy(alpha = 0.4f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArOsTheme.colors.accent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (recoveryCodeInput.isNotBlank() && !isChecking && lockoutRemainingSeconds <= 0) {
                                isChecking = true
                                errorMessage = null
                                kotlinx.coroutines.GlobalScope.let {
                                    // Submit recovery
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ArOsTheme.colors.accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Verify Recovery Code", color = ArOsTheme.colors.contentOnAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bottom: On-screen Keypad or Cancel Recovery
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                if (!isRecoveryMode) {
                    KeypadGrid(
                        enabled = !isChecking && lockoutRemainingSeconds <= 0,
                        onDigit = { digit ->
                            if (pinInput.length < 12) {
                                pinInput += digit
                                errorMessage = null
                            }
                        },
                        onDelete = {
                            if (pinInput.isNotEmpty()) {
                                pinInput = pinInput.dropLast(1)
                                errorMessage = null
                            }
                        },
                        onUnlock = {
                            if (pinInput.length >= 6 && !isChecking && lockoutRemainingSeconds <= 0) {
                                isChecking = true
                                errorMessage = null
                            }
                        }
                    )

                    // Execute unlock effect when triggered
                    LaunchedEffect(isChecking) {
                        if (isChecking) {
                            val result = onVerifyPin(pinInput)
                            isChecking = false
                            when (result) {
                                is VerifyResult.Success -> {
                                    onSuccessUnlock()
                                }
                                is VerifyResult.WrongPin -> {
                                    pinInput = ""
                                    if (result.lockoutSeconds > 0) {
                                        lockoutRemainingSeconds = result.lockoutSeconds
                                    } else {
                                        errorMessage = "Wrong PIN. ${result.remainingFreeAttempts} free tries left."
                                    }
                                }
                                is VerifyResult.LockedOut -> {
                                    pinInput = ""
                                    lockoutRemainingSeconds = result.remainingLockoutSeconds
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = { isRecoveryMode = true }) {
                        Text("Forgot PIN?", color = ArOsTheme.colors.accent, fontSize = 13.sp)
                    }
                } else {
                    TextButton(onClick = { isRecoveryMode = false; errorMessage = null }) {
                        Text("Back to PIN Pad", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "5x Volume Down Emergency SOS remains active.",
                    style = ArOsTheme.typography.caption,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun KeypadGrid(
    enabled: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onUnlock: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("DEL", "0", "OK")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(0.85f)
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when (key) {
                                    "OK" -> ArOsTheme.colors.accent
                                    "DEL" -> Color.White.copy(alpha = 0.08f)
                                    else -> Color.White.copy(alpha = 0.08f)
                                }
                            )
                            .clickable(enabled = enabled) {
                                when (key) {
                                    "DEL" -> onDelete()
                                    "OK" -> onUnlock()
                                    else -> onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "DEL" -> Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            "OK" -> Text(
                                text = "Unlock",
                                color = ArOsTheme.colors.contentOnAccent,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            else -> Text(
                                text = key,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
