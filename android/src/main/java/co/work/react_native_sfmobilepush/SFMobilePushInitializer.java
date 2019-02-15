package co.work.react_native_sfmobilepush;

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

import com.salesforce.marketingcloud.InitializationStatus;
import com.salesforce.marketingcloud.MarketingCloudConfig;
import com.salesforce.marketingcloud.MarketingCloudSdk;
import com.salesforce.marketingcloud.notifications.NotificationCustomizationOptions;
import com.salesforce.marketingcloud.notifications.NotificationManager;
import com.salesforce.marketingcloud.notifications.NotificationMessage;

import java.util.Map;
import java.util.Random;

public class SFMobilePushInitializer {


    private static String createNotificationChannel(Application application, String channelId, String channelName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, android.app.NotificationManager.IMPORTANCE_DEFAULT);
            android.app.NotificationManager notificationManager = application.getSystemService(android.app.NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        return channelId;
    }

    public static void init(final Class mainActivity, Application application, String applicationId, String accessToken, String senderId, String channelName, String configChannelId) {
        final String packageName = application.getPackageName();
        final Resources resources = application.getResources();
        final String channelId = configChannelId != null ? createNotificationChannel(application, configChannelId, channelName) : NotificationManager.createDefaultNotificationChannel(application);

        final int smallIconId = resources.getIdentifier("ic_notification", "mipmap", packageName);

        final int largeIconId = resources.getIdentifier("ic_launcher", "mipmap", packageName);
        final Bitmap largeIconBitmap = BitmapFactory.decodeResource(resources, largeIconId);

        final NotificationManager.NotificationBuilder notificationBuilder = new NotificationManager.NotificationBuilder() {
            @Override
            public NotificationCompat.Builder setupNotificationBuilder(@NonNull Context context, @NonNull NotificationMessage notificationMessage) {
                NotificationCompat.Builder builder =
                        NotificationManager.getDefaultNotificationBuilder(context, notificationMessage, channelId, smallIconId);
                builder.setLargeIcon(largeIconBitmap);

                Bundle bundle = new Bundle();
                bundle.putBoolean(SFMobilePushModule.SF_BUNDLE_IDENTIFIER, true);
                for (Map.Entry<String, String> entry : notificationMessage.payload().entrySet()) {
                    bundle.putString(entry.getKey(), entry.getValue());
                }

                Intent intent = new Intent(context, mainActivity);
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
                        .build(application);

        final MarketingCloudSdk.InitializationListener listener = new MarketingCloudSdk.InitializationListener() {
            @Override
            public void complete(@NonNull InitializationStatus initializationStatus) {
            }
        };


        MarketingCloudSdk.init(application, config, listener);
    }
}
