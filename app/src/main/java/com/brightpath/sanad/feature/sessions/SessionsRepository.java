package com.brightpath.sanad.feature.sessions;
import android.content.Context;
import androidx.annotation.NonNull;
import com.brightpath.sanad.data.ApiClient;
import java.util.Map;
import retrofit2.*;

/** Thin Retrofit wrapper for /sessions list and detail used by SessionsViewModel. */
public class SessionsRepository {
    private final SessionsApi api;
    public SessionsRepository(Context ctx){
        api = ApiClient.get(ctx).create(SessionsApi.class);
    }
    public interface ListListener{ void onSuccess(SessionModels.SessionList d); void onError(Throwable t); }
    public interface OneListener{ void onSuccess(SessionModels.Session d); void onError(Throwable t); }

    public void list(ListListener l){
        list(null, l);
    }

    public void list(Map<String,String> params, ListListener l){
        Call<SessionModels.SessionList> call = (params == null || params.isEmpty())
                ? api.getSessions()
                : api.getSessions(params);
        call.enqueue(new Callback<SessionModels.SessionList>(){
            @Override public void onResponse(@NonNull Call<SessionModels.SessionList> c, @NonNull Response<SessionModels.SessionList> r){
                if(!r.isSuccessful()||r.body()==null){ l.onError(new RuntimeException("bad_response")); return; } l.onSuccess(r.body());
            }
            @Override public void onFailure(@NonNull Call<SessionModels.SessionList> c, @NonNull Throwable t){ l.onError(t); }
        });
    }
    public void show(int id, OneListener l){
        api.getSession(id).enqueue(new Callback<SessionModels.Session>(){
            @Override public void onResponse(@NonNull Call<SessionModels.Session> c, @NonNull Response<SessionModels.Session> r){
                if(!r.isSuccessful()||r.body()==null){ l.onError(new RuntimeException("bad_response")); return; } l.onSuccess(r.body());
            }
            @Override public void onFailure(@NonNull Call<SessionModels.Session> c, @NonNull Throwable t){ l.onError(t); }
        });
    }
}
