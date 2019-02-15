#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <UserNotifications/UserNotifications.h>

@interface SFMobilePush : RCTEventEmitter <RCTBridgeModule>

+ (void)initialize:(NSString *) applicationId accessToken:(NSString *) accessToken;
+ (void)didReceiveRemoteNotification:(UNNotificationResponse *) response;
+ (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken;
+ (void)didReceiveRemoteNotificationUserInfo:(NSDictionary *) userInfo;
- (void)handleRemoteNotification:(NSNotification *) notification;
- (NSURL*) getConfigPath;

@end
