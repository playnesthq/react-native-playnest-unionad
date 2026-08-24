package com.playnestunionad

import android.content.Context
import android.view.View
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.events.Event

/** Fabric 直接事件载体。 */
internal class AdEvent(
    surfaceId: Int,
    viewId: Int,
    private val name: String,
    private val data: WritableMap,
) : Event<AdEvent>(surfaceId, viewId) {
    override fun getEventName(): String = name
    override fun getEventData(): WritableMap = data
}

/** 向 JS 派发一个视图广告事件。 */
internal fun View.dispatchAdEvent(name: String, data: WritableMap = Arguments.createMap()) {
    val ctx = context as? ReactContext ?: return
    val dispatcher = UIManagerHelper.getEventDispatcherForReactTag(ctx, id) ?: return
    dispatcher.dispatchEvent(AdEvent(UIManagerHelper.getSurfaceId(this), id, name, data))
}

/** 视图广告统一事件常量（供 ViewManager.getExportedCustomDirectEventTypeConstants 使用）。 */
internal fun viewAdEventConstants(vararg names: String): MutableMap<String, Any> {
    val map = HashMap<String, Any>()
    for (n in names) {
        map[n] = mapOf("registrationName" to n)
    }
    return map
}

/** dp -> px */
internal fun dp2px(context: Context, dp: Float): Int =
    (dp * context.resources.displayMetrics.density + 0.5f).toInt()
