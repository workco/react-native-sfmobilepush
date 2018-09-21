#import <React/RCTBridgeModule.h>
#import <UserNotifications/UserNotifications.h>

@interface SFMobilePush : NSObject <RCTBridgeModule, UNUserNotificationCenterDelegate>
@end
