#import "PlaynestNativeView.h"

#import <react/renderer/components/PlaynestUnionadSpec/ComponentDescriptors.h>
#import <react/renderer/components/PlaynestUnionadSpec/EventEmitters.h>
#import <react/renderer/components/PlaynestUnionadSpec/Props.h>
#import <react/renderer/components/PlaynestUnionadSpec/RCTComponentViewHelpers.h>
#import <React/RCTFabricComponentsPlugins.h>
#import <BUAdSDK/BUAdSDK.h>

using namespace facebook::react;

static UIViewController *PNV_currentViewController(void) {
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

static std::string PNV_ecpmJSON(id ritInfo) {
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

@interface PlaynestNativeView () <RCTPlaynestNativeViewViewProtocol, BUNativeExpressAdViewDelegate, BUCustomEventProtocol>
@end

@implementation PlaynestNativeView {
    BUNativeExpressAdManager *_adManager;
    BUNativeExpressAdView *_adView;
    NSString *_loadedCodeId;
}

+ (void)load {
    [super load];
}

+ (ComponentDescriptorProvider)componentDescriptorProvider {
    return concreteComponentDescriptorProvider<PlaynestNativeViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        static const auto defaultProps = std::make_shared<const PlaynestNativeViewProps>();
        _props = defaultProps;
    }
    return self;
}

- (const PlaynestNativeViewEventEmitter &)nativeEventEmitter {
    return static_cast<const PlaynestNativeViewEventEmitter &>(*_eventEmitter);
}

- (void)updateProps:(const Props::Shared &)props oldProps:(const Props::Shared &)oldProps {
    const auto &newProps = *std::static_pointer_cast<const PlaynestNativeViewProps>(props);
    NSString *codeId = newProps.iosCodeId.empty()
        ? nil
        : [NSString stringWithUTF8String:newProps.iosCodeId.c_str()];
    CGFloat w = (CGFloat)newProps.expressWidth;
    CGFloat h = (CGFloat)newProps.expressHeight;
    if (codeId.length > 0 && ![codeId isEqualToString:_loadedCodeId]) {
        _loadedCodeId = codeId;
        [self loadNative:codeId width:w height:h muted:newProps.isMuted];
    }
    [super updateProps:props oldProps:oldProps];
}

- (void)loadNative:(NSString *)codeId width:(CGFloat)w height:(CGFloat)h muted:(BOOL)muted {
    if (w <= 0) w = self.bounds.size.width > 0 ? self.bounds.size.width : UIScreen.mainScreen.bounds.size.width;
    if (h <= 0) h = self.bounds.size.height > 0 ? self.bounds.size.height : 100;
    CGSize size = CGSizeMake(w, h);
    BUAdSlot *slot = [[BUAdSlot alloc] init];
    slot.ID = codeId;
    slot.adType = BUAdSlotAdTypeFeed;
    slot.position = BUAdSlotPositionFeed;
    slot.mediation.mutedIfCan = muted;
    BUSize *imgSize = [[BUSize alloc] init];
    imgSize.width = (NSInteger)w;
    imgSize.height = (NSInteger)h;
    slot.imgSize = imgSize;
    _adManager = [[BUNativeExpressAdManager alloc] initWithSlot:slot adSize:size];
    _adManager.delegate = self;
    [_adManager loadAdDataWithCount:1];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    _adView.frame = self.bounds;
}

#pragma mark BUNativeExpressAdViewDelegate

- (void)nativeExpressAdSuccessToLoad:(BUNativeExpressAdManager *)nativeExpressAdManager
                               views:(NSArray<__kindof BUNativeExpressAdView *> *)views {
    if (views.count > 0) {
        BUNativeExpressAdView *v = views.firstObject;
        v.rootViewController = PNV_currentViewController();
        [_adView removeFromSuperview];
        _adView = v;
        [v render];
        v.frame = self.bounds;
        [self addSubview:v];
    }
}

- (void)nativeExpressAdFailToLoad:(BUNativeExpressAdManager *)nativeExpressAdManager error:(NSError *_Nullable)error {
    if (_eventEmitter) {
        [self nativeEventEmitter].onAdFail(
            {.error = std::string(error.localizedDescription.UTF8String ?: "")});
    }
}

- (void)nativeExpressAdViewRenderSuccess:(BUNativeExpressAdView *)nativeExpressAdView {
    nativeExpressAdView.frame = self.bounds;
    if (_eventEmitter) {
        [self nativeEventEmitter].onAdShow(
            {.width = (double)nativeExpressAdView.frame.size.width,
             .height = (double)nativeExpressAdView.frame.size.height});
    }
}

- (void)nativeExpressAdViewWillShow:(BUNativeExpressAdView *)nativeExpressAdView {
    if (_eventEmitter) {
        std::string ecpm = PNV_ecpmJSON([nativeExpressAdView.mediation getShowEcpmInfo]);
        if (!ecpm.empty()) {
            [self nativeEventEmitter].onAdEcpm({.ecpm = ecpm});
        }
    }
}

- (void)nativeExpressAdViewDidClick:(BUNativeExpressAdView *)nativeExpressAdView {
    if (_eventEmitter) {
        [self nativeEventEmitter].onAdClick({});
    }
}

- (void)nativeExpressAdView:(BUNativeExpressAdView *)nativeExpressAdView
          dislikeWithReason:(NSArray<BUDislikeWords *> *)filterWords {
    if (_eventEmitter) {
        NSString *reason = filterWords.firstObject.name ?: @"";
        [self nativeEventEmitter].onAdDislike({.reason = std::string(reason.UTF8String)});
    }
    [_adView removeFromSuperview];
}

@end

Class<RCTComponentViewProtocol> PlaynestNativeViewCls(void) {
    return PlaynestNativeView.class;
}
