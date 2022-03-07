/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.shawnyang.jpreader_lib.ui.reader.react

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shawnyang.jpreader_lib.data.repo.BookRepository
import com.shawnyang.jpreader_lib.data.room.BookDatabase
import com.shawnyang.jpreader_lib.data.room.model.Book
import com.shawnyang.jpreader_lib.exts.EventChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.PublicationId
import java.util.*

class ReaderViewModel(context: Context, arguments: ReaderContract.Input) : ViewModel() {
    val initialLocation: Locator? = arguments.initialLocator
    val bookId = arguments.bookId
    val fragmentChannel = EventChannel(Channel<FeedbackEvent>(Channel.BUFFERED), viewModelScope)
    private val bookRepository: BookRepository
    val publication: Publication = arguments.publication
    val publicationId: PublicationId get() = bookId.toString()

    init {
        val booksDao = BookDatabase.getDatabase(context.applicationContext).booksDao()
        bookRepository = BookRepository(booksDao)
    }

    class Factory(private val context: Context, private val arguments: ReaderContract.Input)
        : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                modelClass.getDeclaredConstructor(Context::class.java, ReaderContract.Input::class.java)
                        .newInstance(context.applicationContext, arguments)
    }

    suspend fun getBookById(id: Long): Book?{
        return bookRepository.get(id)
    }

    fun saveProgression(locator: Locator) = viewModelScope.launch {
        bookRepository.saveProgression(locator, bookId)
    }

    fun getBookmarks() = bookRepository.bookmarksForBook(bookId)

    fun insertBookmark(locator: Locator) = viewModelScope.launch {
        val id = bookRepository.insertBookmark(bookId, publication, locator)
        if (id != -1L) {
            //添加成功
            fragmentChannel.send(FeedbackEvent.BookmarkSuccessfullyAdded)
        } else {
            //添加失败 重复
            fragmentChannel.send(FeedbackEvent.BookmarkFailed)
        }
    }

    fun deleteBookmark(id: Long) = viewModelScope.launch {
        bookRepository.deleteBookmark(id)
    }

    private fun getDir(appCtx: Context): String {
        val properties = Properties()
        val inputStream = appCtx.assets.open("configs/config.properties")
        properties.load(inputStream)
        val useExternalFileDir =
                properties.getProperty("useExternalFileDir", "false")!!.toBoolean()
        return if (useExternalFileDir) {
            appCtx.getExternalFilesDir(null)?.path + "/"
        } else {
            appCtx.filesDir?.path + "/"
        }
    }

    sealed class FeedbackEvent {
        object BookmarkSuccessfullyAdded : FeedbackEvent()
        object BookmarkFailed : FeedbackEvent()
    }
}

