#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint open_file_plugin.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'open_file_plugin'
  s.version          = '0.0.4'
  s.summary          = 'A plugin to open file'
  s.description      = <<-DESC
  The \"Open File plugin\" streamlines file handling by seamlessly managing various file types within the application. Users can effortlessly view files in their default software, enhancing accessibility and simplifying workflows
                       DESC
  s.homepage         = 'https://github.com/whizpool/Open_File_Plugin'
  s.license          = { :file => '../LICENSE',:type => 'BSD' }
  s.author           = { 'Whizpool' => 'whizpool.com' }
  s.source           = { :git => 'https://github.com/whizpool/Open_File_Plugin.git', :tag => 'v0.0.4'}
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'
  s.platform = :ios, '11.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
end
