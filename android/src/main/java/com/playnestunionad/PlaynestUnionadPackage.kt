package com.playnestunionad

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.uimanager.ViewManager

class PlaynestUnionadPackage : BaseReactPackage() {
  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
    return if (name == PlaynestUnionadModule.NAME) {
      PlaynestUnionadModule(reactContext)
    } else {
      null
    }
  }

  override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> =
    listOf(
      PlaynestBannerViewManager(),
      PlaynestNativeViewManager(),
      PlaynestDrawViewManager(),
      PlaynestSplashViewManager(),
    )

  override fun getReactModuleInfoProvider() = ReactModuleInfoProvider {
    mapOf(
      PlaynestUnionadModule.NAME to ReactModuleInfo(
        name = PlaynestUnionadModule.NAME,
        className = PlaynestUnionadModule.NAME,
        canOverrideExistingModule = false,
        needsEagerInit = false,
        isCxxModule = false,
        isTurboModule = true
      )
    )
  }
}
