package co.work.react_native_sfmobilepush;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.facebook.react.bridge.ActivityEventListener;
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
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.salesforce.marketingcloud.InitializationStatus;
import com.salesforce.marketingcloud.MarketingCloudConfig;
import com.salesforce.marketingcloud.MarketingCloudSdk;
import com.salesforce.marketingcloud.notifications.NotificationCustomizationOptions;
import com.salesforce.marketingcloud.notifications.NotificationManager;
import com.salesforce.marketingcloud.notifications.NotificationMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class SFMobilePushModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    static public final String SF_BUNDLE_IDENTIFIER = "SF_BUNDLE_IDENTIFIER";
    static public final String EVENT_SF_NOTIFICATION = "SFMobilePushNotificationReceived";

    private final Application application;
    private boolean isInitialized = false;

    public SFMobilePushModule(ReactApplicationContext reactContext, Application application) {
        super(reactContext);
        this.application = application;
        reactContext.addActivityEventListener(this);
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("notificationEvent", EVENT_SF_NOTIFICATION);
        return constants;
    }

    @Override
    public String getName() {
        return "SFMobilePush";
    }

    private JSONObject bundleToJson(Bundle bundle) throws JSONException {
        JSONObject json = new JSONObject();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            Object value = bundle.get(key);
            if (value instanceof Bundle) {
                json.put(key, bundleToJson((Bundle) value));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                json.put(key, JSONObject.wrap(value));
            } else {
                json.put(key, value);
            }
        }
        return json;
    }

    private String bundleToJsonString(Bundle bundle) {
        try {
            JSONObject json = bundleToJson(bundle);
            return json.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    public void onNewIntent(Intent intent) {
        Bundle bundle = intent.getExtras();

        if (bundle != null && bundle.getBoolean(SF_BUNDLE_IDENTIFIER)) {
            String bundleJSON = bundleToJsonString(bundle);
            if (bundleJSON != null) {
                getReactApplicationContext()
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit(EVENT_SF_NOTIFICATION, bundleJSON);
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Do nothing. This method is required to comply with ActivityEventListener
    }

    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        //Do nothing. This method is required to comply with ActivityEventListener
    }

    public Class getMainActivityClass() {
        String packageName = application.getPackageName();
        Intent launchIntent = application.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
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

    private String createNotificationChannel(String channelId, String channelName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, android.app.NotificationManager.IMPORTANCE_DEFAULT);
            android.app.NotificationManager notificationManager = this.application.getSystemService(android.app.NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        return channelId;
    }

    @ReactMethod
    public void init(ReadableMap arguments, final Promise promise) {
        if (isInitialized) {
            promise.resolve(null);
            return;
        }
        final String applicationId = getStringOrNull(arguments, "appId");
        final String accessToken = getStringOrNull(arguments, "accessToken");
        final String senderId = getStringOrNull(arguments, "senderId");
        final String channelName = getStringOrNull(arguments, "channelName");
        final String configChannelId = getStringOrNull(arguments, "channelId");
        final String channelId = configChannelId != null ? createNotificationChannel(configChannelId, channelName) : NotificationManager.createDefaultNotificationChannel(this.application);
        final String smallIcon = getStringOrNull(arguments, "smallIcon");
        final String largeIcon = getStringOrNull(arguments, "largeIcon");

        try {
            String packageName = getReactApplicationContext().getPackageName();
            final Resources resources = getReactApplicationContext().getResources();

            final int smallIconId;
            if (smallIcon != null) {
                smallIconId = resources.getIdentifier(smallIcon, "mipmap", packageName);
            } else {
                smallIconId = resources.getIdentifier("ic_notification", "mipmap", packageName);
            }

            final int largeIconId;
            if (largeIcon != null) {
                largeIconId = resources.getIdentifier(largeIcon, "mipmap", packageName);
            } else {
                largeIconId = resources.getIdentifier("ic_launcher", "mipmap", packageName);
            }
            final Bitmap largeIconBitmap = BitmapFactory.decodeResource(resources, largeIconId);
            final NotificationManager.NotificationBuilder notificationBuilder = new NotificationManager.NotificationBuilder() {
                @Override
                public NotificationCompat.Builder setupNotificationBuilder(@NonNull Context context, @NonNull NotificationMessage notificationMessage) {
                    NotificationCompat.Builder builder =
                            NotificationManager.getDefaultNotificationBuilder(context, notificationMessage, channelId, smallIconId);
                    builder.setLargeIcon(largeIconBitmap);

                    Bundle bundle = new Bundle();
                    bundle.putBoolean(SF_BUNDLE_IDENTIFIER, true);
                    for (Map.Entry<String, String> entry : notificationMessage.payload().entrySet()) {
                        bundle.putString(entry.getKey(), entry.getValue());
                    }

                    Intent intent = new Intent(context, getMainActivityClass());
                    intent.putExtras(bundle);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, new Random().nextInt(), intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

                    PendingIntent contentIntent = NotificationManager.redirectIntentForAnalytics(context, pendingIntent, notificationMessage, true);
                    builder.setContentIntent(contentIntent);
                    return builder;
                }
            };

            final NotificationCustomizationOptions customizationOptions = NotificationCustomizationOptions.create(notificationBuilder);

            final MarketingCloudConfig config =
                    MarketingCloudConfig
                            .builder()
                            .setApplicationId(applicationId)
                            .setAccessToken(accessToken)
                            .setSenderId(senderId)
                            .setNotificationCustomizationOptions(customizationOptions)
                            .build(this.application);

            final MarketingCloudSdk.InitializationListener listener = new MarketingCloudSdk.InitializationListener() {
                @Override
                public void complete(@NonNull InitializationStatus initializationStatus) {
                    if (initializationStatus.isUsable()) {
                        promise.resolve(null);
                    } else {
                        promise.reject(new Exception("SFMobilePush could not be initialized"));
                    }
                }
            };


            MarketingCloudSdk.init(this.application, config, listener);
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
                    Map<String, String> attributesMap = marketingCloudSdk.getRegistrationManager().getAttributes();
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
