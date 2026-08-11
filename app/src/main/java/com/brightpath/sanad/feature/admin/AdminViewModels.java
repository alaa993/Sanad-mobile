package com.brightpath.sanad.feature.admin;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.*;
public class AdminViewModels {
  public static class HomeVM extends AndroidViewModel {
    private final MutableLiveData<UiState> state = new MutableLiveData<>(UiState.loading());
    private final AdminRepository repo; public HomeVM(@NonNull Application app){ super(app); repo=new AdminRepository(app); }
    public LiveData<UiState> state(){ return state; }
    public void load(){
      state.postValue(UiState.loading());
      repo.dashboard(new AdminRepository.Cb<AdminModels.Dashboard>(){ public void ok(AdminModels.Dashboard t){ state.postValue(UiState.success(t)); } public void err(Throwable e){ state.postValue(UiState.error(e.getMessage())); } });
    }
    public static class UiState{
      public final boolean loading; public final AdminModels.Dashboard data; public final String error;
      private UiState(boolean l, AdminModels.Dashboard d, String e){ loading=l; data=d; error=e; }
      static UiState loading(){ return new UiState(true,null,null); }
      static UiState success(AdminModels.Dashboard d){ return new UiState(false,d,null); }
      static UiState error(String msg){ return new UiState(false,null,msg==null?"تعذر تحميل البيانات":msg); }
    }
  }
  public static class SpecListVM extends AndroidViewModel {
    private final AdminRepository repo;
    private final MutableLiveData<ListState<AdminModels.Specialists.Specialist>> state =
        new MutableLiveData<>(ListState.loading());
    public SpecListVM(@NonNull Application app){ super(app); repo=new AdminRepository(app); }
    public LiveData<ListState<AdminModels.Specialists.Specialist>> state(){ return state; }
    public void load(){
      state.postValue(ListState.loading());
      repo.specialists(new AdminRepository.Cb<AdminModels.Specialists>(){
        public void ok(AdminModels.Specialists t){
          state.postValue(ListState.success(t != null && t.data != null ? t.data : java.util.Collections.emptyList()));
        }
        public void err(Throwable e){ state.postValue(ListState.error(e.getMessage())); }
      });
    }
    public void approve(int id){
      repo.approveSpec(id, new AdminRepository.Cb<AdminModels.Toggle>(){
        public void ok(AdminModels.Toggle t){ load(); }
        public void err(Throwable e){ state.postValue(ListState.error(e.getMessage())); }
      });
    }
    public void reject (int id, @Nullable String reason){
      repo.rejectSpec(id, reason, new AdminRepository.Cb<AdminModels.Toggle>(){
        public void ok(AdminModels.Toggle t){ load(); }
        public void err(Throwable e){ state.postValue(ListState.error(e.getMessage())); }
      });
    }
    public void createSpecialist(java.util.Map<String,Object> body){
      repo.createSpecialist(body, new AdminRepository.Cb<AdminModels.Toggle>(){
        public void ok(AdminModels.Toggle t){ load(); }
        public void err(Throwable e){ state.postValue(ListState.error(e.getMessage())); }
      });
    }
    public void documents(int id, AdminRepository.Cb<AdminModels.SpecialistDocuments> cb){ repo.specialistDocs(id, cb); }
    public void review(int id, AdminModels.ReviewRequest body, AdminRepository.Cb<AdminModels.Toggle> cb){
      repo.reviewSpec(id, body, new AdminRepository.Cb<AdminModels.Toggle>(){
        public void ok(AdminModels.Toggle t){ load(); cb.ok(t); }
        public void err(Throwable e){ cb.err(e); }
      });
    }
  }
  public static class OrgListVM extends AndroidViewModel {
    private final AdminRepository repo;
    private final MutableLiveData<ListState<AdminModels.Organizations.Organization>> state =
        new MutableLiveData<>(ListState.loading());
    public OrgListVM(@NonNull Application app){ super(app); repo=new AdminRepository(app); }
    public LiveData<ListState<AdminModels.Organizations.Organization>> state(){ return state; }
    public void load(){
      state.postValue(ListState.loading());
      repo.orgs(new AdminRepository.Cb<AdminModels.Organizations>(){
        public void ok(AdminModels.Organizations t){
          state.postValue(ListState.success(t != null && t.data != null ? t.data : java.util.Collections.emptyList()));
        }
        public void err(Throwable e){ state.postValue(ListState.error(e.getMessage())); }
      });
    }
    public void approve(int id){
      repo.approveOrg(id, new AdminRepository.Cb<AdminModels.Toggle>(){
        public void ok(AdminModels.Toggle t){ load(); }
        public void err(Throwable e){ state.postValue(ListState.error(e.getMessage())); }
      });
    }
    public void reject (int id, @Nullable String reason){
      repo.rejectOrg(id, reason, new AdminRepository.Cb<AdminModels.Toggle>(){
        public void ok(AdminModels.Toggle t){ load(); }
        public void err(Throwable e){ state.postValue(ListState.error(e.getMessage())); }
      });
    }
  }
  public static class OrgDetailVM extends AndroidViewModel {
    public MutableLiveData<AdminModels.OrganizationDetail> detail=new MutableLiveData<>();
    private final AdminRepository repo;
    public OrgDetailVM(@NonNull Application app){ super(app); repo=new AdminRepository(app); }
    public void load(int id){ repo.orgDetail(id, new AdminRepository.Cb<AdminModels.OrganizationDetail>(){ public void ok(AdminModels.OrganizationDetail t){ detail.postValue(t);} public void err(Throwable e){} }); }
    public void approve(int id){ repo.approveOrg(id, new AdminRepository.Cb<AdminModels.Toggle>(){ public void ok(AdminModels.Toggle t){ load(id);} public void err(Throwable e){} }); }
    public void reject (int id, @Nullable String reason){ repo.rejectOrg(id, reason, new AdminRepository.Cb<AdminModels.Toggle>(){ public void ok(AdminModels.Toggle t){ load(id);} public void err(Throwable e){} }); }
  }

