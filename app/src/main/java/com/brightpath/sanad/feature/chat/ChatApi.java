package com.brightpath.sanad.feature.chat;
import retrofit2.Call;
import retrofit2.http.*;
public interface ChatApi {
    @GET("api/v1/chats") Call<ChatModels.ChatListResponse> getChats();
    @POST("api/v1/chats") Call<java.util.Map<String,Integer>> createChat(@Body java.util.Map<String,Object> body);
    @GET("api/v1/chats/{id}/messages") Call<ChatModels.MessageListResponse> getMessages(@Path("id") int chatId, @Query("since") String sinceIso);
    @POST("api/v1/chats/{id}/messages") Call<ChatModels.Message> send(@Path("id") int chatId, @Body java.util.Map<String,String> body);
}
