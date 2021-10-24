package com.shawnyang.jpreader_lib

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import com.shawnyang.jpreader_lib.BuildConfig.DEBUG
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant
import timber.log.Timber

/**
 * @author ShineYang
 * @date 2021/9/17
 * description:
 */
class App: Application() {

    override fun onCreate() {
        super.onCreate()
        // Configure Kovenant with standard dispatchers
        // suitable for an Android environment.
        startKovenant()
        if (DEBUG) Timber.plant(Timber.DebugTree())
    }

    override fun onTerminate() {
        super.onTerminate()
        // Dispose of the Kovenant thread pools.
        // For quicker shutdown you could use
        // `force=true`, which ignores all current
        // scheduled tasks
        stopKovenant()
    }
}

val Context.resolver: ContentResolver
    get() = applicationContext.contentResolver
