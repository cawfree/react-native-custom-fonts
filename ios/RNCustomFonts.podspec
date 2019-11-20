
Pod::Spec.new do |s|
  s.name         = "RNCustomFonts"
  s.version      = "1.0.0"
  s.summary      = "RNCustomFonts"
  s.description  = <<-DESC
                  Use fonts sourced from a network address.
                   DESC
  s.homepage     = "https://github.com/cawfree/react-native-custom-fonts"
  s.license      = "MIT"
  s.author             = { "author" => "author@domain.cn" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/author/RNCustomFonts.git", :tag => "master" }
  s.source_files  = "RNCustomFonts*.{h,m}"
  s.requires_arc = true


  s.dependency "React"
  #s.dependency "others"

end

  
