import {
  codegenNativeComponent,
  type ViewProps,
  type HostComponent,
  type CodegenTypes,
} from 'react-native';

// Draw 信息流（沉浸式视频流）广告视图。事件与信息流一致，另加视频播放状态事件。
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
  /** 视频开始播放 */
  onAdVideoPlay?: CodegenTypes.DirectEventHandler<Readonly<{}>>;
  /** 视频暂停 */
  onAdVideoPause?: CodegenTypes.DirectEventHandler<Readonly<{}>>;
  /** 视频停止 */
  onAdVideoStop?: CodegenTypes.DirectEventHandler<Readonly<{}>>;
}

export default codegenNativeComponent<NativeProps>(
  'PlaynestDrawView'
) as HostComponent<NativeProps>;
