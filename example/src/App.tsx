/**
 * react-native-playnest-unionad 示例：覆盖全部广告能力，供接入者参考。
 *
 * 使用穿山甲官方测试 appId / 广告位（与 flutter_unionad example 一致）。
 * 注意：
 *  - 广告需在 arm64 真机 / arm64 模拟器 上测试；穿山甲无 x86 库。
 *  - iOS 调用「请求 ATT 权限」前，宿主 Info.plist 必须配 NSUserTrackingUsageDescription。
 *  - 测试位在模拟器上常无填充/频控，属正常现象。
 */
import { useState, type ReactNode } from 'react';
import {
  Button,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {
  register,
  UnionadGender,
  getSDKVersion,
  getThemeStatus,
  requestPermissionIfNecessary,
  loadRewardVideoAd,
  showRewardVideoAd,
  loadFullScreenVideoAd,
  showFullScreenVideoAd,
  showSplashAd,
  PlaynestBannerAd,
  PlaynestNativeAd,
  PlaynestDrawAd,
  PlaynestSplashAd,
} from 'react-native-playnest-unionad';

const APP_ID = '5750023';
const REWARD_CODE_ID = '103685185';
const FULLSCREEN_CODE_ID = '103687132';
const SPLASH_CODE_ID = '103687131';
const BANNER_CODE_ID = '103686668';
const NATIVE_CODE_ID = '103686791';
const DRAW_CODE_ID = '103687068';

export default function App() {
  const [log, setLog] = useState<string[]>([]);
  const [showBanner, setShowBanner] = useState(false);
  const [showNative, setShowNative] = useState(false);
  const [showDraw, setShowDraw] = useState(false);
  const [showSplashView, setShowSplashView] = useState(false);
  const append = (line: string) =>
    setLog((prev) => [`${new Date().toLocaleTimeString()}  ${line}`, ...prev]);

  const onRegister = async () => {
    try {
      const ok = await register({
        androidAppId: APP_ID,
        iosAppId: APP_ID,
        appName: 'PlaynestUnionadExample',
        debug: true,
        // Android 隐私控制（仅 Android；不传则用 SDK 默认）
        androidPrivacy: {
          isCanUseLocation: true,
          isCanUsePhoneState: true,
          isLimitPersonalAds: false,
          isProgrammaticRecommend: true,
        },
        // 流量分组（iOS + Android 均生效）
        userInfo: {
          userId: 'demo_user_001',
          age: 24,
          gender: UnionadGender.MALE,
          channel: 'appstore',
          userValueGroup: 'high',
          customInfos: { vip: '1' },
        },
      });
      append(`register -> ${ok ? '成功' : '失败'}`);
    } catch (e) {
      append(`register error: ${String(e)}`);
    }
  };

  const onVersion = async () => append(`SDK 版本: ${await getSDKVersion()}`);
  const onTheme = async () => append(`主题模式: ${await getThemeStatus()}`);
  const onPermission = async () =>
    append(`ATT 权限: ${await requestPermissionIfNecessary()}`);

  const onLoadReward = () => {
    append('激励视频: 开始加载…');
    const unsub = loadRewardVideoAd(
      { androidCodeId: REWARD_CODE_ID, iosCodeId: REWARD_CODE_ID },
      {
        onReady: () => append('激励视频: onReady'),
        onCache: () => append('激励视频: onCache'),
        onShow: () => append('激励视频: onShow'),
        onClick: () => append('激励视频: onClick'),
        onClose: () => {
          append('激励视频: onClose');
          unsub();
        },
        onSkip: () => append('激励视频: onSkip'),
        onVerify: (v) => append(`激励视频: onVerify ${JSON.stringify(v)}`),
        onRewardArrived: (v) =>
          append(`激励视频: onRewardArrived ${JSON.stringify(v)}`),
        onEcpm: (info) => append(`激励视频: onEcpm ${JSON.stringify(info)}`),
        onFail: (e) => {
          append(`激励视频: onFail ${e.error}`);
          unsub();
        },
        onUnReady: (e) => append(`激励视频: onUnReady ${e.error}`),
      }
    );
  };
  const onShowReward = () => showRewardVideoAd();

  const onLoadFull = () => {
    append('全屏插屏: 开始加载…');
    const unsub = loadFullScreenVideoAd(
      { androidCodeId: FULLSCREEN_CODE_ID, iosCodeId: FULLSCREEN_CODE_ID },
      {
        onReady: () => append('全屏插屏: onReady'),
        onShow: () => append('全屏插屏: onShow'),
        onClick: () => append('全屏插屏: onClick'),
        onClose: () => {
          append('全屏插屏: onClose');
          unsub();
        },
        onFinish: () => append('全屏插屏: onFinish'),
        onSkip: () => append('全屏插屏: onSkip'),
        onEcpm: (info) => append(`全屏插屏: onEcpm ${JSON.stringify(info)}`),
        onFail: (e) => {
          append(`全屏插屏: onFail ${e.error}`);
          unsub();
        },
        onUnReady: (e) => append(`全屏插屏: onUnReady ${e.error}`),
      }
    );
  };
  const onShowFull = () => showFullScreenVideoAd();

  const onSplash = () => {
    append('开屏: 开始加载并展示…');
    const unsub = showSplashAd(
      { androidCodeId: SPLASH_CODE_ID, iosCodeId: SPLASH_CODE_ID },
      {
        onShow: () => append('开屏: onShow'),
        onClick: () => append('开屏: onClick'),
        onSkip: () => {
          append('开屏: onSkip');
          unsub();
        },
        onFinish: () => {
          append('开屏: onFinish');
          unsub();
        },
        onEcpm: (info) => append(`开屏: onEcpm ${JSON.stringify(info)}`),
        onFail: (e) => {
          append(`开屏: onFail ${e.error}`);
          unsub();
        },
      }
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <Text style={styles.title}>react-native-playnest-unionad</Text>

        <Section title="基础">
          <Button title="1. 初始化 register" onPress={onRegister} />
          <Button title="2. 获取 SDK 版本" onPress={onVersion} />
          <Button title="3. 主题模式" onPress={onTheme} />
          <Button title="4. 请求 ATT 权限（iOS）" onPress={onPermission} />
        </Section>

        <Section title="激励视频">
          <Button title="加载激励视频" onPress={onLoadReward} />
          <Button title="展示激励视频" onPress={onShowReward} />
        </Section>

        <Section title="全屏 / 插屏">
          <Button title="加载全屏插屏" onPress={onLoadFull} />
          <Button title="展示全屏插屏" onPress={onShowFull} />
        </Section>

        <Section title="开屏">
          <Button title="开屏广告（方法式全屏）" onPress={onSplash} />
          <Button
            title={
              showSplashView ? '隐藏开屏视图版' : '开屏视图版（不传尺寸=全屏）'
            }
            onPress={() => setShowSplashView((v) => !v)}
          />
        </Section>
        {showSplashView && (
          <PlaynestSplashAd
            androidCodeId={SPLASH_CODE_ID}
            iosCodeId={SPLASH_CODE_ID}
            onShow={(e) => append(`开屏视图: onShow ${e.width}x${e.height}`)}
            onClick={() => append('开屏视图: onClick')}
            onSkip={() => {
              append('开屏视图: onSkip');
              setShowSplashView(false);
            }}
            onFinish={() => {
              append('开屏视图: onFinish');
              setShowSplashView(false);
            }}
            onFail={(e) => {
              append(`开屏视图: onFail ${e.error}`);
              setShowSplashView(false);
            }}
            onEcpm={(info) =>
              append(`开屏视图: onEcpm ${JSON.stringify(info)}`)
            }
          />
        )}

        <Section title="Banner">
          <Button
            title={showBanner ? '隐藏 Banner' : '显示 Banner'}
            onPress={() => setShowBanner((v) => !v)}
          />
        </Section>
        {showBanner && (
          <PlaynestBannerAd
            androidCodeId={BANNER_CODE_ID}
            iosCodeId={BANNER_CODE_ID}
            width={300}
            height={150}
            style={styles.centered}
            onShow={(e) => append(`Banner: onShow ${e.width}x${e.height}`)}
            onClick={() => append('Banner: onClick')}
            onFail={(e) => append(`Banner: onFail ${e.error}`)}
            onEcpm={(info) => append(`Banner: onEcpm ${JSON.stringify(info)}`)}
            onDislike={(e) => append(`Banner: onDislike ${e.reason}`)}
          />
        )}

        <Section title="信息流原生">
          <Button
            title={showNative ? '隐藏信息流' : '显示信息流'}
            onPress={() => setShowNative((v) => !v)}
          />
        </Section>
        {showNative && (
          <PlaynestNativeAd
            androidCodeId={NATIVE_CODE_ID}
            iosCodeId={NATIVE_CODE_ID}
            width={330}
            height={280}
            style={styles.centered}
            onShow={(e) => append(`信息流: onShow ${e.width}x${e.height}`)}
            onClick={() => append('信息流: onClick')}
            onFail={(e) => append(`信息流: onFail ${e.error}`)}
            onEcpm={(info) => append(`信息流: onEcpm ${JSON.stringify(info)}`)}
            onDislike={(e) => append(`信息流: onDislike ${e.reason}`)}
          />
        )}

        <Section title="Draw 信息流（沉浸式视频）">
          <Button
            title={showDraw ? '隐藏 Draw' : '显示 Draw'}
            onPress={() => setShowDraw((v) => !v)}
          />
        </Section>
        {showDraw && (
          <PlaynestDrawAd
            androidCodeId={DRAW_CODE_ID}
            iosCodeId={DRAW_CODE_ID}
            width={340}
            height={500}
            style={styles.centered}
            onShow={(e) => append(`Draw: onShow ${e.width}x${e.height}`)}
            onClick={() => append('Draw: onClick')}
            onFail={(e) => append(`Draw: onFail ${e.error}`)}
            onEcpm={(info) => append(`Draw: onEcpm ${JSON.stringify(info)}`)}
            onDislike={(e) => append(`Draw: onDislike ${e.reason}`)}
            onVideoPlay={() => append('Draw: onVideoPlay')}
            onVideoPause={() => append('Draw: onVideoPause')}
            onVideoStop={() => append('Draw: onVideoStop')}
          />
        )}

        <View style={styles.logBox}>
          {log.map((l, i) => (
            <Text key={i} style={styles.logLine}>
              {l}
            </Text>
          ))}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, paddingTop: 40, paddingHorizontal: 16 },
  scrollContent: { paddingBottom: 60 },
  title: { fontSize: 18, fontWeight: '700', marginBottom: 12 },
  section: { marginTop: 12, gap: 8 },
  sectionTitle: { fontSize: 13, fontWeight: '600', color: '#666' },
  centered: { alignSelf: 'center', marginTop: 8 },
  logBox: {
    marginTop: 16,
    minHeight: 200,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#ccc',
    padding: 8,
  },
  logLine: { fontSize: 12, marginBottom: 4 },
});
