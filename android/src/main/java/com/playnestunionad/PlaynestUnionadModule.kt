package com.playnestunionad

import android.text.TextUtils
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap

class PlaynestUnionadModule(reactContext: ReactApplicationContext) :
  NativePlaynestUnionadSpec(reactContext) {

  private val ctx: ReactApplicationContext = reactContext

  override fun register(config: ReadableMap, promise: Promise) {
    val appId = if (config.hasKey("androidAppId")) config.getString("androidAppId") else null
    if (appId.isNullOrBlank()) {
      promise.resolve(false)
      return
    }
    try {
      PangleSdkManager.init(
        context = ctx.applicationContext,
        appId = appId,
        appName = config.optStringOr("appName", ""),
        useMediation = config.optBooleanOr("useMediation", true),
        paid = config.optBooleanOr("paid", false),
        keywords = config.optStringOr("keywords", ""),
        allowShowNotify = config.optBooleanOr("allowShowNotify", true),
        debug = config.optBooleanOr("debug", false),
        supportMultiProcess = config.optBooleanOr("supportMultiProcess", false),
        themeStatus = config.optIntOr("themeStatus", 0),
        callback = object : TTAdSdk.Callback {
          override fun success() {
            promise.resolve(true)
          }

          override fun fail(code: Int, msg: String?) {
            promise.resolve(false)
          }
        },
      )
    } catch (e: Throwable) {
      promise.reject("register_error", e.message, e)
    }
  }

  override fun getSDKVersion(promise: Promise) {
    try {
      val version = TTAdSdk.getAdManager().sdkVersion
      if (TextUtils.isEmpty(version)) {
        promise.reject("0", "获取失败")
      } else {
        promise.resolve(version)
      }
    } catch (e: Throwable) {
      promise.reject("0", "获取失败: ${e.message}", e)
    }
  }

  override fun getThemeStatus(promise: Promise) {
    try {
      promise.resolve(TTAdSdk.getAdManager().themeStatus)
    } catch (e: Throwable) {
      promise.reject("theme_error", e.message, e)
    }
  }

  override fun requestPermissionIfNecessary(promise: Promise) {
    try {
      TTAdSdk.getAdManager().requestPermissionIfNecessary(ctx.applicationContext)
      promise.resolve(3.0)
    } catch (e: Throwable) {
      promise.reject("permission_error", e.message, e)
    }
  }

  override fun loadRewardVideoAd(config: ReadableMap, promise: Promise) {
    val activity = getCurrentActivity()
    if (activity == null) {
      promise.reject("no_activity", "当前无 Activity，无法加载激励视频")
      return
    }
    try {
      RewardVideoAd.load(activity, config) { event ->
        emitOnAdEvent(event)
      }
      promise.resolve(true)
    } catch (e: Throwable) {
      promise.reject("load_reward_error", e.message, e)
    }
  }

  override fun showRewardVideoAd(promise: Promise) {
    val activity = getCurrentActivity()
    if (activity == null) {
      promise.reject("no_activity", "当前无 Activity，无法展示激励视频")
      return
    }
    try {
      activity.runOnUiThread { RewardVideoAd.show(activity) }
      promise.resolve(true)
    } catch (e: Throwable) {
      promise.reject("show_reward_error", e.message, e)
    }
  }

  override fun loadFullScreenVideoAd(config: ReadableMap, promise: Promise) {
    val activity = getCurrentActivity()
    if (activity == null) {
      promise.reject("no_activity", "当前无 Activity，无法加载全屏/插屏广告")
      return
    }
    try {
      FullScreenVideoAd.load(activity, config) { event ->
        emitOnAdEvent(event)
      }
      promise.resolve(true)
    } catch (e: Throwable) {
      promise.reject("load_fullscreen_error", e.message, e)
    }
  }

  override fun showFullScreenVideoAd(promise: Promise) {
    val activity = getCurrentActivity()
    if (activity == null) {
      promise.reject("no_activity", "当前无 Activity，无法展示全屏/插屏广告")
      return
    }
    try {
      activity.runOnUiThread { FullScreenVideoAd.show(activity) }
      promise.resolve(true)
    } catch (e: Throwable) {
      promise.reject("show_fullscreen_error", e.message, e)
    }
  }

  override fun showSplashAd(config: ReadableMap, promise: Promise) {
    val activity = getCurrentActivity()
    if (activity == null) {
      promise.reject("no_activity", "当前无 Activity，无法展示开屏广告")
      return
    }
    try {
      activity.runOnUiThread {
        SplashAd.load(activity, config) { event -> emitOnAdEvent(event) }
      }
      promise.resolve(true)
    } catch (e: Throwable) {
      promise.reject("splash_error", e.message, e)
    }
  }

  companion object {
    const val NAME = NativePlaynestUnionadSpec.NAME
  }
}

/* ---- ReadableMap 便捷读取（带默认值） ---- */
private fun ReadableMap.optStringOr(key: String, def: String): String =
  if (hasKey(key) && !isNull(key)) getString(key) ?: def else def

private fun ReadableMap.optBooleanOr(key: String, def: Boolean): Boolean =
  if (hasKey(key) && !isNull(key)) getBoolean(key) else def

private fun ReadableMap.optIntOr(key: String, def: Int): Int =
  if (hasKey(key) && !isNull(key)) getInt(key) else def
