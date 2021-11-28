package com.shawnyang.jpreader_lib.ui.reader.outline

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.shawnyang.jpreader_lib.R
import com.shawnyang.jpreader_lib.data.Bookmark
import com.shawnyang.jpreader_lib.data.db.BookData
import com.shawnyang.jpreader_lib.ui.reader.react.ReaderViewModel
import kotlinx.android.synthetic.main.layout_bookmark_list.*
import org.readium.r2.shared.publication.Publication

/**
 * @author ShineYang
 * @date 2021/9/6
 * description:
 */

class BookmarksFragment : Fragment(R.layout.layout_bookmark_list) {
    lateinit var publication: Publication
    lateinit var persistence: BookData
    private lateinit var rvAdapter: BookMarkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            publication = it.publication
            persistence = it.persistence
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val comparator: Comparator<Bookmark> = compareBy({ it.resourceIndex }, { it.location.progression })
        val bookmarks = persistence.getBookmarks(comparator).toMutableList()
        setUpRV(publication, bookmarks)
    }

    private fun setUpRV(publication: Publication, items: MutableList<Bookmark>) {
        rvAdapter = BookMarkAdapter(publication, R.layout.item_recycle_bookmark)
        rvAdapter.setOnBookmarkDeleteRequested(object : BookMarkAdapter.OnBookmarkDeleteRequested {
            override fun onBookmarkDeleteRequested(id: Long) {
                persistence.removeBookmark(id)
            }
        })
        rvAdapter.setOnItemClickListener { _, _, position ->
            onBookmarkSelected(items[position])
        }
        rv_book_mark.layoutManager = LinearLayoutManager(activity)
        rv_book_mark.adapter = rvAdapter
        rvAdapter.data = items
    }

    private fun onBookmarkSelected(bookmark: Bookmark) {
        setFragmentResult(
                OutlineContract.REQUEST_KEY,
                OutlineContract.createResult(bookmark.locator)
        )
    }
}