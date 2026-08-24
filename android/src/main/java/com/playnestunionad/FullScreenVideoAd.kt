package com.playnestunionad

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap

/**
 * 新模板渲染插屏（全屏/插屏二合一）。事件语义对齐 flutter_unionad，
 * adType = fullScreenVideoAdInteraction，通过 [emit] 发送。
 */
@SuppressLint("StaticFieldLeak")
object FullScreenVideoAd {
    private const val TAG = "PlaynestUnionad"
    private const val AD_TYPE = "fullScreenVideoAdInteraction"

    private var mttFullVideoAd: TTFullScreenVideoAd? = null
    private var mCodeId: String? = null
    private var orientation: Int = 1
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
        this.mCodeId = if (params.hasKey("androidCodeId")) params.getString("androidCodeId") else null
        this.orientation = if (params.hasKey("orientation")) params.getInt("orientation") else 1
        loadFullScreenVideoAd(activity)
    }

    private fun loadFullScreenVideoAd(activity: Activity) {
        val adSlot = AdSlot.Builder()
            .setCodeId(mCodeId)
            .setOrientation(orientation)
            .setMediationAdSlot(
                MediationAdSlot.Builder()
                    .setMuted(true)
                    .setVolume(0.5f)
                    .setBidNotify(true)
                    .build()
            )
            .build()
        val mTTAdNative = TTAdSdk.getAdManager().createAdNative(activity)
        mTTAdNative.loadFullScreenVideoAd(adSlot, object : TTAdNative.FullScreenVideoAdListener {
            override fun onError(code: Int, message: String) {
                Log.e(TAG, "全屏/插屏加载失败 $code $message")
                event("onFail") { putString("error", "$code , $message") }
            }

            override fun onFullScreenVideoAdLoad(ad: TTFullScreenVideoAd) {
                mttFullVideoAd = ad
                event("onReady")
            }

            override fun onFullScreenVideoCached() {}

            override fun onFullScreenVideoCached(ad: TTFullScreenVideoAd?) {}
        })
    }

    fun show(activity: Activity) {
        val ad = mttFullVideoAd
        if (ad == null) {
            event("onUnReady") { putString("error", "广告预加载未完成") }
            return
        }
        ad.setFullScreenVideoAdInteractionListener(object :
            TTFullScreenVideoAd.FullScreenVideoAdInteractionListener {
            override fun onAdShow() {
                event("onShow")
                event("onEcpm") {
                    putString("ecpm", EcpmUtil.toJson(mttFullVideoAd?.mediationManager?.showEcpm))
                }
            }

            override fun onAdVideoBarClick() {
                event("onClick")
            }

            override fun onAdClose() {
                event("onClose")
            }

            override fun onVideoComplete() {
                event("onFinish")
            }

            override fun onSkippedVideo() {
                event("onSkip")
            }
        })
        ad.showFullScreenVideoAd(activity)
        mttFullVideoAd = null
    }
}
