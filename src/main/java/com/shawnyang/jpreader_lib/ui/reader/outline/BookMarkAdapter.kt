package com.shawnyang.jpreader_lib.ui.reader.outline

import android.widget.ImageView
import android.widget.PopupMenu
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.shawnyang.jpreader_lib.R
import com.shawnyang.jpreader_lib.data.Bookmark
import kotlinx.coroutines.NonDisposableHandle.parent
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.readium.r2.shared.publication.Publication
import outlineTitle
import kotlin.math.roundToInt

/**
 * @author ShineYang
 * @date 2021/11/27
 * description:
 */
class BookMarkAdapter(val publication: Publication,
                      layoutId: Int): BaseQuickAdapter<Bookmark, BaseViewHolder>(layoutId) {
    private var listener: OnBookmarkDeleteRequested? = null

    fun setOnBookmarkDeleteRequested(listener: OnBookmarkDeleteRequested){
        this.listener = listener
    }

    override fun convert(holder: BaseViewHolder, item: Bookmark) {
        holder.setText(R.id.bookmark_chapter, getBookSpineItem(item.resourceHref)
                ?:  "*未知标题*")

        item.location.progression?.let { progression ->
            val formattedProgression = "${(progression * 100).roundToInt()}%"
            holder.setText(R.id.bookmark_progression, formattedProgression)
        }

        val formattedDate = DateTime(item.creationDate).toString(DateTimeFormat.shortDateTime())
        holder.setText(R.id.bookmark_timestamp, formattedDate)
        val overflow = holder.getViewOrNull<ImageView>(R.id.overflow)
        overflow?.setOnClickListener {
            val popupMenu = PopupMenu(context, overflow)
            popupMenu.menuInflater.inflate(R.menu.menu_bookmark, popupMenu.menu)
            popupMenu.show()
            popupMenu.setOnMenuItemClickListener { itm ->
                if (itm.itemId == R.id.delete) {
                    listener?.onBookmarkDeleteRequested(item.id!!)
                    data.remove(item)
                }
                notifyItemRemoved(getItemPosition(item))
                false
            }
        }
    }

    private fun getBookSpineItem(href: String): String? {
        for (link in publication.tableOfContents) {
            if (link.href == href) {
                return link.outlineTitle
            }
        }
        for (link in publication.readingOrder) {
            if (link.href == href) {
                return link.outlineTitle
            }
        }
        return null
    }

    interface OnBookmarkDeleteRequested{
        fun onBookmarkDeleteRequested(id: Long)
    }
}