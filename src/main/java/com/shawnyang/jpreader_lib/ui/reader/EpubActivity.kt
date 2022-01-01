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

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import com.mcxiaoke.koi.ext.toast
import com.shawnyang.jpreader_lib.ui.reader.react.ReaderContract
import com.shawnyang.jpreader_lib.ui.reader.react.ReaderViewModel
import com.shawnyang.jpreader_lib.R
import com.shawnyang.jpreader_lib.databinding.ActivityReaderBinding
import com.shawnyang.jpreader_lib.exts.*
import com.shawnyang.jpreader_lib.ui.reader.outline.OutlineContract
import org.greenrobot.eventbus.EventBus
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.R2EpubActivity
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.ReadiumCSSName
import org.readium.r2.shared.SCROLL_REF
import java.lang.Exception


class EpubActivity : R2EpubActivity() {

    private lateinit var modelFactory: ReaderViewModel.Factory
    private lateinit var readerFragment: EpubReaderFragment

    //Accessibility
    private var isExploreByTouchEnabled = false
    private var pageEnded = false

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
            binding.activityContainer.dispatchApplyWindowInsets(newInsets)
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

    private fun updateSystemUiVisibility() {
        if (readerFragment.isHidden)
            showSystemUi()
        else
            readerFragment.updateSystemUiVisibility()
        // Seems to be required to adjust padding when transitioning from the outlines to the screen reader
        binding.activityContainer.requestApplyInsets()
    }

    private fun updateActivityTitle() {
        title = when (supportFragmentManager.fragments.last()) {
            else -> null
        }
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return modelFactory
    }

    override fun onPageChanged(pageIndex: Int, totalPages: Int, url: String) {
        val currentFragment =
                ((resourcePager.adapter as R2PagerAdapter).mFragments.get((resourcePager.adapter as R2PagerAdapter).getItemId(resourcePager.currentItem))) as? R2EpubPageFragment
        val webView = currentFragment?.webView
        loadParagraphTextInjection(webView)
    }

    private fun loadParagraphTextInjection(webView: WebView?){
        webView?.webViewClient = object : WebViewClient(){
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                if (!request!!.isForMainFrame && request.url.path!!.endsWith("/favicon.ico")) {
                    try {
                        return WebResourceResponse("image/png", null, null)
                    } catch (e: Exception) {
                        Log.e("LoadFavicon", "shouldInterceptRequest failed", e)
                    }
                }
                return null
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val paragraph = request?.url?.getQueryParameters("paragraphText")
                return if (paragraph != null && paragraph.size > 0){
                    EventBus.getDefault().post(paragraph[0])
                    true
                }else false
            }
        }
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
                toast(getString(R.string.end_of_the_chapter))
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