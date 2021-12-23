package com.shawnyang.jpreader_lib.ui.reader.outline

import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.loper7.tab_expand.ext.buildIndicator
import com.loper7.tab_expand.ext.buildText
import com.loper7.tab_expand.ext.toPx
import com.loper7.tab_expand.indicator.BaseIndicator
import com.loper7.tab_expand.indicator.LinearIndicator
import com.loper7.tab_expand.text.BaseText
import com.shawnyang.jpreader_lib.R
import com.shawnyang.jpreader_lib.data.db.BookData
import com.shawnyang.jpreader_lib.ui.base.BaseBottomSheetFragment
import com.shawnyang.jpreader_lib.ui.reader.react.ReaderViewModel
import kotlinx.android.synthetic.main.layout_reader_outline_sheet.*
import org.readium.r2.shared.publication.Publication

/**
 * @author ShineYang
 * @date 2021/9/2
 * description: Fragment to show navigation links (Table of Contents, Page lists & Landmarks)
 */
class ReaderOutlineSheet : BaseBottomSheetFragment() {
    override fun setLayoutId() = R.layout.layout_reader_outline_sheet

    private lateinit var publication: Publication
    private lateinit var persistence: BookData

    override
    fun setUp() {
        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            publication = it.publication
            persistence = it.persistence
        }

        childFragmentManager.setFragmentResultListener(
            OutlineContract.REQUEST_KEY,
            this,
            FragmentResultListener { requestKey, bundle ->
                run {
                    setFragmentResult(requestKey, bundle)
                    dismissAllowingStateLoss()
                }
            }
        )

        val recyclerView = outline_pager.getRecyclerView()
        recyclerView?.isNestedScrollingEnabled = false
        recyclerView?.overScrollMode = View.OVER_SCROLL_NEVER // Optional
        outline_pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Because the fragment might or might not be created yet,
                // we need to check for the size of the fragmentManager
                // before accessing it.
                if (childFragmentManager.fragments.size > position) {
                    val fragment = childFragmentManager.fragments.get(position)
                    fragment.view?.let {
                        // Now we've got access to the fragment Root View
                        // we will use it to calculate the height and
                        // apply it to the ViewPager2
                        updatePagerHeightForChild(it, outline_pager)
                    }
                }
            }
        })

        initData()
    }

    private fun initData() {
        val outlines: List<Outline> = listOf(Outline.Contents, Outline.Bookmarks)

        outline_pager.adapter = OutlineFragmentStateAdapter(this, publication, outlines)
        outline_tab_layout.buildText<BaseText>()
            .setSelectTextBold(true)
            .setNormalTextBold(false)
            .setNormalTextSize(15f)
            .setSelectTextSize(15f)
            .bind()
        outline_tab_layout.buildIndicator<LinearIndicator>()
            .bind()
        TabLayoutMediator(outline_tab_layout, outline_pager) { tab, idx -> tab.text = outlines[idx].label }.attach()
        tv_book_title.text = publication.metadata.title
    }

    override fun fetchData() {
    }

    fun ViewPager2.getRecyclerView(): RecyclerView? {
        try {
            val field = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            field.isAccessible = true
            return field.get(this) as RecyclerView
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return null
    }

    fun updatePagerHeightForChild(view: View, pager: ViewPager2) {
        view.post {
            val height: Int = requireActivity().resources.displayMetrics.heightPixels
            val maxHeight = (height * 0.90).toInt()
            pager.layoutParams = (pager.layoutParams)
                    .also { lp ->
                        // applying Fragment Root View Height to
                        // the pager LayoutParams, so they match
                        lp.height = maxHeight
                    }
        }
    }
}