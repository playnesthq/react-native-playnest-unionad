import {
  TurboModuleRegistry,
  type TurboModule,
  type CodegenTypes,
} from 'react-native';
// 注意：事件类型必须用 CodegenTypes.EventEmitter / CodegenTypes.Int32 的“限定名”直接书写，
// 不要 `type EventEmitter<T> = CodegenTypes.EventEmitter<T>` 这样起本地别名——codegen 解析
// 事件类型时按注解名查找本地类型别名，同名别名会自引用导致死循环、卡住 codegen。

/**
 * 统一广告事件负载。所有广告类型共用一个事件流 onAdEvent，通过 adType + onAdMethod 区分。
 * 复杂结构（如 eCPM 信息）以 JSON 字符串放在 ecpm 字段，由 JS 层解析还原。
 */
export type UnionadNativeEvent = {
  /** 广告类型，如 rewardAd / fullScreenVideoAdInteraction */
  adType: string;
  /** 回调方法名，如 onReady/onShow/onClick/onClose/onVerify/onRewardArrived/onSkip/onFail/onEcpm/onCache/onUnReady/onFinish */
  onAdMethod: string;
  error?: string;
  errorCode?: CodegenTypes.Int32;
  rewardVerify?: boolean;
  rewardAmount?: CodegenTypes.Int32;
  rewardName?: string;
  rewardType?: CodegenTypes.Int32;
  /** 奖励额外参数（propose，字符串化） */
  propose?: string;
  /** eCPM 信息，JSON 字符串 */
  ecpm?: string;
};

export interface Spec extends TurboModule {
  /**
   * 初始化穿山甲 SDK
   * @param config 初始化配置（appId 等），见 index.tsx 的 UnionadConfig
   */
  register(config: Object): Promise<boolean>;

  /** 获取穿山甲 SDK 版本号 */
  getSDKVersion(): Promise<string>;

  /** 获取主题模式：0 正常(日间)，1 夜间 */
  getThemeStatus(): Promise<number>;

  /**
   * 请求权限。iOS：ATT 广告跟踪授权(iOS14+)，返回 0 未确定/1 受限/2 拒绝/3 已授权；
   * Android：请求穿山甲必要权限，返回 3。
   */
  requestPermissionIfNecessary(): Promise<number>;

  /**
   * 预加载激励视频广告。全过程通过 onAdEvent 事件回调（adType=rewardAd）。
   * @param config 见 index.tsx 的 RewardVideoOptions
   */
  loadRewardVideoAd(config: Object): Promise<boolean>;

  /** 展示已预加载的激励视频广告。 */
  showRewardVideoAd(): Promise<boolean>;

  /**
   * 预加载新模板渲染插屏（全屏/插屏二合一）。全过程通过 onAdEvent 事件回调
   * （adType=fullScreenVideoAdInteraction）。
   * @param config 见 index.tsx 的 FullScreenVideoOptions
   */
  loadFullScreenVideoAd(config: Object): Promise<boolean>;

  /** 展示已预加载的全屏/插屏广告。 */
  showFullScreenVideoAd(): Promise<boolean>;

  /**
   * 加载并全屏展示开屏广告。全过程通过 onAdEvent 事件回调（adType=splashAd）。
   * @param config 见 index.tsx 的 SplashOptions
   */
  showSplashAd(config: Object): Promise<boolean>;

  /** 统一广告事件流（所有广告类型） */
  readonly onAdEvent: CodegenTypes.EventEmitter<UnionadNativeEvent>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('PlaynestUnionad');
