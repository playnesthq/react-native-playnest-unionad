#import "PlaynestBannerView.h"

#import <react/renderer/components/PlaynestUnionadSpec/ComponentDescriptors.h>
#import <react/renderer/components/PlaynestUnionadSpec/EventEmitters.h>
#import <react/renderer/components/PlaynestUnionadSpec/Props.h>
#import <react/renderer/components/PlaynestUnionadSpec/RCTComponentViewHelpers.h>
#import <React/RCTFabricComponentsPlugins.h>
#import <BUAdSDK/BUAdSDK.h>

using namespace facebook::react;

// eCPM 信息序列化为 JSON 字符串（KVC 取值，兼容不同 SDK 版本）
static std::string PBV_ecpmJSON(id ritInfo) {
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

// 获取当前可展示的 UIViewController（与 PlaynestUnionad.mm 中的逻辑一致，独立一份避免跨文件依赖）
static UIViewController *PBV_currentViewController(void) {
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

@interface PlaynestBannerView () <RCTPlaynestBannerViewViewProtocol, BUNativeExpressBannerViewDelegate>
@end

@implementation PlaynestBannerView {
    BUNativeExpressBannerView *_bannerView;
    NSString *_loadedCodeId;
}

+ (void)load {
    [super load];
}

+ (ComponentDescriptorProvider)componentDescriptorProvider {
    return concreteComponentDescriptorProvider<PlaynestBannerViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        static const auto defaultProps = std::make_shared<const PlaynestBannerViewProps>();
        _props = defaultProps;
    }
    return self;
}

- (void)updateProps:(const Props::Shared &)props oldProps:(const Props::Shared &)oldProps {
    const auto &newProps = *std::static_pointer_cast<const PlaynestBannerViewProps>(props);
    NSString *codeId = newProps.iosCodeId.empty()
        ? nil
        : [NSString stringWithUTF8String:newProps.iosCodeId.c_str()];
    CGFloat w = (CGFloat)newProps.expressWidth;
    CGFloat h = (CGFloat)newProps.expressHeight;
    if (codeId.length > 0 && ![codeId isEqualToString:_loadedCodeId]) {
        _loadedCodeId = codeId;
        [self loadBanner:codeId width:w height:h];
    }
    [super updateProps:props oldProps:oldProps];
}

- (void)loadBanner:(NSString *)codeId width:(CGFloat)w height:(CGFloat)h {
    if (w <= 0) w = self.bounds.size.width > 0 ? self.bounds.size.width : UIScreen.mainScreen.bounds.size.width;
    if (h <= 0) h = self.bounds.size.height > 0 ? self.bounds.size.height : 60;
    CGSize size = CGSizeMake(w, h);
    BUNativeExpressBannerView *bv =
        [[BUNativeExpressBannerView alloc] initWithSlotID:codeId
                                       rootViewController:PBV_currentViewController()
                                                   adSize:size];
    bv.delegate = self;
    bv.frame = CGRectMake(0, 0, w, h);
    [_bannerView removeFromSuperview];
    _bannerView = bv;
    [self addSubview:bv];
    [bv loadAdData];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    _bannerView.frame = self.bounds;
}

- (const PlaynestBannerViewEventEmitter &)bannerEventEmitter {
    return static_cast<const PlaynestBannerViewEventEmitter &>(*_eventEmitter);
}

#pragma mark BUNativeExpressBannerViewDelegate

- (void)nativeExpressBannerAdViewRenderSuccess:(BUNativeExpressBannerView *)bannerAdView {
    bannerAdView.frame = self.bounds;
    if (_eventEmitter) {
        [self bannerEventEmitter].onAdShow(
            {.width = (double)bannerAdView.frame.size.width,
             .height = (double)bannerAdView.frame.size.height});
        std::string ecpm = PBV_ecpmJSON([bannerAdView.mediation getShowEcpmInfo]);
        if (!ecpm.empty()) {
            [self bannerEventEmitter].onAdEcpm({.ecpm = ecpm});
        }
    }
}

- (void)nativeExpressBannerAdViewRenderFail:(BUNativeExpressBannerView *)bannerAdView error:(NSError *__nullable)error {
    if (_eventEmitter) {
        [self bannerEventEmitter].onAdFail(
            {.error = std::string(error.localizedDescription.UTF8String ?: "")});
    }
    [bannerAdView removeFromSuperview];
}

- (void)nativeExpressBannerAdView:(BUNativeExpressBannerView *)bannerAdView didLoadFailWithError:(NSError *_Nullable)error {
    if (_eventEmitter) {
        [self bannerEventEmitter].onAdFail(
            {.error = std::string(error.localizedDescription.UTF8String ?: "")});
    }
    [bannerAdView removeFromSuperview];
}

- (void)nativeExpressBannerAdViewDidClick:(BUNativeExpressBannerView *)bannerAdView {
    if (_eventEmitter) {
        [self bannerEventEmitter].onAdClick({});
    }
}

- (void)nativeExpressBannerAdView:(BUNativeExpressBannerView *)bannerAdView dislikeWithReason:(NSArray<BUDislikeWords *> *_Nullable)filterwords {
    if (_eventEmitter) {
        NSString *reason = filterwords.firstObject.name ?: @"";
        [self bannerEventEmitter].onAdDislike({.reason = std::string(reason.UTF8String)});
    }
    [bannerAdView removeFromSuperview];
}

@end

Class<RCTComponentViewProtocol> PlaynestBannerViewCls(void) {
    return PlaynestBannerView.class;
}
