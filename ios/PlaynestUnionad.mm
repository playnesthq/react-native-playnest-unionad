#import "PlaynestUnionad.h"
#import <BUAdSDK/BUAdSDK.h>
#import <UIKit/UIKit.h>
#import <AppTrackingTransparency/AppTrackingTransparency.h>

#pragma mark - 获取当前可展示的 UIViewController

static UIViewController *PU_findBestViewController(UIViewController *vc) {
    if (vc.presentedViewController) {
        return PU_findBestViewController(vc.presentedViewController);
    } else if ([vc isKindOfClass:[UISplitViewController class]]) {
        UISplitViewController *svc = (UISplitViewController *)vc;
        return svc.viewControllers.count > 0 ? PU_findBestViewController(svc.viewControllers.lastObject) : vc;
    } else if ([vc isKindOfClass:[UINavigationController class]]) {
        UINavigationController *nvc = (UINavigationController *)vc;
        return nvc.viewControllers.count > 0 ? PU_findBestViewController(nvc.topViewController) : vc;
    } else if ([vc isKindOfClass:[UITabBarController class]]) {
        UITabBarController *tvc = (UITabBarController *)vc;
        return tvc.viewControllers.count > 0 ? PU_findBestViewController(tvc.selectedViewController) : vc;
    }
    return vc;
}

static UIViewController *PU_currentViewController(void) {
    UIViewController *root = nil;
    NSArray<UIWindow *> *windows = nil;
    if (@available(iOS 13.0, *)) {
        NSMutableArray<UIWindow *> *all = [NSMutableArray array];
        for (UIScene *scene in UIApplication.sharedApplication.connectedScenes) {
            if ([scene isKindOfClass:[UIWindowScene class]]) {
                [all addObjectsFromArray:((UIWindowScene *)scene).windows];
            }
        }
        windows = all;
    }
    for (UIWindow *w in windows) {
        if (w.isKeyWindow) { root = w.rootViewController; break; }
    }
    if (root == nil) {
        for (UIWindow *w in windows) {
            if (!w.isHidden && w.windowLevel == UIWindowLevelNormal) { root = w.rootViewController; break; }
        }
    }
    return root ? PU_findBestViewController(root) : [UIViewController new];
}

#pragma mark - eCPM 序列化（KVC 取值，兼容不同 SDK 版本）

static NSString *PU_ecpmJSON(id ritInfo) {
    if (ritInfo == nil) return nil;
    NSArray<NSString *> *keys = @[
        @"adnName", @"customAdnName", @"slotID", @"levelTag", @"ecpm",
        @"biddingType", @"errorMsg", @"requestID", @"creativeID", @"adRitType",
        @"segmentId", @"abtestId", @"channel", @"sub_channel", @"scenarioId", @"subRitType"
    ];
    NSMutableDictionary *dict = [NSMutableDictionary dictionary];
    for (NSString *key in keys) {
        id value = nil;
        @try { value = [ritInfo valueForKey:key]; } @catch (__unused NSException *e) {}
        if (value) dict[key] = value;
    }
    NSData *data = [NSJSONSerialization dataWithJSONObject:dict options:0 error:nil];
    return data ? [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] : nil;
}

#pragma mark - PlaynestUnionad

@interface PlaynestUnionad () <BUNativeExpressRewardedVideoAdDelegate, BUNativeExpressFullscreenVideoAdDelegate, BUSplashAdDelegate>
@property (nonatomic, strong) BUNativeExpressRewardedVideoAd *rewardAd;
@property (nonatomic, assign) BOOL hasSentShowEvent;
@property (nonatomic, assign) BOOL hasSentReadyEvent;
@property (nonatomic, assign) NSInteger showAttemptCount;
@property (nonatomic, strong) BUNativeExpressFullscreenVideoAd *fullVideoAd;
@property (nonatomic, strong) BUSplashAd *splashAd;
@end

@implementation PlaynestUnionad

#pragma mark 初始化 / 版本

