package com.shawnyang.jpreader_lib.ui.reader

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.*
import com.shawnyang.jpreader_lib.data.repo.BookRepository
import com.shawnyang.jpreader_lib.data.room.BookDatabase
import com.shawnyang.jpreader_lib.data.room.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.mediatype.MediaType
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

/**
 * @author ShineYang
 * @date 2021/12/31
 * description:
 */

class ShelfViewModel : ViewModel() {
    private lateinit var coverDir: String
    private lateinit var bookRepository: BookRepository
    var books: MutableList<Book> = mutableListOf()

    fun initWithContext(appCtx: Application){
        coverDir = getDir(appCtx)
        bookRepository = BookRepository(BookDatabase.getDatabase(appCtx).booksDao())
    }

    suspend fun getBookById(id: Long): Book?{
        return bookRepository.get(id)
    }

    suspend fun getBookList(){
        books.clear()
        books.addAll(bookRepository.getBookList())
    }

    fun deleteBook(id: Long) = viewModelScope.launch {
        val book = getBookById(id) ?: return@launch
        book.id?.let { bookRepository.deleteBook(it) }
        tryOrNull { File(book.href).delete() }
    }

    suspend fun addPublicationToDatabase(
            href: String,
            mediaType: MediaType,
            publication: Publication
    ): Long {
        val coverBitmap: Bitmap? = publication.cover()

        val coverToDB: ByteArray = if(coverBitmap != null){
            val resizedCoverHeight = 400
            val originRatio: Float = 1.0f * coverBitmap.width / 1.0f * coverBitmap.height
            val resizedCover = coverBitmap.let { Bitmap.createScaledBitmap(it, (originRatio * resizedCoverHeight).toInt(), resizedCoverHeight, true) }
            val outStream = ByteArrayOutputStream()
            resizedCover.compress(Bitmap.CompressFormat.PNG, 80, outStream)
            outStream.toByteArray()
        }else byteArrayOf()
        return bookRepository.insertBook(href, mediaType, publication, coverToDB)
    }

    private fun getDir(appCtx: Context): String {
        val useExternalFileDir = false
        return if (useExternalFileDir) {
            appCtx.getExternalFilesDir(null)?.path + "/"
        } else {
            appCtx.filesDir?.path + "/"
        }
    }
}