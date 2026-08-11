package com.brightpath.sanad.feature.admin;

import android.content.Context;
import com.brightpath.sanad.data.ApiClient;
import retrofit2.*;
import androidx.annotation.Nullable;

/**
 * Admin API access. {@link #wrap} never lets ClassCastException / parse quirks
 * crash the UI thread (common on release builds with Gson type erasure).
 */
public class AdminRepository {
  private final AdminApi api;
  public AdminRepository(Context ctx){ api=ApiClient.get(ctx).create(AdminApi.class); }
  public interface Cb<T>{ void ok(T t); void err(Throwable e); }

  private static <T> Callback<T> wrap(Cb<T> cb){
    return new Callback<T>(){
      @Override public void onResponse(Call<T> c, retrofit2.Response<T> r){
        try {
          if (r.isSuccessful() && r.body() != null) {
            cb.ok(r.body());
          } else {
            cb.err(new Exception("HTTP " + r.code()));
          }
        } catch (ClassCastException | IllegalStateException cast) {
          cb.err(new Exception("bad_response_shape", cast));
        } catch (Throwable t) {
          cb.err(t);
        }
      }
      @Override public void onFailure(Call<T> c, Throwable t){
        cb.err(t != null ? t : new Exception("network"));
      }
    };
  }

  public void dashboard(Cb<AdminModels.Dashboard> cb){ api.dashboard().enqueue(wrap(cb)); }
  public void users(Cb<AdminModels.Users> cb){ api.users().enqueue(wrap(cb)); }
  public void specialists(Cb<AdminModels.Specialists> cb){ api.specialists().enqueue(wrap(cb)); }
  public void createSpecialist(java.util.Map<String,Object> body, Cb<AdminModels.Toggle> cb){
    api.createSpecialist(body).enqueue(new Callback<java.util.Map<String,Object>>(){
      @Override public void onResponse(Call<java.util.Map<String,Object>> call, retrofit2.Response<java.util.Map<String,Object>> response) {
        try {
          if (response.isSuccessful()) {
            AdminModels.Toggle t = new AdminModels.Toggle();
            t.ok = true;
            cb.ok(t);
          } else {
            cb.err(new Exception("HTTP " + response.code()));
          }
        } catch (Throwable e) {
          cb.err(e);
        }
      }
      @Override public void onFailure(Call<java.util.Map<String,Object>> call, Throwable t){ cb.err(t); }
    });
  }
  public void orgs(Cb<AdminModels.Organizations> cb){ api.orgs().enqueue(wrap(cb)); }
  public void appointments(Cb<AdminModels.Appointments> cb){ api.appointments().enqueue(wrap(cb)); }
  public void posts(Cb<AdminModels.Posts> cb){ api.posts().enqueue(wrap(cb)); }
  public void toggle(int id, Cb<AdminModels.Toggle> cb){ api.toggle(id).enqueue(wrap(cb)); }
  public void approveSpec(int id, Cb<AdminModels.Toggle> cb){ api.approveSpec(id).enqueue(wrap(cb)); }
  public void rejectSpec(int id, @Nullable String reason, Cb<AdminModels.Toggle> cb){
    AdminModels.RejectRequest body = new AdminModels.RejectRequest();
    body.reason = reason;
    api.rejectSpec(id, body).enqueue(wrap(cb));
  }
  public void specialistDocs(int id, Cb<AdminModels.SpecialistDocuments> cb){ api.specialistDocs(id).enqueue(wrap(cb)); }
  public void reviewSpec(int id, AdminModels.ReviewRequest body, Cb<AdminModels.Toggle> cb){ api.reviewSpec(id, body).enqueue(wrap(cb)); }
  public void approveOrg(int id, Cb<AdminModels.Toggle> cb){ api.approveOrg(id).enqueue(wrap(cb)); }
  public void rejectOrg(int id, @Nullable String reason, Cb<AdminModels.Toggle> cb){
    AdminModels.RejectRequest body = new AdminModels.RejectRequest();
    body.reason = reason;
    api.rejectOrg(id, body).enqueue(wrap(cb));
  }
  public void orgDetail(int id, Cb<AdminModels.OrganizationDetail> cb){ api.orgDetail(id).enqueue(wrap(cb)); }
  public void profile(Cb<AdminModels.AdminProfile> cb){ api.profile().enqueue(wrap(cb)); }
  public void updateProfile(java.util.Map<String,Object> body, Cb<AdminModels.Toggle> cb){ api.updateProfile(body).enqueue(wrap(cb)); }
  public void updatePassword(String current, String password, String confirm, Cb<AdminModels.Toggle> cb){
    java.util.Map<String,String> b=new java.util.HashMap<>(); b.put("current_password", current); b.put("new_password", password); b.put("new_password_confirmation", confirm);
    api.updatePassword(b).enqueue(wrap(cb));
  }
  public void uploadAvatar(android.net.Uri uri, android.content.Context ctx, Cb<String> cb){
    try {
      android.content.ContentResolver resolver = ctx.getContentResolver();
      String mime = resolver.getType(uri);
      if (mime == null) mime = "image/jpeg";
      String name = "avatar_"+System.currentTimeMillis();
      java.io.InputStream in = resolver.openInputStream(uri);
      byte[] bytes = readBytes(in);
      okhttp3.RequestBody fileBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse(mime), bytes);
      okhttp3.MultipartBody.Part part = okhttp3.MultipartBody.Part.createFormData("avatar", name, fileBody);
      api.uploadAvatar(part).enqueue(new Callback<java.util.Map<String,Object>>(){
        @Override public void onResponse(Call<java.util.Map<String,Object>> call, retrofit2.Response<java.util.Map<String,Object>> response) {
          try {
            Object url = response.body() != null ? response.body().get("url") : null;
            if (response.isSuccessful() && url != null) {
              cb.ok(String.valueOf(url));
            } else {
              cb.err(new Exception("HTTP "+response.code()));
            }
          } catch (Throwable e) {
            cb.err(e);
          }
        }
        @Override public void onFailure(Call<java.util.Map<String,Object>> call, Throwable t){ cb.err(t); }
      });
    } catch (Exception e){ cb.err(e); }
  }
  public void settings(Cb<AdminModels.AdminSettings> cb){ api.settings().enqueue(wrap(cb)); }
  public void saveSettings(String privacy, String contact, String feePercent, Cb<AdminModels.Toggle> cb){
    java.util.Map<String,String> b=new java.util.HashMap<>(); b.put("privacy_policy", privacy); b.put("contact_info", contact);
    if (feePercent != null && !feePercent.isEmpty()) b.put("platform_fee_percent", feePercent);
    api.saveSettings(b).enqueue(wrap(cb));
  }
  public void createCoupon(String code, int points, String expires, Cb<AdminModels.Toggle> cb){
    java.util.Map<String,Object> b=new java.util.HashMap<>(); b.put("code", code); b.put("points", points);
    if(expires!=null && !expires.isEmpty()) b.put("expires_at", expires);
    api.createCoupon(b).enqueue(wrap(cb));
  }
  public void credit(int userId, int points, Cb<AdminModels.Toggle> cb){
    java.util.Map<String,Object> b=new java.util.HashMap<>(); b.put("user_id", userId); b.put("points", points);
    api.credit(b).enqueue(wrap(cb));
  }
  public void ventReports(Cb<AdminModels.VentReports> cb){ api.ventReports().enqueue(wrap(cb)); }
  public void hideVentPost(int id, Cb<AdminModels.Toggle> cb){ api.hideVentPost(id).enqueue(wrap(cb)); }
  public void dailyTips(Cb<AdminModels.DailyTips> cb){ api.dailyTips().enqueue(wrap(cb)); }
  public void createDailyTip(java.util.Map<String,Object> body, Cb<AdminModels.DailyTip> cb){ api.createDailyTip(body).enqueue(wrap(cb)); }
  public void updateDailyTip(int id, java.util.Map<String,Object> body, Cb<AdminModels.DailyTip> cb){ api.updateDailyTip(id, body).enqueue(wrap(cb)); }
  public void deleteDailyTip(int id, Cb<AdminModels.Toggle> cb){
    api.deleteDailyTip(id).enqueue(new Callback<java.util.Map<String,Object>>(){
      @Override public void onResponse(Call<java.util.Map<String,Object>> call, retrofit2.Response<java.util.Map<String,Object>> response) {
        try {
          if (response.isSuccessful()) {
            AdminModels.Toggle t = new AdminModels.Toggle();
            t.ok = true;
            cb.ok(t);
          } else {
            cb.err(new Exception("HTTP " + response.code()));
          }
        } catch (Throwable e) {
          cb.err(e);
        }
      }
      @Override public void onFailure(Call<java.util.Map<String,Object>> call, Throwable t){ cb.err(t); }
    });
  }

  private static byte[] readBytes(java.io.InputStream in) throws Exception{
    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
    byte[] data = new byte[4096]; int n;
    while ((n = in.read(data)) != -1){ buffer.write(data,0,n); }
    in.close();
    return buffer.toByteArray();
  }
}
