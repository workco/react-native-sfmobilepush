
#import "SFMobilePush.h"
#import <MarketingCloudSDK/MarketingCloudSDK.h>

#define CONFIG_JSON_PATH @"sfconfig.json"

@implementation SFMobilePush

RCT_EXPORT_MODULE(SFMobilePush)
BOOL isInitialized = NO;

- (NSURL*) getConfigPath {
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsDirectory = [paths objectAtIndex:0];
    NSString *appFile = [documentsDirectory stringByAppendingPathComponent:CONFIG_JSON_PATH];
    return [NSURL fileURLWithPath:appFile];
}

RCT_REMAP_METHOD(init,
                 arguments:(NSDictionary*)arguments
                 findEventsWithResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    if(isInitialized) {
        resolve(nil);
        return;
    }
    NSString *applicationId = [arguments valueForKey:@"appId"];
    NSString *accessToken = [arguments valueForKey:@"accessToken"];

    BOOL successful = NO;
    NSError *error = nil;
    NSDictionary* dict = [NSDictionary dictionaryWithObjectsAndKeys:applicationId, @"appid",accessToken, @"accesstoken",@YES, @"etanalytics",@YES, @"pianalytics", @YES, @"location", @YES, @"inbox", nil];
    NSArray* config = [NSArray arrayWithObjects:dict,nil];
    NSURL* configPath = [self getConfigPath];
    NSData* jsonData = [NSJSONSerialization dataWithJSONObject:config options:NSJSONWritingPrettyPrinted error:&error];
    BOOL writeSuccess = [jsonData writeToURL:configPath options:NSAtomicWrite error:&error];
    if(!writeSuccess) {
        reject(@"SFMobilePush_init_fail",[NSString stringWithFormat:@"SFMobilePush could not write the config data: %@", [error localizedDescription]], error);
        return;
    }

    successful = [[MarketingCloudSDK sharedInstance] sfmc_configureWithURL:configPath configurationIndex:[NSNumber numberWithInteger:0]  error:&error];
    if (!successful) {
        reject(@"SFMobilePush_init_fail", [NSString stringWithFormat:@"SFMobilePush could not be initialized: %@", [error localizedDescription]], error);
        return;
    }
    else {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (@available(iOS 10, *)) {
                [[UNUserNotificationCenter currentNotificationCenter] requestAuthorizationWithOptions:UNAuthorizationOptionAlert | UNAuthorizationOptionSound | UNAuthorizationOptionBadge
                                                                                    completionHandler:^(BOOL granted, NSError * _Nullable error) {
                                                                                        if (error == nil) {
                                                                                            if (granted == YES) {
                                                                                                dispatch_async(dispatch_get_main_queue(), ^{
                                                                                                    [[UIApplication sharedApplication] registerForRemoteNotifications];
                                                                                                });
                                                                                            }
                                                                                        }
                                                                                    }];
            }
            else {
#if __IPHONE_OS_VERSION_MIN_REQUIRED < 100000
                UIUserNotificationSettings *settings = [UIUserNotificationSettings settingsForTypes:
                                                        UIUserNotificationTypeBadge |
                                                        UIUserNotificationTypeSound |
                                                        UIUserNotificationTypeAlert
                                                                                         categories:nil];
                [[UIApplication sharedApplication] registerUserNotificationSettings:settings];
#endif
                [[UIApplication sharedApplication] registerForRemoteNotifications];
            }
            isInitialized = YES;
            resolve(nil);
        });
    }
}

RCT_REMAP_METHOD(getTags,
                 getTagsResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    NSSet *tags = [[MarketingCloudSDK sharedInstance] sfmc_tags];
    resolve([tags allObjects]);
}

RCT_REMAP_METHOD(addTag,
                 tag:(NSString *)tag
                 addTagResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    [[MarketingCloudSDK sharedInstance] sfmc_addTag:tag];
    resolve(nil);
}

RCT_REMAP_METHOD(removeTag,
                 tag:(NSString *)tag
                 removeTagResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    [[MarketingCloudSDK sharedInstance] sfmc_removeTag:tag];
    resolve(nil);
}

RCT_REMAP_METHOD(getSubscriberKey,
                 getSubscriberKeyResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    NSString *subscriberKey = [[MarketingCloudSDK sharedInstance] sfmc_contactKey];
    resolve(subscriberKey);
}

RCT_REMAP_METHOD(setSubscriberKey,
                 subscriberKey:(NSString *)subscriberKey
                 setSubscriberKeyResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    [[MarketingCloudSDK sharedInstance] sfmc_setContactKey:subscriberKey];
    resolve(nil);
}

RCT_REMAP_METHOD(getAttributes,
                 getAttributesResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    NSDictionary *attributes = [[MarketingCloudSDK sharedInstance] sfmc_attributes];
    resolve(attributes);
}

RCT_REMAP_METHOD(setAttribute,
                 key:(NSString *)key
                 value:(NSString *)value
                 setAttributeResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    [[MarketingCloudSDK sharedInstance] sfmc_setAttributeNamed:key value:value];
    resolve(nil);
}

@end
