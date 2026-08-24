package com.playnestunionad

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.TTAdConstant
import com.bytedance.sdk.openadsdk.TTAdNative.RewardVideoAdListener
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTRewardVideoAd
import com.bytedance.sdk.openadsdk.TTRewardVideoAd.RewardAdInteractionListener
import com.bytedance.sdk.openadsdk.mediation.MediationConstant
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

/**
 * 激励视频广告管理（对齐 flutter_unionad 的事件语义）。
 * 事件通过 [emit] 回调发送，payload 结构见 src/NativePlaynestUnionad.ts 的 UnionadNativeEvent。
 */
@SuppressLint("StaticFieldLeak")
object RewardVideoAd {
    private const val TAG = "PlaynestUnionad"
    private const val AD_TYPE = "rewardAd"

    private var mttRewardVideoAd: TTRewardVideoAd? = null

    private var mCodeId: String? = null
    private var rewardName: String? = null
    private var rewardAmount: Int = 0
    private var userID: String? = null
    private var orientation: Int = 0
    private var mediaExtra: String = ""
    private var mutedIfCan: Boolean = true

    private var emit: ((WritableMap) -> Unit)? = null

    private fun event(onAdMethod: String, build: (WritableMap.() -> Unit)? = null) {
        val map = Arguments.createMap()
        map.putString("adType", AD_TYPE)
        map.putString("onAdMethod", onAdMethod)
        build?.invoke(map)
        emit?.invoke(map)
    }

    fun load(activity: Activity, params: com.facebook.react.bridge.ReadableMap, emit: (WritableMap) -> Unit) {
        this.emit = emit
        this.mCodeId = if (params.hasKey("androidCodeId")) params.getString("androidCodeId") else null
        this.rewardName = if (params.hasKey("rewardName")) params.getString("rewardName") else ""
        this.rewardAmount = if (params.hasKey("rewardAmount")) params.getInt("rewardAmount") else 0
        this.userID = if (params.hasKey("userID")) params.getString("userID") else ""
        this.mutedIfCan = if (params.hasKey("mutedIfCan")) params.getBoolean("mutedIfCan") else true
        this.orientation = if (params.hasKey("orientation")) params.getInt("orientation") else 0
        this.mediaExtra = if (params.hasKey("mediaExtra")) params.getString("mediaExtra") ?: "" else ""
        loadRewardVideoAd(activity)
    }

    private fun loadRewardVideoAd(activity: Activity) {
        val adSlot = AdSlot.Builder()
            .setCodeId(mCodeId)
            .setUserID(userID)
            .setOrientation(orientation)
            .setMediationAdSlot(
                MediationAdSlot.Builder()
                    .setRewardName(rewardName)
                    .setRewardAmount(rewardAmount)
                    .setExtraObject(MediationConstant.ADN_PANGLE, mediaExtra)
                    .setExtraObject(MediationConstant.KEY_GROMORE_EXTRA, mediaExtra)
                    .setExtraObject(MediationConstant.ADN_GDT, mediaExtra)
                    .setExtraObject(MediationConstant.ADN_BAIDU, mediaExtra)
                    .setExtraObject(MediationConstant.ADN_KS, mediaExtra)
                    .setExtraObject(MediationConstant.ADN_KLEVIN, mediaExtra)
                    .setExtraObject(MediationConstant.ADN_ADMOB, mediaExtra)
                    .setExtraObject(MediationConstant.ADN_SIGMOB, mediaExtra)
                    .setExtraObject(MediationConstant.ADN_UNITY, mediaExtra)
                    .setMuted(mutedIfCan)
                    .build()
            )
            .setMediaExtra(mediaExtra)
            .build()
        val mTTAdNative = TTAdSdk.getAdManager().createAdNative(activity)
        mTTAdNative.loadRewardVideoAd(adSlot, object : RewardVideoAdListener {
            override fun onError(code: Int, message: String) {
                Log.e(TAG, "激励视频加载失败 $code $message")
                event("onFail") { putString("error", "$code $message") }
            }

            override fun onRewardVideoCached() {}

            override fun onRewardVideoCached(ad: TTRewardVideoAd?) {
                event("onCache")
            }

            override fun onRewardVideoAdLoad(ad: TTRewardVideoAd) {
                mttRewardVideoAd = ad
                event("onReady")
            }
        })
    }

    private fun bindAdListener(ad: TTRewardVideoAd) {
        ad.setRewardAdInteractionListener(object : RewardAdInteractionListener {
            override fun onAdShow() {
                event("onShow")
                event("onEcpm") {
                    putString("ecpm", EcpmUtil.toJson(mttRewardVideoAd?.mediationManager?.showEcpm))
                }
            }

            override fun onAdVideoBarClick() {
                event("onClick")
            }

            override fun onAdClose() {
                event("onClose")
            }

            override fun onVideoError() {}

            override fun onVideoComplete() {}

            override fun onRewardVerify(
                rewardVerify: Boolean,
                rewardAmount: Int,
                rewardName: String?,
                errorCode: Int,
                errorMsg: String?
            ) {
                event("onVerify") {
                    putBoolean("rewardVerify", rewardVerify)
                    putInt("rewardAmount", rewardAmount)
                    putString("rewardName", rewardName)
                    putInt("errorCode", errorCode)
                    putString("error", errorMsg)
                }
            }

            override fun onRewardArrived(isRewardValid: Boolean, rewardType: Int, extraInfo: Bundle) {
                val amount = when (val v = extraInfo["reward_extra_key_reward_amount"]) {
                    is Int -> v
                    is Float -> v.toInt()
                    is Number -> v.toInt()
                    else -> 0
                }
                event("onRewardArrived") {
                    putBoolean("rewardVerify", isRewardValid)
                    putInt("rewardType", rewardType)
                    putInt("rewardAmount", amount)
                    putString("rewardName", extraInfo.getString("reward_extra_key_reward_name"))
                    putString("propose", extraInfo["reward_extra_key_reward_propose"]?.toString())
                    putInt("errorCode", extraInfo.getInt("reward_extra_key_error_code"))
                    putString("error", extraInfo.getString("reward_extra_key_error_msg"))
                }
            }

            override fun onSkippedVideo() {
                event("onSkip")
            }
        })
    }

    fun show(activity: Activity) {
        val ad = mttRewardVideoAd
        if (ad == null) {
            event("onUnReady") { putString("error", "广告预加载未完成") }
            return
        }
        bindAdListener(ad)
        ad.showRewardVideoAd(
            activity,
            TTAdConstant.RitScenes.CUSTOMIZE_SCENES,
            "scenes_test"
        )
        mttRewardVideoAd = null
    }
}
