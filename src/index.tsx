import { View, type StyleProp, type ViewStyle } from 'react-native';
import PlaynestUnionad, {
  type UnionadNativeEvent,
} from './NativePlaynestUnionad';
import PlaynestBannerNativeView from './PlaynestBannerViewNativeComponent';
import PlaynestNativeNativeView from './PlaynestNativeViewNativeComponent';
import PlaynestDrawNativeView from './PlaynestDrawViewNativeComponent';
import PlaynestSplashNativeView from './PlaynestSplashViewNativeComponent';

/** 主题模式 */
export const UnionadTheme = {
  /** 日间 */
  DAY: 0,
  /** 夜间 */
  NIGHT: 1,
} as const;

/** 穿山甲 SDK 初始化配置 */
export interface UnionadConfig {
  /** Android appId 必填 */
  androidAppId: string;
  /** iOS appId 必填 */
  iosAppId: string;
  /** 应用名称 选填 */
  appName?: string;
  /** 是否使用聚合(GroMore)功能，默认 true */
  useMediation?: boolean;
  /** 是否为计费用户 选填 */
  paid?: boolean;
  /** 用户画像关键词 选填 */
  keywords?: string;
  /** 是否允许 SDK 弹出通知栏提示 选填，默认 true */
  allowShowNotify?: boolean;
  /** 是否打开 debug 日志 选填，默认 false */
  debug?: boolean;
  /** 是否支持多进程 选填，默认 false */
  supportMultiProcess?: boolean;
  /** 主题模式 见 UnionadTheme，默认 DAY */
  themeStatus?: number;
  /** iOS 隐私合规配置 */
  iosPrivacy?: IOSPrivacy;
  /** Android 隐私信息控制配置（仅 Android；不传则用 SDK 默认） */
  androidPrivacy?: AndroidPrivacy;
  /** 流量分组参数（聚合维度，iOS + Android；不传则不下发 Segment） */
  userInfo?: UnionadUserInfo;
}

/** AndroidPrivacy 各字段默认值（对齐 flutter_unionad toMap） */
const ANDROID_PRIVACY_DEFAULTS: Required<AndroidPrivacy> = {
  isCanUseLocation: false,
  lat: 0,
  lon: 0,
  isCanUsePhoneState: false,
  imei: '',
  isCanUseWifiState: false,
  macAddress: '',
  isCanUseWriteExternal: false,
  oaid: '',
  alist: false,
  isCanUseAndroidId: false,
  androidId: '',
  isCanUsePermissionRecordAudio: false,
  isLimitPersonalAds: false,
  isProgrammaticRecommend: false,
  userPrivacyConfig: {},
};

/** UnionadUserInfo 各字段默认值（对齐 flutter_unionad toMap） */
const USER_INFO_DEFAULTS: Required<UnionadUserInfo> = {
  userId: '',
  age: 0,
  gender: 3, // UnionadGender.UNSET（此处用字面量避免前向引用）
  channel: '',
  subChannel: '',
  userValueGroup: '',
  customInfos: {},
};

/** iOS 隐私合规配置（聚合维度） */
export interface IOSPrivacy {
  /** 是否限制个性化广告，默认 false */
  limitPersonalAds?: boolean;
  /** 是否限制程序化广告，默认 false */
  limitProgrammaticAds?: boolean;
  /** 是否禁止 IDFA/CAID，默认 false */
  forbiddenCAID?: boolean;
}

/**
 * Android 隐私信息控制配置（仅 Android 生效）。
 * 仅当传入 `androidPrivacy` 时才会应用 TTCustomController；不传则用 SDK 默认行为。
 * 未显式设置的字段按下述默认值下发。
 */
