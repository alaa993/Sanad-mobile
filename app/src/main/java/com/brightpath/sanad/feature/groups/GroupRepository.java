package com.brightpath.sanad.feature.groups;

import android.content.Context;
import com.brightpath.sanad.data.ApiClient;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupRepository {
    private final GroupApi api;
    public GroupRepository(Context ctx){ api = ApiClient.get(ctx).create(GroupApi.class); }

    public interface ListCb { void ok(GroupModels.GroupSessionList list); void err(Throwable t); }
    public interface DetailCb { void ok(GroupModels.GroupSession g); void err(Throwable t); }

    public void list(ListCb cb){ list(null, null, cb); }
    public void list(String ageCategory, String disorderTag, ListCb cb){
        api.list(ageCategory, disorderTag).enqueue(new Callback<GroupModels.GroupSessionList>() {
            @Override public void onResponse(Call<GroupModels.GroupSessionList> call, Response<GroupModels.GroupSessionList> response) {
                if (response.isSuccessful() && response.body()!=null) cb.ok(response.body());
                else cb.err(new RuntimeException("groups_fetch_failed"));
            }
            @Override public void onFailure(Call<GroupModels.GroupSessionList> call, Throwable t) { cb.err(t); }
        });
    }

    public void detail(int id, DetailCb cb){
        api.detail(id).enqueue(new Callback<GroupModels.GroupSession>() {
            @Override public void onResponse(Call<GroupModels.GroupSession> call, Response<GroupModels.GroupSession> response) {
                if (response.isSuccessful() && response.body()!=null) cb.ok(response.body());
                else cb.err(new RuntimeException("group_detail_failed"));
            }
            @Override public void onFailure(Call<GroupModels.GroupSession> call, Throwable t) { cb.err(t); }
        });
    }

    public void join(int id, DetailCb cb){
        Map<String,Object> body = new HashMap<>();
        api.join(id, body).enqueue(new Callback<GroupModels.GroupSession>() {
            @Override public void onResponse(Call<GroupModels.GroupSession> call, Response<GroupModels.GroupSession> response) {
                if (response.isSuccessful() && response.body()!=null) cb.ok(response.body());
                else cb.err(new RuntimeException("group_join_failed"));
            }
            @Override public void onFailure(Call<GroupModels.GroupSession> call, Throwable t) { cb.err(t); }
        });
    }

    public void leave(int id, DetailCb cb){
        Map<String,Object> body = new HashMap<>();
        api.leave(id, body).enqueue(new Callback<GroupModels.GroupSession>() {
            @Override public void onResponse(Call<GroupModels.GroupSession> call, Response<GroupModels.GroupSession> response) {
                if (response.isSuccessful() && response.body()!=null) cb.ok(response.body());
                else cb.err(new RuntimeException("group_leave_failed"));
            }
            @Override public void onFailure(Call<GroupModels.GroupSession> call, Throwable t) { cb.err(t); }
        });
    }

    public void create(String title, String topic, String type, String startAt, String endAt,
                       java.util.List<Integer> participantIds, String timezone, DetailCb cb){
        Map<String,Object> body = new HashMap<>();
        body.put("title", title);
        if (topic != null) body.put("topic", topic);
        body.put("type", type);
        body.put("start_at", startAt);
        body.put("end_at", endAt);
        if (participantIds != null && !participantIds.isEmpty()) {
            body.put("participant_ids", participantIds);
        }
        if (timezone != null) body.put("timezone", timezone);
        api.create(body).enqueue(new Callback<GroupModels.GroupSession>() {
            @Override public void onResponse(Call<GroupModels.GroupSession> call, Response<GroupModels.GroupSession> response) {
                if (response.isSuccessful() && response.body()!=null) cb.ok(response.body());
                else cb.err(new RuntimeException("group_create_failed"));
            }
            @Override public void onFailure(Call<GroupModels.GroupSession> call, Throwable t) { cb.err(t); }
        });
    }
}