- (void)register:(NSDictionary *)config
         resolve:(RCTPromiseResolveBlock)resolve
          reject:(RCTPromiseRejectBlock)reject
{
    NSString *appId = config[@"iosAppId"];
    if (appId == nil || appId.length == 0) {
        resolve(@(NO));
        return;
    }
    BUAdSDKConfiguration *configuration = [BUAdSDKConfiguration configuration];
    configuration.appID = appId;
    configuration.useMediation = [config[@"useMediation"] boolValue];
    configuration.debugLog = @([config[@"debug"] boolValue] ? 1 : 0);
    if (config[@"themeStatus"] != nil) {
        configuration.themeStatus = @([config[@"themeStatus"] integerValue]);
    }
    // iOS 隐私合规配置（聚合维度）
    NSDictionary *iosPrivacy = config[@"iosPrivacy"];
    if ([iosPrivacy isKindOfClass:[NSDictionary class]]) {
        configuration.mediation.limitPersonalAds = @([iosPrivacy[@"limitPersonalAds"] boolValue]);
        configuration.mediation.limitProgrammaticAds = @([iosPrivacy[@"limitProgrammaticAds"] boolValue]);
        configuration.mediation.forbiddenIDFA = @([iosPrivacy[@"forbiddenCAID"] boolValue]);
    }
    [BUAdSDKManager startWithAsyncCompletionHandler:^(BOOL success, NSError * _Nullable error) {
        resolve(@(success));
    }];
}

- (void)getSDKVersion:(RCTPromiseResolveBlock)resolve
               reject:(RCTPromiseRejectBlock)reject
{
    NSString *version = [BUAdSDKManager SDKVersion];
    if (version == nil || version.length == 0) {
        reject(@"0", @"获取失败", nil);
    } else {
        resolve(version);
    }
}

- (void)getThemeStatus:(RCTPromiseResolveBlock)resolve
                reject:(RCTPromiseRejectBlock)reject
{
    // BUAdSDKThemeStatus_Normal = 0, _Night = 1
    resolve(@((NSInteger)[BUAdSDKManager themeStatus]));
}

- (void)requestPermissionIfNecessary:(RCTPromiseResolveBlock)resolve
                              reject:(RCTPromiseRejectBlock)reject
{
    if (@available(iOS 14, *)) {
        [ATTrackingManager requestTrackingAuthorizationWithCompletionHandler:^(ATTrackingManagerAuthorizationStatus status) {
            resolve(@((NSInteger)status));
        }];
    } else {
        resolve(@(3));
    }
}

#pragma mark 事件发送（统一 onAdEvent）

- (void)emitAd:(NSString *)adType method:(NSString *)onAdMethod extra:(nullable NSDictionary *)extra {
    NSMutableDictionary *map = [NSMutableDictionary dictionary];
    map[@"adType"] = adType;
    map[@"onAdMethod"] = onAdMethod;
    if (extra) [map addEntriesFromDictionary:extra];
    [self emitOnAdEvent:map];
}

#pragma mark 激励视频

- (void)emitReward:(NSString *)onAdMethod extra:(nullable NSDictionary *)extra {
    [self emitAd:@"rewardAd" method:onAdMethod extra:extra];
}

- (void)loadRewardVideoAd:(NSDictionary *)config
                  resolve:(RCTPromiseResolveBlock)resolve
                   reject:(RCTPromiseRejectBlock)reject
{
    NSString *codeId = config[@"iosCodeId"];
    if (codeId == nil || codeId.length == 0) {
        reject(@"no_code_id", @"iosCodeId 不能为空", nil);
        return;
    }
    BURewardedVideoModel *model = [[BURewardedVideoModel alloc] init];
    model.userId = config[@"userID"] ?: @"";
    if (config[@"rewardName"]) model.rewardName = config[@"rewardName"];
    if (config[@"rewardAmount"]) model.rewardAmount = [config[@"rewardAmount"] integerValue];
    if (config[@"mediaExtra"]) model.extra = config[@"mediaExtra"];

    BUAdSlot *slot = [[BUAdSlot alloc] init];
    slot.ID = codeId;
    slot.mediation.mutedIfCan = config[@"mutedIfCan"] ? [config[@"mutedIfCan"] boolValue] : YES;

    self.rewardAd = [[BUNativeExpressRewardedVideoAd alloc] initWithSlot:slot rewardedVideoModel:model];
    self.rewardAd.delegate = self;
    self.hasSentShowEvent = NO;
    self.hasSentReadyEvent = NO;
    self.showAttemptCount = 0;
    // 不同 BUAdSDK 版本加载方法命名不同（loadData / loadAdData），运行时兼容
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
    if ([self.rewardAd respondsToSelector:@selector(loadData)]) {
        [self.rewardAd performSelector:@selector(loadData)];
    } else if ([self.rewardAd respondsToSelector:@selector(loadAdData)]) {
        [self.rewardAd performSelector:@selector(loadAdData)];
    }
#pragma clang diagnostic pop
    resolve(@(YES));
}

