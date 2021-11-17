package com.shawnyang.jpreader_lib.ui.widget

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * @author ShineYang
 * @date 2021/9/6
 * description:
 */
class FixedHeightBottomSheetDialog(
    context: Context,
    theme: Int,
    private val fixedHeight: Int
) : BottomSheetDialog(context, theme) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPeekHeight(fixedHeight)
    }

    private fun setPeekHeight(peekHeight: Int) {
        val bottomSheetBehavior = getBottomSheetBehavior()
        bottomSheetBehavior?.peekHeight = peekHeight
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        window?.setGravity(Gravity.BOTTOM)
    }

    private fun getBottomSheetBehavior(): BottomSheetBehavior<View>? {
        val view: View? = window?.findViewById(com.google.android.material.R.id.design_bottom_sheet)
        return view?.let { BottomSheetBehavior.from(view) }
    }
}