export interface AndroidPrivacy {
  /** 是否允许 SDK 主动使用地理位置信息，默认 false */
  isCanUseLocation?: boolean;
  /** isCanUseLocation=false 时可传入的纬度，默认 0 */
  lat?: number;
  /** isCanUseLocation=false 时可传入的经度，默认 0 */
  lon?: number;
  /** 是否允许 SDK 主动使用手机硬件参数(如 imei)，默认 false */
  isCanUsePhoneState?: boolean;
  /** isCanUsePhoneState=false 时可传入的 imei，默认 "" */
  imei?: string;
  /** 是否允许 SDK 主动使用 ACCESS_WIFI_STATE 权限，默认 false */
  isCanUseWifiState?: boolean;
  /** isCanUseWifiState=false 时可传入的 Mac 地址，默认 "" */
  macAddress?: string;
  /** 是否允许 SDK 主动使用 WRITE_EXTERNAL_STORAGE 权限，默认 false */
  isCanUseWriteExternal?: boolean;
  /** 开发者可传入的 oaid，默认 "" */
  oaid?: string;
  /** 是否允许 SDK 主动获取设备应用安装列表，默认 false */
  alist?: boolean;
  /** 是否能获取 android id，默认 false */
  isCanUseAndroidId?: boolean;
  /** 开发者可传入的 android id，默认 "" */
  androidId?: string;
  /** 是否允许 SDK 在已授权情况下使用录音权限，默认 false */
  isCanUsePermissionRecordAudio?: boolean;
  /** 是否限制个性化推荐接口，默认 false */
  isLimitPersonalAds?: boolean;
  /** 是否启用程序化广告推荐，默认 false */
  isProgrammaticRecommend?: boolean;
  /** 自定义隐私配置，默认 {} */
  userPrivacyConfig?: Record<string, unknown>;
}

/** 性别（userInfo.gender） */
export const UnionadGender = {
  FEMALE: 0,
  MALE: 1,
  UNKNOWN: 2,
  /** 不使用 */
  UNSET: 3,
} as const;

/**
 * 流量分组参数（聚合维度，iOS + Android 均生效）。
 * 仅当传入 `userInfo` 时才会下发 Segment。
 */
export interface UnionadUserInfo {
  /** 设备 ID（开发者自定义，用于分组统计/测试），默认 "" */
  userId?: string;
  /** 年龄，默认 0 */
  age?: number;
  /** 性别，见 UnionadGender：0 女 / 1 男 / 2 未知 / 3 不使用，默认 3 */
  gender?: number;
  /** 渠道，建议 [A-Za-z0-9_]，默认 "" */
  channel?: string;
  /** 子渠道，建议 [A-Za-z0-9_]，默认 "" */
  subChannel?: string;
  /** 分组，默认 "" */
  userValueGroup?: string;
  /** 自定义参数，默认 {} */
  customInfos?: Record<string, string>;
}

/** 权限状态码（iOS ATT） */
export const UnionadPermission = {
  /** 未确定 */
  notDetermined: 0,
  /** 受限 */
  restricted: 1,
  /** 拒绝 */
  denied: 2,
  /** 已授权 */
  authorized: 3,
} as const;

/**
 * 初始化穿山甲 SDK。调用任何广告接口前必须先成功初始化。
 * @returns 是否初始化成功
 */
export function register(config: UnionadConfig): Promise<boolean> {
  const payload: Record<string, unknown> = {
    appName: '',
    useMediation: true,
    paid: false,
    keywords: '',
    allowShowNotify: true,
    debug: false,
    supportMultiProcess: false,
    themeStatus: UnionadTheme.DAY,
    ...config,
    iosPrivacy: {
      limitPersonalAds: false,
      limitProgrammaticAds: false,
      forbiddenCAID: false,
      ...config.iosPrivacy,
    },
  };
  // androidPrivacy / userInfo 仅在显式传入时下发（填齐默认字段，避免原生读取缺键）。
  // 不传则保持 SDK 默认行为，不注入 TTCustomController / Segment。
  if (config.androidPrivacy) {
    payload.androidPrivacy = {
      ...ANDROID_PRIVACY_DEFAULTS,
      ...config.androidPrivacy,
    };
  } else {
    delete payload.androidPrivacy;
  }
  if (config.userInfo) {
    payload.userInfo = { ...USER_INFO_DEFAULTS, ...config.userInfo };
  } else {
    delete payload.userInfo;
  }
  return PlaynestUnionad.register(payload);
}

/** 获取穿山甲 SDK 版本号 */
export function getSDKVersion(): Promise<string> {
  return PlaynestUnionad.getSDKVersion();
}

