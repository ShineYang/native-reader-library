/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.shawnyang.jpreader_lib.ui.reader

import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.view.*
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mcxiaoke.koi.ext.toast
import com.shawnyang.jpreader_lib.ui.reader.react.ReaderViewModel
import com.shawnyang.jpreader_lib.ui.reader.outline.ReaderOutlineSheet
import com.shawnyang.jpreader_lib.R
import com.shawnyang.jpreader_lib.data.db.BookData
import com.shawnyang.jpreader_lib.exts.toggleSystemUi
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.APPEARANCE_REF
import org.readium.r2.shared.publication.Publication
import java.net.URL

class EpubReaderFragment : VisualReaderFragment(), EpubNavigatorFragment.Listener {

    override lateinit var model: ReaderViewModel
    override lateinit var navigator: Navigator
    private lateinit var publication: Publication
    private lateinit var persistence: BookData
    lateinit var navigatorFragment: EpubNavigatorFragment

    private var isSearchViewIconified = true

    private val activity: EpubActivity
        get() = requireActivity() as EpubActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            isSearchViewIconified = savedInstanceState.getBoolean(IS_SEARCH_VIEW_ICONIFIED)
        }

        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            model = it
            publication = it.publication
            persistence = it.persistence
        }

        val baseUrl = checkNotNull(requireArguments().getString(BASE_URL_ARG))

        childFragmentManager.fragmentFactory =
                EpubNavigatorFragment.createFactory(
                        publication,
                        baseUrl,
                        persistence.savedLocation,
                        this
                )
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val navigatorFragmentTag = getString(R.string.epub_navigator_tag)

        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                add(
                        R.id.fragment_reader_container,
                        EpubNavigatorFragment::class.java,
                        Bundle(),
                        navigatorFragmentTag
                )
            }
        }
        navigator = childFragmentManager.findFragmentByTag(navigatorFragmentTag) as Navigator
        navigatorFragment = navigator as EpubNavigatorFragment
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // This is a hack to draw the right background color on top and bottom blank spaces
        navigatorFragment.lifecycleScope.launchWhenStarted {
            val appearancePref = activity.preferences.getInt(APPEARANCE_REF, 0)
            val backgroundsColors = mutableListOf("#ffffff", "#faf4e8", "#000000")
            navigatorFragment.resourcePager.setBackgroundColor(Color.parseColor(backgroundsColors[appearancePref]))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater)
        menuInflater.inflate(R.menu.menu_epub, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (super.onOptionsItemSelected(item)) {
            return true
        }

        return when (item.itemId) {
            R.id.menu_bookmark -> {
                val added = model.persistence.addBookmark(navigator.currentLocator.value)
                toast(if (added) "已添加书签" else "书签已存在")
                true
            }

            R.id.menu_settings -> {
                activity.userSettings.userSettingsPopUp().showAsDropDown(
                        requireActivity().findViewById(R.id.menu_settings),
                        0,
                        0,
                        Gravity.END
                )
                true
            }

            R.id.menu_navi -> {
                ReaderOutlineSheet().show(activity.supportFragmentManager, "ReaderProgressSheet")
                true
            }
            else -> false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_SEARCH_VIEW_ICONIFIED, isSearchViewIconified)
    }

    override fun onTap(point: PointF): Boolean {
        requireActivity().toggleSystemUi()
        return true
    }

    companion object {

        private const val BASE_URL_ARG = "baseUrl"
        private const val BOOK_ID_ARG = "bookId"

        private const val IS_SEARCH_VIEW_ICONIFIED = "isSearchViewIconified"

        fun newInstance(baseUrl: URL, bookId: Long): EpubReaderFragment {
            return EpubReaderFragment().apply {
                arguments = Bundle().apply {
                    putString(BASE_URL_ARG, baseUrl.toString())
                    putLong(BOOK_ID_ARG, bookId)
                }
            }
        }
    }
}