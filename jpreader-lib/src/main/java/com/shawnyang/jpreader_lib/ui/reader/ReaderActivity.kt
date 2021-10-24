/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.shawnyang.jpreader_lib.ui.reader

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import com.shawnyang.jpreader_lib.ui.reader.react.ReaderContract
import com.shawnyang.jpreader_lib.ui.reader.react.ReaderViewModel
import com.shawnyang.jpreader_lib.R
import com.shawnyang.jpreader_lib.data.db.BookData
import com.shawnyang.jpreader_lib.databinding.ActivityReaderBinding
import org.readium.r2.shared.publication.Publication
import timber.log.Timber

/*
 * An activity to read a publication
 *
 * This class can be used as it is or be inherited from.
 */
open class ReaderActivity : AppCompatActivity() {
    private val binding = ActivityReaderBinding.inflate(layoutInflater)

    protected lateinit var readerFragment: VisualReaderFragment
    private lateinit var modelFactory: ReaderViewModel.Factory
    private lateinit var publication: Publication
    private lateinit var persistence: BookData

    override fun onCreate(savedInstanceState: Bundle?) {
        val inputData = ReaderContract.parseIntent(this)
        modelFactory = ReaderViewModel.Factory(applicationContext, inputData)
        super.onCreate(savedInstanceState)

        ViewModelProvider(this).get(ReaderViewModel::class.java).let { model ->
            publication = model.publication
            persistence = model.persistence
            model.channel.receive(this) {
                //处理reader交互
            }
        }

        if (savedInstanceState == null) {
            if (publication.type == Publication.TYPE.EPUB) {
                val baseUrl = requireNotNull(inputData.baseUrl)
                readerFragment = EpubReaderFragment.newInstance(baseUrl, inputData.bookId)

                supportFragmentManager.commitNow {
                    replace(R.id.activity_container, readerFragment, READER_FRAGMENT_TAG)
                }
            }else Timber.v("不支持的类型")
        }

        readerFragment = supportFragmentManager.findFragmentByTag(READER_FRAGMENT_TAG) as VisualReaderFragment

        // Add support for display cutout.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return modelFactory
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, intent)
        super.finish()
    }

    companion object {
        const val READER_FRAGMENT_TAG = "reader"
    }
}
