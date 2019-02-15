package co.work.react_native_sfmobilepush;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.salesforce.marketingcloud.MarketingCloudSdk;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SFMobilePushModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    static public final String SF_BUNDLE_IDENTIFIER = "SF_BUNDLE_IDENTIFIER";
    static public final String EVENT_SF_NOTIFICATION = "SFMobilePushNotificationReceived";

    public SFMobilePushModule(ReactApplicationContext reactContext) {
        super(reactContext);
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
