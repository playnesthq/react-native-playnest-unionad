package com.rnunionad

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.uimanager.ViewManager

class RNUnionadPackage : BaseReactPackage() {
  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
    return if (name == RNUnionadModule.NAME) {
      RNUnionadModule(reactContext)
    } else {
      null
    }
  }

  override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> =
    listOf(
      RNUnionadBannerViewManager(),
      RNUnionadNativeViewManager(),
      RNUnionadDrawViewManager(),
      RNUnionadSplashViewManager(),
    )

  override fun getReactModuleInfoProvider() = ReactModuleInfoProvider {
    mapOf(
      RNUnionadModule.NAME to ReactModuleInfo(
        name = RNUnionadModule.NAME,
        className = RNUnionadModule.NAME,
        canOverrideExistingModule = false,
        needsEagerInit = false,
        isCxxModule = false,
        isTurboModule = true
      )
    )
  }
}
