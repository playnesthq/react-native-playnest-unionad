import {
  codegenNativeComponent,
  type ViewProps,
  type HostComponent,
  type CodegenTypes,
} from 'react-native';

export interface NativeProps extends ViewProps {
  /** Android 广告位 id 必填 */
  androidCodeId: string;
  /** iOS 广告位 id 必填 */
  iosCodeId: string;
  /** 期望模板宽度 dp */
  expressWidth: CodegenTypes.Double;
  /** 期望模板高度 dp */
  expressHeight: CodegenTypes.Double;
  /** 视频广告是否静音，默认 true */
  isMuted?: CodegenTypes.WithDefault<boolean, true>;
  /** 广告渲染/展示成功 */
  onAdShow?: CodegenTypes.DirectEventHandler<
    Readonly<{ width: CodegenTypes.Double; height: CodegenTypes.Double }>
  >;
  /** 广告点击 */
  onAdClick?: CodegenTypes.DirectEventHandler<Readonly<{}>>;
  /** 加载/渲染失败 */
  onAdFail?: CodegenTypes.DirectEventHandler<Readonly<{ error: string }>>;
  /** eCPM 信息（JSON 字符串） */
  onAdEcpm?: CodegenTypes.DirectEventHandler<Readonly<{ ecpm: string }>>;
  /** 点击不感兴趣（关闭） */
  onAdDislike?: CodegenTypes.DirectEventHandler<Readonly<{ reason: string }>>;
}

export default codegenNativeComponent<NativeProps>(
  'RNUnionadNativeView'
) as HostComponent<NativeProps>;
