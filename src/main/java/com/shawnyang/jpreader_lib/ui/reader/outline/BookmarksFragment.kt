package com.shawnyang.jpreader_lib.ui.reader.outline

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shawnyang.jpreader_lib.R
import com.shawnyang.jpreader_lib.data.room.model.Bookmark
import com.shawnyang.jpreader_lib.ui.reader.react.ReaderViewModel
import org.readium.r2.shared.publication.Publication

/**
 * @author ShineYang
 * @date 2021/9/6
 * description:
 */

class BookmarksFragment : Fragment(R.layout.layout_bookmark_list) {
    lateinit var rootView: View
    lateinit var viewModel: ReaderViewModel
    lateinit var publication: Publication
    private lateinit var rvAdapter: BookMarkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            viewModel = it
            publication = it.publication
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view
        val comparator: Comparator<Bookmark> = compareBy({ it.resourceIndex }, { it.locator.locations.progression })
        viewModel.getBookmarks().observe(viewLifecycleOwner, {
            val bookmarks = it.sortedWith(comparator).toMutableList()
            setUpRV(publication, bookmarks)
        })

    }

    private fun setUpRV(publication: Publication, items: MutableList<Bookmark>) {
        rvAdapter = BookMarkAdapter(publication, R.layout.item_recycle_bookmark)
        rvAdapter.setOnBookmarkDeleteRequested(object : BookMarkAdapter.OnBookmarkDeleteRequested {
            @SuppressLint("NotifyDataSetChanged")
            override fun onBookmarkDeleteRequested(id: Long) {
                viewModel.deleteBookmark(id)
                rvAdapter.notifyDataSetChanged()
            }
        })
        rvAdapter.setOnItemClickListener { _, _, position ->
            onBookmarkSelected(items[position])
        }
        val rv_book_mark = rootView.findViewById<RecyclerView>(R.id.rv_book_mark)
        rv_book_mark?.layoutManager = LinearLayoutManager(activity)
        rv_book_mark?.adapter = rvAdapter
        rvAdapter.data = items
    }

    private fun onBookmarkSelected(bookmark: Bookmark) {
        setFragmentResult(
                OutlineContract.REQUEST_KEY,
                OutlineContract.createResult(bookmark.locator)
        )
    }
}