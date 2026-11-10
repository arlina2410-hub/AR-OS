package com.aros.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.aros.app.designsystem.theme.ArOsTheme as NewArOsTheme

@Composable
fun ArOsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    NewArOsTheme(
        darkTheme = darkTheme,
        content = content
    )
}
