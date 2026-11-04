#import "PBBFirebaseBridge.h"
#import <objc/message.h>

@implementation PBBFirebaseBridge

+ (BOOL)configureIfNeeded {
    if (![NSBundle.mainBundle URLForResource:@"GoogleService-Info" withExtension:@"plist"]) {
        NSLog(@"Firebase Messaging is not configured: GoogleService-Info.plist is missing from the iOS app target.");
        return NO;
    }

    Class firebaseApp = NSClassFromString(@"FIRApp");
    if (firebaseApp == Nil) {
        NSLog(@"FirebaseCore is not linked into the iOS app.");
        return NO;
    }
    SEL defaultAppSelector = NSSelectorFromString(@"defaultApp");
    id defaultApp = ((id (*)(id, SEL))objc_msgSend)(firebaseApp, defaultAppSelector);
    if (defaultApp == nil) {
        SEL configureSelector = NSSelectorFromString(@"configure");
        ((void (*)(id, SEL))objc_msgSend)(firebaseApp, configureSelector);
    }
    return ((id (*)(id, SEL))objc_msgSend)(firebaseApp, defaultAppSelector) != nil;
}

+ (BOOL)isConfigured {
    Class firebaseApp = NSClassFromString(@"FIRApp");
    if (firebaseApp == Nil) {
        return NO;
    }
    return ((id (*)(id, SEL))objc_msgSend)(firebaseApp, NSSelectorFromString(@"defaultApp")) != nil;
}

+ (void)setAPNSToken:(NSData *)deviceToken {
    Class messagingClass = NSClassFromString(@"FIRMessaging");
    if (messagingClass == Nil) {
        NSLog(@"FirebaseMessaging is not linked into the iOS app.");
        return;
    }
    id messaging = ((id (*)(id, SEL))objc_msgSend)(messagingClass, NSSelectorFromString(@"messaging"));
    ((void (*)(id, SEL, id))objc_msgSend)(messaging, NSSelectorFromString(@"setAPNSToken:"), deviceToken);
}

+ (void)subscribeToTopic:(NSString *)topic {
    Class messagingClass = NSClassFromString(@"FIRMessaging");
    if (messagingClass == Nil) {
        NSLog(@"FirebaseMessaging is not linked into the iOS app.");
        return;
    }
    id messaging = ((id (*)(id, SEL))objc_msgSend)(messagingClass, NSSelectorFromString(@"messaging"));
    void (^completion)(NSError *) = ^(NSError *error) {
        if (error) {
            NSLog(@"Firebase topic subscription failed for %@: %@", topic, error.localizedDescription);
        }
    };
    ((void (*)(id, SEL, id, id))objc_msgSend)(
        messaging,
        NSSelectorFromString(@"subscribeToTopic:completion:"),
        topic,
        completion
    );
}

@end