/** 获取主题模式：0 正常(日间)，1 夜间 */
export function getThemeStatus(): Promise<number> {
  return PlaynestUnionad.getThemeStatus();
}

/**
 * 请求权限。iOS 返回 ATT 授权状态（见 UnionadPermission），Android 返回 3。
 */
export function requestPermissionIfNecessary(): Promise<number> {
  return PlaynestUnionad.requestPermissionIfNecessary();
}

/* ============================ 激励视频 ============================ */

/** eCPM 信息（聚合维度，部分字段依 ADN 而定） */
export interface EcpmInfo {
  adnName?: string;
  customAdnName?: string;
  slotID?: string;
  levelTag?: string;
  ecpm?: string;
  biddingType?: number;
  errorMsg?: string;
  requestID?: string;
  creativeID?: string;
  adRitType?: string;
  segmentId?: string;
  abtestId?: string;
  channel?: string;
  sub_channel?: string;
  scenarioId?: string;
  subRitType?: string;
  [key: string]: unknown;
}

/** 激励视频加载参数 */
export interface RewardVideoOptions {
  /** Android 广告位 id 必填 */
  androidCodeId: string;
  /** iOS 广告位 id 必填 */
  iosCodeId: string;
  /** 奖励名称 选填 */
  rewardName?: string;
  /** 奖励数量 选填 */
  rewardAmount?: number;
  /** 用户 id（服务端奖励验证用）选填 */
  userID?: string;
  /** 服务端奖励验证透传参数 选填 */
  mediaExtra?: string;
  /** 横竖屏，0 竖屏 1 横屏，默认 0 */
  orientation?: number;
  /** 是否静音，默认 true */
  mutedIfCan?: boolean;
}

/** 奖励验证回调参数 */
export interface RewardVerify {
  rewardVerify: boolean;
  rewardAmount: number;
  rewardName: string;
  errorCode: number;
  error: string;
  /** onRewardArrived 独有 */
  rewardType?: number;
  propose?: string;
}

/** 激励视频回调集合 */
export interface RewardVideoCallback {
  /** 广告物料加载完成，可展示 */
  onReady?: () => void;
  /** 视频文件缓存完成 */
  onCache?: () => void;
  /** 广告展示 */
  onShow?: () => void;
  /** 广告点击 */
  onClick?: () => void;
  /** 广告关闭 */
  onClose?: () => void;
  /** 跳过视频 */
  onSkip?: () => void;
  /** 奖励验证（旧版回调） */
  onVerify?: (v: RewardVerify) => void;
  /** 奖励到账（新版回调，推荐以此发奖） */
  onRewardArrived?: (v: RewardVerify) => void;
  /** 加载/渲染失败 */
  onFail?: (e: { error: string }) => void;
  /** 未加载完成就调用展示 */
  onUnReady?: (e: { error: string }) => void;
  /** eCPM 信息回调 */
  onEcpm?: (info: EcpmInfo | null) => void;
}

/* ---- 统一事件分发：每种 adType 仅一个持久监听器，避免重复 load 造成监听器泄漏 ---- */
type AdEventHandler = (e: UnionadNativeEvent) => void;
const adEventHandlers: Record<string, AdEventHandler | undefined> = {};
let adEventSub: { remove(): void } | null = null;

function ensureAdEventSub() {
  if (adEventSub) return;
  adEventSub = PlaynestUnionad.onAdEvent((e: UnionadNativeEvent) => {
    adEventHandlers[e.adType]?.(e);
  });
}

/** 为某个 adType 注册/替换事件处理器，返回取消函数。 */
function setAdEventHandler(adType: string, handler: AdEventHandler): () => void {
  ensureAdEventSub();
  adEventHandlers[adType] = handler;
  return () => {
    if (adEventHandlers[adType] === handler) {
      adEventHandlers[adType] = undefined;
    }
  };
}

function parseEcpmString(ecpm?: string): EcpmInfo | null {
  if (!ecpm) return null;
  try {
    return JSON.parse(ecpm) as EcpmInfo;
  } catch {
    return null;
  }
}

function parseEcpm(e: UnionadNativeEvent): EcpmInfo | null {
  return parseEcpmString(e.ecpm);
}

