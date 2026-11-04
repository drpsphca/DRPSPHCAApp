#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface PBBFirebaseBridge : NSObject
+ (BOOL)configureIfNeeded;
+ (BOOL)isConfigured;
+ (void)setAPNSToken:(NSData *)deviceToken;
+ (void)subscribeToTopic:(NSString *)topic;
@end

NS_ASSUME_NONNULL_END
