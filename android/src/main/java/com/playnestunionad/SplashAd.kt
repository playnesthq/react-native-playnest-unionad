package com.playnestunionad

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.CSJAdError
import com.bytedance.sdk.openadsdk.CSJSplashAd
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap

/**
 * 开屏广告（方法式全屏）。渲染成功后将 splashView 全屏加到 Activity 根视图，
 * 关闭时移除。adType = splashAd，事件通过 [emit] 发送。
 */
@SuppressLint("StaticFieldLeak")
object SplashAd {
    private const val TAG = "PlaynestUnionad"
    private const val AD_TYPE = "splashAd"

    private var splashAd: CSJSplashAd? = null
    private var container: FrameLayout? = null
    private var rootView: ViewGroup? = null
    private var emit: ((WritableMap) -> Unit)? = null

    private fun event(onAdMethod: String, build: (WritableMap.() -> Unit)? = null) {
        val map = Arguments.createMap()
        map.putString("adType", AD_TYPE)
        map.putString("onAdMethod", onAdMethod)
        build?.invoke(map)
        emit?.invoke(map)
    }

    fun load(activity: Activity, params: ReadableMap, emit: (WritableMap) -> Unit) {
        this.emit = emit
        val codeId = if (params.hasKey("androidCodeId")) params.getString("androidCodeId") else null
        val supportDeepLink = if (params.hasKey("supportDeepLink")) params.getBoolean("supportDeepLink") else true
        val isShake = if (params.hasKey("isShake")) params.getBoolean("isShake") else false
        val timeout = if (params.hasKey("timeout")) params.getInt("timeout") else 3000

        val dm = activity.resources.displayMetrics
        var width = if (params.hasKey("width")) params.getDouble("width") else 0.0
        var height = if (params.hasKey("height")) params.getDouble("height") else 0.0
        val density = dm.density
        val widthPx = if (width == 0.0) dm.widthPixels else (width * density).toInt()
        val heightPx = if (height == 0.0) dm.heightPixels else (height * density).toInt()

        val adSlot = AdSlot.Builder()
            .setCodeId(codeId)
            .setSupportDeepLink(supportDeepLink)
            .setImageAcceptedSize(widthPx, heightPx)
            .setMediationAdSlot(
                MediationAdSlot.Builder()
                    .setSplashShakeButton(isShake)
                    .build()
            )
            .build()
        val mTTAdNative = TTAdSdk.getAdManager().createAdNative(activity)
        mTTAdNative.loadSplashAd(adSlot, object : TTAdNative.CSJSplashAdListener {
            override fun onSplashLoadSuccess(ad: CSJSplashAd?) {}

            override fun onSplashLoadFail(err: CSJAdError?) {
                event("onFail") { putString("error", err?.msg ?: "开屏加载失败") }
            }

            override fun onSplashRenderSuccess(ad: CSJSplashAd?) {
                if (ad == null) {
                    event("onFail") { putString("error", "拉取广告失败") }
                    return
                }
                splashAd = ad
                showSplashAd(activity, ad)
            }

            override fun onSplashRenderFail(ad: CSJSplashAd?, err: CSJAdError?) {
                event("onFail") { putString("error", err?.msg ?: "开屏渲染失败") }
            }
        }, timeout)
    }

    private fun showSplashAd(activity: Activity, ad: CSJSplashAd) {
        ad.setSplashAdListener(object : CSJSplashAd.SplashAdListener {
            override fun onSplashAdShow(a: CSJSplashAd?) {
                event("onShow")
                event("onEcpm") {
                    putString("ecpm", EcpmUtil.toJson(splashAd?.mediationManager?.showEcpm))
                }
            }

            override fun onSplashAdClick(a: CSJSplashAd?) {
                event("onClick")
            }

            override fun onSplashAdClose(a: CSJSplashAd?, closeType: Int) {
                // closeType 1 跳过, 2 倒计时结束
                if (closeType == 1) event("onSkip") else event("onFinish")
                removeSplash()
            }
        })
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        val fl = FrameLayout(activity)
        root.addView(fl, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        fl.addView(ad.splashView)
        this.rootView = root
        this.container = fl
    }

    private fun removeSplash() {
        container?.let { rootView?.removeView(it) }
        container = null
        rootView = null
        splashAd?.mediationManager?.destroy()
        splashAd = null
    }
}
