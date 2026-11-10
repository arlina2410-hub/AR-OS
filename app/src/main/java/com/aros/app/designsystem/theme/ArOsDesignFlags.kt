package com.aros.app.designsystem.theme

/**
 * Feature flags for AR/OS Design System.
 */
object ArOsDesignFlags {
    /**
     * Tier 0 (false): Opaque layered glass surfaces with crisp hairline borders (default, high performance).
     * Tier 1 (true): Realtime backdrop blur (Haze / RenderEffect) if enabled and supported by GPU.
     */
    const val GlassBlur: Boolean = false
}
