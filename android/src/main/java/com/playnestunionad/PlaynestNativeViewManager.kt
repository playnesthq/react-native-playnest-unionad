package com.playnestunionad

import android.view.View
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdDislike
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTFeedAd
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot
import com.bytedance.sdk.openadsdk.mediation.ad.MediationExpressRenderListener
import com.facebook.react.bridge.Arguments
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.viewmanagers.PlaynestNativeViewManagerInterface
import com.facebook.react.viewmanagers.PlaynestNativeViewManagerDelegate

class PlaynestNativeAdView(context: ThemedReactContext) : PlaynestAdFrameLayout(context) {
    var androidCodeId: String? = null
    var expressWidth: Double = 0.0
    var expressHeight: Double = 0.0
    var isMuted: Boolean = true
    private var feedAd: TTFeedAd? = null
    private var loaded = false

    fun maybeLoad() {
        val codeId = androidCodeId
        if (codeId.isNullOrEmpty() || loaded) return
        loaded = true
        load(codeId)
    }

    private fun load(codeId: String) {
        val activity = (context as ThemedReactContext).currentActivity ?: return
        var w = expressWidth.toFloat()
        var h = expressHeight.toFloat()
        if (w <= 0f) w = (width / resources.displayMetrics.density)
        if (h <= 0f) h = 0f
        val adSlot = AdSlot.Builder()
            .setCodeId(codeId)
            .setAdCount(1)
            .setImageAcceptedSize(dp2px(context, w), if (h > 0f) dp2px(context, h) else 0)
            .setExpressViewAcceptedSize(w, h)
            .setMediationAdSlot(MediationAdSlot.Builder().setMuted(isMuted).build())
            .build()
        val native = TTAdSdk.getAdManager().createAdNative(activity)
        native.loadFeedAd(adSlot, object : TTAdNative.FeedAdListener {
            override fun onError(code: Int, message: String?) {
                dispatchAdEvent("onAdFail", Arguments.createMap().apply { putString("error", "$code $message") })
            }

            override fun onFeedAdLoad(ads: MutableList<TTFeedAd>?) {
                if (ads.isNullOrEmpty()) {
                    dispatchAdEvent("onAdFail", Arguments.createMap().apply { putString("error", "ads is empty") })
                    return
                }
                feedAd = ads[0]
                show()
            }
        })
    }

    private fun show() {
        val ad = feedAd ?: return
        val manager = ad.mediationManager
        if (manager == null || !manager.isExpress) {
            dispatchAdEvent("onAdFail", Arguments.createMap().apply { putString("error", "自渲染信息流广告暂不支持") })
            return
        }
        bindDislike(ad)
        ad.setExpressRenderListener(object : MediationExpressRenderListener {
            override fun onRenderSuccess(v: View?, w: Float, h: Float, boolean: Boolean) {
                removeAllViews()
                addView(ad.adView)
            }

            override fun onRenderFail(v: View?, msg: String?, code: Int) {
                dispatchAdEvent("onAdFail", Arguments.createMap().apply { putString("error", "$code $msg") })
            }

            override fun onAdClick() {
                dispatchAdEvent("onAdClick")
            }

            override fun onAdShow() {
                dispatchAdEvent("onAdShow", Arguments.createMap().apply {
                    putDouble("width", (ad.adView?.width ?: 0).toDouble())
                    putDouble("height", (ad.adView?.height ?: 0).toDouble())
                })
                dispatchAdEvent("onAdEcpm", Arguments.createMap().apply {
                    putString("ecpm", EcpmUtil.toJson(ad.mediationManager?.showEcpm))
                })
            }
        })
        ad.render()
    }

    private fun bindDislike(ad: TTFeedAd) {
        ad.setDislikeCallback((context as ThemedReactContext).currentActivity, object : TTAdDislike.DislikeInteractionCallback {
            override fun onShow() {}
            override fun onSelected(pos: Int, reason: String?, enforce: Boolean) {
                removeAllViews()
                dispatchAdEvent("onAdDislike", Arguments.createMap().apply { putString("reason", reason ?: "") })
            }
            override fun onCancel() {}
        })
    }

    fun destroyAd() {
        feedAd?.destroy()
        feedAd = null
    }
}

@ReactModule(name = PlaynestNativeViewManager.NAME)
class PlaynestNativeViewManager : SimpleViewManager<PlaynestNativeAdView>(),
    PlaynestNativeViewManagerInterface<PlaynestNativeAdView> {

    private val mDelegate = PlaynestNativeViewManagerDelegate(this)

    override fun getDelegate() = mDelegate
    override fun getName() = NAME
    override fun createViewInstance(context: ThemedReactContext) = PlaynestNativeAdView(context)

    override fun setAndroidCodeId(view: PlaynestNativeAdView, value: String?) { view.androidCodeId = value }
    override fun setIosCodeId(view: PlaynestNativeAdView, value: String?) {}
    override fun setExpressWidth(view: PlaynestNativeAdView, value: Double) { view.expressWidth = value }
    override fun setExpressHeight(view: PlaynestNativeAdView, value: Double) { view.expressHeight = value }
    override fun setIsMuted(view: PlaynestNativeAdView, value: Boolean) { view.isMuted = value }

    override fun onAfterUpdateTransaction(view: PlaynestNativeAdView) {
        super.onAfterUpdateTransaction(view)
        view.maybeLoad()
    }

    override fun onDropViewInstance(view: PlaynestNativeAdView) {
        view.destroyAd()
        super.onDropViewInstance(view)
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> =
        viewAdEventConstants("onAdShow", "onAdClick", "onAdFail", "onAdEcpm", "onAdDislike")

    companion object {
        const val NAME = "PlaynestNativeView"
    }
}
