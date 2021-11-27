package com.shawnyang.jpreader_lib.ui.reader.outline

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.shawnyang.jpreader_lib.R
import com.shawnyang.jpreader_lib.data.db.BookData
import com.shawnyang.jpreader_lib.ui.reader.react.ReaderViewModel
import kotlinx.android.synthetic.main.layout_sheet_content_listview.*
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.toLocator

/**
 * @author ShineYang
 * @date 2021/9/6
 * description:
 */
class NavigationFragment : Fragment(R.layout.layout_sheet_content_listview) {

    private lateinit var publication: Publication
    private lateinit var persistence: BookData
    private lateinit var links: List<Link>
    private var rvAdapter: BookNavigationAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            publication = it.publication
            persistence = it.persistence
        }

        links = requireNotNull(requireArguments().getParcelableArrayList(LINKS_ARG))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val flatLinks = mutableListOf<Pair<Int, Link>>()

        for (link in links) {
            val children = childrenOf(Pair(0, link))
            // Append parent.
            flatLinks.add(Pair(0, link))
            // Append children, and their children... recursive.
            flatLinks.addAll(children)
        }
        setUpRV(flatLinks.toMutableList(), flatLinks)

    }

    private fun setUpRV(items: MutableList<Any>, link: MutableList<Pair<Int, Link>>){
        if(rvAdapter == null){
            rvAdapter = BookNavigationAdapter(R.layout.item_recycle_navigation)
            rvAdapter?.setOnItemClickListener { _, _, position ->
                onLinkSelected(link[position].second)
            }
            rv_book_navi.layoutManager = LinearLayoutManager(activity)
            rv_book_navi.adapter = rvAdapter
        }
        rvAdapter?.data = items
    }

    private fun onLinkSelected(link: Link) {
        val locator = link.toLocator().let {
            // progression is mandatory in some contexts
            if (it.locations.fragments.isEmpty())
                it.copyWithLocations(progression = 0.0)
            else
                it
        }

        setFragmentResult(
            OutlineContract.REQUEST_KEY,
            OutlineContract.createResult(locator)
        )
    }

    companion object {

        private const val LINKS_ARG = "links"

        fun newInstance(links: List<Link>) =
            NavigationFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(LINKS_ARG, if (links is ArrayList<Link>) links else ArrayList(links))
                }
            }
    }
}

fun childrenOf(parent: Pair<Int, Link>): MutableList<Pair<Int, Link>> {
    val indentation = parent.first + 1
    val children = mutableListOf<Pair<Int, Link>>()
    for (link in parent.second.children) {
        children.add(Pair(indentation,link))
        children.addAll(childrenOf(Pair(indentation,link)))
    }
    return children
}
