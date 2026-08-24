package com.playnestunionad

import android.widget.FrameLayout
import com.facebook.react.uimanager.ThemedReactContext

/**
 * 视图广告容器基类。
 *
 * Fabric（新架构）下，React 的布局来自 JS 的 shadow tree，且是一次性下发的。
 * 广告 SDK 渲染成功后通过 addView() 动态塞进来的原生子 View 不在 shadow tree 里，
 * 不会再被 measure/layout，尺寸会停留在 0×0 → 广告虽然渲染成功却不可见。
 *
 * 解决办法：重写 requestLayout()，在下一帧用容器自身（由 React 赋予的）边界，
 * 主动对容器及其子 View 走一遍 measure + layout。
 * 这是 RN 原生视图库（webview / video / mobile-ads 等）通用的做法。
 */
abstract class PlaynestAdFrameLayout(context: ThemedReactContext) : FrameLayout(context) {

    private val measureAndLayout = Runnable {
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        layout(left, top, right, bottom)
    }

    override fun requestLayout() {
        super.requestLayout()
        // React 管理的视图 requestLayout 不会向上冒泡触发重新布局，
        // 手动 post 一次，确保运行时 addView 进来的广告子视图被摆放。
        post(measureAndLayout)
    }
}
