# react-native-playnest-unionad

React Native 穿山甲(GroMore / UnionAD)广告插件。完整移植自 Flutter 插件 [flutter_unionad](https://github.com/gstory0404/flutter_unionad)，覆盖其全部广告能力：

| 类型 | API | iOS | Android |
|---|---|---|---|
| 初始化 / SDK 版本 / 主题 / ATT 权限 | `register` / `getSDKVersion` / `getThemeStatus` / `requestPermissionIfNecessary` | ✅ | ✅ |
| 激励视频 | `loadRewardVideoAd` / `showRewardVideoAd` | ✅ | ✅ |
| 全屏视频 / 插屏（二合一） | `loadFullScreenVideoAd` / `showFullScreenVideoAd` | ✅ | ✅ |
| 开屏（方法式全屏） | `showSplashAd` | ✅ | ✅ |
| 开屏（视图版） | `<PlaynestSplashAd />` | ✅ | ✅ |
| Banner | `<PlaynestBannerAd />` | ✅ | ✅ |
| 信息流原生（模板渲染） | `<PlaynestNativeAd />` | ✅ | ✅ |
| Draw 信息流（沉浸式视频） | `<PlaynestDrawAd />` | ✅ | ✅ |

> **仅支持 React Native 新架构（New Architecture / Fabric + TurboModules）。** 本库基于 codegen 生成 TurboModule 与 Fabric 组件，未提供旧架构（Paper）桥接。

---

## 环境要求

| 项 | 要求 |
|---|---|
| React Native | **0.87.0+**，且 **newArchEnabled = true** |
| iOS | **iOS 12.0+**（ATT 需 iOS 14+）。SDK 支持 arm64 真机与 **arm64 模拟器**（Apple Silicon） |
| Android | **minSdkVersion 24（Android 7.0）**。穿山甲 68 版本起强制 minSdk 24 |
| Android CPU 架构 | **仅 arm64-v8a / armeabi-v7a**。SDK 不含 x86/x86_64，普通 Intel 模拟器无法加载；需真机或 **arm64 模拟器** |
| Java | 编译需 **JDK 17+** |

---

## 安装

```sh
npm install react-native-playnest-unionad
# 或
yarn add react-native-playnest-unionad
```

自动链接（autolinking）会处理原生依赖，但**每个平台仍有必做的接入配置**，见下文。

---

## iOS 接入

### 1. Pod 依赖

穿山甲 SDK 通过 CocoaPods 引入（`Ads-CN-Beta/BUAdSDK` + `Ads-CN-Beta/CSJMediation-Only` 7.6.0.4，随本库 podspec 自动带入），来自 CocoaPods 官方 CDN，通常无需额外 source。安装：

```sh
cd ios && pod install
```

若你的 `Podfile` 使用了自定义 `source`，请确保保留官方主源：

```ruby
source 'https://cdn.cocoapods.org/'
```

### 2. Info.plist：ATT 授权文案（**必配，否则崩溃**）

调用 `requestPermissionIfNecessary()`（iOS 14+ ATT）时，若宿主 App 的 `Info.plist` 缺少 `NSUserTrackingUsageDescription`，**App 会直接崩溃**。请在 `ios/<App>/Info.plist` 添加：

```xml
<key>NSUserTrackingUsageDescription</key>
<string>用于向您展示更相关的广告</string>
```

即使你不主动调用 ATT，也建议配置以便合规。

### 3. arm64 模拟器

本库 podspec **未锁定** `x86_64`，`Ads-CN-Beta` 7.6.0.4 自带 `arm64_x86_64-simulator` 切片，可在 Apple Silicon 上直接跑 arm64 模拟器调试广告（免真机、免签名）。

### 4. 最低系统版本

`Podfile` 里 `platform :ios, '12.0'`（或更高）。

---

## Android 接入

### 1. 添加穿山甲 maven 源（**必配**）

穿山甲融合 SDK（`com.pangle_beta.cn:mediation-sdk`）来自字节跳动的 maven 仓库。请在**宿主工程根目录** `android/build.gradle` 的 `allprojects.repositories` 添加：

```gradle
allprojects {
    repositories {
        maven { url 'https://artifact.bytedance.com/repository/pangle' }
        // ...其余仓库
    }
}
```

> 本库自身也声明了该源，但传递依赖需在宿主/根工程可解析，故此处必须添加。

### 2. minSdkVersion 24

在 `android/build.gradle` 确认 `minSdkVersion` ≥ 24。若你的工程低于 24，参考穿山甲官方的 `tools:overrideLibrary` 方案。

### 3. `allowBackup` 冲突（如报清单合并错误再配）

若 SDK 的 `android:allowBackup=true` 与你 App 的清单冲突，在 `android/app/src/main/AndroidManifest.xml` 的 `<application>` 上加：

```xml
<manifest xmlns:tools="http://schemas.android.com/tools" ...>
    <application tools:replace="android:allowBackup" ...>
```

### 4. CPU 架构 / 模拟器

穿山甲原生库只含 `arm64-v8a` / `armeabi-v7a`。**Intel 模拟器无法加载**，请用真机或 **arm64 模拟器**（Apple Silicon 上的 Android Studio 模拟器即为 arm64）。可选：在 app `build.gradle` 用 `abiFilters 'arm64-v8a'` 精简包体。

### 5. 下载类广告的 FileProvider（可选，默认不需要）

穿山甲官方文档建议为下载类广告配置 `TTFileProvider`。但该 provider 继承旧版 `android.support.v4.content.FileProvider`，在**纯 AndroidX（未开启 jetifier）** 的 RN 工程中声明它会导致启动崩溃。因此**本库默认不声明**该 provider —— 开屏 / 激励 / 插屏 / Banner / 信息流 / Draw 的展示都不需要它。

如你确实需要下载类广告的文件共享，请在宿主 App 侧启用 jetifier（`android/gradle.properties` 加 `android.enableJetifier=true`）后，自行按官方文档声明 `TTFileProvider` 与 `res/xml/pangle_file_paths.xml`（本库已内置该 xml 资源可复用）。

---

## 快速开始

```tsx
import {
  register,
  getSDKVersion,
  loadRewardVideoAd,
  showRewardVideoAd,
} from 'react-native-playnest-unionad';

// 1) 初始化（调用任何广告接口前必须先成功初始化）
await register({
  androidAppId: '5750023',
  iosAppId: '5750023',
  appName: 'MyApp',
  debug: __DEV__,
});

console.log('SDK 版本', await getSDKVersion());

// 2) 预加载激励视频，回调驱动整个生命周期
const unsub = loadRewardVideoAd(
  { androidCodeId: '103685185', iosCodeId: '103685185' },
  {
    onReady: () => showRewardVideoAd(),            // 加载完成即展示
    onRewardArrived: (v) => console.log('发奖', v), // 推荐以此发奖
    onClose: () => unsub(),                         // 生命周期结束后取消监听
    onFail: (e) => { console.warn(e.error); unsub(); },
  }
);
```

---

## API

### 初始化与通用

#### `register(config): Promise<boolean>`

| 字段 | 类型 | 说明 |
|---|---|---|
| `androidAppId` | `string` | **必填**，Android appId |
| `iosAppId` | `string` | **必填**，iOS appId |
| `appName` | `string?` | 应用名 |
| `useMediation` | `boolean?` | 是否使用聚合(GroMore)，默认 `true` |
| `paid` | `boolean?` | 是否计费用户 |
| `keywords` | `string?` | 用户画像关键词 |
| `allowShowNotify` | `boolean?` | 允许弹通知，默认 `true` |
| `debug` | `boolean?` | debug 日志，默认 `false` |
| `supportMultiProcess` | `boolean?` | 多进程，默认 `false`（仅 Android） |
| `themeStatus` | `number?` | 主题，见 `UnionadTheme`，默认 `DAY` |
| `iosPrivacy` | `IOSPrivacy?` | iOS 隐私合规（`limitPersonalAds` / `limitProgrammaticAds` / `forbiddenCAID`） |

- `getSDKVersion(): Promise<string>` — SDK 版本号。
- `getThemeStatus(): Promise<number>` — `0` 日间 / `1` 夜间。
- `requestPermissionIfNecessary(): Promise<number>` — iOS 返回 ATT 状态（见 `UnionadPermission`：`notDetermined 0` / `restricted 1` / `denied 2` / `authorized 3`）；Android 恒返回 `3`。**iOS 需先配好 `NSUserTrackingUsageDescription`。**

常量：`UnionadTheme`（`DAY/NIGHT`）、`UnionadOrientation`（`VERTICAL 1 / HORIZONTAL 2`）、`UnionadPermission`。

### 激励视频

```tsx
const unsub = loadRewardVideoAd(options, callback); // 预加载
await showRewardVideoAd();                           // 展示
```

`RewardVideoOptions`：`androidCodeId` / `iosCodeId`（必填）、`rewardName?` / `rewardAmount?` / `userID?` / `mediaExtra?`（服务端验证）、`orientation?`（0 竖 / 1 横）、`mutedIfCan?`（默认 `true`）。

`RewardVideoCallback`：`onReady` / `onCache` / `onShow` / `onClick` / `onClose` / `onSkip` / `onVerify` / `onRewardArrived`（推荐发奖）/ `onFail` / `onUnReady` / `onEcpm`。

### 全屏视频 / 插屏（二合一）

```tsx
const unsub = loadFullScreenVideoAd(options, callback);
await showFullScreenVideoAd();
```

`FullScreenVideoOptions`：`androidCodeId` / `iosCodeId`、`orientation?`（仅 Android）。
`FullScreenVideoCallback`：`onReady` / `onShow` / `onClick` / `onClose` / `onFinish` / `onSkip` / `onFail` / `onUnReady` / `onEcpm`。

### 开屏（方法式全屏）

```tsx
const unsub = showSplashAd(
  { androidCodeId: '103687131', iosCodeId: '103687131' },
  { onShow: () => {}, onFinish: () => unsub(), onSkip: () => unsub(), onFail: (e) => unsub() }
);
```

`SplashOptions`：`androidCodeId` / `iosCodeId`、`timeout?`（仅 Android，默认 3000）、`width?` / `height?`（0 表示全屏）、`isShake?`、`supportDeepLink?`（仅 Android）。
`SplashCallback`：`onShow` / `onClick` / `onSkip` / `onFinish` / `onFail` / `onEcpm`。

### 视图类广告组件

四个视图广告都是 Fabric 原生组件，公用的回调见 `ViewAdCallbacks`：`onShow({width,height})` / `onClick` / `onFail({error})` / `onEcpm(info)` / `onDislike({reason})`。

```tsx
import {
  PlaynestBannerAd, PlaynestNativeAd, PlaynestDrawAd, PlaynestSplashAd,
} from 'react-native-playnest-unionad';

<PlaynestBannerAd androidCodeId="..." iosCodeId="..." width={300} height={150}
  onShow={(e) => {}} onFail={(e) => {}} />

<PlaynestNativeAd androidCodeId="..." iosCodeId="..." width={330} height={280} isMuted />

<PlaynestDrawAd androidCodeId="..." iosCodeId="..." width={340} height={500}
  onVideoPlay={() => {}} onVideoStop={() => {}} />

// 开屏视图版：不传尺寸=全屏；传尺寸=按区域内嵌（底部可留 logo 区）
<PlaynestSplashAd androidCodeId="..." iosCodeId="..."
  onShow={(e) => {}} onFinish={() => {}} onSkip={() => {}} />
```

- `PlaynestBannerAd` / `PlaynestNativeAd` / `PlaynestDrawAd` / `PlaynestSplashAd` 均支持 `width?` / `height?`（dp，省略则由 `style` 决定尺寸）与 `style`。
- `PlaynestNativeAd` / `PlaynestDrawAd` 支持 `isMuted`（默认 `true`）。
- `PlaynestDrawAd` 额外有 `onVideoPlay` / `onVideoPause` / `onVideoStop`。

---

## 平台差异需要注意

| 点 | iOS | Android |
|---|---|---|
| **开屏视图不传尺寸** | 全屏（`showSplashViewInRootViewController`） | 全屏（挂 Activity 根视图） |
| **开屏视图传尺寸** | 顶部指定区域（SDK 仅支持全屏呈现） | 内嵌组件容器指定区域 |
| `orientation`（激励/全屏） | 部分参数不生效 | 生效 |
| `timeout` / `supportDeepLink`（开屏） | 不生效 | 生效 |
| 信息流「自渲染」广告 | 暂不支持（仅模板渲染） | 暂不支持（仅模板渲染） |

> **信息流 / Banner / Draw 只支持模板渲染（express）广告位。** 请在穿山甲后台为对应广告位选择「模板渲染」。自渲染广告会触发 `onFail`。

---

## 测试广告位

穿山甲官方测试 appId 与广告位（与 flutter_unionad example 一致，iOS / Android 通用）：

| 用途 | id |
|---|---|
| appId | `5750023` |
| 激励视频 | `103685185` |
| 全屏/插屏 | `103687132` |
| 开屏 | `103687131` |
| Banner | `103686668` |
| 信息流 | `103686791` |
| Draw | `103687068` |

> 测试广告位在模拟器上常出现无填充（`20005`）、频控（`20001`）或自渲染内容，属正常现象，不代表接入错误。真机 + 正式模板渲染广告位可正常展示。

---

## 常见问题

- **iOS 调 ATT 崩溃** → 宿主 `Info.plist` 缺 `NSUserTrackingUsageDescription`，见上文。
- **Android 找不到穿山甲依赖 / 解析失败** → 根 `build.gradle` 未加 pangle maven 源。
- **Android 广告位不合法（40006）** → 用错了 SDK。本库用的是融合 SDK（`mediation-sdk`），非基础 `ads-sdk-pro`；正式广告位需在 GroMore 后台创建。
- **视图广告有 `onShow` 却看不到** → 确认给了尺寸（`width/height` 或 `style`）；开屏视图不传尺寸时才默认全屏。
- **只出现无填充** → 模拟器 / 测试位特性，换真机或正式广告位。

---

## 维护者

本库移植自 Flutter 插件 flutter_unionad。两者的能力对应关系、移植决策，以及 flutter_unionad 有新提交时如何快速同步，见 [docs/PORTING.md](docs/PORTING.md)。

## 许可证

MIT · playnest
