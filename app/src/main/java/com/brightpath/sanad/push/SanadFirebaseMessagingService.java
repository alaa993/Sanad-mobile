package com.brightpath.sanad.push;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

public class SanadFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "SanadFCM";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        new PushDeviceRepository(this).registerToken(token, new PushDeviceRepository.SimpleCb() {
            @Override public void ok() {}
            @Override public void err(Throwable t) {
                Log.w(TAG, "register token failed", t);
            }
        });
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        String title = message.getNotification() != null ? message.getNotification().getTitle() : getString(com.brightpath.sanad.R.string.app_name);
        String body = message.getNotification() != null ? message.getNotification().getBody() : "";
        Map<String, String> data = message.getData();
        if ((body == null || body.isEmpty()) && data != null) {
            body = data.get("body");
        }
        if (body != null && !body.isEmpty()) {
            String type = data != null ? data.get("type") : null;
            String sessionId = data != null ? data.get("session_id") : null;
            String specialistId = data != null ? data.get("specialist_id") : null;
            android.app.PendingIntent pi = PushRegistrar.buildDeepLinkIntent(this, type, sessionId, specialistId);
            PushRegistrar.showLocalNotification(this, title != null ? title : getString(com.brightpath.sanad.R.string.app_name), body, pi);
        }
    }
}
