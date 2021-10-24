package com.shawnyang.jpreader_lib.ui.reader.outline

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.landmarks
import org.readium.r2.shared.publication.epub.pageList
import org.readium.r2.shared.publication.opds.images

/**
 * @author ShineYang
 * @date 2021/9/6
 * description:
 */
class OutlineFragmentStateAdapter(fragment: Fragment, val publication: Publication, private val outlines: List<Outline>)
    : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return outlines.size
    }

    override fun createFragment(position: Int): Fragment {
        return when (this.outlines[position]) {
            Outline.Contents -> createContentsFragment()//目录
            Outline.Bookmarks -> BookmarksFragment()//书签
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

enum class Outline(val label: String) {
    Contents("目录"),
    Bookmarks("书签"),
}