function dispatchRewardEvent(e: UnionadNativeEvent, cb: RewardVideoCallback) {
  const verify: RewardVerify = {
    rewardVerify: e.rewardVerify ?? false,
    rewardAmount: e.rewardAmount ?? 0,
    rewardName: e.rewardName ?? '',
    errorCode: e.errorCode ?? 0,
    error: e.error ?? '',
    rewardType: e.rewardType,
    propose: e.propose,
  };
  switch (e.onAdMethod) {
    case 'onReady':
      cb.onReady?.();
      break;
    case 'onCache':
      cb.onCache?.();
      break;
    case 'onShow':
      cb.onShow?.();
      break;
    case 'onClick':
      cb.onClick?.();
      break;
    case 'onClose':
      cb.onClose?.();
      break;
    case 'onSkip':
      cb.onSkip?.();
      break;
    case 'onVerify':
      cb.onVerify?.(verify);
      break;
    case 'onRewardArrived':
      cb.onRewardArrived?.(verify);
      break;
    case 'onFail':
      cb.onFail?.({ error: e.error ?? '' });
      break;
    case 'onUnReady':
      cb.onUnReady?.({ error: e.error ?? '' });
      break;
    case 'onEcpm':
      cb.onEcpm?.(parseEcpm(e));
      break;
  }
}

/**
 * 预加载激励视频广告。加载/展示/奖励的全过程通过 callback 回调。
 * 返回一个取消订阅函数，建议在广告生命周期结束（onClose/onFail）后调用以移除监听。
 */
export function loadRewardVideoAd(
  options: RewardVideoOptions,
  callback: RewardVideoCallback = {}
): () => void {
  const unsub = setAdEventHandler('rewardAd', (e) =>
    dispatchRewardEvent(e, callback)
  );
  PlaynestUnionad.loadRewardVideoAd({
    rewardName: '',
    rewardAmount: 1,
    userID: '',
    mediaExtra: '',
    orientation: 0,
    mutedIfCan: true,
    ...options,
  });
  return unsub;
}

/** 展示已预加载的激励视频广告。 */
export function showRewardVideoAd(): Promise<boolean> {
  return PlaynestUnionad.showRewardVideoAd();
}

/* ==================== 全屏视频/插屏（二合一） ==================== */

/** 屏幕方向 */
export const UnionadOrientation = {
  /** 竖屏 */
  VERTICAL: 1,
  /** 横屏 */
  HORIZONTAL: 2,
} as const;

/** 全屏/插屏加载参数 */
export interface FullScreenVideoOptions {
  /** Android 广告位 id 必填 */
  androidCodeId: string;
  /** iOS 广告位 id 必填 */
  iosCodeId: string;
  /** 屏幕方向，见 UnionadOrientation，默认竖屏。仅 Android 生效 */
  orientation?: number;
}

/** 全屏/插屏回调集合 */
export interface FullScreenVideoCallback {
  /** 广告加载/缓存完成，可展示 */
  onReady?: () => void;
  /** 广告展示 */
  onShow?: () => void;
  /** 广告点击 */
  onClick?: () => void;
  /** 广告关闭 */
  onClose?: () => void;
  /** 视频播放完成 */
  onFinish?: () => void;
  /** 跳过视频 */
  onSkip?: () => void;
  /** 加载/渲染失败 */
  onFail?: (e: { error: string }) => void;
  /** 未加载完成就调用展示 */
  onUnReady?: (e: { error: string }) => void;
  /** eCPM 信息回调 */
  onEcpm?: (info: EcpmInfo | null) => void;
}

function dispatchFullScreenEvent(
  e: UnionadNativeEvent,
  cb: FullScreenVideoCallback
) {
  switch (e.onAdMethod) {
    case 'onReady':
      cb.onReady?.();
      break;
    case 'onShow':
      cb.onShow?.();
      break;
    case 'onClick':
      cb.onClick?.();
      break;
    case 'onClose':
      cb.onClose?.();
      break;
    case 'onFinish':
      cb.onFinish?.();
      break;
    case 'onSkip':
      cb.onSkip?.();
      break;
    case 'onFail':
      cb.onFail?.({ error: e.error ?? '' });
      break;
    case 'onUnReady':
      cb.onUnReady?.({ error: e.error ?? '' });
      break;
    case 'onEcpm':
      cb.onEcpm?.(parseEcpm(e));
      break;
  }
}

