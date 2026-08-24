#import "PlaynestSplashView.h"

#import <react/renderer/components/PlaynestUnionadSpec/ComponentDescriptors.h>
#import <react/renderer/components/PlaynestUnionadSpec/EventEmitters.h>
#import <react/renderer/components/PlaynestUnionadSpec/Props.h>
#import <react/renderer/components/PlaynestUnionadSpec/RCTComponentViewHelpers.h>
#import <React/RCTFabricComponentsPlugins.h>
#import <BUAdSDK/BUAdSDK.h>

using namespace facebook::react;

static UIViewController *PSV_currentViewController(void) {
    UIViewController *root = nil;
    if (@available(iOS 13.0, *)) {
        for (UIScene *scene in UIApplication.sharedApplication.connectedScenes) {
            if ([scene isKindOfClass:[UIWindowScene class]]) {
                for (UIWindow *w in ((UIWindowScene *)scene).windows) {
                    if (w.isKeyWindow) { root = w.rootViewController; break; }
                }
            }
            if (root) break;
        }
    }
    while (root.presentedViewController) root = root.presentedViewController;
    return root ?: [UIViewController new];
}

static std::string PSV_ecpmJSON(id ritInfo) {
    if (ritInfo == nil) return std::string();
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
    NSString *s = data ? [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] : nil;
    return s ? std::string(s.UTF8String) : std::string();
}

@interface PlaynestSplashView () <RCTPlaynestSplashViewViewProtocol, BUSplashAdDelegate>
@end

@implementation PlaynestSplashView {
    BUSplashAd *_splashAd;
    NSString *_loadedCodeId;
}

+ (void)load {
    [super load];
}

+ (ComponentDescriptorProvider)componentDescriptorProvider {
    return concreteComponentDescriptorProvider<PlaynestSplashViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        static const auto defaultProps = std::make_shared<const PlaynestSplashViewProps>();
        _props = defaultProps;
    }
    return self;
}

- (const PlaynestSplashViewEventEmitter &)splashEventEmitter {
    return static_cast<const PlaynestSplashViewEventEmitter &>(*_eventEmitter);
}

- (void)updateProps:(const Props::Shared &)props oldProps:(const Props::Shared &)oldProps {
    const auto &newProps = *std::static_pointer_cast<const PlaynestSplashViewProps>(props);
    NSString *codeId = newProps.iosCodeId.empty()
        ? nil
        : [NSString stringWithUTF8String:newProps.iosCodeId.c_str()];
    CGFloat w = (CGFloat)newProps.expressWidth;
    CGFloat h = (CGFloat)newProps.expressHeight;
    if (codeId.length > 0 && ![codeId isEqualToString:_loadedCodeId]) {
        _loadedCodeId = codeId;
        [self loadSplash:codeId width:w height:h];
    }
    [super updateProps:props oldProps:oldProps];
}

- (void)loadSplash:(NSString *)codeId width:(CGFloat)w height:(CGFloat)h {
    if (w <= 0) w = self.bounds.size.width > 0 ? self.bounds.size.width : UIScreen.mainScreen.bounds.size.width;
    if (h <= 0) h = self.bounds.size.height > 0 ? self.bounds.size.height : UIScreen.mainScreen.bounds.size.height;
    CGSize size = CGSizeMake(w, h);
    _splashAd = [[BUSplashAd alloc] initWithSlotID:codeId adSize:size];
    _splashAd.delegate = self;
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
    if ([_splashAd respondsToSelector:@selector(loadData)]) {
        [_splashAd performSelector:@selector(loadData)];
    } else if ([_splashAd respondsToSelector:@selector(loadAdData)]) {
        [_splashAd performSelector:@selector(loadAdData)];
    }
#pragma clang diagnostic pop
}

- (void)layoutSubviews {
    [super layoutSubviews];
    _splashAd.splashView.frame = self.bounds;
}

#pragma mark BUSplashAdDelegate

- (void)splashAdLoadSuccess:(BUSplashAd *)splashAd {
}

- (void)splashAdLoadFail:(BUSplashAd *)splashAd error:(BUAdError *_Nullable)error {
    if (_eventEmitter) {
        [self splashEventEmitter].onAdFail({.error = std::string(error.description.UTF8String ?: "")});
    }
}

- (void)splashAdRenderSuccess:(BUSplashAd *)splashAd {
    // iOS 开屏 SDK 仅支持全屏弹出（与 flutter iOS 一致，splashView 不支持任意容器嵌入渲染）
    [splashAd showSplashViewInRootViewController:PSV_currentViewController()];
}

- (void)splashAdRenderFail:(BUSplashAd *)splashAd error:(BUAdError *_Nullable)error {
    if (_eventEmitter) {
        [self splashEventEmitter].onAdFail({.error = std::string(error.description.UTF8String ?: "")});
    }
}

- (void)splashAdDidShow:(BUSplashAd *)splashAd {
    if (_eventEmitter) {
        [self splashEventEmitter].onAdShow(
            {.width = (double)UIScreen.mainScreen.bounds.size.width,
             .height = (double)UIScreen.mainScreen.bounds.size.height});
        std::string ecpm = PSV_ecpmJSON([splashAd.mediation getShowEcpmInfo]);
        if (!ecpm.empty()) {
            [self splashEventEmitter].onAdEcpm({.ecpm = ecpm});
        }
    }
}

- (void)splashAdDidClick:(BUSplashAd *)splashAd {
    if (_eventEmitter) {
        [self splashEventEmitter].onAdClick({});
    }
}

- (void)splashAdDidClose:(BUSplashAd *)splashAd closeType:(BUSplashAdCloseType)closeType {
    if (_eventEmitter) {
        if (closeType == BUSplashAdCloseType_ClickSkip) {
            [self splashEventEmitter].onAdSkip({});
        } else {
            [self splashEventEmitter].onAdFinish({});
        }
    }
    [_splashAd.mediation destoryAd];
    _splashAd = nil;
}

@end

Class<RCTComponentViewProtocol> PlaynestSplashViewCls(void) {
    return PlaynestSplashView.class;
}
