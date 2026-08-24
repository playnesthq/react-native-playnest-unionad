package com.playnestunionad

import android.content.Context
import android.util.Log
import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTAdManager
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.mediation.init.MediationConfig

/**
 * 穿山甲(Pangle) SDK 初始化封装。
 * 切片0：仅实现核心初始化 + 版本号；隐私/流量分组等配置将在后续切片补全。
 */
object PangleSdkManager {
    private const val TAG = "PlaynestUnionad"

    @Volatile
    var isInit = false
        private set

    fun getAdManager(): TTAdManager {
        if (!isInit) {
            throw RuntimeException("调用广告前，请先调用 register() 初始化 react-native-playnest-unionad")
        }
        return TTAdSdk.getAdManager()
    }

    fun init(
        context: Context,
        appId: String,
        appName: String,
        useMediation: Boolean,
        paid: Boolean,
        keywords: String,
        allowShowNotify: Boolean,
        debug: Boolean,
        supportMultiProcess: Boolean,
        themeStatus: Int,
        callback: TTAdSdk.Callback,
    ) {
        val adConfig = TTAdConfig.Builder()
            .appId(appId)
            .appName(appName)
            .useMediation(useMediation)
            .paid(paid)
            .keywords(keywords)
            .allowShowNotify(allowShowNotify)
            .debug(debug)
            .supportMultiProcess(supportMultiProcess)
            .setMediationConfig(MediationConfig.Builder().build())
            .themeStatus(themeStatus)
            .build()
        TTAdSdk.init(context, adConfig)
        TTAdSdk.start(object : TTAdSdk.Callback {
            override fun success() {
                isInit = true
                Log.i(TAG, "穿山甲初始化成功")
                callback.success()
            }

            override fun fail(code: Int, msg: String?) {
                isInit = false
                Log.e(TAG, "穿山甲初始化失败 code=$code msg=$msg")
                callback.fail(code, msg)
            }
        })
    }
}
