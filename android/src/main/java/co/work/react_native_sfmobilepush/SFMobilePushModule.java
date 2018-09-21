package co.work.react_native_sfmobilepush;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.facebook.react.bridge.NoSuchKeyException;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.salesforce.marketingcloud.InitializationStatus;
import com.salesforce.marketingcloud.MarketingCloudConfig;
import com.salesforce.marketingcloud.MarketingCloudSdk;
import com.salesforce.marketingcloud.notifications.NotificationManager;
import com.salesforce.marketingcloud.notifications.NotificationMessage;

import java.util.Map;
import java.util.Set;

public class SFMobilePushModule extends ReactContextBaseJavaModule {
    private final Application application;
    private boolean isInitialized = false;

    public SFMobilePushModule(ReactApplicationContext reactContext, Application application) {
        super(reactContext);
        this.application = application;
    }

    @Override
    public String getName() {
        return "SFMobilePush";
    }

    @ReactMethod
    public void checkPermissions(Promise promise) {
        try {
            ReactContext reactContext = getReactApplicationContext();
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(reactContext);
            promise.resolve(managerCompat.areNotificationsEnabled());
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    private String getStringOrNull(ReadableMap map, String key) {
        try {
            return map.getString(key);
        } catch (NoSuchKeyException e) {
            return null;
        }
    }

    @ReactMethod
    public void init(ReadableMap arguments, final Promise promise) {
        if (isInitialized) {
            promise.resolve(null);
            return;
        }
        final String applicationId = getStringOrNull(arguments, "appId");
        final String accessToken = getStringOrNull(arguments,"accessToken");
        final String gcmSenderId = getStringOrNull(arguments,"gcmSenderId");
        final String channelName = getStringOrNull(arguments,"channelName");
        final String channelId = getStringOrNull(arguments,"channelId");
        final String smallIcon = getStringOrNull(arguments,"smallIcon");
        final String largeIcon = getStringOrNull(arguments,"largeIcon");

        try {
            String packageName = getReactApplicationContext().getPackageName();
            final Resources resources = getReactApplicationContext().getResources();

            int smallIconId;
            if (smallIcon != null) {
                smallIconId = resources.getIdentifier(smallIcon, "mipmap", packageName);
            } else {
                smallIconId = resources.getIdentifier("ic_notification", "mipmap", packageName);
            }

            int largeIconId;
            if (largeIcon != null) {
                largeIconId = resources.getIdentifier(largeIcon, "mipmap", packageName);
            } else {
                largeIconId = resources.getIdentifier("ic_launcher", "mipmap", packageName);
            }
            final Bitmap largeIconBitmap = BitmapFactory.decodeResource(resources, largeIconId);

            NotificationManager.NotificationBuilder notificationBuilder = new NotificationManager.NotificationBuilder() {
                @Override
                public NotificationCompat.Builder setupNotificationBuilder(@NonNull Context context, @NonNull NotificationMessage notificationMessage) {
                    NotificationCompat.Builder builder =
                            NotificationManager.setupNotificationBuilder(context, notificationMessage);

                    builder.setLargeIcon(largeIconBitmap);

                    return builder;
                }
            };

            MarketingCloudConfig.Builder config = MarketingCloudConfig
                    .builder()
                    .setApplicationId(applicationId)
                    .setAccessToken(accessToken)
                    .setGcmSenderId(gcmSenderId)
                    .setNotificationChannelName(channelName)
                    .setNotificationSmallIconResId(smallIconId)
                    .setNotificationBuilder(notificationBuilder);




            if (channelId != null) {
                config.setNotificationChannelIdProvider(new NotificationManager.NotificationChannelIdProvider() {
                    @Override
                    public String getNotificationChannelId(@NonNull Context context, @NonNull NotificationMessage notificationMessage) {
                        return channelId;
                    }
                });
            }

            MarketingCloudSdk.InitializationListener listener = new MarketingCloudSdk.InitializationListener() {

                @Override
                public void complete(@NonNull InitializationStatus initializationStatus) {
                    if (initializationStatus.isUsable()) {
                        promise.resolve(null);
                    } else {
                        promise.reject(new Exception("SFMobilePush could not be initialized"));
                    }
                }
            };

            MarketingCloudSdk.init(
                    this.application,
                    config.build(),
                    listener
            );
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void addTag(final String tag, final Promise promise) {
        try {
            MarketingCloudSdk.requestSdk(new MarketingCloudSdk.WhenReadyListener() {
                @Override
                public void ready(@NonNull MarketingCloudSdk marketingCloudSdk) {
                    marketingCloudSdk.getRegistrationManager()
                            .edit()
                            .addTag(tag)
                            .commit();
                    promise.resolve(null);
                }
            });


        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void removeTag(final String tag, final Promise promise) {
        try {
            MarketingCloudSdk.requestSdk(new MarketingCloudSdk.WhenReadyListener() {
                @Override
                public void ready(@NonNull MarketingCloudSdk marketingCloudSdk) {
                    marketingCloudSdk.getRegistrationManager()
                            .edit()
                            .removeTag(tag)
                            .commit();
                    promise.resolve(null);
                }
            });


        } catch (Exception e) {
            promise.reject(e);
        }
    }


    @ReactMethod
    public void getTags(final Promise promise) {
        try {
            MarketingCloudSdk.requestSdk(new MarketingCloudSdk.WhenReadyListener() {
                @Override
                public void ready(@NonNull MarketingCloudSdk marketingCloudSdk) {
                    Set<String> tags = marketingCloudSdk.getRegistrationManager().getTags();
                    WritableArray array = new WritableNativeArray();
                    for (String tag : tags) {
                        array.pushString(tag);
                    }
                    promise.resolve(array);
                }
            });

        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void getSubscriberKey(final Promise promise) {
        try {
            MarketingCloudSdk.requestSdk(new MarketingCloudSdk.WhenReadyListener() {
                @Override
                public void ready(@NonNull MarketingCloudSdk marketingCloudSdk) {
                    //On the Android Version the SDK names SubscriberKey as ContactKey
                    String subscriberKey = marketingCloudSdk.getRegistrationManager()
                            .getContactKey();
                    promise.resolve(subscriberKey);
                }
            });

        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void setSubscriberKey(final String subscriberKey, final Promise promise) {
        try {
            MarketingCloudSdk.requestSdk(new MarketingCloudSdk.WhenReadyListener() {
                @Override
                public void ready(@NonNull MarketingCloudSdk marketingCloudSdk) {
                    //On the Android Version the SDK names SubscriberKey as ContactKey
                    marketingCloudSdk.getRegistrationManager()
                            .edit()
                            .setContactKey(subscriberKey)
                            .commit();
                    promise.resolve(null);
                }
            });

        } catch (Exception e) {
            promise.reject(e);
        }
    }


    @ReactMethod
    public void setAttribute(final String key, final String value, final Promise promise) {
        try {
            MarketingCloudSdk.requestSdk(new MarketingCloudSdk.WhenReadyListener() {
                @Override
                public void ready(@NonNull MarketingCloudSdk marketingCloudSdk) {
                    //On the Android Version the SDK names SubscriberKey as ContactKey
                    marketingCloudSdk.getRegistrationManager()
                            .edit()
                            .setAttribute(key, value)
                            .commit();
                    promise.resolve(null);
                }
            });

        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void getAttributes(final Promise promise) {
        try {
            MarketingCloudSdk.requestSdk(new MarketingCloudSdk.WhenReadyListener() {
                @Override
                public void ready(@NonNull MarketingCloudSdk marketingCloudSdk) {
                    //On the Android Version the SDK names SubscriberKey as ContactKey
                    Map<String, String> attributesMap = marketingCloudSdk.getRegistrationManager().getAttributesMap();
                    WritableMap attributes = new WritableNativeMap();
                    for (Map.Entry<String, String> entry : attributesMap.entrySet()) {
                        attributes.putString(entry.getKey(), entry.getValue());
                    }
                    promise.resolve(attributes);
                }
            });

        } catch (Exception e) {
            promise.reject(e);
        }
    }


}
