/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.shawnyang.jpreader_lib.ui.reader

import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.fragment.app.Fragment
import com.shawnyang.jpreader_lib.ui.base.BaseReaderFragment
import com.shawnyang.jpreader_lib.exts.clearPadding
import com.shawnyang.jpreader_lib.exts.hideSystemUi
import com.shawnyang.jpreader_lib.exts.padSystemUi
import com.shawnyang.jpreader_lib.exts.showSystemUi
import kotlinx.android.synthetic.main.fragment_reader.*

/*
 * Adds fullscreen support to the BaseReaderFragment
 */
abstract class VisualReaderFragment : BaseReaderFragment() {

    private lateinit var navigatorFragment: Fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigatorFragment = navigator as Fragment

        childFragmentManager.addOnBackStackChangedListener {
            updateSystemUiVisibility()
        }

        fragment_reader_container.setOnApplyWindowInsetsListener { container, insets ->
            updateSystemUiPadding(container, insets)
            insets
        }
    }

    fun updateSystemUiVisibility() {
        if (navigatorFragment.isHidden)
            requireActivity().showSystemUi()
        else
            requireActivity().hideSystemUi()

        requireView().requestApplyInsets()
    }

    private fun updateSystemUiPadding(container: View, insets: WindowInsets) {
        if (navigatorFragment.isHidden) {
            container.padSystemUi(insets, requireActivity())
        } else {
            container.clearPadding()
        }
    }
}