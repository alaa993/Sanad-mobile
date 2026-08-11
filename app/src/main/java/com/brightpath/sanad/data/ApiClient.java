package com.brightpath.sanad.data;

import android.content.Context;
import com.brightpath.sanad.data.auth.AuthInterceptor;
import com.brightpath.sanad.data.auth.TokenAuthenticator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class ApiClient {
    private static Retrofit retrofit = null;
    private static OkHttpClient client = null;

    public static Retrofit get(Context context) {  // ✅ نمرّر الـ context هنا
        if (retrofit == null) {
            Gson gson = new GsonBuilder().setLenient().create();
            if (client == null) {
                client = buildClient(context.getApplicationContext());
            }

            retrofit = new Retrofit.Builder()
                    .baseUrl(ensureTrailingSlash(AppConfig.BASE_URL))
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    private static String ensureTrailingSlash(String url) {
        if (url == null || url.isEmpty()) return "https://dashboard.sanadhub.cloud/";
        return url.endsWith("/") ? url : url + "/";
    }

    private static OkHttpClient buildClient(Context context) {
        Cache cache = new Cache(context.getCacheDir(), 10 * 1024 * 1024);
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(32);
        dispatcher.setMaxRequestsPerHost(8);

        Interceptor cacheInterceptor = chain -> {
            okhttp3.Request request = chain.request();
            Response response = chain.proceed(request);
            if (!"GET".equalsIgnoreCase(request.method())) {
                return response;
            }
            String path = request.url().encodedPath();
            // Authenticated feeds that change often stay fresh.
            // Catalog/directory GETs can be reused briefly to make screens feel instant.
            boolean noStore = path.contains("/api/")
                    && !path.contains("/directory/")
                    && !path.contains("/library/")
                    && !path.contains("/v1/tips");
            if (noStore) {
                return response.newBuilder()
                        .header("Cache-Control", "no-store, no-cache, must-revalidate")
                        .header("Pragma", "no-cache")
                        .removeHeader("Expires")
                        .build();
            }
            CacheControl cc = new CacheControl.Builder()
                    .maxAge(45, TimeUnit.SECONDS)
                    .build();
            return response.newBuilder()
                    .header("Cache-Control", cc.toString())
                    .removeHeader("Pragma")
                    .build();
        };

        return new OkHttpClient.Builder()
                .cache(cache)
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(8, 5, TimeUnit.MINUTES))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .callTimeout(35, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(context))
                .authenticator(new TokenAuthenticator(context))
                .addNetworkInterceptor(cacheInterceptor)
                .build();
    }
}
