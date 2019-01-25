#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <UserNotifications/UserNotifications.h>

@interface SFMobilePush : RCTEventEmitter <RCTBridgeModule>

+ (void)didReceiveRemoteNotification:(UNNotificationResponse *) response;
- (void)handleRemoteNotification:(NSNotification *) notification;
- (NSURL*) getConfigPath;

@end
