package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.viewmodel.Screen

/**
 * Utility for managing Immersive Sticky Full-Screen Mode and System Bars across all screens.
 * Uses WindowInsetsControllerCompat to hide the Android Navigation Bar during gameplay,
 * allowing transient swipe interactions that auto-hide without shifting UI elements or interrupting gestures.
 */
object ImmersiveModeManager {

    /**
     * Initializes the Activity Window for modern edge-to-edge drawing and cutout handling.
     */
    fun setupWindow(activity: Activity) {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    /**
     * Applies immersive mode based on the current active screen.
     * - Game & Play screens: Hides the navigation bar in Sticky Immersive mode (transient on swipe, auto-disappearing).
     * - Forms & Settings screens: Displays standard system bars for easy typing and navigation.
     */
    fun applyScreenMode(activity: Activity, screen: Screen) {
        val window = activity.window ?: return
        val decorView = window.decorView ?: return
        val insetsController = WindowCompat.getInsetsController(window, decorView)

        when (screen) {
            is Screen.Login,
            is Screen.PrivacySecurity,
            is Screen.EditProfile,
            is Screen.AdminPanel -> {
                // Standard System Bars for forms & admin controls
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            is Screen.Game,
            is Screen.Matchmaking,
            is Screen.Lobby -> {
                // Strict Gaming Immersive Mode: Hides Navigation Bar with sticky swipe-to-show
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.navigationBars())
            }
            else -> {
                // Home, Challenges, Wallet, Profile, etc.
                // Clean game dashboard experience: Hide navigation bar transiently so UI has maximum space
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }
}

/**
 * Finds the host Activity from a Compose Context.
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Composable lifecycle effect that automatically syncs the window insets and immersive mode
 * whenever the user transitions between screens or when the window regains focus.
 */
@Composable
fun ImmersiveSystemBarsEffect(currentScreen: Screen) {
    val context = LocalContext.current
    val activity = context.findActivity() ?: return

    DisposableEffect(currentScreen) {
        ImmersiveModeManager.applyScreenMode(activity, currentScreen)
        onDispose { }
    }
}
