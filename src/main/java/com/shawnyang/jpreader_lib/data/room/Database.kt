package com.shawnyang.jpreader_lib.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.shawnyang.jpreader_lib.data.room.model.Book
import com.shawnyang.jpreader_lib.data.room.model.Bookmark
import com.shawnyang.jpreader_lib.data.room.model.Catalog
import org.readium.r2.testapp.db.CatalogDao

@Database(
    entities = [Book::class, Bookmark::class, Catalog::class],
    version = 1,
    exportSchema = false
)
abstract class BookDatabase : RoomDatabase() {

    abstract fun booksDao(): BooksDao

    abstract fun catalogDao(): CatalogDao

    companion object {
        @Volatile
        private var INSTANCE: BookDatabase? = null

        fun getDatabase(context: Context): BookDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookDatabase::class.java,
                    "books_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}