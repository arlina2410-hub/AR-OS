package com.aros.app.feature.guard.platform

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.guard.domain.GuardRepository
import com.aros.app.feature.guard.ui.GuardGateOverlayView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore = store

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    fun attachTo(view: View) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
    }
}

class GuardOverlayManager(
    private val context: Context,
    private val guardRepository: GuardRepository,
    private val scope: CoroutineScope
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private var activeOverlayView: View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var testDismissRunnable: Runnable? = null

    val isShowing: Boolean
        get() = activeOverlayView != null

    fun showOverlay(isTestMode: Boolean, onUnlocked: () -> Unit) {
        if (activeOverlayView != null || windowManager == null) return

        try {
            val owner = OverlayLifecycleOwner()
            lifecycleOwner = owner

            var testRemainingSec = 20

            val composeView = ComposeView(context).apply {
                owner.attachTo(this)
                setContent {
                    ArOsTheme {
                        GuardGateOverlayView(
                            isTestMode = isTestMode,
                            testRemainingSeconds = testRemainingSec,
                            onVerifyPin = { pin ->
                                guardRepository.verifyPin(pin)
                            },
                            onVerifyRecovery = { code ->
                                guardRepository.verifyRecoveryCode(code)
                            },
                            onSuccessUnlock = {
                                dismissOverlay()
                                onUnlocked()
                            },
                            onCloseTest = {
                                dismissOverlay()
                            }
                        )
                    }
                }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_SECURE,
                PixelFormat.TRANSLUCENT
            )

            windowManager.addView(composeView, params)
            activeOverlayView = composeView
            guardRepository.setGateShowing(true)

            if (isTestMode) {
                testDismissRunnable = object : Runnable {
                    override fun run() {
                        testRemainingSec--
                        if (testRemainingSec <= 0) {
                            dismissOverlay()
                        } else {
                            mainHandler.postDelayed(this, 1000L)
                        }
                    }
                }
                mainHandler.postDelayed(testDismissRunnable!!, 1000L)
            }
        } catch (_: Exception) {
            // Never crash service
            dismissOverlay()
        }
    }

    fun dismissOverlay() {
        testDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        testDismissRunnable = null

        val view = activeOverlayView ?: return
        activeOverlayView = null
        guardRepository.setGateShowing(false)

        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {
            // Ignore removal errors
        }

        try {
            lifecycleOwner?.destroy()
            lifecycleOwner = null
        } catch (_: Exception) {
            // Ignore cleanup errors
        }
    }
}
