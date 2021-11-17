/*
 * Module: r2-testapp-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package com.shawnyang.jpreader_lib.ui.reader.react

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.shawnyang.jpreader_lib.ui.reader.EpubActivity
import org.readium.r2.shared.extensions.destroyPublication
import org.readium.r2.shared.extensions.getPublication
import org.readium.r2.shared.extensions.putPublication
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.mediatype.MediaType
import java.io.File
import java.net.URL

class ReaderContract : ActivityResultContract<ReaderContract.Input, ReaderContract.Output>() {

    data class Input(
        val file: File,
        val mediaType: MediaType?,
        val publication: Publication,
        val bookId: Long,
        val initialLocator: Locator? = null,
        val deleteOnResult: Boolean = false,
        val baseUrl: URL? = null
    )

    data class Output(
        val file: File,
        val publication: Publication,
        val deleteOnResult: Boolean
    )

    override fun createIntent(context: Context, input: Input): Intent {
        val intent = Intent(context, when (input.mediaType) {
            MediaType.EPUB -> EpubActivity::class.java
            else -> throw IllegalArgumentException("Unknown [mediaType]")
        })

        return intent.apply {
            putPublication(input.publication)
            putExtra("bookId", input.bookId)
            putExtra("publicationPath", input.file.path)
            putExtra("publicationFileName", input.file.name)
            putExtra("deleteOnResult", input.deleteOnResult)
            putExtra("baseUrl", input.baseUrl?.toString())
            putExtra("locator", input.initialLocator)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Output? {
        if (intent == null)
            return null

        val path = intent.getStringExtra("publicationPath")
            ?: throw Exception("publicationPath required")

        intent.destroyPublication(null)

        return Output(
            file = File(path),
            publication = intent.getPublication(null),
            deleteOnResult = intent.getBooleanExtra("deleteOnResult", false)
        )
    }

    companion object {

        fun parseIntent(activity: Activity): Input = with(activity) {
            Input(
                file = File(intent.getStringExtra("publicationPath")!!),
                mediaType = null,
                publication = intent.getPublication(activity),
                bookId = intent.getLongExtra("bookId", -1),
                initialLocator = intent.getParcelableExtra("locator"),
                deleteOnResult = intent.getBooleanExtra("deleteOnResult", false),
                baseUrl = URL(intent.getStringExtra("baseUrl"))
            )
        }
    }
}