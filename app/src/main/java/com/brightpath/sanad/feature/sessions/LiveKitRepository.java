package com.brightpath.sanad.feature.sessions;

import android.content.Context;
import android.util.Log;
import com.brightpath.sanad.data.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveKitRepository {
    private static final String TAG = "LiveKitRepo";
    private final LiveKitApi api;

    public LiveKitRepository(Context ctx){
        api = ApiClient.get(ctx).create(LiveKitApi.class);
    }

    public static class TokenResponse {
        public String token;
        public String url;
        public String room;
    }

    public interface TokenCb { void ok(TokenResponse resp); void err(Throwable t); }

    public void fetchSessionToken(int sessionId, TokenCb cb){
        api.sessionToken(sessionId).enqueue(new Callback<TokenResponse>() {
            @Override public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cb.ok(response.body());
                    return;
                }
                String code = String.valueOf(response.code());
                String err = "token_failed";
                try {
                    if (response.errorBody() != null) {
                        err = response.errorBody().string();
                    }
                } catch (Exception ignored) {}
                Log.e(TAG, "sessionToken failed: code=" + code + " body=" + err);
                cb.err(new RuntimeException("token_failed_" + code));
            }
            @Override public void onFailure(Call<TokenResponse> call, Throwable t) { cb.err(t); }
        });
    }

    public void fetchGroupToken(int groupId, TokenCb cb){
        api.groupToken(groupId).enqueue(new Callback<TokenResponse>() {
            @Override public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cb.ok(response.body());
                    return;
                }
                String code = String.valueOf(response.code());
                String err = "token_failed";
                try {
                    if (response.errorBody() != null) {
                        err = response.errorBody().string();
                    }
                } catch (Exception ignored) {}
                Log.e(TAG, "groupToken failed: code=" + code + " body=" + err);
                cb.err(new RuntimeException("token_failed_" + code));
            }
            @Override public void onFailure(Call<TokenResponse> call, Throwable t) { cb.err(t); }
        });
    }
}