- (void)showRewardVideoAd:(RCTPromiseResolveBlock)resolve
                   reject:(RCTPromiseRejectBlock)reject
{
    if (self.rewardAd == nil) {
        [self emitReward:@"onUnReady" extra:@{ @"error": @"广告预加载未完成" }];
        resolve(@(NO));
        return;
    }
    if (self.hasSentShowEvent || self.showAttemptCount >= 2) {
        resolve(@(NO));
        return;
    }
    self.showAttemptCount += 1;
    BUNativeExpressRewardedVideoAd *ad = self.rewardAd;
    dispatch_async(dispatch_get_main_queue(), ^{
        [ad showAdFromRootViewController:PU_currentViewController()];
    });
    resolve(@(YES));
}

- (void)emitShowAndEcpmIfNeeded:(BUNativeExpressRewardedVideoAd *)ad {
    if (self.hasSentShowEvent) return;
    self.hasSentShowEvent = YES;
    [self emitReward:@"onShow" extra:nil];
    NSString *ecpm = PU_ecpmJSON([ad.mediation getShowEcpmInfo]);
    [self emitReward:@"onEcpm" extra:(ecpm ? @{ @"ecpm": ecpm } : nil)];
}

- (void)emitReadyIfNeeded {
    if (self.hasSentReadyEvent) return;
    self.hasSentReadyEvent = YES;
    [self emitReward:@"onReady" extra:nil];
}

#pragma mark BUNativeExpressRewardedVideoAdDelegate

- (void)nativeExpressRewardedVideoAdDidLoad:(BUNativeExpressRewardedVideoAd *)rewardedVideoAd {
    [self emitReadyIfNeeded];
}

- (void)nativeExpressRewardedVideoAdDidDownLoadVideo:(BUNativeExpressRewardedVideoAd *)rewardedVideoAd {
    [self emitReward:@"onCache" extra:nil];
    if (!self.hasSentShowEvent && self.showAttemptCount < 2) {
        [self emitReadyIfNeeded];
    }
}

- (void)nativeExpressRewardedVideoAdDidVisible:(BUNativeExpressRewardedVideoAd *)rewardedVideoAd {
    [self emitShowAndEcpmIfNeeded:rewardedVideoAd];
}

- (void)nativeExpressRewardedVideoAdDidClose:(BUNativeExpressRewardedVideoAd *)rewardedVideoAd {
    [self emitShowAndEcpmIfNeeded:rewardedVideoAd];
    [self emitReward:@"onClose" extra:nil];
    self.hasSentReadyEvent = NO;
    self.showAttemptCount = 0;
    self.rewardAd = nil;
}

- (void)nativeExpressRewardedVideoAd:(BUNativeExpressRewardedVideoAd *)rewardedVideoAd didFailWithError:(NSError *_Nullable)error {
    [self emitReward:@"onFail" extra:@{ @"error": error.localizedDescription ?: @"" }];
    self.hasSentReadyEvent = NO;
    self.showAttemptCount = 0;
    self.rewardAd = nil;
}

- (void)nativeExpressRewardedVideoAdViewRenderFail:(BUNativeExpressRewardedVideoAd *)rewardedVideoAd error:(NSError *_Nullable)error {
    [self emitReward:@"onFail" extra:@{ @"error": error.localizedDescription ?: @"" }];
}

- (void)nativeExpressRewardedVideoAdDidClickSkip:(BUNativeExpressRewardedVideoAd *)rewardedVideoAd {
    [self emitReward:@"onSkip" extra:nil];
}

- (void)nativeExpressRewardedVideoAdDidClick:(BUNativeExpressRewardedVideoAd *)rewardedVideoAd {
    [self emitShowAndEcpmIfNeeded:rewardedVideoAd];
    [self emitReward:@"onClick" extra:nil];
}

- (void)nativeExpressRewardedVideoAdServerRewardDidSucceed:(BUNativeExpressRewardedVideoAd *)rewardedVideoAd verify:(BOOL)verify {
    BURewardedVideoModel *m = rewardedVideoAd.rewardedVideoModel;
    [self emitReward:@"onVerify" extra:@{
        @"rewardVerify": @(verify),
        @"rewardAmount": @(m.rewardAmount),
        @"rewardName": m.rewardName ?: @"",
        @"errorCode": @(0),
        @"error": @"",
    }];
    [self emitReward:@"onRewardArrived" extra:@{
        @"rewardVerify": @(verify),
        @"rewardAmount": @(m.rewardAmount),
        @"rewardName": m.rewardName ?: @"",
        @"errorCode": @(0),
        @"error": @"",
        @"rewardType": @(m.rewardType),
        @"propose": [NSString stringWithFormat:@"%.2f", m.rewardPropose],
    }];
}

