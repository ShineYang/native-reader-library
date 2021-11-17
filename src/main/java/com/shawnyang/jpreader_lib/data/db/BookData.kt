/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.shawnyang.jpreader_lib.data.db

import android.content.Context
import com.shawnyang.jpreader_lib.data.Book
import com.shawnyang.jpreader_lib.data.Bookmark
import com.shawnyang.jpreader_lib.data.BookmarksDatabase
import com.shawnyang.jpreader_lib.data.BooksDatabase
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref

class BookData(context: Context, private val bookId: Long, private val publication: Publication) {

    private val pubId: String = publication.metadata.identifier ?: publication.metadata.title
    private val booksDb = BooksDatabase(context)
    private val bookmarksDb = BookmarksDatabase(context)

    var savedLocation: Locator?
        get() = booksDb.books.currentLocator(bookId)
        set(locator) { booksDb.books.saveProgression(locator, bookId) }

    fun addBookmark(locator: Locator): Boolean {
        val resource = publication.readingOrder.indexOfFirstWithHref(locator.href)!!
        val bookmark = Bookmark(bookId, pubId, resource.toLong(), locator)
        return bookmarksDb.bookmarks.insert(bookmark) != null
    }

    fun removeBookmark(id: Long) {
        bookmarksDb.bookmarks.delete(id)
    }

    fun getBookmarks(comparator: Comparator<Bookmark>): List<Bookmark> {
        return bookmarksDb.bookmarks.list(bookId).sortedWith(comparator)
    }
}
