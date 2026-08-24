import {
  codegenNativeComponent,
  type ViewProps,
  type HostComponent,
  type CodegenTypes,
} from 'react-native';

// 开屏（Fabric 视图版，嵌入式，对齐 flutter splashAdView）。
export interface NativeProps extends ViewProps {
  /** Android 广告位 id 必填 */
  androidCodeId: string;
  /** iOS 广告位 id 必填 */
  iosCodeId: string;
  /** 期望宽度 dp */
  expressWidth: CodegenTypes.Double;
  /** 期望高度 dp */
  expressHeight: CodegenTypes.Double;
  /** 加载超时(ms)，默认 3000。仅 Android 生效 */
  timeout?: CodegenTypes.Int32;
  /** 是否支持摇一摇，默认 false */
  isShake?: CodegenTypes.WithDefault<boolean, false>;
  /** 是否支持 DeepLink，默认 true（仅 Android） */
  supportDeepLink?: CodegenTypes.WithDefault<boolean, true>;
  /** 广告展示 */
  onAdShow?: CodegenTypes.DirectEventHandler<
    Readonly<{ width: CodegenTypes.Double; height: CodegenTypes.Double }>
  >;
  /** 广告点击 */
  onAdClick?: CodegenTypes.DirectEventHandler<Readonly<{}>>;
  /** 用户点击跳过 */
  onAdSkip?: CodegenTypes.DirectEventHandler<Readonly<{}>>;
  /** 倒计时结束正常关闭 */
  onAdFinish?: CodegenTypes.DirectEventHandler<Readonly<{}>>;
  /** 加载/渲染失败 */
  onAdFail?: CodegenTypes.DirectEventHandler<Readonly<{ error: string }>>;
  /** eCPM 信息（JSON 字符串） */
  onAdEcpm?: CodegenTypes.DirectEventHandler<Readonly<{ ecpm: string }>>;
}

export default codegenNativeComponent<NativeProps>(
  'PlaynestSplashView'
) as HostComponent<NativeProps>;
