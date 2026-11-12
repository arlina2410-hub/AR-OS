package com.aros.app.feature.guard.ui

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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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

enum class GateMode {
    PIN_ENTRY,
    RECOVERY_CODE_ENTRY,
    SET_NEW_PIN_FIRST,
    SET_NEW_PIN_CONFIRM,
    RECOVERY_SUCCESS_NOTICE
}

@Composable
fun GuardGateOverlayView(
    isTestMode: Boolean,
    testRemainingSeconds: Int = 20,
    onVerifyPin: suspend (String) -> VerifyResult,
    onVerifyRecovery: suspend (String) -> RecoveryResult,
    onResetPinWithRecovery: suspend (String, String) -> Boolean,
    onSuccessUnlock: () -> Unit,
    onCloseTest: () -> Unit
) {
    var mode by remember { mutableStateOf(GateMode.PIN_ENTRY) }

    var pinInput by remember { mutableStateOf("") }
    var recoveryCodeInput by remember { mutableStateOf("") }
    var newPinFirst by remember { mutableStateOf("") }
    var newPinConfirm by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutRemainingSeconds by remember { mutableStateOf(0L) }

    // Separate checking flags per Fix 1.2
    var isPinChecking by remember { mutableStateOf(false) }
    var isRecoveryChecking by remember { mutableStateOf(false) }
    var isResetChecking by remember { mutableStateOf(false) }

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

    // Effect for PIN verification
    LaunchedEffect(isPinChecking) {
        if (isPinChecking) {
            val result = onVerifyPin(pinInput)
            isPinChecking = false
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

    // Effect for Recovery Code verification
    LaunchedEffect(isRecoveryChecking) {
        if (isRecoveryChecking) {
            val result = onVerifyRecovery(recoveryCodeInput)
            isRecoveryChecking = false
            when (result) {
                is RecoveryResult.Success -> {
                    // Step into Set New PIN per Fix 1.3
                    errorMessage = null
                    newPinFirst = ""
                    newPinConfirm = ""
                    mode = GateMode.SET_NEW_PIN_FIRST
                }
                is RecoveryResult.WrongCode -> {
                    recoveryCodeInput = ""
                    if (result.lockoutSeconds > 0) {
                        lockoutRemainingSeconds = result.lockoutSeconds
                    } else {
                        errorMessage = "Wrong recovery code. ${result.remainingFreeAttempts} free tries left."
                    }
                }
                is RecoveryResult.LockedOut -> {
                    recoveryCodeInput = ""
                    lockoutRemainingSeconds = result.remainingLockoutSeconds
                }
            }
        }
    }

    // Effect for Reset PIN with verified recovery code
    LaunchedEffect(isResetChecking) {
        if (isResetChecking) {
            val success = onResetPinWithRecovery(recoveryCodeInput, newPinFirst)
            isResetChecking = false
            if (success) {
                mode = GateMode.RECOVERY_SUCCESS_NOTICE
            } else {
                errorMessage = "Failed to reset PIN. Please try again."
                mode = GateMode.RECOVERY_CODE_ENTRY
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D12)) // Fully opaque sovereign backdrop
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: Test mode banner & Shield
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 18.dp)
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
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ArOsTheme.colors.warning,
                                    modifier = Modifier.size(16.dp)
                                )
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
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(ArOsTheme.colors.accent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (mode) {
                            GateMode.PIN_ENTRY -> Icons.Default.Lock
                            else -> Icons.Default.Key
                        },
                        contentDescription = null,
                        tint = ArOsTheme.colors.accent,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = when (mode) {
                        GateMode.PIN_ENTRY -> "AR Guard Protected"
                        GateMode.RECOVERY_CODE_ENTRY -> "AR Guard Recovery"
                        GateMode.SET_NEW_PIN_FIRST -> "Set New PIN"
                        GateMode.SET_NEW_PIN_CONFIRM -> "Confirm New PIN"
                        GateMode.RECOVERY_SUCCESS_NOTICE -> "Recovery Complete"
                    },
                    style = ArOsTheme.typography.title1,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (mode) {
                        GateMode.PIN_ENTRY -> "Enter your AR Guard PIN to access apps"
                        GateMode.RECOVERY_CODE_ENTRY -> "Type your 16-character recovery code"
                        GateMode.SET_NEW_PIN_FIRST -> "Choose a new 6 to 12 digit PIN"
                        GateMode.SET_NEW_PIN_CONFIRM -> "Re-enter your new PIN to confirm"
                        GateMode.RECOVERY_SUCCESS_NOTICE -> "Your PIN has been updated successfully"
                    },
                    style = ArOsTheme.typography.footnote,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            // Middle Display Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (lockoutRemainingSeconds > 0) {
                    Box(
                        modifier = Modifier
                            .background(ArOsTheme.colors.sosCritical.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, ArOsTheme.colors.sosCritical, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Too many wrong attempts. Try again in ${lockoutRemainingSeconds}s",
                            color = ArOsTheme.colors.sosCritical,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = ArOsTheme.colors.sosCritical,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                when (mode) {
                    GateMode.PIN_ENTRY -> {
                        PinDotsDisplay(pinLength = pinInput.length, minDots = 6)
                    }
                    GateMode.RECOVERY_CODE_ENTRY -> {
                        // Formatted 16-character recovery code display in chunks of 4
                        val formattedCode = formatRecoveryDisplay(recoveryCodeInput)
                        Text(
                            text = formattedCode.ifBlank { "____-____-____-____" },
                            style = ArOsTheme.typography.headline,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (recoveryCodeInput.isEmpty()) Color.White.copy(alpha = 0.3f) else ArOsTheme.colors.accent,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    GateMode.SET_NEW_PIN_FIRST -> {
                        PinDotsDisplay(pinLength = newPinFirst.length, minDots = 6)
                    }
                    GateMode.SET_NEW_PIN_CONFIRM -> {
                        PinDotsDisplay(pinLength = newPinConfirm.length, minDots = 6)
                    }
                    GateMode.RECOVERY_SUCCESS_NOTICE -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ArOsTheme.colors.surfaceRaised, RoundedCornerShape(12.dp))
                                .border(1.dp, ArOsTheme.colors.success.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "New PIN set. Your recovery code is unchanged. Keep it safe.",
                                color = Color.White,
                                style = ArOsTheme.typography.body,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onSuccessUnlock,
                            colors = ButtonDefaults.buttonColors(containerColor = ArOsTheme.colors.accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp).fillMaxWidth(0.85f)
                        ) {
                            Text("Unlock AR Guard", color = ArOsTheme.colors.contentOnAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Bottom Area: Keypads and navigation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                when (mode) {
                    GateMode.PIN_ENTRY -> {
                        NumericKeypad(
                            enabled = !isPinChecking && lockoutRemainingSeconds <= 0,
                            actionLabel = "Unlock",
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
                            onAction = {
                                if (pinInput.length >= 6 && !isPinChecking && lockoutRemainingSeconds <= 0) {
                                    isPinChecking = true
                                    errorMessage = null
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                // Fix 1.2: Switching between PIN pad and recovery mode never submits anything
                                isPinChecking = false
                                errorMessage = null
                                recoveryCodeInput = ""
                                mode = GateMode.RECOVERY_CODE_ENTRY
                            }
                        ) {
                            Text("Forgot PIN?", color = ArOsTheme.colors.accent, fontSize = 13.sp)
                        }
                    }

                    GateMode.RECOVERY_CODE_ENTRY -> {
                        // Fix 1.1: Custom on-screen recovery keyboard drawn by the gate (30 recovery chars)
                        RecoveryKeyboard(
                            enabled = !isRecoveryChecking && lockoutRemainingSeconds <= 0,
                            codeLength = recoveryCodeInput.length,
                            onChar = { ch ->
                                if (recoveryCodeInput.length < 16) {
                                    recoveryCodeInput += ch
                                    errorMessage = null
                                }
                            },
                            onDelete = {
                                if (recoveryCodeInput.isNotEmpty()) {
                                    recoveryCodeInput = recoveryCodeInput.dropLast(1)
                                    errorMessage = null
                                }
                            },
                            onVerify = {
                                if (recoveryCodeInput.length == 16 && !isRecoveryChecking && lockoutRemainingSeconds <= 0) {
                                    isRecoveryChecking = true
                                    errorMessage = null
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                // Switching back to PIN pad never submits anything
                                isRecoveryChecking = false
                                errorMessage = null
                                pinInput = ""
                                mode = GateMode.PIN_ENTRY
                            }
                        ) {
                            Text("Back to PIN Pad", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                    }

                    GateMode.SET_NEW_PIN_FIRST -> {
                        NumericKeypad(
                            enabled = true,
                            actionLabel = "Next",
                            onDigit = { digit ->
                                if (newPinFirst.length < 12) {
                                    newPinFirst += digit
                                    errorMessage = null
                                }
                            },
                            onDelete = {
                                if (newPinFirst.isNotEmpty()) {
                                    newPinFirst = newPinFirst.dropLast(1)
                                    errorMessage = null
                                }
                            },
                            onAction = {
                                if (newPinFirst.length in 6..12) {
                                    errorMessage = null
                                    mode = GateMode.SET_NEW_PIN_CONFIRM
                                } else {
                                    errorMessage = "PIN must be 6 to 12 digits"
                                }
                            }
                        )
                    }

                    GateMode.SET_NEW_PIN_CONFIRM -> {
                        NumericKeypad(
                            enabled = !isResetChecking,
                            actionLabel = "Save",
                            onDigit = { digit ->
                                if (newPinConfirm.length < 12) {
                                    newPinConfirm += digit
                                    errorMessage = null
                                }
                            },
                            onDelete = {
                                if (newPinConfirm.isNotEmpty()) {
                                    newPinConfirm = newPinConfirm.dropLast(1)
                                    errorMessage = null
                                }
                            },
                            onAction = {
                                if (newPinConfirm != newPinFirst) {
                                    errorMessage = "PINs do not match. Try again."
                                    newPinConfirm = ""
                                } else {
                                    isResetChecking = true
                                    errorMessage = null
                                }
                            }
                        )
                    }

                    GateMode.RECOVERY_SUCCESS_NOTICE -> {
                        // Notice is displayed in the middle section
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
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

private fun formatRecoveryDisplay(code: String): String {
    return code.chunked(4).joinToString("-")
}

@Composable
private fun PinDotsDisplay(pinLength: Int, minDots: Int = 6) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 10.dp)
    ) {
        val dotCount = pinLength.coerceAtLeast(minDots)
        for (i in 0 until dotCount) {
            val filled = i < pinLength
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
}

@Composable
private fun NumericKeypad(
    enabled: Boolean,
    actionLabel: String,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onAction: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("DEL", "0", actionLabel)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(0.85f)
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when (key) {
                                    actionLabel -> ArOsTheme.colors.accent
                                    "DEL" -> Color.White.copy(alpha = 0.08f)
                                    else -> Color.White.copy(alpha = 0.08f)
                                }
                            )
                            .clickable(enabled = enabled) {
                                when (key) {
                                    "DEL" -> onDelete()
                                    actionLabel -> onAction()
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
                            actionLabel -> Text(
                                text = actionLabel,
                                color = ArOsTheme.colors.contentOnAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            else -> Text(
                                text = key,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * On-screen recovery keyboard drawn by the gate (Fix 1.1).
 * Uses the exact recovery charset: digits 2-9 and letters A-Z excluding I and O (total 30 characters).
 * Zero system keyboard dependency; window remains FLAG_NOT_FOCUSABLE.
 */
@Composable
private fun RecoveryKeyboard(
    enabled: Boolean,
    codeLength: Int,
    onChar: (String) -> Unit,
    onDelete: () -> Unit,
    onVerify: () -> Unit
) {
    // 30 characters organized in 5 rows of 6 keys, plus bottom row for DEL and VERIFY
    val charRows = listOf(
        listOf("2", "3", "4", "5", "6", "7"),
        listOf("8", "9", "A", "B", "C", "D"),
        listOf("E", "F", "G", "H", "J", "K"),
        listOf("L", "M", "N", "P", "Q", "R"),
        listOf("S", "T", "U", "V", "W", "X")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        charRows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { char ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(enabled = enabled) {
                                onChar(char)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bottom row: remaining 2 chars (Y, Z) + Delete + Verify
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("Y", "Z").forEach { char ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(enabled = enabled) {
                            onChar(char)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Delete key
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(enabled = enabled) {
                        onDelete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Verify Key
            Box(
                modifier = Modifier
                    .weight(2.5f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (codeLength == 16) ArOsTheme.colors.accent else ArOsTheme.colors.accent.copy(alpha = 0.4f)
                    )
                    .clickable(enabled = enabled && codeLength == 16) {
                        onVerify()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Verify",
                    color = ArOsTheme.colors.contentOnAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
