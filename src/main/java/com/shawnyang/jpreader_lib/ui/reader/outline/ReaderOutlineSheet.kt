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

    override fun setUp() {
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
            .setAngle(6.toPx())
            .setHeight(BaseIndicator.MATCH)
            .setColor(ContextCompat.getColor(requireContext(), R.color.white))
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
}