
package com.brightpath.sanad.feature.community;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.Nullable;

import com.brightpath.sanad.data.ApiClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.*;

/**
 * Community REST: list/join/feed/post/like/comment plus multipart media upload from content URIs.
 */
public class CommunityRepository {
  private final CommunityApi api;
  private final Context appContext;
  public CommunityRepository(Context ctx){
    this.appContext = ctx.getApplicationContext();
    api = ApiClient.get(ctx).create(CommunityApi.class);
  }
  public interface Cb<T>{ void ok(T t); void err(Throwable e); }

  public static class JournalLockedException extends Exception {
    public JournalLockedException(String message) { super(message); }
  }

  private static <T> Callback<T> wrap(Cb<T> cb){ return new Callback<T>(){
    @Override public void onResponse(Call<T> c, Response<T> r){
      if(r.isSuccessful() && r.body()!=null) cb.ok(r.body());
      else if (r.code() == 403) cb.err(new JournalLockedException("journal_locked_until_recovery"));
      else cb.err(new Exception("HTTP "+r.code()));
    }
    @Override public void onFailure(Call<T> c, Throwable t){ cb.err(t); }
  };}
  public void communities(Cb<CommunityModels.ListResponse<CommunityModels.Community>> cb){ communities(null, cb); }
  public void communities(@Nullable String category, Cb<CommunityModels.ListResponse<CommunityModels.Community>> cb){
    api.communities(category).enqueue(wrap(cb));
  }
  public void community(int id, Cb<CommunityModels.ItemResponse<CommunityModels.Community>> cb){ api.community(id).enqueue(wrap(cb)); }
  public void join(int id, Cb<CommunityModels.SimpleResponse> cb){ api.join(id).enqueue(wrap(cb)); }
  public void leave(int id, Cb<CommunityModels.SimpleResponse> cb){ api.leave(id).enqueue(wrap(cb)); }
  public void feed(int id, Cb<CommunityModels.ListResponse<CommunityModels.Post>> cb){ api.feed(id).enqueue(wrap(cb)); }
  public void post(int id, String text, String mediaUrl, Cb<CommunityModels.SimpleResponse> cb){
    post(id, text, mediaUrl, null, null, cb);
  }
  public void post(int id, String text, String mediaUrl, String postKind, Integer questionId, Cb<CommunityModels.SimpleResponse> cb){
    Map<String,Object> b=new HashMap<>();
    b.put("body",text);
    if(mediaUrl!=null && !mediaUrl.isEmpty()) b.put("media_url",mediaUrl);
    if(postKind!=null) b.put("post_kind", postKind);
    if(questionId!=null) b.put("question_id", questionId);
    api.post(id,b).enqueue(wrap(cb));
  }
  public void acceptAnswer(int communityId, int questionId, int answerId, Cb<CommunityModels.SimpleResponse> cb){
    api.acceptAnswer(communityId, questionId, answerId).enqueue(wrap(cb));
  }
  public void toggleLike(int communityId, int postId, Cb<CommunityModels.SimpleResponse> cb){ api.toggleLike(communityId, postId).enqueue(wrap(cb)); }
  public void comment(int communityId, int postId, String text, Cb<CommunityModels.SimpleResponse> cb){ Map<String,String> b=new HashMap<>(); b.put("body",text); api.comment(communityId, postId, b).enqueue(wrap(cb)); }
  public void uploadMedia(Uri uri, Cb<CommunityModels.UploadResponse> cb){
    try{
      InputStream source = appContext.getContentResolver().openInputStream(uri);
      if(source==null) throw new IOException("Unable to open file");
      byte[] data = readAll(source);
      if(data.length > 50 * 1024 * 1024) throw new IOException("File too large");
      String mime = appContext.getContentResolver().getType(uri);
      MediaType mediaType = mime!=null? MediaType.parse(mime) : MediaType.parse("application/octet-stream");
      String name = queryFileName(uri);
      RequestBody body = RequestBody.create(data, mediaType);
      MultipartBody.Part part = MultipartBody.Part.createFormData("file", name, body);
      api.uploadMedia(part).enqueue(wrap(cb));
    } catch (Exception e){
      cb.err(e);
    }
  }
  public void articles(Cb<CommunityModels.ListResponse<CommunityModels.Article>> cb){ api.articles(null).enqueue(wrap(cb)); }
  public void journal(Cb<CommunityModels.ListResponse<CommunityModels.Journal>> cb){ api.journal().enqueue(wrap(cb)); }
  public void addJournal(String entry, Cb<CommunityModels.SimpleResponse> cb){ java.util.Map<String,String> b=new java.util.HashMap<>(); b.put("entry",entry); api.addJournal(b).enqueue(wrap(cb)); }
  public void delJournal(int id, Cb<CommunityModels.SimpleResponse> cb){ api.delJournal(id).enqueue(wrap(cb)); }
  public void createCommunity(String slug, String nameAr, String about, String visibility, String kind, Cb<CommunityModels.SimpleResponse> cb){
    Map<String,Object> body = new HashMap<>();
    Map<String,String> name = new HashMap<>();
    name.put("ar", nameAr);
    name.put("en", nameAr);
    body.put("slug", slug);
    body.put("name", name);
    body.put("visibility", visibility != null ? visibility : "public");
    body.put("kind", kind != null ? kind : "discussion");
    if (about != null && !about.isEmpty()) {
      Map<String,String> aboutMap = new HashMap<>();
      aboutMap.put("ar", about);
      body.put("about", aboutMap);
    }
    api.create(body).enqueue(wrap(cb));
  }
  public void favoriteArticle(int id, Cb<CommunityModels.SimpleResponse> cb){ api.favoriteArticle(id).enqueue(wrap(cb)); }
  public void unfavoriteArticle(int id, Cb<CommunityModels.SimpleResponse> cb){ api.unfavoriteArticle(id).enqueue(wrap(cb)); }
  private byte[] readAll(InputStream is) throws IOException{
    try(InputStream input = is; ByteArrayOutputStream bos = new ByteArrayOutputStream()){
      byte[] buffer = new byte[8192];
      int len;
      while((len = input.read(buffer)) != -1){
        bos.write(buffer,0,len);
      }
      return bos.toByteArray();
    }
  }
  private String queryFileName(Uri uri){
    String name = "attachment_"+System.currentTimeMillis();
    ContentResolver resolver = appContext.getContentResolver();
    try(Cursor cursor = resolver.query(uri,null,null,null,null)){
      if(cursor!=null && cursor.moveToFirst()){
        int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        if(idx>=0){
          String value = cursor.getString(idx);
          if(value!=null) name = value;
        }
      }
    } catch (Exception ignored){}
    return name;
  }
}
