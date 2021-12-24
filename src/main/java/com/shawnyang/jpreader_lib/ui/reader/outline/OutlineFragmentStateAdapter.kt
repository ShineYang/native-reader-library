package com.shawnyang.jpreader_lib.ui.reader.outline

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.shawnyang.jpreader_lib.R
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.landmarks
import org.readium.r2.shared.publication.epub.pageList
import org.readium.r2.shared.publication.opds.images

/**
 * @author ShineYang
 * @date 2021/9/6
 * description:
 */
class OutlineFragmentStateAdapter(val fragment: Fragment, val publication: Publication, private val outlines: List<String>)
    : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return outlines.size
    }

    override fun createFragment(position: Int): Fragment {
        return when (this.outlines[position]) {
            fragment.getString(R.string.tab_navigation) -> createContentsFragment()//目录
            fragment.getString(R.string.tab_book_mark) -> BookmarksFragment()//书签
            else -> createContentsFragment()
        }
    }

    private fun createContentsFragment() =
        NavigationFragment.newInstance(when {
            publication.tableOfContents.isNotEmpty() -> publication.tableOfContents
            publication.readingOrder.isNotEmpty() -> publication.readingOrder
            publication.images.isNotEmpty() -> publication.images
            else -> mutableListOf()
        })

    private fun createPageListFragment() =
        NavigationFragment.newInstance(publication.pageList)

    private fun createLandmarksFragment() =
        NavigationFragment.newInstance(publication.landmarks)
}