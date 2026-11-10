package com.aros.app.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import com.aros.app.designsystem.theme.ArOsTheme

/**
 * Standard edge-to-edge AR/OS screen container with iOS-style collapsing large title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArOsScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    ),
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = ArOsTheme.colors.background,
        contentColor = ArOsTheme.colors.contentPrimary,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = if (scrollBehavior.state.collapsedFraction > 0.5f) {
                                ArOsTheme.typography.headline
                            } else {
                                ArOsTheme.typography.largeTitle
                            }
                        )
                        if (subtitle != null && scrollBehavior.state.collapsedFraction <= 0.5f) {
                            Text(
                                text = subtitle,
                                style = ArOsTheme.typography.caption,
                                color = ArOsTheme.colors.contentSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = ArOsTheme.colors.background,
                    scrolledContainerColor = ArOsTheme.colors.surfaceRaised.copy(alpha = 0.95f),
                    titleContentColor = ArOsTheme.colors.contentPrimary,
                    actionIconContentColor = ArOsTheme.colors.contentPrimary,
                    navigationIconContentColor = ArOsTheme.colors.contentPrimary
                )
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ArOsTheme.colors.background)
        ) {
            content(innerPadding)
        }
    }
}
