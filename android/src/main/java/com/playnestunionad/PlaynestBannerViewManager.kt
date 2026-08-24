package com.playnestunionad

import android.view.View
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTNativeExpressAd
import com.bytedance.sdk.openadsdk.TTAdDislike
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot
import com.facebook.react.bridge.Arguments
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.viewmanagers.PlaynestBannerViewManagerInterface
import com.facebook.react.viewmanagers.PlaynestBannerViewManagerDelegate

class PlaynestBannerAdView(context: ThemedReactContext) : PlaynestAdFrameLayout(context) {
    var androidCodeId: String? = null
    var expressWidth: Double = 0.0
    var expressHeight: Double = 0.0
    private var bannerAd: TTNativeExpressAd? = null
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
        if (h <= 0f) h = 60f
        val adSlot = AdSlot.Builder()
            .setCodeId(codeId)
            .setImageAcceptedSize(dp2px(context, w), dp2px(context, h))
            .setExpressViewAcceptedSize(w, h)
            .setMediationAdSlot(MediationAdSlot.Builder().build())
            .build()
        val native = TTAdSdk.getAdManager().createAdNative(activity)
        native.loadBannerExpressAd(adSlot, object : TTAdNative.NativeExpressAdListener {
            override fun onError(code: Int, message: String?) {
                dispatchAdEvent("onAdFail", Arguments.createMap().apply { putString("error", "$code $message") })
            }

            override fun onNativeExpressAdLoad(ads: MutableList<TTNativeExpressAd>?) {
                if (ads.isNullOrEmpty()) {
                    dispatchAdEvent("onAdFail", Arguments.createMap().apply { putString("error", "ads is empty") })
                    return
                }
                bannerAd = ads[0]
                show()
            }
        })
    }

    private fun show() {
        val ad = bannerAd ?: return
        ad.setExpressInteractionListener(object : TTNativeExpressAd.ExpressAdInteractionListener {
            override fun onAdClicked(v: View?, type: Int) {
                dispatchAdEvent("onAdClick")
            }

            override fun onAdShow(v: View?, type: Int) {
                dispatchAdEvent("onAdShow", Arguments.createMap().apply {
                    putDouble("width", (v?.width ?: 0).toDouble())
                    putDouble("height", (v?.height ?: 0).toDouble())
                })
                dispatchAdEvent("onAdEcpm", Arguments.createMap().apply {
                    putString("ecpm", EcpmUtil.toJson(ad.mediationManager?.showEcpm))
                })
            }

            override fun onRenderFail(v: View?, msg: String?, code: Int) {
                dispatchAdEvent("onAdFail", Arguments.createMap().apply { putString("error", "$code $msg") })
            }

            override fun onRenderSuccess(v: View?, w: Float, h: Float) {
                removeAllViews()
                addView(ad.expressAdView)
            }
        })
        ad.setDislikeCallback((context as ThemedReactContext).currentActivity, object : TTAdDislike.DislikeInteractionCallback {
            override fun onShow() {}
            override fun onSelected(pos: Int, reason: String?, enforce: Boolean) {
                removeAllViews()
                dispatchAdEvent("onAdDislike", Arguments.createMap().apply { putString("reason", reason ?: "") })
            }
            override fun onCancel() {}
        })
        ad.render()
    }

    fun destroyAd() {
        bannerAd?.destroy()
        bannerAd = null
    }
}

@ReactModule(name = PlaynestBannerViewManager.NAME)
class PlaynestBannerViewManager : SimpleViewManager<PlaynestBannerAdView>(),
    PlaynestBannerViewManagerInterface<PlaynestBannerAdView> {

    private val mDelegate = PlaynestBannerViewManagerDelegate(this)

    override fun getDelegate() = mDelegate
    override fun getName() = NAME
    override fun createViewInstance(context: ThemedReactContext) = PlaynestBannerAdView(context)

    override fun setAndroidCodeId(view: PlaynestBannerAdView, value: String?) {
        view.androidCodeId = value
    }
    override fun setIosCodeId(view: PlaynestBannerAdView, value: String?) {}
    override fun setExpressWidth(view: PlaynestBannerAdView, value: Double) {
        view.expressWidth = value
    }
    override fun setExpressHeight(view: PlaynestBannerAdView, value: Double) {
        view.expressHeight = value
    }

    override fun onAfterUpdateTransaction(view: PlaynestBannerAdView) {
        super.onAfterUpdateTransaction(view)
        view.maybeLoad()
    }

    override fun onDropViewInstance(view: PlaynestBannerAdView) {
        view.destroyAd()
        super.onDropViewInstance(view)
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> =
        viewAdEventConstants("onAdShow", "onAdClick", "onAdFail", "onAdEcpm", "onAdDislike")

    companion object {
        const val NAME = "PlaynestBannerView"
    }
}
