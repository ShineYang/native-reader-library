package com.shawnyang.jpreader_lib.ui.base

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.shawnyang.jpreader_lib.exts.windowHeight
import com.shawnyang.jpreader_lib.ui.widget.FixedHeightBottomSheetDialog

/**
 * @author ShineYang
 * @date 2021/9/5
 * description:
 */
abstract class BaseBottomSheetFragment : BottomSheetDialogFragment() {
    private var isFirstVisible: Boolean = true

    /**
     * @return 布局 layout id
     */
    protected abstract fun setLayoutId(): Int

    /**
     * 页面 view / 数据的初始化
     */
    protected abstract fun setUp()

    /**
     * 通过执行耗时任务获取数据
     */
    protected abstract fun fetchData()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return setLayout(inflater, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isFirstVisible) {
            setUp()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return FixedHeightBottomSheetDialog(requireContext(), theme, windowHeight)
    }

    /**
     * 初始化页面布局
     * 给 statusLayout 设置各种页面状态 view
     */
    private fun setLayout(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View {
        return inflater.inflate(setLayoutId(), container, false)
    }

    protected fun getStr(resId: Int): String {
        return resources.getString(resId)
    }

    /**
     * 每次当切换页面时 会调用onResume
     * 可以在此处获取 本地 数据
     */
    override fun onResume() {
        super.onResume()
        if (isFirstVisible) {
            isFirstVisible = false
            fetchData()
        }
    }
}