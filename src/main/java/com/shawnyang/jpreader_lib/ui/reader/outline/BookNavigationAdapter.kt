package com.shawnyang.jpreader_lib.ui.reader.outline

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.shawnyang.jpreader_lib.R
import org.readium.r2.shared.publication.Link
import outlineTitle

/**
 * @author ShineYang
 * @date 2021/11/27
 * description:
 */
class BookNavigationAdapter(layoutId: Int): BaseQuickAdapter<Any, BaseViewHolder>(layoutId) {
    override fun convert(holder: BaseViewHolder, item: Any) {
        if (item is Pair<*, *>) {
            item as Pair<Int, Link>
            holder.setText(R.id.navigation_textView, item.second.outlineTitle)
        } else {
            item as Link
            holder.setText(R.id.navigation_textView, item.outlineTitle)
        }
    }
}