/**
 * 预加载新模板渲染插屏（全屏/插屏二合一）。加载/展示的全过程通过 callback 回调。
 * 返回一个取消订阅函数，建议在广告生命周期结束（onClose/onFail）后调用以移除监听。
 */
export function loadFullScreenVideoAd(
  options: FullScreenVideoOptions,
  callback: FullScreenVideoCallback = {}
): () => void {
  const unsub = setAdEventHandler('fullScreenVideoAdInteraction', (e) =>
    dispatchFullScreenEvent(e, callback)
  );
  PlaynestUnionad.loadFullScreenVideoAd({
    orientation: UnionadOrientation.VERTICAL,
    ...options,
  });
  return unsub;
}

/** 展示已预加载的全屏/插屏广告。 */
export function showFullScreenVideoAd(): Promise<boolean> {
  return PlaynestUnionad.showFullScreenVideoAd();
}

/* ==================== 开屏（全屏方法式） ==================== */

/** 开屏加载参数 */
export interface SplashOptions {
  /** Android 广告位 id 必填 */
  androidCodeId: string;
  /** iOS 广告位 id 必填 */
  iosCodeId: string;
  /** 加载超时(ms)，默认 3000。仅 Android 生效 */
  timeout?: number;
  /** 期望宽度(dp/pt)，0 表示全屏，默认 0 */
  width?: number;
  /** 期望高度(dp/pt)，0 表示全屏，默认 0 */
  height?: number;
  /** 是否支持摇一摇，默认 false */
  isShake?: boolean;
  /** 是否支持 DeepLink，默认 true（仅 Android） */
  supportDeepLink?: boolean;
}

/** 开屏回调集合 */
export interface SplashCallback {
  /** 广告展示 */
  onShow?: () => void;
  /** 广告点击 */
  onClick?: () => void;
  /** 用户点击跳过 */
  onSkip?: () => void;
  /** 倒计时结束正常关闭 */
  onFinish?: () => void;
  /** 加载/渲染失败 */
  onFail?: (e: { error: string }) => void;
  /** eCPM 信息回调 */
  onEcpm?: (info: EcpmInfo | null) => void;
}

function dispatchSplashEvent(e: UnionadNativeEvent, cb: SplashCallback) {
  switch (e.onAdMethod) {
    case 'onShow':
      cb.onShow?.();
      break;
    case 'onClick':
      cb.onClick?.();
      break;
    case 'onSkip':
      cb.onSkip?.();
      break;
    case 'onFinish':
      cb.onFinish?.();
      break;
    case 'onFail':
      cb.onFail?.({ error: e.error ?? '' });
      break;
    case 'onEcpm':
      cb.onEcpm?.(parseEcpm(e));
      break;
  }
}

/**
 * 加载并全屏展示开屏广告。加载/展示的全过程通过 callback 回调。
 * 返回一个取消订阅函数，建议在广告关闭（onFinish/onSkip/onFail）后调用以移除监听。
 */
export function showSplashAd(
  options: SplashOptions,
  callback: SplashCallback = {}
): () => void {
  const unsub = setAdEventHandler('splashAd', (e) =>
    dispatchSplashEvent(e, callback)
  );
  PlaynestUnionad.showSplashAd({
    timeout: 3000,
    width: 0,
    height: 0,
    isShake: false,
    supportDeepLink: true,
    ...options,
  });
  return unsub;
}

/* ==================== Banner（视图组件） ==================== */

/** Banner 广告参数 */
/** 视图类广告（Banner / 信息流）通用事件回调 */
export interface ViewAdCallbacks {
  /** 渲染/展示成功，携带实际宽高 */
  onShow?: (e: { width: number; height: number }) => void;
  /** 广告点击 */
  onClick?: () => void;
  /** 加载/渲染失败 */
  onFail?: (e: { error: string }) => void;
  /** eCPM 信息 */
  onEcpm?: (info: EcpmInfo | null) => void;
  /** 点击不感兴趣（广告已移除） */
  onDislike?: (e: { reason: string }) => void;
}