- (void)nativeExpressRewardedVideoAdServerRewardDidFail:(BUNativeExpressRewardedVideoAd *)rewardedVideoAd error:(NSError *_Nullable)error {
    BURewardedVideoModel *m = rewardedVideoAd.rewardedVideoModel;
    NSInteger code = error ? error.code : 0;
    [self emitReward:@"onVerify" extra:@{
        @"rewardVerify": @(NO),
        @"rewardAmount": @(m.rewardAmount),
        @"rewardName": m.rewardName ?: @"",
        @"errorCode": @(code),
        @"error": error.localizedDescription ?: @"",
    }];
    [self emitReward:@"onRewardArrived" extra:@{
        @"rewardVerify": @(NO),
        @"rewardAmount": @(m.rewardAmount),
        @"rewardName": m.rewardName ?: @"",
        @"errorCode": @(code),
        @"error": error.localizedDescription ?: @"",
        @"rewardType": @(m.rewardType),
        @"propose": [NSString stringWithFormat:@"%.2f", m.rewardPropose],
    }];
}

#pragma mark 全屏视频/插屏（二合一）

static NSString *const kFullAdType = @"fullScreenVideoAdInteraction";

- (void)emitFull:(NSString *)onAdMethod extra:(nullable NSDictionary *)extra {
    [self emitAd:kFullAdType method:onAdMethod extra:extra];
}

- (void)loadFullScreenVideoAd:(NSDictionary *)config
                      resolve:(RCTPromiseResolveBlock)resolve
                       reject:(RCTPromiseRejectBlock)reject
{
    NSString *codeId = config[@"iosCodeId"];
    if (codeId == nil || codeId.length == 0) {
        reject(@"no_code_id", @"iosCodeId 不能为空", nil);
        return;
    }
    self.fullVideoAd = [[BUNativeExpressFullscreenVideoAd alloc] initWithSlotID:codeId];
    self.fullVideoAd.delegate = self;
    // 不同 BUAdSDK 版本加载方法命名不同（loadData / loadAdData），运行时兼容
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
    if ([self.fullVideoAd respondsToSelector:@selector(loadData)]) {
        [self.fullVideoAd performSelector:@selector(loadData)];
    } else if ([self.fullVideoAd respondsToSelector:@selector(loadAdData)]) {
        [self.fullVideoAd performSelector:@selector(loadAdData)];
    }
#pragma clang diagnostic pop
    resolve(@(YES));
}

- (void)showFullScreenVideoAd:(RCTPromiseResolveBlock)resolve
                       reject:(RCTPromiseRejectBlock)reject
{
    if (self.fullVideoAd == nil) {
        [self emitFull:@"onUnReady" extra:@{ @"error": @"广告预加载未完成" }];
        resolve(@(NO));
        return;
    }
    BUNativeExpressFullscreenVideoAd *ad = self.fullVideoAd;
    dispatch_async(dispatch_get_main_queue(), ^{
        [ad showAdFromRootViewController:PU_currentViewController()];
    });
    resolve(@(YES));
}

#pragma mark BUNativeExpressFullscreenVideoAdDelegate

- (void)nativeExpressFullscreenVideoAdDidDownLoadVideo:(BUNativeExpressFullscreenVideoAd *)fullscreenVideoAd {
    [self emitFull:@"onReady" extra:nil];
}

- (void)nativeExpressFullscreenVideoAdViewRenderFail:(BUNativeExpressFullscreenVideoAd *)fullscreenVideoAd error:(NSError *_Nullable)error {
    [self emitFull:@"onFail" extra:@{ @"error": error.localizedDescription ?: @"" }];
}

- (void)nativeExpressFullscreenVideoAd:(BUNativeExpressFullscreenVideoAd *)fullscreenVideoAd didFailWithError:(NSError *_Nullable)error {
    [self emitFull:@"onFail" extra:@{ @"error": error.localizedDescription ?: @"" }];
}

- (void)nativeExpressFullscreenVideoAdDidVisible:(BUNativeExpressFullscreenVideoAd *)fullscreenVideoAd {
    [self emitFull:@"onShow" extra:nil];
    NSString *ecpm = PU_ecpmJSON([fullscreenVideoAd.mediation getShowEcpmInfo]);
    [self emitFull:@"onEcpm" extra:(ecpm ? @{ @"ecpm": ecpm } : nil)];
}