  public static class UsersVM extends AndroidViewModel {
    private final AdminRepository repo;
    private final MutableLiveData<ListState<AdminModels.Users.User>> state =
        new MutableLiveData<>(ListState.loading());
    public UsersVM(@NonNull Application app){ super(app); repo=new AdminRepository(app); }
    public LiveData<ListState<AdminModels.Users.User>> state(){ return state; }
    public void load(){
      state.postValue(ListState.loading());
      repo.users(new AdminRepository.Cb<AdminModels.Users>() {
        @Override public void ok(AdminModels.Users t){
          state.postValue(ListState.success(t != null && t.data != null ? t.data : java.util.Collections.emptyList()));
        }
        @Override public void err(Throwable e){ state.postValue(ListState.error(e.getMessage())); }
      });
    }
  }

  public static class SessionsVM extends AndroidViewModel {
    private final AdminRepository repo;
    private final MutableLiveData<ListState<AdminModels.Appointments.Appointment>> state =
        new MutableLiveData<>(ListState.loading());
    public SessionsVM(@NonNull Application app){ super(app); repo=new AdminRepository(app); }
    public LiveData<ListState<AdminModels.Appointments.Appointment>> state(){ return state; }
    public void load(){
      state.postValue(ListState.loading());
      repo.appointments(new AdminRepository.Cb<AdminModels.Appointments>() {
        @Override public void ok(AdminModels.Appointments t){
          state.postValue(ListState.success(t != null && t.data != null ? t.data : java.util.Collections.emptyList()));
        }
        @Override public void err(Throwable e){ state.postValue(ListState.error(e.getMessage())); }
      });
    }
  }

  public static class WalletVM extends AndroidViewModel {
    private final AdminRepository repo;
    public final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    public final MutableLiveData<String> toast = new MutableLiveData<>();
    public WalletVM(@NonNull Application app){ super(app); repo = new AdminRepository(app); }
    public void createCoupon(String code, int points, String expiresAt){
      loading.postValue(true);
      repo.createCoupon(code, points, expiresAt, new AdminRepository.Cb<AdminModels.Toggle>(){
        @Override public void ok(AdminModels.Toggle t){
          loading.postValue(false);
          toast.postValue(getApplication().getString(com.brightpath.sanad.R.string.admin_wallet_success));
        }
        @Override public void err(Throwable e){ loading.postValue(false); toast.postValue("تعذر إنشاء الكود"); }
      });
    }
    public void credit(int userId, int points){
      loading.postValue(true);
      repo.credit(userId, points, new AdminRepository.Cb<AdminModels.Toggle>(){
        @Override public void ok(AdminModels.Toggle t){
          loading.postValue(false);
          toast.postValue(getApplication().getString(com.brightpath.sanad.R.string.admin_wallet_success));
        }
        @Override public void err(Throwable e){ loading.postValue(false); toast.postValue("تعذر الشحن"); }
      });
    }
  }

