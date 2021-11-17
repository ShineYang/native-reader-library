package com.shawnyang.jpreader_lib.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * @author ShineYang
 * @date 2021/9/6
 * description:
 */
abstract class BaseLazyFragment : Fragment() {

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

    /**
     * 初始化页面布局
     */
    private fun setLayout(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View {
        return inflater.inflate(setLayoutId(), container, false)
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

    override fun onDestroy() {
        super.onDestroy()
    }
}