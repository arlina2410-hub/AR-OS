package com.aros.app.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface Clock {
    fun currentTimeMillis(): Long
    fun elapsedRealtime(): Long

    object System : Clock {
        override fun currentTimeMillis(): Long = java.lang.System.currentTimeMillis()
        override fun elapsedRealtime(): Long = android.os.SystemClock.elapsedRealtime()
    }
}

interface DispatchersProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher

    object Default : DispatchersProvider {
        override val main: CoroutineDispatcher = Dispatchers.Main
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
    }
}
