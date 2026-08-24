# flutter_unionad → react-native-playnest-unionad 移植记录

本库从 Flutter 插件 **[flutter_unionad](https://github.com/gstory0404/flutter_unionad)** 逐能力移植而来。本文档记录两者的对应关系与移植中的关键决策，供 flutter_unionad 后续有新提交时**快速同步**到本库。

## 移植基线

| | 版本 / 提交 |
|---|---|
| flutter_unionad 移植基线 | **v2.2.8**（commit `cb314bf`：android SDK 7.6.1.1 / ios SDK 7.6.0.4） |
| 本库对应 SDK（iOS） | `Ads-CN-Beta/BUAdSDK` + `Ads-CN-Beta/CSJMediation-Only` **7.6.0.4** |
| 本库对应 SDK（Android） | `com.pangle_beta.cn:mediation-sdk` **7.6.1.1**（融合/GroMore SDK 的 maven 版本） |

> 同步新版本时，先在下面「同步 checklist」对照 flutter 的改动点。

---

## 架构映射（桥接层重写，原生逻辑高度复用）

Flutter 与 RN 的差别只在**桥接层**；穿山甲 SDK 的调用逻辑（load/show/回调/eCPM）可以几乎照搬。

| 能力 | Flutter | react-native（新架构） |
|---|---|---|
| 方法调用 | `MethodChannel('flutter_unionad')` | **TurboModule** `PlaynestUnionad`（codegen spec `src/NativePlaynestUnionad.ts`） |
| 事件回调 | 单条 `EventChannel('com.gstory.flutter_unionad/adevent')`，payload 带 `adType` + `onAdMethod` | 单个 codegen `EventEmitter` `onAdEvent`，payload 同样带 `adType` + `onAdMethod`；JS 侧按 `adType` 分发（`setAdEventHandler`） |
| 视图广告 | `PlatformView`（`registerViewFactory`） | **Fabric 原生组件**（`codegenNativeComponent`；iOS `RCTViewComponentView`，Android `SimpleViewManager`） |
| 视图广告事件 | 同一条 EventChannel | **Fabric 直接事件**（组件自带 `DirectEventHandler`，如 `onAdShow/onAdFail`），独立于方法事件流 |

事件模型刻意与 flutter 对齐：**方法类广告（激励/全屏/开屏方法式）走单条 `onAdEvent`**，payload 里用 `adType`（`rewardAd` / `fullScreenVideoAdInteraction` / `splashAd`）区分；**视图类广告走各自组件的 Fabric 直接事件**。

---

## 文件对应表

### JS / Dart 接口层

| flutter (lib/) | 本库 (src/ + index.tsx 导出) |
|---|---|
| `flutter_unionad.dart`（总入口/方法） | `src/index.tsx`（`register`/`getSDKVersion`/`getThemeStatus`/`requestPermissionIfNecessary`/各广告 API） |
| `flutter_unionad_callback.dart` | `RewardVideoCallback` / `FullScreenVideoCallback` / `SplashCallback` / `ViewAdCallbacks`（index.tsx 内） |
| `flutter_unionad_code.dart`（常量） | `UnionadTheme` / `UnionadOrientation` / `UnionadPermission` |
| `flutter_unionad_stream.dart`（事件流） | 统一 `onAdEvent` 分发（`setAdEventHandler` / `ensureAdEventSub`） |
| `FlutterUnionadEcpm.dart` | `EcpmInfo` + `parseEcpmString` |
| `flutter_unionad_privacy.dart` / `flutter_unionad_user_info.dart` | `IOSPrivacy`（部分；userInfo 分段配置尚未全量移植，见「未尽事项」） |
| `bannerad/BannerAdView.dart` | `PlaynestBannerAd` + `src/PlaynestBannerViewNativeComponent.ts` |
| `nativead/NativeAdView.dart` | `PlaynestNativeAd` + `src/PlaynestNativeViewNativeComponent.ts` |
| `drawfeedad/DrawFeedAdView.dart` | `PlaynestDrawAd` + `src/PlaynestDrawViewNativeComponent.ts` |
| `splashad/SplashAdView.dart` | `PlaynestSplashAd` + `src/PlaynestSplashViewNativeComponent.ts` |

### iOS（Swift → ObjC）

| flutter (ios/Classes/) | 本库 (ios/) |
|---|---|
| `SwiftFlutterUnionadPlugin.swift` / `TTAdManagerHolder.swift` | `PlaynestUnionad.mm`（register/版本/主题/ATT） |
| `rewardedvideoad/RewardedVideoAd.swift` | `PlaynestUnionad.mm` 内激励部分 |
| `fullscreenvideoadinteraction/FullScreenVideoAdInteraction.swift` | `PlaynestUnionad.mm` 内全屏/插屏部分 |
| `splashad/SplashAdView*.swift` | `PlaynestUnionad.mm`（方法式）+ `PlaynestSplashView.mm`（视图版） |
| `bannerad/BannerAdView*.swift` | `PlaynestBannerView.mm` |
| `nativead/NativeAdView*.swift` | `PlaynestNativeView.mm` |
| `drawfeedad/DrawFeedAdView*.swift` | `PlaynestDrawView.mm` |
| `utils/BUMRitInfo+Dictionary.swift` | `PlaynestUnionad.mm` 内 eCPM KVC→JSON |

### Android（Kotlin → Kotlin）

| flutter (…/com/gstory/flutter_unionad/) | 本库 (…/com/playnestunionad/) |
|---|---|
| 插件主类 + `TTAdManagerHolder` | `PlaynestUnionadModule.kt` + `PangleSdkManager.kt` |
| `rewardvideoad/` | `RewardVideoAd.kt` |
| `fullscreenvideoadinteraction/` | `FullScreenVideoAd.kt` |
| `splashad/`（方法式） | `SplashAd.kt` |
| `bannerad/` | `PlaynestBannerViewManager.kt` |
| `nativead/` | `PlaynestNativeViewManager.kt` |
| `drawfeedad/` | `PlaynestDrawViewManager.kt` |
| `splashad/`（视图版） | `PlaynestSplashViewManager.kt` |
| eCPM 工具 | `EcpmUtil.kt` |
| —（RN 特有） | `PlaynestUnionadPackage.kt`（注册模块+ViewManager）、`AdEventUtil.kt`（Fabric 事件派发）、`PlaynestAdFrameLayout.kt`（见下方布局修复） |

---

## 命名对照

| flutter_unionad | 本库 |
|---|---|
| 包名 `com.gstory.flutter_unionad` | `com.playnestunionad` |
| 插件名 `flutter_unionad` | `PlaynestUnionad` |
| 视图 `com.gstory.flutter_unionad/BannerAdView` 等 | Fabric 组件 `PlaynestBannerView` / `PlaynestNativeView` / `PlaynestDrawView` / `PlaynestSplashView` |

> 本库刻意采用全新命名，不复用任何 gstory / flutter_unionad 标识。

---

## 移植中的关键决策 / 坑（新架构专属）

这些是 flutter 版本没有、但 RN 新架构必须处理的点，同步时不要回退：

1. **codegen 类型不能起本地别名**。spec 里必须写 `CodegenTypes.EventEmitter<T>` / `CodegenTypes.Double` 等**限定名**，绝不能 `type EventEmitter<T> = CodegenTypes.EventEmitter<T>` —— 会让 codegen 死循环卡死。
2. **iOS 模块须继承生成基类**：`@interface PlaynestUnionad : NativePlaynestUnionadSpecBase <NativePlaynestUnionadSpec>`，否则 `emitOnAdEvent:` 找不到。
3. **改了 spec（`NativePlaynestUnionad.ts` 方法/事件）后必须重跑 `pod install`** 重新生成 codegen，否则 iOS 编译报 "no visible selector"。
4. **Android 视图广告的布局修复（重要）**：Fabric 下 React 布局一次性下发，SDK 在渲染成功回调里 `addView()` 动态加入的广告子视图不在 shadow tree 中，不会被 measure/layout → 停在 0×0 不可见。解决：所有视图广告容器继承 `PlaynestAdFrameLayout`（重写 `requestLayout()` 主动 `post` 一次 measure+layout）。**新增任何视图广告都要继承它。**
5. **Android SDK 依赖**：用融合 SDK `com.pangle_beta.cn:mediation-sdk`（GroMore），**不要**用基础 `com.pangle.cn:ads-sdk-pro`（会报广告位不合法 40006）。
6. **Android 不声明 `TTFileProvider`**：SDK 的该 provider 继承旧 support-v4，在纯 AndroidX RN 工程会启动崩溃。核心广告展示不需要它。
7. **iOS ATT 崩溃**：宿主 Info.plist 必须有 `NSUserTrackingUsageDescription`（库无法替宿主补）。
8. **开屏视图默认尺寸对齐**：不传 width/height → 全屏（iOS `showSplashViewInRootViewController`；Android 挂 `android.R.id.content`），传了 → 按区域（iOS 顶部区域 / Android 内嵌容器）。
9. **监听器防泄漏**：JS 侧每个 `adType` 只保留一个 `onAdEvent` 处理器（`setAdEventHandler` 替换而非新增），重复 load 不会叠加监听。

---

## 当 flutter_unionad 有新提交时的同步 checklist

1. **看 diff 属于哪一类**：
   - 纯 SDK 升版 → 见第 2 步；
   - 新增/修改某广告能力 → 定位到「文件对应表」里对应的 RN 文件改；
   - 新增广告类型 → JS 加 API/组件 spec、iOS 加 `.mm`、Android 加 Manager，并在 `PlaynestUnionadPackage.kt`（Android）、`package.json > codegenConfig.ios.componentProvider`（iOS 视图）注册；视图类务必继承 `PlaynestAdFrameLayout`（Android）。
2. **SDK 升版**：
   - iOS：改 `*.podspec` 的 `Ads-CN-Beta/*` 版本 → `pod install`。留意 SDK 选择器变更（历史上 `loadData` vs `loadAdData` 用 `respondsToSelector` 兜底）。
   - Android：改 `android/build.gradle` 的 `com.pangle_beta.cn:mediation-sdk:<版本>`（[可用版本列表](https://artifact.bytedance.com/repository/pangle/com/pangle_beta/cn/mediation-sdk/maven-metadata.xml)）。**只改一行版本号即可**，无需再动本地 aar。
3. **改了 TurboModule spec** → `pod install` 重新 codegen（见坑 #3）。
4. **验证**（无真机也可）：
   - iOS：NestHotDemo 上 arm64 模拟器直接跑（免签名），逐个广告看真广告 + 事件。
   - Android：arm64 模拟器跑（穿山甲无 x86）；方法式开屏最易填充，视图广告看 dumpsys 容器尺寸是否非 0。
5. **更新本文档「移植基线」表**的版本号与 commit。

---

## 未尽事项（相对 flutter_unionad 尚未全量移植）

- 完整的 `userInfo` 流量分组 / 隐私分段配置（当前 iOS 仅 `IOSPrivacy` 的 limitPersonalAds/limitProgrammaticAds/forbiddenCAID，Android 隐私配置最小化）。
- 部分平台专属参数在另一端不生效（见 README「平台差异」表）：`orientation`、开屏 `timeout` / `supportDeepLink` 等。
- 信息流「自渲染」广告未支持（与 flutter 一致，仅模板渲染）。
