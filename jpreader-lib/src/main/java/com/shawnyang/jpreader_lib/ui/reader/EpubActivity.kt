/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package com.shawnyang.jpreader_lib.ui.reader

import android.annotation.SuppressLint
import android.graphics.PointF
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.*
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import com.shawnyang.jpreader_lib.ui.reader.react.ReaderContract
import com.shawnyang.jpreader_lib.ui.reader.react.ReaderViewModel
import com.shawnyang.jpreader_lib.R
import com.shawnyang.jpreader_lib.data.db.BookData
import com.shawnyang.jpreader_lib.databinding.ActivityReaderBinding
import com.shawnyang.jpreader_lib.exts.*
import com.shawnyang.jpreader_lib.ui.reader.outline.OutlineContract
import kotlinx.android.synthetic.main.activity_reader.*
import org.jetbrains.anko.toast
import org.json.JSONException
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.R2EpubActivity
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.ReadiumCSSName
import org.readium.r2.shared.SCROLL_REF


class EpubActivity : R2EpubActivity() {

    private lateinit var modelFactory: ReaderViewModel.Factory
    private lateinit var readerFragment: EpubReaderFragment

    private lateinit var persistence: BookData

    //Accessibility
    private var isExploreByTouchEnabled = false
    private var pageEnded = false

    // Highlights
    private var mode: ActionMode? = null

    private lateinit var binding: ActivityReaderBinding

    lateinit var userSettings: UserSettings

    override fun navigatorFragment(): EpubNavigatorFragment =
        readerFragment.childFragmentManager.findFragmentByTag(getString(R.string.epub_navigator_tag)) as EpubNavigatorFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        val inputData = ReaderContract.parseIntent(this)
        modelFactory = ReaderViewModel.Factory(applicationContext, inputData)
        super.onCreate(savedInstanceState)
        binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewModelProvider(this).get(ReaderViewModel::class.java).let { model ->
            persistence = model.persistence
        }

        /* FIXME: When the OutlineFragment is left by pressing the back button,
        * the Webview is not updated, so removed highlights will still be visible.
        */

        supportFragmentManager.setFragmentResultListener(
            OutlineContract.REQUEST_KEY,
            this,
            FragmentResultListener { _, result ->
                val locator = OutlineContract.parseResult(result).destination
                closeOutlineFragment(locator)
            }
        )

        supportFragmentManager.addOnBackStackChangedListener {
            updateActivityTitle()
        }

        if (savedInstanceState == null) {
            val bookId = inputData.bookId
            val baseUrl = requireNotNull(inputData.baseUrl)
            readerFragment = EpubReaderFragment.newInstance(baseUrl, bookId)

            supportFragmentManager.commitNow {
                replace(R.id.activity_container, readerFragment, READER_FRAGMENT_TAG)
            }

        } else {
            readerFragment = supportFragmentManager.findFragmentByTag(READER_FRAGMENT_TAG) as EpubReaderFragment
        }

        // Without this, activity_reader_container receives the insets only once,
        // although we need a call every time the reader is hidden
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            val newInsets = view.onApplyWindowInsets(insets)
            activity_container.dispatchApplyWindowInsets(newInsets)
        }


        supportFragmentManager.addOnBackStackChangedListener {
            updateSystemUiVisibility()
        }
    }

    override fun onStart() {
        super.onStart()
        updateSystemUiVisibility()
        updateActivityTitle()
    }

    override fun onTap(point: PointF): Boolean {
        return super.onTap(point)
    }

    private fun updateSystemUiVisibility() {
        if (readerFragment.isHidden)
            showSystemUi()
        else
            readerFragment.updateSystemUiVisibility()

        // Seems to be required to adjust padding when transitioning from the outlines to the screen reader
        activity_container.requestApplyInsets()
    }

    private fun updateSystemUiPadding(container: View, insets: WindowInsets) {
        if (readerFragment.isHidden)
            container.padSystemUi(insets, this)
        else
            container.clearPadding()
    }

    private fun updateActivityTitle() {
        title = when (supportFragmentManager.fragments.last()) {
            else -> null
        }
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return modelFactory
    }

    @SuppressLint("JavascriptInterface")
    override fun onActionModeStarted(mode: ActionMode?) {
        super.onActionModeStarted(mode)
        mode?.menu?.run {
            menuInflater.inflate(R.menu.menu_action_mode, this)
            findItem(R.id.highlight).setOnMenuItemClickListener {
                val currentFragment =
                    ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment
                currentFragment?.webView?.getCurrentSelectionRect {
                    val rect = JSONObject(it).run {
                        try {
                            val display = windowManager.defaultDisplay
                            val metrics = DisplayMetrics()
                            display.getMetrics(metrics)
                            val left = getDouble("left")
                            val width = getDouble("width")
                            val top = getDouble("top") * metrics.density
                            val height = getDouble("height") * metrics.density
                            Rect(left.toInt(), top.toInt(), width.toInt() + left.toInt(), top.toInt() + height.toInt())
                        } catch (e: JSONException) {
                            null
                        }
                    }
                }
                true
            }
        }
        this.mode = mode
    }

    override fun onPageLoaded() {
        super.onPageLoaded()
    }

    override fun highlightActivated(id: String) {

    }

    override fun highlightAnnotationMarkActivated(id: String) {
    }

    /**
     * Manage what happens when the focus is put back on the EpubActivity.
     */
    override fun onResume() {
        super.onResume()

        /*
         * If TalkBack or any touch exploration service is activated
         * we force scroll mode (and override user preferences)
         */
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        isExploreByTouchEnabled = am.isTouchExplorationEnabled

        if (isExploreByTouchEnabled) {

            //Preset & preferences adapted
            publication.userSettingsUIPreset[ReadiumCSSName.ref(SCROLL_REF)] = true
            preferences.edit().putBoolean(SCROLL_REF, true).apply() //overriding user preferences

            userSettings = UserSettings(preferences, this, publication.userSettingsUIPreset)
            userSettings.saveChanges()

            Handler().postDelayed({
                userSettings.resourcePager = resourcePager
                userSettings.updateViewCSS(SCROLL_REF)
            }, 500)
        } else {
            if (publication.cssStyle != "cjk-vertical") {
                publication.userSettingsUIPreset.remove(ReadiumCSSName.ref(SCROLL_REF))
            }

            userSettings = UserSettings(preferences, this, publication.userSettingsUIPreset)
            userSettings.resourcePager = resourcePager
        }
    }

    /**
     * Communicate with the user using a toast if touch exploration is enabled, to indicate the end of a chapter.
     */
    override fun onPageEnded(end: Boolean) {
        if (isExploreByTouchEnabled) {
            if (!pageEnded == end && end) {
                toast("End of chapter")
            }
            pageEnded = end
        }
    }

    private fun closeOutlineFragment(locator: Locator) {
        readerFragment.go(locator, true)
        supportFragmentManager.popBackStack()
    }

    companion object {
        const val READER_FRAGMENT_TAG = "reader"
    }
}