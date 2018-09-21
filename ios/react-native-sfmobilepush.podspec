require "json"

package = JSON.parse(File.read(File.join(__dir__, "../package.json")))
version = package["version"]
giturl = package["repository"]["url"]
bugsurl = '' 
name = package["name"]
description = package["description"]
license = package["license"]
author = package["author"]

Pod::Spec.new do |s|
  s.name                = name
  s.version             = version
  s.summary             = description
  s.description         = description
  s.homepage            = giturl
  s.license             = license
  s.author              = author
  s.platform            = :ios, "9.0"
  s.source              = { :git => giturl, :tag => version }
  s.source_files        = 'react-native-sfmobilepush/*.{h,m}'
  s.requires_arc        = true


  s.dependency 'React'
  s.dependency 'MarketingCloudSDK', '~> 5.2'
end
