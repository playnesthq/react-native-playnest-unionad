package com.playnestunionad

import android.view.View
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdDislike
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTDrawFeedAd
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot
import com.bytedance.sdk.openadsdk.mediation.ad.MediationExpressRenderListener
import com.facebook.react.bridge.Arguments
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.viewmanagers.PlaynestDrawViewManagerInterface
import com.facebook.react.viewmanagers.PlaynestDrawViewManagerDelegate

class PlaynestDrawAdView(context: ThemedReactContext) : PlaynestAdFrameLayout(context) {
    var androidCodeId: String? = null
    var expressWidth: Double = 0.0
    var expressHeight: Double = 0.0
    var isMuted: Boolean = true
    private var drawAd: TTDrawFeedAd? = null
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
        if (h <= 0f) h = (height / resources.displayMetrics.density)
        val adSlot = AdSlot.Builder()
            .setCodeId(codeId)
            .setAdCount(1)
            .setImageAcceptedSize(dp2px(context, w), dp2px(context, h))
            .setExpressViewAcceptedSize(w, h)
            .setMediationAdSlot(MediationAdSlot.Builder().setMuted(isMuted).build())
            .build()
        val native = TTAdSdk.getAdManager().createAdNative(activity)
        native.loadDrawFeedAd(adSlot, object : TTAdNative.DrawFeedAdListener {
            override fun onError(code: Int, message: String?) {
                dispatchAdEvent("onAdFail", Arguments.createMap().apply { putString("error", "$code $message") })
            }

            override fun onDrawFeedAdLoad(ads: MutableList<TTDrawFeedAd>?) {
                if (ads.isNullOrEmpty()) {
                    dispatchAdEvent("onAdFail", Arguments.createMap().apply { putString("error", "ads is empty") })
                    return
                }
                drawAd = ads[0]
                show()
            }
        })
    }

    private fun show() {
        val ad = drawAd ?: return
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

    private fun bindDislike(ad: TTDrawFeedAd) {
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
        drawAd?.destroy()
        drawAd = null
    }
}

@ReactModule(name = PlaynestDrawViewManager.NAME)
class PlaynestDrawViewManager : SimpleViewManager<PlaynestDrawAdView>(),
    PlaynestDrawViewManagerInterface<PlaynestDrawAdView> {

    private val mDelegate = PlaynestDrawViewManagerDelegate(this)

    override fun getDelegate() = mDelegate
    override fun getName() = NAME
    override fun createViewInstance(context: ThemedReactContext) = PlaynestDrawAdView(context)

    override fun setAndroidCodeId(view: PlaynestDrawAdView, value: String?) { view.androidCodeId = value }
    override fun setIosCodeId(view: PlaynestDrawAdView, value: String?) {}
    override fun setExpressWidth(view: PlaynestDrawAdView, value: Double) { view.expressWidth = value }
    override fun setExpressHeight(view: PlaynestDrawAdView, value: Double) { view.expressHeight = value }
    override fun setIsMuted(view: PlaynestDrawAdView, value: Boolean) { view.isMuted = value }

    override fun onAfterUpdateTransaction(view: PlaynestDrawAdView) {
        super.onAfterUpdateTransaction(view)
        view.maybeLoad()
    }

    override fun onDropViewInstance(view: PlaynestDrawAdView) {
        view.destroyAd()
        super.onDropViewInstance(view)
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> =
        viewAdEventConstants(
            "onAdShow", "onAdClick", "onAdFail", "onAdEcpm", "onAdDislike",
            "onAdVideoPlay", "onAdVideoPause", "onAdVideoStop"
        )

    companion object {
        const val NAME = "PlaynestDrawView"
    }
}
