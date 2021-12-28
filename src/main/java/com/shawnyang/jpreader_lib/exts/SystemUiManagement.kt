/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.shawnyang.jpreader_lib.exts

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.gyf.immersionbar.ktx.immersionBar
import com.shawnyang.jpreader_lib.R
import org.jetbrains.anko.indeterminateProgressDialog

/** Returns `true` if fullscreen or immersive mode is not set. */
private fun Activity.isSystemUiVisible(): Boolean {
    return this.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0
}

/** Enable fullscreen or immersive mode. */
fun Activity.hideSystemUi() {
    this.window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
}

// Using ViewCompat and WindowInsetsCompat does not work properly in all versions of Android
@RequiresApi(Build.VERSION_CODES.M)
@Suppress("DEPRECATION")
        /** Disable fullscreen or immersive mode. */
fun Activity.showSystemUi(needLight: Boolean = false) {
    if(needLight)
        this.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE//防止系统栏隐藏时内容区域大小发生变化
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    else this.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE//防止系统栏隐藏时内容区域大小发生变化
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
}

/** Toggle fullscreen or immersive mode. */
@RequiresApi(Build.VERSION_CODES.M)
fun Activity.toggleSystemUi() {
    if (this.isSystemUiVisible()) {
        this.hideSystemUi()
    } else {
        immersionBar {
            statusBarColor(R.color.status_bar_bg)
            navigationBarColor(R.color.status_bar_bg)
        }
        this.showSystemUi(needLight = !isDarkTheme(this))
    }
}

fun isDarkTheme(activity: Activity): Boolean {
    return activity.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

/** Set padding around view so that content doesn't overlap system UI */
fun View.padSystemUi(insets: WindowInsets, activity: Activity) =
    setPadding(
        insets.systemWindowInsetLeft,
        insets.systemWindowInsetTop + (activity as AppCompatActivity).supportActionBar!!.height,
        insets.systemWindowInsetRight,
        insets.systemWindowInsetBottom
    )

/** Clear padding around view */
fun View.clearPadding() =
    setPadding(0, 0, 0, 0)

inline val Fragment.windowHeight: Int
    @RequiresApi(Build.VERSION_CODES.M)
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = requireActivity().windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsets(WindowInsets.Type.systemBars())
            metrics.bounds.height() - insets.bottom - insets.top
        } else {
            val view = requireActivity().window.decorView
            val insets = WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets, view).getInsets(WindowInsetsCompat.Type.systemBars())
            resources.displayMetrics.heightPixels - insets.bottom - insets.top
        }
    }

val Fragment.actionBarHeight: Int
    get() {
        return if (this.activity?.actionBar != null) {
            this.requireActivity().actionBar!!.height;
        } else 0
    }


fun Context.blockingProgressDialog(message: String) =
    indeterminateProgressDialog(message)
        .apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }

