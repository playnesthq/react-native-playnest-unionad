require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "RNUnionad"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => min_ios_version_supported }
  s.source       = { :git => "https://github.com/playnesthq/react-native-playnest-unionad.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift,cpp}"
  s.private_header_files = "ios/**/*.h"

  # ATT tracking authorization + IDFA (AdSupport)
  s.frameworks = "AppTrackingTransparency", "AdSupport"

  # Pangle (CSJ) SDK 7.8.0.5
  s.dependency "Ads-CN-Beta/BUAdSDK", "7.8.0.5"
  s.dependency "Ads-CN-Beta/CSJMediation-Only", "7.8.0.5"

  s.pod_target_xcconfig = {
    "DEFINES_MODULE" => "YES"
  }

  install_modules_dependencies(s)
end