export interface BannerAdProps extends ViewAdCallbacks {
  /** Android 广告位 id 必填 */
  androidCodeId: string;
  /** iOS 广告位 id 必填 */
  iosCodeId: string;
  /** 期望宽度 dp（同时作为容器宽度）。不传则由 style 决定尺寸 */
  width?: number;
  /** 期望高度 dp（同时作为容器高度）。不传则由 style 决定尺寸 */
  height?: number;
  style?: StyleProp<ViewStyle>;
}

/** Banner 广告视图组件。 */
export function PlaynestBannerAd({
  androidCodeId,
  iosCodeId,
  width,
  height,
  style,
  onShow,
  onClick,
  onFail,
  onEcpm,
  onDislike,
}: BannerAdProps) {
  const sizeStyle =
    width != null || height != null ? { width, height } : undefined;
  return (
    <View style={[sizeStyle, style]}>
      <PlaynestBannerNativeView
        androidCodeId={androidCodeId}
        iosCodeId={iosCodeId}
        expressWidth={width ?? 0}
        expressHeight={height ?? 0}
        style={{ flex: 1 }}
        onAdShow={(e) => onShow?.(e.nativeEvent)}
        onAdClick={() => onClick?.()}
        onAdFail={(e) => onFail?.(e.nativeEvent)}
        onAdEcpm={(e) => onEcpm?.(parseEcpmString(e.nativeEvent.ecpm))}
        onAdDislike={(e) => onDislike?.(e.nativeEvent)}
      />
    </View>
  );
}

/* ==================== 信息流原生（视图组件） ==================== */

export interface NativeAdProps extends ViewAdCallbacks {
  /** Android 广告位 id 必填 */
  androidCodeId: string;
  /** iOS 广告位 id 必填 */
  iosCodeId: string;
  /** 期望宽度 dp（同时作为容器宽度）。不传则由 style 决定尺寸 */
  width?: number;
  /** 期望高度 dp（同时作为容器高度）。不传则由 style 决定尺寸 */
  height?: number;
  /** 视频广告是否静音，默认 true */
  isMuted?: boolean;
  style?: StyleProp<ViewStyle>;
}

/** 信息流原生广告视图组件。 */
export function PlaynestNativeAd({
  androidCodeId,
  iosCodeId,
  width,
  height,
  isMuted = true,
  style,
  onShow,
  onClick,
  onFail,
  onEcpm,
  onDislike,
}: NativeAdProps) {
  const sizeStyle =
    width != null || height != null ? { width, height } : undefined;
  return (
    <View style={[sizeStyle, style]}>
      <PlaynestNativeNativeView
        androidCodeId={androidCodeId}
        iosCodeId={iosCodeId}
        expressWidth={width ?? 0}
        expressHeight={height ?? 0}
        isMuted={isMuted}
        style={{ flex: 1 }}
        onAdShow={(e) => onShow?.(e.nativeEvent)}
        onAdClick={() => onClick?.()}
        onAdFail={(e) => onFail?.(e.nativeEvent)}
        onAdEcpm={(e) => onEcpm?.(parseEcpmString(e.nativeEvent.ecpm))}
        onAdDislike={(e) => onDislike?.(e.nativeEvent)}
      />
    </View>
  );
}

/* ==================== Draw 信息流（沉浸式视频，视图组件） ==================== */

export interface DrawAdProps extends ViewAdCallbacks {
  /** Android 广告位 id 必填 */
  androidCodeId: string;
  /** iOS 广告位 id 必填 */
  iosCodeId: string;
  /** 期望宽度 dp（同时作为容器宽度）。不传则由 style 决定尺寸 */
  width?: number;
  /** 期望高度 dp（同时作为容器高度）。不传则由 style 决定尺寸 */
  height?: number;
  /** 视频广告是否静音，默认 true */
  isMuted?: boolean;
  /** 视频开始播放 */
  onVideoPlay?: () => void;
  /** 视频暂停 */
  onVideoPause?: () => void;
  /** 视频停止 */
  onVideoStop?: () => void;
  style?: StyleProp<ViewStyle>;
}

