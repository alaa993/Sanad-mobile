package com.brightpath.sanad.feature.chat;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.brightpath.sanad.data.auth.TokenStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
public class ChatViewModel extends AndroidViewModel {
    private final ChatRepository repo;
    private final MutableLiveData<java.util.List<ChatModels.Chat>> chats = new MutableLiveData<>(new java.util.ArrayList<>());
    private final MutableLiveData<java.lang.Boolean> chatsLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> chatsError = new MutableLiveData<>(null);
    private final MutableLiveData<java.util.List<ChatModels.Message>> messages = new MutableLiveData<>(new java.util.ArrayList<>());
    private int activeChatId = -1; private String lastSince = null;
    private final int myUserId;
    public ChatViewModel(@NonNull Application app){
        super(app);
        repo = new ChatRepository(app);
        myUserId = new TokenStore(app).getUserId();
    }
    public LiveData<java.util.List<ChatModels.Chat>> getChats(){ return chats; }
    public LiveData<Boolean> getChatsLoading(){ return chatsLoading; }
    public LiveData<String> getChatsError(){ return chatsError; }
    public LiveData<java.util.List<ChatModels.Message>> getMessages(){ return messages; }
    public void loadChats(){
        chatsLoading.postValue(true);
        chatsError.postValue(null);
        repo.list(new ChatRepository.ListCb(){
            @Override public void ok(java.util.List<ChatModels.Chat> list){
                chats.postValue(list);
                chatsLoading.postValue(false);
            }
            @Override public void err(Throwable t){
                chatsLoading.postValue(false);
                chatsError.postValue(t!=null ? t.getMessage() : "error");
            }
        });
    }
    public void openChat(int chatId){ activeChatId = chatId; lastSince = null; fetchNew(); }
    public void fetchNew(){ if(activeChatId<=0) return; repo.messages(activeChatId, lastSince, new ChatRepository.MsgCb(){
        @Override public void ok(java.util.List<ChatModels.Message> msgs){
            if(msgs!=null && !msgs.isEmpty()){
                java.util.List<ChatModels.Message> curr = messages.getValue(); if(curr==null) curr=new java.util.ArrayList<>();
                for (ChatModels.Message m : msgs) {
                    removePendingMatch(curr, m);
                    curr.add(m);
                }
                messages.postValue(curr); lastSince = msgs.get(msgs.size()-1).created_at;
            }
        }
        @Override public void err(Throwable t){} }); }
    public void send(String text){ sendInternal("text", text); }

    public void sendImage(String base64){ sendInternal("image", base64); }

    private void sendInternal(String type, String body){
        if(activeChatId<=0) return;
        ChatModels.Message pending = buildPendingMessage(type, body);
        int pendingId = pending != null ? pending.id : 0;
        if (pending != null) {
            java.util.List<ChatModels.Message> curr = messages.getValue();
            if (curr == null) curr = new java.util.ArrayList<>();
            curr.add(pending);
            messages.setValue(curr);
        }
        repo.send(activeChatId, type, body, new ChatRepository.SendCb(){
            @Override public void ok(ChatModels.Message m){
                java.util.List<ChatModels.Message> curr = messages.getValue(); if(curr==null) curr=new java.util.ArrayList<>();
                if (pendingId != 0) {
                    for (int i = 0; i < curr.size(); i++) {
                        ChatModels.Message existing = curr.get(i);
                        if (existing != null && existing.id == pendingId) {
                            curr.set(i, m);
                            messages.postValue(curr);
                            lastSince = m.created_at;
                            return;
                        }
                    }
                }
                for (ChatModels.Message existing : curr) {
                    if (existing != null && existing.id != 0 && existing.id == m.id) {
                        lastSince = m.created_at;
                        return;
                    }
                }
                curr.add(m); messages.postValue(curr); lastSince = m.created_at;
            }
            @Override public void err(Throwable t){} });
    }

    public void applyRealtimeMessage(ChatModels.Message m){
        if(m==null || m.chat_id != activeChatId) return;
        java.util.List<ChatModels.Message> curr = messages.getValue();
        if(curr==null) curr=new java.util.ArrayList<>();
        removePendingMatch(curr, m);
        for(ChatModels.Message existing : curr){
            if(existing!=null && existing.id!=0 && existing.id == m.id){
                return;
            }
        }
        curr.add(m);
        messages.postValue(curr);
        if(m.created_at!=null) lastSince = m.created_at;
    }

    private ChatModels.Message buildPendingMessage(String type, String body) {
        ChatModels.Message msg = new ChatModels.Message();
        int tempId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        if (tempId == 0) tempId = 1;
        msg.id = -tempId;
        msg.chat_id = activeChatId;
        msg.type = type;
        msg.body = body;
        msg.created_at = formatIso(System.currentTimeMillis());
        ChatModels.UserRef sender = new ChatModels.UserRef();
        sender.id = myUserId;
        msg.sender = sender;
        return msg;
    }

    private void removePendingMatch(java.util.List<ChatModels.Message> curr, ChatModels.Message incoming) {
        if (curr == null || incoming == null) return;
        for (int i = 0; i < curr.size(); i++) {
            ChatModels.Message existing = curr.get(i);
            if (existing == null || existing.id >= 0) continue;
            if (existing.sender != null && incoming.sender != null
                    && existing.sender.id != 0 && incoming.sender.id != 0
                    && existing.sender.id == incoming.sender.id
                    && safeEquals(existing.type, incoming.type)
                    && safeEquals(existing.body, incoming.body)) {
                curr.remove(i);
                return;
            }
        }
    }

    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private String formatIso(long millis) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        return fmt.format(new Date(millis));
    }
}
