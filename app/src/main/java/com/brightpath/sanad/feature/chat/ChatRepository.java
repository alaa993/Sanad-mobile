package com.brightpath.sanad.feature.chat;
import android.content.Context;
import com.brightpath.sanad.data.ApiClient;
import retrofit2.*;

/** Chat REST: thread list, messages (optional since), send, create. Room UI merges socket events on top. */
public class ChatRepository {
    private final ChatApi api;
    public ChatRepository(Context ctx){ api = ApiClient.get(ctx).create(ChatApi.class); }
    public interface ListCb { void ok(java.util.List<ChatModels.Chat> list); void err(Throwable t); }
    public interface MsgCb  { void ok(java.util.List<ChatModels.Message> msgs); void err(Throwable t); }
    public interface SendCb { void ok(ChatModels.Message m); void err(Throwable t); }
    public interface CreateCb { void ok(int chatId); void err(Throwable t); }
    public void list(ListCb cb){ api.getChats().enqueue(new Callback<ChatModels.ChatListResponse>(){
        @Override public void onResponse(Call<ChatModels.ChatListResponse> c, Response<ChatModels.ChatListResponse> r){ if(r.isSuccessful()&&r.body()!=null) cb.ok(r.body().data); else cb.err(new Exception("HTTP "+r.code())); }
        @Override public void onFailure(Call<ChatModels.ChatListResponse> c, Throwable t){ cb.err(t); }
    });}
    public void messages(int chatId, String since, MsgCb cb){ api.getMessages(chatId, since).enqueue(new Callback<ChatModels.MessageListResponse>(){
        @Override public void onResponse(Call<ChatModels.MessageListResponse> c, Response<ChatModels.MessageListResponse> r){ if(r.isSuccessful()&&r.body()!=null) cb.ok(r.body().data); else cb.err(new Exception("HTTP "+r.code())); }
        @Override public void onFailure(Call<ChatModels.MessageListResponse> c, Throwable t){ cb.err(t); }
    });}
    public void send(int chatId, String type, String body, SendCb cb){
        java.util.Map<String,String> b = new java.util.HashMap<>(); b.put("type", type); b.put("body", body);
        api.send(chatId, b).enqueue(new Callback<ChatModels.Message>() {
            @Override public void onResponse(Call<ChatModels.Message> c, Response<ChatModels.Message> r){ if(r.isSuccessful()&&r.body()!=null) cb.ok(r.body()); else cb.err(new Exception("HTTP "+r.code())); }
            @Override public void onFailure(Call<ChatModels.Message> c, Throwable t){ cb.err(t); }
        });
    }

    public void createChat(java.util.List<Integer> participantIds, String subject, CreateCb cb){
        java.util.Map<String,Object> body = new java.util.HashMap<>();
        body.put("participant_ids", participantIds);
        if (subject != null && !subject.trim().isEmpty()) {
            body.put("subject", subject.trim());
        }
        api.createChat(body).enqueue(new Callback<java.util.Map<String,Integer>>() {
            @Override public void onResponse(Call<java.util.Map<String,Integer>> call, Response<java.util.Map<String,Integer>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    cb.err(new Exception("HTTP " + response.code()));
                    return;
                }
                java.util.Map<String,Integer> map = response.body();
                Integer chatId = map.get("chat_id");
                if (chatId == null) chatId = map.get("id");
                if (chatId == null || chatId <= 0) {
                    cb.err(new Exception("invalid chat id"));
                    return;
                }
                cb.ok(chatId);
            }
            @Override public void onFailure(Call<java.util.Map<String,Integer>> call, Throwable t) { cb.err(t); }
        });
    }
}
