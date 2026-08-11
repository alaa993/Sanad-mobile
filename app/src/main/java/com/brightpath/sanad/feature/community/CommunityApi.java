
package com.brightpath.sanad.feature.community;
import okhttp3.MultipartBody;
import retrofit2.Call; import retrofit2.http.*; import java.util.Map;
public interface CommunityApi {
  @GET("api/v1/community") Call<CommunityModels.ListResponse<CommunityModels.Community>> communities(@Query("category") String category);
  @GET("api/v1/community/{id}") Call<CommunityModels.ItemResponse<CommunityModels.Community>> community(@Path("id") int id);
  @GET("api/v1/community/{id}/feed") Call<CommunityModels.ListResponse<CommunityModels.Post>> feed(@Path("id") int id);
  @POST("api/v1/community/{id}/post") Call<CommunityModels.SimpleResponse> post(@Path("id") int id, @Body Map<String,Object> body);
  @POST("api/v1/community") Call<CommunityModels.SimpleResponse> create(@Body Map<String,Object> body);
  @POST("api/v1/community/{community}/question/{question}/accept/{answer}") Call<CommunityModels.SimpleResponse> acceptAnswer(@Path("community") int communityId, @Path("question") int questionId, @Path("answer") int answerId);
  @POST("api/v1/community/{id}/post/{postId}/like") Call<CommunityModels.SimpleResponse> toggleLike(@Path("id") int id, @Path("postId") int postId);
  @POST("api/v1/community/{id}/post/{postId}/comment") Call<CommunityModels.SimpleResponse> comment(@Path("id") int id, @Path("postId") int postId, @Body Map<String,String> body);
  @Multipart @POST("api/v1/community/media") Call<CommunityModels.UploadResponse> uploadMedia(@Part MultipartBody.Part file);
  @POST("api/v1/community/{id}/join") Call<CommunityModels.SimpleResponse> join(@Path("id") int id);
  @POST("api/v1/community/{id}/leave") Call<CommunityModels.SimpleResponse> leave(@Path("id") int id);
  @GET("api/v1/articles") Call<CommunityModels.ListResponse<CommunityModels.Article>> articles(@Query("tag") String tag);
  @POST("api/v1/articles/{id}") Call<CommunityModels.SimpleResponse> favoriteArticle(@Path("id") int id);
  @DELETE("api/v1/articles/{id}") Call<CommunityModels.SimpleResponse> unfavoriteArticle(@Path("id") int id);
  @GET("api/v1/articles/{id}") Call<CommunityModels.ListResponse<CommunityModels.Article>> article(@Path("id") int id);
  @GET("api/v1/journal") Call<CommunityModels.ListResponse<CommunityModels.Journal>> journal();
  @POST("api/v1/journal") Call<CommunityModels.SimpleResponse> addJournal(@Body Map<String,String> body);
  @DELETE("api/v1/journal/{id}") Call<CommunityModels.SimpleResponse> delJournal(@Path("id") int id);
}
