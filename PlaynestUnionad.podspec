require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "PlaynestUnionad"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => min_ios_version_supported }
  s.source       = { :git => "https://github.com/playnest/react-native-playnest-unionad.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift,cpp}"
  s.private_header_files = "ios/**/*.h"

  # ATT 广告跟踪授权
  s.frameworks = "AppTrackingTransparency"

  # 穿山甲(Pangle) SDK 7.8.0.0（与 flutter_unionad 2.2.9 对齐）
  s.dependency "Ads-CN-Beta/BUAdSDK", "7.8.0.0"
  s.dependency "Ads-CN-Beta/CSJMediation-Only", "7.8.0.0"

  # Ads-CN-Beta 提供 arm64 模拟器切片，无需再锁 x86_64，
  # 从而支持 Apple Silicon 上的 arm64 模拟器原生调试。
  s.pod_target_xcconfig = {
    "DEFINES_MODULE" => "YES"
  }

  install_modules_dependencies(s)
end
