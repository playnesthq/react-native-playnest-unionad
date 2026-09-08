# react-native-playnest-unionad

React Native 穿山甲(UnionAD)广告插件。完整移植自 Flutter 插件 [flutter_unionad](https://github.com/gstory0404/flutter_unionad)，覆盖其全部广告能力：

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

穿山甲 SDK 通过 CocoaPods 引入（`Ads-CN-Beta/BUAdSDK` + `Ads-CN-Beta/CSJMediation-Only` 7.8.0.0，随本库 podspec 自动带入），来自 CocoaPods 官方 CDN，通常无需额外 source。安装：

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

### 3. Info.plist：允许 HTTP 素材（ATS，建议）

穿山甲部分广告素材/落地页可能走 HTTP，iOS 的 ATS 默认禁止明文请求，不放开可能导致部分素材加载不全。建议在 `Info.plist` 加 ATS 例外：

```xml
<key>NSAppTransportSecurity</key>
<dict>
  <key>NSAllowsArbitraryLoads</key>
  <true/>
</dict>
```

> ⚠️ `NSAllowsArbitraryLoads=true` 是全局放开,App Store 审核可能要求说明理由。若担心审核,可改用更精细的 `NSExceptionDomains` 只对穿山甲相关域名放行(见穿山甲官方 iOS 文档的域名清单)。基本的广告展示不加也能跑(主素材是 HTTPS),但为完整加载建议配置。

### 4. arm64 模拟器

本库 podspec **未锁定** `x86_64`，`Ads-CN-Beta` 自带 `arm64_x86_64-simulator` 切片，可在 Apple Silicon 上直接跑 arm64 模拟器调试广告（免真机、免签名）。

### 5. 最低系统版本

`Podfile` 里 `platform :ios, '12.0'`（或更高）。

---

## Android 接入

### 1. 添加穿山甲 maven 源（**必配**）

本库依赖的穿山甲 SDK（`com.pangle_beta.cn:mediation-sdk`）来自字节跳动的 maven 仓库。请在**宿主工程根目录** `android/build.gradle` 的 `allprojects.repositories` 添加：

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

### 3. AndroidManifest 的 `<application>` 配置

在 `android/app/src/main/AndroidManifest.xml` 的 `<application>` 上：

- **允许 HTTP 素材（建议）**：穿山甲部分广告素材/落地页走 HTTP，Android 9+ 默认禁明文，不开可能导致部分素材加载不全 → 加 `android:usesCleartextTraffic="true"`。
- **`allowBackup` 冲突（如报清单合并错误再配）**：若 SDK 的 `android:allowBackup=true` 与你 App 冲突 → 加 `tools:replace="android:allowBackup"`。

```xml
<manifest xmlns:tools="http://schemas.android.com/tools" ...>
    <application
        android:usesCleartextTraffic="true"
        tools:replace="android:allowBackup"
        ...>
```

> 基本广告展示不加 `usesCleartextTraffic` 也能跑（主素材是 HTTPS），但为完整加载素材建议开启。若你已用 `network_security_config` 精细管控明文，也可在其中只放行穿山甲域名。

### 4. CPU 架构 / 模拟器

穿山甲原生库只含 `arm64-v8a` / `armeabi-v7a`。**Intel 模拟器无法加载**，请用真机或 **arm64 模拟器**（Apple Silicon 上的 Android Studio 模拟器即为 arm64）。可选：在 app `build.gradle` 用 `abiFilters 'arm64-v8a'` 精简包体。

### 5. 可选权限（按需自取，默认不加）

本库只声明**必需权限**（`INTERNET` / `WAKE_LOCK` / `ACCESS_NETWORK_STATE`）。穿山甲官方另有一批**可选权限**，用于防作弊与提升广告填充/定向——**加了能提升变现效果，不加广告也能正常展示**。是否添加由你的 App 自行权衡（变现收益 vs 隐私合规 / 商店审核），在宿主 `android/app/src/main/AndroidManifest.xml` 声明：

```xml
<!-- 可选：防作弊 + 提升广告填充/定向，按需自取 -->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
<uses-permission android:name="android.permission.GET_TASKS" />
<!-- QUERY_ALL_PACKAGES：判定广告应用是否已安装以提升体验。⚠️ Google Play 严格管控，
     需单独申报用途，且必须在隐私政策中声明，慎用（国内商店影响较小） -->
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

> 这些均为敏感/危险权限，会影响应用商店审核与隐私合规。建议**只加你确实需要的**，并在隐私政策中如实声明。

### 6. 下载类广告的 FileProvider（可选，默认不需要）

穿山甲官方文档建议为下载类广告配置 `TTFileProvider`。但该 provider 继承旧版 `android.support.v4.content.FileProvider`，在**纯 AndroidX（未开启 jetifier）** 的 RN 工程中声明它会导致启动崩溃。因此**本库默认不声明**该 provider —— 开屏 / 激励 / 插屏 / Banner / 信息流 / Draw 的展示都不需要它。

如你确实需要下载类广告的文件共享，请在宿主 App 侧启用 jetifier（`android/gradle.properties` 加 `android.enableJetifier=true`）后，自行按官方文档声明 `TTFileProvider` 与 `res/xml/pangle_file_paths.xml`（本库已内置该 xml 资源可复用）。

---

# 使用指南（可直接复制）

以下代码段包含每个接口的**全部参数与全部回调**，复制后按需删改即可。测试用 appId / 广告位见文末。

## 一、初始化 SDK

**调用任何广告接口前，必须先 `await register()` 成功。** 建议在 App 启动时执行一次。

```tsx
import { register, UnionadTheme } from 'react-native-playnest-unionad';

async function initAd() {
  const ok = await register({
    androidAppId: '5750023',          // 必填：Android appId
    iosAppId: '5750023',              // 必填：iOS appId
    appName: 'MyApp',                 // 选填：应用名
    useMediation: true,               // 选填：启用聚合能力，默认 true。用穿山甲广告位保持默认即可
    paid: false,                      // 选填：是否计费用户，默认 false
    keywords: '',                     // 选填：用户画像关键词
    allowShowNotify: true,            // 选填：允许 SDK 弹通知，默认 true
    debug: __DEV__,                   // 选填：debug 日志，默认 false
    supportMultiProcess: false,       // 选填：多进程，默认 false（仅 Android）
    themeStatus: UnionadTheme.DAY,    // 选填：主题 DAY(0)/NIGHT(1)，默认 DAY
    iosPrivacy: {                     // 选填：iOS 隐私合规（聚合维度）
      limitPersonalAds: false,        //   是否限制个性化广告，默认 false
      limitProgrammaticAds: false,    //   是否限制程序化广告，默认 false
      forbiddenCAID: false,           //   是否禁止 IDFA/CAID，默认 false
    },
    // 选填：Android 隐私信息控制（仅 Android；不传则用 SDK 默认，传了才注入 TTCustomController）
    androidPrivacy: {
      isCanUseLocation: false,        //   是否允许 SDK 主动使用地理位置，默认 false
      lat: 0, lon: 0,                 //   isCanUseLocation=false 时可传入经纬度
      isCanUsePhoneState: false,      //   是否允许使用手机硬件参数(imei)，默认 false
      imei: '',
      isCanUseWifiState: false, macAddress: '',
      isCanUseWriteExternal: false,
      oaid: '',
      alist: false,                   //   是否允许采集应用安装列表，默认 false
      isCanUseAndroidId: false, androidId: '',
      isCanUsePermissionRecordAudio: false,
      isLimitPersonalAds: false,      //   是否限制个性化推荐，默认 false
      isProgrammaticRecommend: false, //   是否启用程序化广告推荐，默认 false
      userPrivacyConfig: {},
    },
    // 选填：流量分组（iOS + Android 均生效；不传则不下发 Segment）
    userInfo: {
      userId: '',                     //   设备 ID(开发者自定义)
      age: 0,
      gender: UnionadGender.UNSET,    //   0 女 /1 男 /2 未知 /3 不使用
      channel: '', subChannel: '',
      userValueGroup: '',             //   分组
      customInfos: {},                //   自定义参数 Record<string,string>
    },
  });
  console.log('穿山甲初始化', ok ? '成功' : '失败');
}
```

> `androidPrivacy` 每个字段都是可选的，只需写你要控制的项；不传 `androidPrivacy` 则完全用 SDK 默认行为。`userInfo` 同理。导入 `UnionadGender` 使用性别常量。

## 二、SDK 版本 / 主题模式

```tsx
import { getSDKVersion, getThemeStatus } from 'react-native-playnest-unionad';

const version = await getSDKVersion();       // 例如 "7.7.1.6"
const theme = await getThemeStatus();        // 0 日间 / 1 夜间
```

## 三、请求 ATT 权限（iOS）

> **iOS 调用前，宿主 `Info.plist` 必须配置 `NSUserTrackingUsageDescription`，否则崩溃。**
> Android 不需要此步，恒返回 `3(authorized)`。

```tsx
import {
  requestPermissionIfNecessary,
  UnionadPermission,
} from 'react-native-playnest-unionad';

const status = await requestPermissionIfNecessary();
// status 取值见 UnionadPermission：
//   0 notDetermined 未确定 / 1 restricted 受限 / 2 denied 拒绝 / 3 authorized 已授权
if (status === UnionadPermission.authorized) {
  console.log('已授权广告跟踪');
}
```

## 四、激励视频

先 `loadRewardVideoAd` 预加载（回调驱动全过程），`onReady` 后再 `showRewardVideoAd` 展示。

```tsx
import {
  loadRewardVideoAd,
  showRewardVideoAd,
} from 'react-native-playnest-unionad';

// 预加载。返回取消订阅函数，广告生命周期结束后调用以移除监听。
const unsub = loadRewardVideoAd(
  {
    androidCodeId: '103685185',   // 必填：Android 广告位 id
    iosCodeId: '103685185',       // 必填：iOS 广告位 id
    rewardName: '金币',           // 选填：奖励名称
    rewardAmount: 1,              // 选填：奖励数量，默认 1
    userID: 'user_123',           // 选填：用户 id（服务端奖励验证用）
    mediaExtra: '',               // 选填：服务端奖励验证透传参数
    orientation: 0,               // 选填：0 竖屏 / 1 横屏，默认 0
    mutedIfCan: true,             // 选填：是否静音，默认 true
  },
  {
    onReady: () => {              // 物料加载完成，可展示
      showRewardVideoAd();        //   这里直接展示；也可存标志位延后展示
    },
    onCache: () => {},            // 视频文件缓存完成
    onShow: () => {},             // 广告展示
    onClick: () => {},            // 广告点击
    onClose: () => { unsub(); },  // 广告关闭 → 取消监听
    onSkip: () => {},             // 跳过视频
    onVerify: (v) => {            // 奖励验证（旧版回调）
      // v: { rewardVerify, rewardAmount, rewardName, errorCode, error }
    },
    onRewardArrived: (v) => {     // 奖励到账（新版回调，★推荐以此发奖）
      // v: { rewardVerify, rewardAmount, rewardName, errorCode, error, rewardType?, propose? }
      if (v.rewardVerify) {
        // 发放奖励
      }
    },
    onFail: (e) => { console.warn(e.error); unsub(); }, // 加载/渲染失败
    onUnReady: (e) => {},         // 未加载完成就调用了展示
    onEcpm: (info) => {           // eCPM 信息（info 见文末 EcpmInfo）
      console.log('reward ecpm', info?.ecpm);
    },
  }
);

// 展示已预加载的激励视频（通常在 onReady 内调用）
await showRewardVideoAd();
```

## 五、全屏视频 / 插屏（二合一）

```tsx
import {
  loadFullScreenVideoAd,
  showFullScreenVideoAd,
  UnionadOrientation,
} from 'react-native-playnest-unionad';

const unsub = loadFullScreenVideoAd(
  {
    androidCodeId: '103687132',              // 必填：Android 广告位 id
    iosCodeId: '103687132',                  // 必填：iOS 广告位 id
    orientation: UnionadOrientation.VERTICAL, // 选填：VERTICAL(1)/HORIZONTAL(2)，默认竖屏。仅 Android
  },
  {
    onReady: () => { showFullScreenVideoAd(); }, // 加载/缓存完成，可展示
    onShow: () => {},                            // 广告展示
    onClick: () => {},                           // 广告点击
    onClose: () => { unsub(); },                 // 广告关闭 → 取消监听
    onFinish: () => {},                          // 视频播放完成
    onSkip: () => {},                            // 跳过视频
    onFail: (e) => { console.warn(e.error); unsub(); }, // 加载/渲染失败
    onUnReady: (e) => {},                        // 未加载完成就调用了展示
    onEcpm: (info) => {},                        // eCPM 信息
  }
);

await showFullScreenVideoAd();
```

## 六、开屏（方法式全屏）

一次调用即「加载 + 全屏展示」，无需单独 show。

```tsx
import { showSplashAd } from 'react-native-playnest-unionad';

const unsub = showSplashAd(
  {
    androidCodeId: '103687131',   // 必填：Android 广告位 id
    iosCodeId: '103687131',       // 必填：iOS 广告位 id
    timeout: 3000,                // 选填：加载超时(ms)，默认 3000。仅 Android
    width: 0,                     // 选填：期望宽度(dp/pt)，0 表示全屏，默认 0
    height: 0,                    // 选填：期望高度(dp/pt)，0 表示全屏，默认 0
    isShake: false,               // 选填：是否支持摇一摇，默认 false
    supportDeepLink: true,        // 选填：是否支持 DeepLink，默认 true。仅 Android
  },
  {
    onShow: () => {},                          // 广告展示
    onClick: () => {},                         // 广告点击
    onSkip: () => { unsub(); },                // 用户点击跳过 → 取消监听
    onFinish: () => { unsub(); },              // 倒计时结束正常关闭 → 取消监听
    onFail: (e) => { console.warn(e.error); unsub(); }, // 加载/渲染失败
    onEcpm: (info) => {},                      // eCPM 信息
  }
);
```

## 七、Banner（视图组件）

视图类广告是 React 组件，直接放进布局即可。**必须给尺寸**（`width/height` 或 `style`），否则不可见。

```tsx
import { PlaynestBannerAd } from 'react-native-playnest-unionad';

<PlaynestBannerAd
  androidCodeId="103686668"      // 必填：Android 广告位 id
  iosCodeId="103686668"          // 必填：iOS 广告位 id
  width={300}                    // 选填：宽度 dp（同时作为容器宽度）
  height={150}                   // 选填：高度 dp（同时作为容器高度）
  style={{ alignSelf: 'center' }}// 选填：容器样式
  onShow={(e) => {               // 渲染/展示成功，携带实际宽高
    // e: { width: number, height: number }
  }}
  onClick={() => {}}             // 广告点击
  onFail={(e) => {               // 加载/渲染失败
    // e: { error: string }
  }}
  onEcpm={(info) => {}}          // eCPM 信息（info 见文末 EcpmInfo）
  onDislike={(e) => {            // 点击不感兴趣（广告已移除）
    // e: { reason: string }
  }}
/>
```

## 八、信息流原生（模板渲染，视图组件）

```tsx
import { PlaynestNativeAd } from 'react-native-playnest-unionad';

<PlaynestNativeAd
  androidCodeId="103686791"      // 必填
  iosCodeId="103686791"          // 必填
  width={330}                    // 选填：宽度 dp
  height={280}                   // 选填：高度 dp
  isMuted={true}                 // 选填：视频广告是否静音，默认 true
  style={{ alignSelf: 'center' }}
  onShow={(e) => {}}             // e: { width, height }
  onClick={() => {}}
  onFail={(e) => {}}             // e: { error }
  onEcpm={(info) => {}}
  onDislike={(e) => {}}          // e: { reason }
/>
```

## 九、Draw 信息流（沉浸式视频，视图组件）

在信息流基础上多了 3 个视频状态回调。

```tsx
import { PlaynestDrawAd } from 'react-native-playnest-unionad';

<PlaynestDrawAd
  androidCodeId="103687068"      // 必填
  iosCodeId="103687068"          // 必填
  width={340}                    // 选填：宽度 dp
  height={500}                   // 选填：高度 dp
  isMuted={true}                 // 选填：是否静音，默认 true
  style={{ alignSelf: 'center' }}
  onShow={(e) => {}}             // e: { width, height }
  onClick={() => {}}
  onFail={(e) => {}}             // e: { error }
  onEcpm={(info) => {}}
  onDislike={(e) => {}}          // e: { reason }
  onVideoPlay={() => {}}         // 视频开始播放
  onVideoPause={() => {}}        // 视频暂停
  onVideoStop={() => {}}         // 视频停止
/>
```

## 十、开屏（视图版，对齐 flutter splashAdView）

与方法式开屏并存。**不传尺寸 = 全屏；传了尺寸 = 按区域内嵌**（底部可留 logo 区）。

```tsx
import { PlaynestSplashAd } from 'react-native-playnest-unionad';

<PlaynestSplashAd
  androidCodeId="103687131"      // 必填
  iosCodeId="103687131"          // 必填
  // 不传 width/height => 全屏；传了 => 指定区域
  width={undefined}              // 选填：宽度 dp
  height={undefined}             // 选填：高度 dp
  timeout={3000}                 // 选填：加载超时(ms)，默认 3000。仅 Android
  isShake={false}                // 选填：是否支持摇一摇，默认 false
  supportDeepLink={true}         // 选填：是否支持 DeepLink，默认 true。仅 Android
  onShow={(e) => {}}             // e: { width, height }
  onClick={() => {}}             // 广告点击
  onSkip={() => {}}              // 用户点击跳过
  onFinish={() => {}}            // 倒计时结束正常关闭
  onFail={(e) => {}}             // e: { error }
  onEcpm={(info) => {}}          // eCPM 信息
/>
```

## eCPM 信息（EcpmInfo）

`onEcpm(info)` 回调的 `info` 为聚合维度对象（部分字段依 ADN 而定），常用字段：

```ts
interface EcpmInfo {
  adnName?: string;       // ADN 名称，如 "pangle"
  slotID?: string;        // 广告位 id
  ecpm?: string;          // eCPM 价格（字符串）
  biddingType?: number;   // 竞价类型
  requestID?: string;     // 请求 id
  creativeID?: string;    // 创意 id
  adRitType?: string;     // 广告类型，如 "splash" / "feed"
  subRitType?: string;    // 细分类型
  segmentId?: string;     // 流量分组 id
  // …更多字段见类型定义
}
```

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
- **广告位不合法（40006）** → 广告位 id 或 appId 不对/不匹配。请使用穿山甲后台创建的广告位，并确保与 `register` 传入的 appId 对应。测试阶段用文末官方测试广告位。
- **视图广告有 `onShow` 却看不到** → 确认给了尺寸（`width/height` 或 `style`）；开屏视图不传尺寸时才默认全屏。
- **只出现无填充** → 模拟器 / 测试位特性，换真机或正式广告位。

完整可运行示例见 [example/src/App.tsx](example/src/App.tsx)。

---

## 维护者

本库移植自 Flutter 插件 flutter_unionad。两者的能力对应关系、移植决策，以及 flutter_unionad 有新提交时如何快速同步，见 [docs/PORTING.md](docs/PORTING.md)。

## 许可证

**Apache License 2.0** · playnest

本项目移植自 [flutter_unionad](https://github.com/gstory0404/flutter_unionad)（Apache-2.0），衍生说明见 [NOTICE](NOTICE)。
