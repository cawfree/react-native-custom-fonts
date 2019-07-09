#import <CoreText/CoreText.h>
#import <UIKit/UIKit.h>
#import "RNCustomFonts.h"

@implementation RNCustomFonts

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(
  onFontFacesChanged:(NSArray *)readableArray
  resolve:(RCTPromiseResolveBlock)resolve
  reject:(RCTPromiseRejectBlock)reject)
{
    // Allocate a Dictionary.
    const NSMutableDictionary* response = [[NSMutableDictionary alloc]initWithCapacity:10];
    // Get the valid faces.
    const NSArray* validFaces = [self getValidFontFaces:readableArray];
    // Iterate the array.
    for (int i = 0; i < [validFaces count]; i += 1) {
        // Fetch the FontFace.
        const NSDictionary* face = validFaces[i];
        // Fetch the contained data.
        NSString* uri = [face objectForKey:@"uri"];
        NSString* fontFamily = [face objectForKey:@"fontFamily"];
        NSString* fontWeight = [face objectForKey:@"fontWeight"];
        // Attempt to write the font to a local path.
        NSString* path = [self downloadFontFromUrl:uri
                   withFontFamily:fontFamily
                   withFontWeight:fontWeight];
        // Attempt to load the font file.
        [self loadFontFile:path];
    }
    // Resolve to the caller.
    resolve(response);
}

RCT_EXPORT_METHOD(
  onRequestFontFamily:(NSInteger *)viewHandle
  fontFamily:(NSString *)fontFamily
  fontWeight:(NSString *)fontWeight
  resolve:(RCTPromiseResolveBlock)resolve
  reject:(RCTPromiseRejectBlock)reject)
{
    // Resolve to the caller directly; this method is only useful for Android only.
    resolve(@{});
}

- (NSArray*) getValidFontFaces:(NSArray *)fontFaces
{
    // Declare the array of valid faces.
    NSMutableArray* validFaces = [[NSMutableArray alloc] initWithCapacity:[fontFaces count]];
    // Iterate the supplied array.
    for (int i = 0; i < [fontFaces count]; i += 1) {
        // Fetch the FontFace.
        const NSDictionary* face = fontFaces[i];
        // Fetch the contained data.
        const NSString* uri = [face objectForKey:@"uri"];
        const NSString* fontFamily = [face objectForKey:@"fontFamily"];
        // Ensure the data dependencies are valid.
        if (uri != nil && fontFamily != nil) {
            // Buffer the face.
            [validFaces addObject:face];
        }
    }
    // Return the valid faces.
    return validFaces;
}

// https://stackoverflow.com/a/14049216
- (void) loadFontFile:(NSString*)path
{
    NSData *data = [[NSFileManager defaultManager] contentsAtPath:path];
    CFErrorRef error;
    CGDataProviderRef provider = CGDataProviderCreateWithCFData((CFDataRef)data);
    CGFontRef font = CGFontCreateWithDataProvider(provider);
    if (! CTFontManagerRegisterGraphicsFont(font, &error)) {
        CFStringRef errorDescription = CFErrorCopyDescription(error);
        NSLog(@"Failed to load font: %@", errorDescription);
        CFRelease(errorDescription);
    }
    CFRelease(font);
    CFRelease(provider);
}

- (NSString *)downloadFontFromUrl:(NSString*)stringURL withFontFamily:(NSString*)withFontFamily withFontWeight:(NSString*)withFontWeight
{
    // Define the URL to fetch from.
    NSURL* url = [NSURL URLWithString:stringURL];
    // Allocate data storage for the contents of the URL.
    NSData* urlData = [NSData dataWithContentsOfURL:url];
    if (urlData)
    {
        // Fetch the app's local storage directory.
        NSString* documentsDirectory = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
        // Declare the directory where we'll be saving the font file to.
        NSString* filePath = [NSString stringWithFormat:@"%@/%@-%@.%@", [NSString stringWithFormat:@"%@/RNCustomFonts", documentsDirectory], withFontFamily, withFontWeight, [stringURL pathExtension]];
        // Ensure that the RNCustomFonts directory exists.
        [[NSFileManager defaultManager] createDirectoryAtPath:[filePath stringByDeletingLastPathComponent] withIntermediateDirectories:YES attributes:nil error:nil];
        // Write the fetched data to the font path.
        [urlData writeToFile:filePath atomically:YES];
        // Return the path that we've written to.
        return filePath;
    }
    return nil;
}

@end