  public static class LibraryVM extends AndroidViewModel {
    private final AdminRepository repo;
    private final MutableLiveData<ListState<AdminModels.Posts.Post>> state =
        new MutableLiveData<>(ListState.loading());
    public LibraryVM(@NonNull Application app){ super(app); repo=new AdminRepository(app); }
    public LiveData<ListState<AdminModels.Posts.Post>> state(){ return state; }
    public void load(){
      state.postValue(ListState.loading());
      repo.posts(new AdminRepository.Cb<AdminModels.Posts>() {
        @Override public void ok(AdminModels.Posts t){
          state.postValue(ListState.success(t != null && t.data != null ? t.data : java.util.Collections.emptyList()));
        }
        @Override public void err(Throwable e){ state.postValue(ListState.error(e.getMessage())); }
      });
    }
    public void toggle(int id){
      repo.toggle(id, new AdminRepository.Cb<AdminModels.Toggle>() {
        @Override public void ok(AdminModels.Toggle t){ load(); }
        @Override public void err(Throwable e){ state.postValue(ListState.error(e.getMessage())); }
      });
    }
  }

  public static class ListState<T>{
    public final boolean loading;
    public final java.util.List<T> data;
    public final String error;
    private ListState(boolean loading, java.util.List<T> data, String error){
      this.loading=loading;
      this.data=data==null?java.util.Collections.emptyList():data;
      this.error=error;
    }
    public static <T> ListState<T> loading(){ return new ListState<>(true, null, null); }
    public static <T> ListState<T> success(java.util.List<T> data){ return new ListState<>(false, data, null); }
    public static <T> ListState<T> error(String msg){ return new ListState<>(false, java.util.Collections.emptyList(), msg==null?"تعذر تحميل البيانات":msg); }
  }

  public static class ProfileVM extends AndroidViewModel {
    private final AdminRepository repo;
    public final MutableLiveData<AdminModels.AdminProfile> state = new MutableLiveData<>();
    public final MutableLiveData<String> toast = new MutableLiveData<>();
    public ProfileVM(@NonNull Application app){ super(app); repo=new AdminRepository(app); }
    public void load(){ repo.profile(new AdminRepository.Cb<AdminModels.AdminProfile>(){ @Override public void ok(AdminModels.AdminProfile t){ state.postValue(t);} @Override public void err(Throwable e){ toast.postValue("تعذر تحميل الملف"); } }); }
    public void save(java.util.Map<String,Object> body){ repo.updateProfile(body, new AdminRepository.Cb<AdminModels.Toggle>(){ @Override public void ok(AdminModels.Toggle t){ toast.postValue("تم الحفظ"); load(); } @Override public void err(Throwable e){ toast.postValue("تعذر الحفظ"); } }); }
    public void changePassword(String current, String pass, String confirm){ repo.updatePassword(current, pass, confirm, new AdminRepository.Cb<AdminModels.Toggle>(){ @Override public void ok(AdminModels.Toggle t){ toast.postValue("تم تحديث كلمة المرور"); } @Override public void err(Throwable e){ toast.postValue("كلمة المرور الحالية غير صحيحة"); } }); }
    public void uploadAvatar(android.net.Uri uri){ repo.uploadAvatar(uri, getApplication(), new AdminRepository.Cb<String>(){ @Override public void ok(String url){ AdminModels.AdminProfile p=state.getValue(); if(p!=null){ p.avatar=url; state.postValue(p);} toast.postValue("تم رفع الصورة"); } @Override public void err(Throwable e){ toast.postValue("فشل رفع الصورة"); } }); }
    public void saveSettings(String privacy, String contact, String feePercent){ repo.saveSettings(privacy, contact, feePercent, new AdminRepository.Cb<AdminModels.Toggle>(){ @Override public void ok(AdminModels.Toggle t){ toast.postValue("تم تحديث الإعدادات"); load(); } @Override public void err(Throwable e){ toast.postValue("تعذر تحديث الإعدادات"); } }); }
    public void fetchSettings(){ repo.settings(new AdminRepository.Cb<AdminModels.AdminSettings>(){ @Override public void ok(AdminModels.AdminSettings t){ AdminModels.AdminProfile p = state.getValue(); if(p==null) p=new AdminModels.AdminProfile(); p.privacy_policy=t.privacy_policy; p.contact_info=t.contact_info; p.platform_fee_percent=t.platform_fee_percent; state.postValue(p); } @Override public void err(Throwable e){} }); }
  }
}
