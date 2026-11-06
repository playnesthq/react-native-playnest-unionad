# Changelog

本项目的所有重要变更都会记录在此文件。
格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。**最新版本在最上面。**

<!--
维护约定：
- 未发布的改动先记到 [Unreleased] 下。
- 发新版时：把 [Unreleased] 的内容移到新版本号标题下、补日期，并在最底部加一行 compare 链接。
- 每个版本按需使用分区：🚀 New Features / 🐛 Bug Fixes / 🔧 Changed / 📝 Docs。
- 发 GitHub Release 时，把对应版本这一段直接粘贴到 “Release notes”。
-->

## [Unreleased]

## [2.0.0] - 2026-11-06

### ⚠️ Breaking Changes

- **视图组件更名，旧名已移除**：`PlaynestBannerAd` / `PlaynestNativeAd` / `PlaynestDrawAd` / `PlaynestSplashAd` → `UnionadBannerAd` / `UnionadNativeAd` / `UnionadDrawAd` / `UnionadSplashAd`。从 1.x 升级需把旧组件名全部替换为新名；函数式 API（`register` / `showSplashAd` / `loadRewardVideoAd` 等）不变。

### 🔧 Changed

- **原生模块 / 视图 / podspec / 包名统一重命名为 `RNUnionad*`**：iOS podspec `PlaynestUnionad` → `RNUnionad`、Android 包 `com.playnestunionad` → `com.rnunionad`、codegen spec `PlaynestUnionadSpec` → `RNUnionadSpec`、Fabric 组件 `Playnest*View` → `RNUnionad*View`。**对使用 autolinking 的集成方透明**：升级后 iOS 重新 `pod install`、Android 重新构建即可，npm 包名与 JS 导入路径不变。
- `package.json` 描述改为英文；扩充 npm keywords（穿山甲 / pangle / gromore / 各广告类型 / idfa / fabric 等）提升可检索性。

## [1.1.1] - 2026-10-31

### 🐛 Bug Fixes

- Android：Banner / 信息流原生（Native）/ Draw 在**未设置 `width`** 时自适应整屏宽。此前广告在 `onAfterUpdateTransaction`（视图测量前）加载，View 宽度为 0，导致 `setExpressViewAcceptedSize(0, …)`，广告不渲染或不铺满；现回退到 `displayMetrics.widthPixels`（Draw 的 height 同样回退到 `heightPixels`），与 iOS 的整屏兜底行为对齐。

### 🔧 Changed

- example 采用 UIScene 生命周期（新增 `SceneDelegate`），作为 iOS UIScene 适配参考。库本身已兼容 UIScene（通过 `UIApplication.connectedScenes` 定位活动 scene 的 window 展示广告），无需改动。

### 📝 Docs

- README 新增 iOS「SKAdNetworkItems（广告归因）」与「PrivacyInfo.xcprivacy（隐私清单）」两节配置说明。

## [1.1.0] - 2026-09-30

### 🚀 New Features

- 新增 `getAdvertisingIdentifier()` 获取广告标识符：
  - **iOS**：返回 IDFA（`ASIdentifierManager`）。需先 `requestPermissionIfNecessary()` 授权 ATT，未授权时系统返回全零。
  - **Android**：不支持，返回 `''`（如需 OAID 请自行接入 MSA OAID SDK，并通过 `register` 的 `androidPrivacy.oaid` 传入）。

## [1.0.1] - 2026-09-24

### 🔧 Changed

- 穿山甲 SDK 升级，与 flutter_unionad 3.0.0 对齐：
  - Android `com.pangle_beta.cn:mediation-sdk` 7.7.1.6 → **7.8.1.4**
  - iOS `Ads-CN-Beta`（BUAdSDK + CSJMediation-Only）7.8.0.0 → **7.8.0.5**

## [1.0.0] - 2026-09-06

### 🚀 New Features

- 首个正式版本。完整移植 Flutter 插件 [flutter_unionad](https://github.com/gstory0404/flutter_unionad) 的全部广告能力到 React Native **新架构**（Fabric + TurboModules），iOS + Android：
  - 广告类型：开屏（方法式全屏 `showSplashAd` + 视图版 `<PlaynestSplashAd>`）、Banner、信息流原生、Draw 信息流、激励视频、全屏/插屏。
  - 通用能力：`register`（含 Android 隐私控制 `androidPrivacy`、流量分组 `userInfo`）、`getSDKVersion`、`getThemeStatus`、`requestPermissionIfNecessary`（iOS ATT）。
- 穿山甲 SDK：Android `mediation-sdk` 7.7.1.6，iOS `Ads-CN-Beta` 7.8.0.0。
- 许可证 Apache-2.0（移植自 flutter_unionad，见 [NOTICE](NOTICE)）。

[Unreleased]: https://github.com/playnesthq/react-native-playnest-unionad/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/playnesthq/react-native-playnest-unionad/compare/v1.1.1...v2.0.0
[1.1.1]: https://github.com/playnesthq/react-native-playnest-unionad/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/playnesthq/react-native-playnest-unionad/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/playnesthq/react-native-playnest-unionad/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/playnesthq/react-native-playnest-unionad/releases/tag/v1.0.0
