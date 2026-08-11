package com.brightpath.sanad.feature.sessions;
import android.content.Context;
import androidx.annotation.NonNull;
import com.brightpath.sanad.data.ApiClient;
import retrofit2.*;

public class DirectoryRepository {
    private final DirectoryApi api;
    public DirectoryRepository(Context ctx){
        api = ApiClient.get(ctx).create(DirectoryApi.class);
    }
    public interface Listener { void onSuccess(DirectoryModels.Paged d); void onError(Throwable t); }
    public interface DetailListener { void onSuccess(DirectoryModels.Detail d); void onError(Throwable t); }
    public void load(boolean specialists, String search, Integer page, Listener l){
        load(specialists, search, page, null, null, null, l);
    }

    public void load(boolean specialists, String search, Integer page,
                     String specialty, String language, String minRating, Listener l){
        Callback<DirectoryModels.Paged> cb = new Callback<DirectoryModels.Paged>(){
            @Override public void onResponse(@NonNull Call<DirectoryModels.Paged> c, @NonNull Response<DirectoryModels.Paged> r){
                if(!r.isSuccessful()||r.body()==null){ l.onError(new RuntimeException("bad_response")); return; } l.onSuccess(r.body());
            }
            @Override public void onFailure(@NonNull Call<DirectoryModels.Paged> c, @NonNull Throwable t){ l.onError(t); }
        };
        if (specialists) api.specialists(search, page, specialty, language, minRating).enqueue(cb);
        else api.organizations(search, page).enqueue(cb);
    }

    public void detail(int specialistId, DetailListener l){
        api.specialist(specialistId).enqueue(new Callback<DirectoryModels.Detail>() {
            @Override public void onResponse(@NonNull Call<DirectoryModels.Detail> call, @NonNull Response<DirectoryModels.Detail> response) {
                if(!response.isSuccessful() || response.body()==null){ l.onError(new RuntimeException("bad_response")); return; }
                l.onSuccess(response.body());
            }
            @Override public void onFailure(@NonNull Call<DirectoryModels.Detail> call, @NonNull Throwable t) { l.onError(t); }
        });
    }
}
