package com.playnestunionad

import android.content.Context
import android.util.Log
import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTAdManager
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTCustomController
import com.bytedance.sdk.openadsdk.TTLocation
import com.bytedance.sdk.openadsdk.mediation.init.IMediationPrivacyConfig
import com.bytedance.sdk.openadsdk.mediation.init.MediationConfig
import com.bytedance.sdk.openadsdk.mediation.init.MediationConfigUserInfoForSegment
import com.bytedance.sdk.openadsdk.mediation.init.MediationPrivacyConfig
import com.facebook.react.bridge.ReadableMap

/**
 * 穿山甲(Pangle) SDK 初始化封装。
 * 支持核心初始化 + 版本号 + Android 隐私配置(TTCustomController) + 流量分组(Segment)。
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
        androidPrivacy: ReadableMap?,
        userInfo: ReadableMap?,
        callback: TTAdSdk.Callback,
    ) {
        val mediationBuilder = MediationConfig.Builder()
        // 流量分组（聚合维度）——仅在传入 userInfo 时下发
        if (userInfo != null) {
            mediationBuilder.setMediationConfigUserInfoForSegment(buildSegment(userInfo))
        }

        val builder = TTAdConfig.Builder()
            .appId(appId)
            .appName(appName)
            .useMediation(useMediation)
            .paid(paid)
            .keywords(keywords)
            .allowShowNotify(allowShowNotify)
            .debug(debug)
            .supportMultiProcess(supportMultiProcess)
            .setMediationConfig(mediationBuilder.build())
            .themeStatus(themeStatus)

        // Android 隐私控制——仅在传入 androidPrivacy 时注入 TTCustomController，
        // 否则保持 SDK 默认行为。
        if (androidPrivacy != null) {
            builder.customController(buildController(Priv.from(androidPrivacy)))
        }

        val adConfig = builder.build()
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

    private fun buildSegment(map: ReadableMap): MediationConfigUserInfoForSegment {
        val segment = MediationConfigUserInfoForSegment()
        segment.userId = map.optStr("userId")
        segment.age = map.optInt("age", 0)
        segment.channel = map.optStr("channel")
        segment.subChannel = map.optStr("subChannel")
        segment.userValueGroup = map.optStr("userValueGroup")
        segment.gender = when (map.optInt("gender", 3)) {
            0 -> MediationConfigUserInfoForSegment.GENDER_FEMALE
            1 -> MediationConfigUserInfoForSegment.GENDER_MALE
            else -> MediationConfigUserInfoForSegment.GENDER_UNKNOWN
        }
        segment.customInfos = map.optStringMap("customInfos")
        return segment
    }

    private fun buildController(p: Priv): TTCustomController = object : TTCustomController() {
        override fun isCanUseLocation(): Boolean = p.isCanUseLocation
        override fun getTTLocation(): TTLocation? =
            if (p.lat == 0.0 || p.lon == 0.0) null else TTLocation(p.lat, p.lon)
        override fun alist(): Boolean = p.alist
        override fun isCanUsePhoneState(): Boolean = p.isCanUsePhoneState
        override fun getDevImei(): String = p.imei
        override fun isCanUseWifiState(): Boolean = p.isCanUseWifiState
        override fun getMacAddress(): String = p.macAddress
        override fun isCanUseWriteExternal(): Boolean = p.isCanUseWriteExternal
        override fun getDevOaid(): String = p.oaid
        override fun isCanUseAndroidId(): Boolean = p.isCanUseAndroidId
        override fun getAndroidId(): String = p.androidId
        override fun isCanUsePermissionRecordAudio(): Boolean = p.isCanUsePermissionRecordAudio
        override fun getMediationPrivacyConfig(): IMediationPrivacyConfig = object : MediationPrivacyConfig() {
            override fun isLimitPersonalAds(): Boolean = p.isLimitPersonalAds
            override fun isProgrammaticRecommend(): Boolean = p.isProgrammaticRecommend
        }
        override fun userPrivacyConfig(): Map<String, Any> = p.userPrivacyConfig
    }

    /** androidPrivacy 提前抽取为不可变值，避免 TTCustomController 回调触发时 ReadableMap 已失效。 */
    private data class Priv(
        val isCanUseLocation: Boolean,
        val lat: Double,
        val lon: Double,
        val isCanUsePhoneState: Boolean,
        val imei: String,
        val isCanUseWifiState: Boolean,
        val macAddress: String,
        val isCanUseWriteExternal: Boolean,
        val oaid: String,
        val alist: Boolean,
        val isCanUseAndroidId: Boolean,
        val androidId: String,
        val isCanUsePermissionRecordAudio: Boolean,
        val isLimitPersonalAds: Boolean,
        val isProgrammaticRecommend: Boolean,
        val userPrivacyConfig: Map<String, Any>,
    ) {
        companion object {
            fun from(m: ReadableMap): Priv = Priv(
                isCanUseLocation = m.optBool("isCanUseLocation"),
                lat = m.optDouble("lat"),
                lon = m.optDouble("lon"),
                isCanUsePhoneState = m.optBool("isCanUsePhoneState"),
                imei = m.optStr("imei"),
                isCanUseWifiState = m.optBool("isCanUseWifiState"),
                macAddress = m.optStr("macAddress"),
                isCanUseWriteExternal = m.optBool("isCanUseWriteExternal"),
                oaid = m.optStr("oaid"),
                alist = m.optBool("alist"),
                isCanUseAndroidId = m.optBool("isCanUseAndroidId"),
                androidId = m.optStr("androidId"),
                isCanUsePermissionRecordAudio = m.optBool("isCanUsePermissionRecordAudio"),
                isLimitPersonalAds = m.optBool("isLimitPersonalAds"),
                isProgrammaticRecommend = m.optBool("isProgrammaticRecommend"),
                userPrivacyConfig = m.getMap("userPrivacyConfig")?.toHashMap()
                    ?.filterValues { it != null }?.mapValues { it.value as Any } ?: emptyMap(),
            )
        }
    }

    // —— ReadableMap 安全读取小工具 ——
    private fun ReadableMap.optBool(key: String, def: Boolean = false): Boolean =
        if (hasKey(key) && !isNull(key)) getBoolean(key) else def

    private fun ReadableMap.optInt(key: String, def: Int): Int =
        if (hasKey(key) && !isNull(key)) getInt(key) else def

    private fun ReadableMap.optDouble(key: String, def: Double = 0.0): Double =
        if (hasKey(key) && !isNull(key)) getDouble(key) else def

    private fun ReadableMap.optStr(key: String, def: String = ""): String =
        if (hasKey(key) && !isNull(key)) getString(key) ?: def else def

    private fun ReadableMap.optStringMap(key: String): Map<String, String> {
        val hm = (if (hasKey(key) && !isNull(key)) getMap(key) else null)?.toHashMap()
            ?: return emptyMap()
        val out = HashMap<String, String>()
        for ((k, v) in hm) out[k] = v?.toString() ?: ""
        return out
    }
}
