package com.aros.app.feature.sos.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory state tracking whether ArSosAccessibilityService is currently bound and active.
 */
object ServiceState {
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    fun setConnected(isConnected: Boolean) {
        _connected.value = isConnected
    }
}
