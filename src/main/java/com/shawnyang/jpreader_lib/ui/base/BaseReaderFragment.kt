/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.shawnyang.jpreader_lib.ui.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.shawnyang.jpreader_lib.R
import com.shawnyang.jpreader_lib.ui.reader.react.ReaderViewModel
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.publication.Locator
import timber.log.Timber

/*
 * Base reader fragment class
 *
 * Provides common menu items and saves last location on stop.
 */
abstract class BaseReaderFragment : Fragment(R.layout.fragment_reader) {

    protected abstract var model: ReaderViewModel
    protected abstract var navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onStop() {
        model.persistence.savedLocation = navigator.currentLocator.value
        super.onStop()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        setMenuVisibility(!hidden)
        requireActivity().invalidateOptionsMenu()
    }

    fun go(locator: Locator, animated: Boolean) =
        navigator.go(locator, animated)
}