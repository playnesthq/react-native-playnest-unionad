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

### 🔧 Changed

- example 采用 UIScene 生命周期（新增 `SceneDelegate`），作为 iOS UIScene 适配参考。库本身已兼容 UIScene（通过 `UIApplication.connectedScenes` 定位活动 scene 的 window 展示广告），无需改动。

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

[Unreleased]: https://github.com/playnesthq/react-native-playnest-unionad/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/playnesthq/react-native-playnest-unionad/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/playnesthq/react-native-playnest-unionad/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/playnesthq/react-native-playnest-unionad/releases/tag/v1.0.0
