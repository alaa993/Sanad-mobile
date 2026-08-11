
package com.brightpath.sanad.feature.specialist;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.brightpath.sanad.data.ApiClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SpecialistRepository {
  private final SpecialistApi api;
  private final Context context;
  public SpecialistRepository(Context ctx){
    context = ctx.getApplicationContext();
    api = ApiClient.get(context).create(SpecialistApi.class);
  }
  public interface Cb<T>{ void ok(T t); void err(Throwable e); }
  private static <T> Callback<T> wrap(Cb<T> cb){ return new Callback<T>(){ @Override public void onResponse(Call<T> c, Response<T> r){ if(r.isSuccessful()&&r.body()!=null) cb.ok(r.body()); else cb.err(new Exception("HTTP "+r.code())); } @Override public void onFailure(Call<T> c, Throwable t){ cb.err(t); } }; }
  public void dashboard(Cb<SpecialistModels.Dashboard> cb){ api.dashboard().enqueue(wrap(cb)); }
  public void sessions(String scope, Cb<SpecialistModels.Appointments> cb){ api.sessions(scope).enqueue(wrap(cb)); }
  public void accept(int id, Cb<SpecialistModels.Simple> cb){ api.accept(id).enqueue(wrap(cb)); }
  public void reject(int id, String reason, Cb<SpecialistModels.Simple> cb){
    java.util.Map<String,Object> b = new java.util.HashMap<>();
    if (reason != null && !reason.trim().isEmpty()) {
      b.put("reason", reason.trim());
    }
    api.reject(id, b).enqueue(wrap(cb));
  }
  public void reschedule(int id, String starts, String ends, Cb<SpecialistModels.Simple> cb){
    Map<String,Object> b=new HashMap<>();
    b.put("starts_at", starts);
    b.put("ends_at", ends);
    b.put("timezone", java.time.ZoneId.systemDefault().getId());
    api.reschedule(id,b).enqueue(wrap(cb));
  }
  public void extend(int id, int minutes, Cb<SpecialistModels.Simple> cb){
    Map<String,Object> b=new HashMap<>();
    b.put("minutes", minutes);
    api.extend(id, b).enqueue(wrap(cb));
  }
  public void complete(int id, Cb<SpecialistModels.Simple> cb){ api.complete(id).enqueue(wrap(cb)); }
  public void profile(Cb<SpecialistModels.Profile> cb){ api.profile().enqueue(wrap(cb)); }
  public void update(Map<String,Object> body, Cb<SpecialistModels.Simple> cb){ api.update(body).enqueue(wrap(cb)); }
  public void documents(Cb<SpecialistModels.DocumentList> cb){ api.documents().enqueue(wrap(cb)); }
  public void patients(Cb<SpecialistModels.Patients> cb){ api.patients().enqueue(wrap(cb)); }
  public void uploadDocument(String type, Uri uri, Cb<SpecialistModels.Document> cb){
    try {
      ContentResolver resolver = context.getContentResolver();
      String mime = resolver.getType(uri);
      if (mime == null) mime = "application/octet-stream";
      String name = queryDisplayName(resolver, uri);
      if (name == null) name = "document_" + System.currentTimeMillis();
      byte[] bytes;
      try (InputStream is = resolver.openInputStream(uri)) {
        bytes = readBytes(is);
      }
      RequestBody fileBody = RequestBody.create(MediaType.parse(mime), bytes);
      MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", name, fileBody);
      RequestBody typeBody = RequestBody.create(MultipartBody.FORM, type);
      RequestBody titleBody = RequestBody.create(MultipartBody.FORM, name);
      api.uploadDocument(typeBody, titleBody, filePart).enqueue(wrap(cb));
    } catch (Exception e){
      cb.err(e);
    }
  }
  public void uploadAvatar(Uri uri, Cb<String> cb){
    try {
      ContentResolver resolver = context.getContentResolver();
      String mime = resolver.getType(uri);
      if (mime == null) mime = "image/jpeg";
      String name = queryDisplayName(resolver, uri);
      if (name == null) name = "avatar_" + System.currentTimeMillis();
      byte[] bytes;
      try (InputStream is = resolver.openInputStream(uri)) { bytes = readBytes(is); }
      RequestBody fileBody = RequestBody.create(MediaType.parse(mime), bytes);
      MultipartBody.Part avatar = MultipartBody.Part.createFormData("avatar", name, fileBody);
      api.uploadAvatar(avatar).enqueue(new Callback<java.util.Map<String,String>>(){
        @Override public void onResponse(Call<java.util.Map<String,String>> call, Response<java.util.Map<String,String>> response) {
          if (response.isSuccessful() && response.body()!=null && response.body().get("url")!=null){
            cb.ok(response.body().get("url"));
          } else {
            cb.err(new Exception("HTTP "+response.code()));
          }
        }
        @Override public void onFailure(Call<java.util.Map<String,String>> call, Throwable t) { cb.err(t); }
      });
    } catch (Exception e){
      cb.err(e);
    }
  }
  public void deleteDocument(int id, Cb<SpecialistModels.Simple> cb){ api.deleteDocument(id).enqueue(wrap(cb)); }

  private static byte[] readBytes(InputStream in) throws Exception {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] data = new byte[4096];
    int nRead;
    while ((nRead = in.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }
    buffer.flush();
    return buffer.toByteArray();
  }

  private static String queryDisplayName(ContentResolver resolver, Uri uri) {
    Cursor cursor = resolver.query(uri, null, null, null, null);
    if (cursor != null) {
      int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
      if (nameIndex >= 0 && cursor.moveToFirst()) {
        String name = cursor.getString(nameIndex);
        cursor.close();
        return name;
      }
      cursor.close();
    }
    return null;
  }
}
