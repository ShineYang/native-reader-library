package com.shawnyang.jpreader_lib.ui.widget

import android.graphics.drawable.PaintDrawable

/**
 * @author ShineYang
 * @date 2021/9/7
 * description:
 */
class RoundedRectDrawable(color: Int, radius: Float) : PaintDrawable(color) {
    init {
        setCornerRadius(radius)
    }
}