package com.brightpath.sanad.feature.notifications;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface NotificationsApi {
    @GET("api/v1/notifications")
    Call<NotificationsApi.NotificationList> list();

    class NotificationItem {
        public int id;
        public String title;
        public String body;
        public String created_at;
        public boolean read;
    }

    class NotificationList {
        public List<NotificationItem> data;
    }
}
