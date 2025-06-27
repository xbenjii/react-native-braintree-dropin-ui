Pod::Spec.new do |s|
  s.name         = "RNBraintreeDropIn"
  s.version      = "2.1.0"
  s.summary      = "RNBraintreeDropIn"
  s.description  = <<-DESC
                  RNBraintreeDropIn
                   DESC
  s.homepage     = "https://github.com/xbenjii/react-native-braintree-dropin-ui"
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "./LICENSE" }
  s.author             = { "author" => "github@xbenjii.co.uk" }
  s.platform     = :ios, "15.6"
  s.source       = { :git => "git@github.com:xbenjii/react-native-braintree-dropin-ui.git", :tag => "master" }
  s.source_files  = "ios/**/*.{h,m}"
  s.requires_arc = true
  s.dependency    'React'
  s.dependency    'Braintree', '5.27.0'
  s.dependency    'BraintreeDropIn', '9.14.0'
  s.dependency    'Braintree/DataCollector', '5.27.0'
  s.dependency    'Braintree/ApplePay', '5.27.0'
  s.dependency    'Braintree/Venmo', '5.27.0'
end
