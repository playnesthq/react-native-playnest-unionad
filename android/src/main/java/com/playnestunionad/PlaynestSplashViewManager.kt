package com.playnestunionad

import android.view.ViewGroup
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.CSJAdError
import com.bytedance.sdk.openadsdk.CSJSplashAd
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot
import com.facebook.react.bridge.Arguments
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.viewmanagers.PlaynestSplashViewManagerInterface
import com.facebook.react.viewmanagers.PlaynestSplashViewManagerDelegate

class PlaynestSplashAdView(context: ThemedReactContext) : PlaynestAdFrameLayout(context) {
    var androidCodeId: String? = null
    var expressWidth: Double = 0.0
    var expressHeight: Double = 0.0
    var timeout: Int = 3000
    var isShake: Boolean = false
    var supportDeepLink: Boolean = true
    private var splashAd: CSJSplashAd? = null
    private var loaded = false

    // 全屏挂载（不传尺寸时）用到的引用，关闭时需从 Activity 根视图移除。
    private var fullscreenRoot: ViewGroup? = null
    private var fullscreenContainer: FrameLayout? = null

    /** 是否显式传入了尺寸；显式 = 内嵌到组件容器按该区域展示，未显式 = 全屏。 */
    private val hasExplicitSize: Boolean
        get() = expressWidth > 0 && expressHeight > 0

    fun maybeLoad() {
        val codeId = androidCodeId
        if (codeId.isNullOrEmpty() || loaded) return
        loaded = true
        load(codeId)
    }

    private fun load(codeId: String) {
        val activity = (context as ThemedReactContext).currentActivity ?: return
        val dm = resources.displayMetrics
        // 请求素材尺寸：显式尺寸 > 容器已测量尺寸 > 屏幕尺寸（与 iOS 的兜底一致）。
        val wPx: Int
        val hPx: Int
        if (hasExplicitSize) {
            wPx = dp2px(context, expressWidth.toFloat())
            hPx = dp2px(context, expressHeight.toFloat())
        } else {
            wPx = if (width > 0) width else dm.widthPixels
            hPx = if (height > 0) height else dm.heightPixels
        }
        val adSlot = AdSlot.Builder()
            .setCodeId(codeId)
            .setSupportDeepLink(supportDeepLink)
            .setImageAcceptedSize(wPx, hPx)
            .setMediationAdSlot(MediationAdSlot.Builder().setSplashShakeButton(isShake).build())
            .build()
        val native = TTAdSdk.getAdManager().createAdNative(activity)
        native.loadSplashAd(adSlot, object : TTAdNative.CSJSplashAdListener {
            override fun onSplashLoadSuccess(ad: CSJSplashAd?) {}

            override fun onSplashLoadFail(err: CSJAdError?) {
                dispatchAdEvent("onAdFail", Arguments.createMap().apply { putString("error", err?.msg ?: "开屏加载失败") })
            }

            override fun onSplashRenderSuccess(ad: CSJSplashAd?) {
                if (ad == null) {
                    dispatchAdEvent("onAdFail", Arguments.createMap().apply { putString("error", "拉取广告失败") })
                    return
                }
                splashAd = ad
                showSplash(ad)
            }

            override fun onSplashRenderFail(ad: CSJSplashAd?, err: CSJAdError?) {
                dispatchAdEvent("onAdFail", Arguments.createMap().apply { putString("error", err?.msg ?: "开屏渲染失败") })
            }
        }, timeout)
    }

    private fun showSplash(ad: CSJSplashAd) {
        val dm = resources.displayMetrics
        ad.setSplashAdListener(object : CSJSplashAd.SplashAdListener {
            override fun onSplashAdShow(a: CSJSplashAd?) {
                dispatchAdEvent("onAdShow", Arguments.createMap().apply {
                    // 全屏时上报屏幕尺寸，内嵌时上报容器尺寸。
                    putDouble("width", (if (hasExplicitSize) width else dm.widthPixels).toDouble())
                    putDouble("height", (if (hasExplicitSize) height else dm.heightPixels).toDouble())
                })
                dispatchAdEvent("onAdEcpm", Arguments.createMap().apply {
                    putString("ecpm", EcpmUtil.toJson(splashAd?.mediationManager?.showEcpm))
                })
            }

            override fun onSplashAdClick(a: CSJSplashAd?) {
                dispatchAdEvent("onAdClick")
            }

            override fun onSplashAdClose(a: CSJSplashAd?, closeType: Int) {
                if (closeType == 1) dispatchAdEvent("onAdSkip") else dispatchAdEvent("onAdFinish")
                detachSplashView()
            }
        })
        if (hasExplicitSize) {
            // 内嵌：按指定区域展示在组件容器里（PlaynestAdFrameLayout 负责摆放子视图）。
            removeAllViews()
            addView(ad.splashView)
        } else {
            // 全屏：挂到 Activity 根视图，铺满整屏（与方法式开屏、iOS 行为对齐）。
            val activity = (context as ThemedReactContext).currentActivity ?: return
            val root = activity.findViewById<ViewGroup>(android.R.id.content)
            val fl = FrameLayout(activity)
            root.addView(fl, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            fl.addView(ad.splashView)
            fullscreenRoot = root
            fullscreenContainer = fl
        }
    }

    private fun detachSplashView() {
        removeAllViews()
        fullscreenContainer?.let { fullscreenRoot?.removeView(it) }
        fullscreenContainer = null
        fullscreenRoot = null
    }

    fun destroyAd() {
        detachSplashView()
        splashAd?.mediationManager?.destroy()
        splashAd = null
    }
}

@ReactModule(name = PlaynestSplashViewManager.NAME)
class PlaynestSplashViewManager : SimpleViewManager<PlaynestSplashAdView>(),
    PlaynestSplashViewManagerInterface<PlaynestSplashAdView> {

    private val mDelegate = PlaynestSplashViewManagerDelegate(this)

    override fun getDelegate() = mDelegate
    override fun getName() = NAME
    override fun createViewInstance(context: ThemedReactContext) = PlaynestSplashAdView(context)

    override fun setAndroidCodeId(view: PlaynestSplashAdView, value: String?) { view.androidCodeId = value }
    override fun setIosCodeId(view: PlaynestSplashAdView, value: String?) {}
    override fun setExpressWidth(view: PlaynestSplashAdView, value: Double) { view.expressWidth = value }
    override fun setExpressHeight(view: PlaynestSplashAdView, value: Double) { view.expressHeight = value }
    override fun setTimeout(view: PlaynestSplashAdView, value: Int) { view.timeout = value }
    override fun setIsShake(view: PlaynestSplashAdView, value: Boolean) { view.isShake = value }
    override fun setSupportDeepLink(view: PlaynestSplashAdView, value: Boolean) { view.supportDeepLink = value }

    override fun onAfterUpdateTransaction(view: PlaynestSplashAdView) {
        super.onAfterUpdateTransaction(view)
        view.maybeLoad()
    }

    override fun onDropViewInstance(view: PlaynestSplashAdView) {
        view.destroyAd()
        super.onDropViewInstance(view)
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> =
        viewAdEventConstants("onAdShow", "onAdClick", "onAdSkip", "onAdFinish", "onAdFail", "onAdEcpm")

    companion object {
        const val NAME = "PlaynestSplashView"
    }
}