- (void)nativeExpressFullscreenVideoAdDidClick:(BUNativeExpressFullscreenVideoAd *)fullscreenVideoAd {
    [self emitFull:@"onClick" extra:nil];
}

- (void)nativeExpressFullscreenVideoAdDidClickSkip:(BUNativeExpressFullscreenVideoAd *)fullscreenVideoAd {
    [self emitFull:@"onSkip" extra:nil];
}

- (void)nativeExpressFullscreenVideoAdDidClose:(BUNativeExpressFullscreenVideoAd *)fullscreenVideoAd {
    [self emitFull:@"onClose" extra:nil];
    self.fullVideoAd = nil;
}

- (void)nativeExpressFullscreenVideoAdDidPlayFinish:(BUNativeExpressFullscreenVideoAd *)fullscreenVideoAd didFailWithError:(NSError *_Nullable)error {
    [self emitFull:@"onFinish" extra:nil];
}

#pragma mark 开屏（全屏方法式）

static NSString *const kSplashAdType = @"splashAd";

- (void)emitSplash:(NSString *)onAdMethod extra:(nullable NSDictionary *)extra {
    [self emitAd:kSplashAdType method:onAdMethod extra:extra];
}

- (void)showSplashAd:(NSDictionary *)config
             resolve:(RCTPromiseResolveBlock)resolve
              reject:(RCTPromiseRejectBlock)reject
{
    NSString *codeId = config[@"iosCodeId"];
    if (codeId == nil || codeId.length == 0) {
        reject(@"no_code_id", @"iosCodeId 不能为空", nil);
        return;
    }
    CGFloat w = [config[@"width"] doubleValue];
    CGFloat h = [config[@"height"] doubleValue];
    CGSize size = (w == 0 || h == 0)
        ? [UIScreen mainScreen].bounds.size
        : CGSizeMake(w, h);
    self.splashAd = [[BUSplashAd alloc] initWithSlotID:codeId adSize:size];
    self.splashAd.delegate = self;
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
    if ([self.splashAd respondsToSelector:@selector(loadData)]) {
        [self.splashAd performSelector:@selector(loadData)];
    } else if ([self.splashAd respondsToSelector:@selector(loadAdData)]) {
        [self.splashAd performSelector:@selector(loadAdData)];
    }
#pragma clang diagnostic pop
    resolve(@(YES));
}

- (void)disposeSplash {
    [self.splashAd.mediation destoryAd];
    self.splashAd = nil;
}

#pragma mark BUSplashAdDelegate

- (void)splashAdLoadSuccess:(BUSplashAd *)splashAd {
    // 加载成功，展示时机在 splashAdRenderSuccess
}

- (void)splashAdLoadFail:(BUSplashAd *)splashAd error:(BUAdError *_Nullable)error {
    [self emitSplash:@"onFail" extra:@{ @"error": error.description ?: @"" }];
    [self disposeSplash];
}

- (void)splashAdRenderSuccess:(BUSplashAd *)splashAd {
    [splashAd showSplashViewInRootViewController:PU_currentViewController()];
}

- (void)splashAdRenderFail:(BUSplashAd *)splashAd error:(BUAdError *_Nullable)error {
    [self emitSplash:@"onFail" extra:@{ @"error": error.description ?: @"" }];
    [self disposeSplash];
}

- (void)splashAdDidShow:(BUSplashAd *)splashAd {
    [self emitSplash:@"onShow" extra:nil];
    NSString *ecpm = PU_ecpmJSON([splashAd.mediation getShowEcpmInfo]);
    [self emitSplash:@"onEcpm" extra:(ecpm ? @{ @"ecpm": ecpm } : nil)];
}

- (void)splashAdDidClick:(BUSplashAd *)splashAd {
    [self emitSplash:@"onClick" extra:nil];
}

- (void)splashAdDidClose:(BUSplashAd *)splashAd closeType:(BUSplashAdCloseType)closeType {
    if (closeType == BUSplashAdCloseType_ClickSkip) {
        [self emitSplash:@"onSkip" extra:nil];
    } else {
        [self emitSplash:@"onFinish" extra:nil];
    }
    [self disposeSplash];
}

#pragma mark TurboModule

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativePlaynestUnionadSpecJSI>(params);
}

+ (NSString *)moduleName
{
  return @"PlaynestUnionad";
}

@end
