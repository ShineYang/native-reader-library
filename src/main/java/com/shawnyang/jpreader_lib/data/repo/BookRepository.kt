/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.shawnyang.jpreader_lib.data.repo

import androidx.lifecycle.LiveData
import com.shawnyang.jpreader_lib.data.room.BooksDao
import com.shawnyang.jpreader_lib.data.room.model.Book
import com.shawnyang.jpreader_lib.data.room.model.Bookmark
import org.joda.time.DateTime
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.util.mediatype.MediaType

class BookRepository(private val booksDao: BooksDao) {

    suspend fun getBookList(): MutableList<Book>{
        return booksDao.getAllBooks()
    }

    suspend fun get(id: Long) = booksDao.get(id)

    suspend fun getByTitle(title: String) = booksDao.getByTitle(title)

    suspend fun insertBook(href: String, mediaType: MediaType, publication: Publication, cover: ByteArray): Long {
        val book = Book(
            creation = DateTime().toDate().time,
            title = publication.metadata.title,
            author = publication.metadata.authors.firstOrNull()?.name ?: "",
            href = href,
            identifier = publication.metadata.identifier ?: "",
            type = mediaType.toString(),
            progression = "{}",
            cover = cover
        )
        return booksDao.insertBook(book)
    }

    suspend fun deleteBook(id: Long) = booksDao.deleteBook(id)

    suspend fun saveProgression(locator: Locator, bookId: Long) =
        booksDao.saveProgression(locator.toJSON().toString(), bookId)

    suspend fun insertBookmark(bookId: Long, publication: Publication, locator: Locator): Long {
        val resource = publication.readingOrder.indexOfFirstWithHref(locator.href)!!
        val bookmark = Bookmark(
            creation = DateTime().toDate().time,
            bookId = bookId,
            publicationId = publication.metadata.identifier ?: publication.metadata.title,
            resourceIndex = resource.toLong(),
            resourceHref = locator.href,
            resourceType = locator.type,
            resourceTitle = locator.title.orEmpty(),
            location = locator.locations.toJSON().toString(),
            locatorText = Locator.Text().toJSON().toString()
        )

        return booksDao.insertBookmark(bookmark)
    }

    fun bookmarksForBook(bookId: Long): LiveData<MutableList<Bookmark>> =
        booksDao.getBookmarksForBook(bookId)

    suspend fun deleteBookmark(bookmarkId: Long) = booksDao.deleteBookmark(bookmarkId)
}