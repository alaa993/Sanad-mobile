package com.brightpath.sanad.feature.chat;
import java.util.List;
public class ChatModels {
    public static class UserRef { public int id; public String name; public String role; }
    public static class Chat { public int id; public String subject; public String last_message; public String updated_at; public List<UserRef> participants; public int unread_count; }
    public static class Message { public int id; public int chat_id; public UserRef sender; public String type; public String body; public String created_at; }
    public static class ChatListResponse { public java.util.List<Chat> data; }
    public static class MessageListResponse { public java.util.List<Message> data; }
}
