package com.playnestunionad

import com.bytedance.sdk.openadsdk.mediation.manager.MediationAdEcpmInfo
import org.json.JSONObject

/**
 * 将聚合 eCPM 信息序列化为 JSON 字符串（事件通过字符串传递，JS 层解析还原为对象）。
 */
object EcpmUtil {
    fun toJson(info: MediationAdEcpmInfo?): String? {
        if (info == null) return null
        val obj = JSONObject()
        obj.put("adnName", info.sdkName)
        obj.put("customAdnName", info.customSdkName)
        obj.put("slotID", info.slotId)
        obj.put("levelTag", info.levelTag)
        obj.put("ecpm", info.ecpm)
        obj.put("biddingType", info.reqBiddingType)
        obj.put("errorMsg", info.errorMsg)
        obj.put("requestID", info.requestId)
        obj.put("creativeID", "")
        obj.put("adRitType", info.ritType)
        obj.put("segmentId", info.segmentId)
        obj.put("abtestId", info.abTestId)
        obj.put("channel", info.channel)
        obj.put("sub_channel", info.subChannel)
        obj.put("scenarioId", info.scenarioId)
        obj.put("subRitType", info.subRitType)
        return obj.toString()
    }
}