/** Draw 信息流（沉浸式视频流）广告视图组件。 */
export function PlaynestDrawAd({
  androidCodeId,
  iosCodeId,
  width,
  height,
  isMuted = true,
  style,
  onShow,
  onClick,
  onFail,
  onEcpm,
  onDislike,
  onVideoPlay,
  onVideoPause,
  onVideoStop,
}: DrawAdProps) {
  const sizeStyle =
    width != null || height != null ? { width, height } : undefined;
  return (
    <View style={[sizeStyle, style]}>
      <PlaynestDrawNativeView
        androidCodeId={androidCodeId}
        iosCodeId={iosCodeId}
        expressWidth={width ?? 0}
        expressHeight={height ?? 0}
        isMuted={isMuted}
        style={{ flex: 1 }}
        onAdShow={(e) => onShow?.(e.nativeEvent)}
        onAdClick={() => onClick?.()}
        onAdFail={(e) => onFail?.(e.nativeEvent)}
        onAdEcpm={(e) => onEcpm?.(parseEcpmString(e.nativeEvent.ecpm))}
        onAdDislike={(e) => onDislike?.(e.nativeEvent)}
        onAdVideoPlay={() => onVideoPlay?.()}
        onAdVideoPause={() => onVideoPause?.()}
        onAdVideoStop={() => onVideoStop?.()}
      />
    </View>
  );
}

/* ==================== 开屏（Fabric 视图版，对齐 flutter splashAdView） ==================== */

export interface SplashAdViewProps {
  /** Android 广告位 id 必填 */
  androidCodeId: string;
  /** iOS 广告位 id 必填 */
  iosCodeId: string;
  /** 期望宽度 dp（同时作为容器宽度）。不传则由 style 决定尺寸 */
  width?: number;
  /** 期望高度 dp（同时作为容器高度）。不传则由 style 决定尺寸 */
  height?: number;
  /** 加载超时(ms)，默认 3000。仅 Android 生效 */
  timeout?: number;
  /** 是否支持摇一摇，默认 false */
  isShake?: boolean;
  /** 是否支持 DeepLink，默认 true（仅 Android） */
  supportDeepLink?: boolean;
  /** 广告展示，携带实际宽高 */
  onShow?: (e: { width: number; height: number }) => void;
  /** 广告点击 */
  onClick?: () => void;
  /** 用户点击跳过 */
  onSkip?: () => void;
  /** 倒计时结束正常关闭 */
  onFinish?: () => void;
  /** 加载/渲染失败 */
  onFail?: (e: { error: string }) => void;
  /** eCPM 信息 */
  onEcpm?: (info: EcpmInfo | null) => void;
  style?: StyleProp<ViewStyle>;
}

/**
 * 开屏广告视图组件（嵌入式，可在布局里摆放、底部留 logo 区）。
 * 若只需全屏展示，用方法式 showSplashAd 更简单。
 */
export function PlaynestSplashAd({
  androidCodeId,
  iosCodeId,
  width,
  height,
  timeout = 3000,
  isShake = false,
  supportDeepLink = true,
  style,
  onShow,
  onClick,
  onSkip,
  onFinish,
  onFail,
  onEcpm,
}: SplashAdViewProps) {
  const sizeStyle =
    width != null || height != null ? { width, height } : undefined;
  return (
    <View style={[sizeStyle, style]}>
      <PlaynestSplashNativeView
        androidCodeId={androidCodeId}
        iosCodeId={iosCodeId}
        expressWidth={width ?? 0}
        expressHeight={height ?? 0}
        timeout={timeout}
        isShake={isShake}
        supportDeepLink={supportDeepLink}
        style={{ flex: 1 }}
        onAdShow={(e) => onShow?.(e.nativeEvent)}
        onAdClick={() => onClick?.()}
        onAdSkip={() => onSkip?.()}
        onAdFinish={() => onFinish?.()}
        onAdFail={(e) => onFail?.(e.nativeEvent)}
        onAdEcpm={(e) => onEcpm?.(parseEcpmString(e.nativeEvent.ecpm))}
      />
    </View>
  );
}
