
package com.brightpath.sanad.feature.org;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;

public class OrgViewModels {
  public static class HomeVM extends AndroidViewModel {
    private final MutableLiveData<UiState> state = new MutableLiveData<>(UiState.loading());
    private final OrgRepository repo;
    public HomeVM(@NonNull Application app){ super(app); repo=new OrgRepository(app);}
    public LiveData<UiState> state(){ return state; }
    public void load(){
      state.postValue(UiState.loading());
      repo.dashboard(new OrgRepository.Cb<OrgModels.Dashboard>(){
        @Override public void ok(OrgModels.Dashboard t){ state.postValue(UiState.success(t)); }
        @Override public void err(Throwable e){ state.postValue(UiState.error(e.getMessage())); }
      });
    }
    public static class UiState {
      public final boolean loading;
      public final OrgModels.Dashboard data;
      public final String error;
      private UiState(boolean l, OrgModels.Dashboard d, String e){ loading=l; data=d; error=e; }
      static UiState loading(){ return new UiState(true,null,null); }
      static UiState success(OrgModels.Dashboard data){ return new UiState(false,data,null); }
      static UiState error(String message){
        return new UiState(false,null,message==null?"تعذر تحميل البيانات":message);
      }
    }
  }
  public static class SpecialistsVM extends AndroidViewModel {
    public final MutableLiveData<java.util.List<OrgModels.Specialist>> list=new MutableLiveData<>(new java.util.ArrayList<>());
    public final MutableLiveData<Boolean> loading=new MutableLiveData<>(true);
    public final MutableLiveData<String> error=new MutableLiveData<>(null);
    private final OrgRepository repo;
    public SpecialistsVM(@NonNull Application app){ super(app); repo=new OrgRepository(app);}
    public void load(){
      loading.postValue(true);
      error.postValue(null);
      repo.specialists(new OrgRepository.Cb<OrgModels.Specialists>(){
        @Override public void ok(OrgModels.Specialists t){
          list.postValue(t.data);
          loading.postValue(false);
        }
        @Override public void err(Throwable e){
          error.postValue(e.getMessage());
          loading.postValue(false);
        }
      });
    }
  }
  public static class SessionsVM extends AndroidViewModel { public MutableLiveData<java.util.List<OrgModels.Appointment>> list=new MutableLiveData<>(new java.util.ArrayList<>()); private final OrgRepository repo; public SessionsVM(@NonNull Application app){ super(app); repo=new OrgRepository(app);} public void load(){ repo.sessions(new OrgRepository.Cb<OrgModels.Appointments>(){ @Override public void ok(OrgModels.Appointments t){ list.postValue(t.data);} @Override public void err(Throwable e){} }); } }

  public static class BeneficiariesVM extends AndroidViewModel {
    public static class UiState {
      public final boolean loading;
      public final java.util.List<OrgModels.Beneficiary> data;
      public final String error;
      UiState(boolean l, java.util.List<OrgModels.Beneficiary> d, String e){ loading=l; data=d; error=e; }
      static UiState loading(){ return new UiState(true,new java.util.ArrayList<>(),null); }
      static UiState success(java.util.List<OrgModels.Beneficiary> d){ return new UiState(false,d,null); }
      static UiState error(String e){ return new UiState(false,new java.util.ArrayList<>(), e==null?"تعذر التحميل":e); }
    }
    private final MutableLiveData<UiState> state=new MutableLiveData<>(UiState.loading());
    private final OrgRepository repo;
    public BeneficiariesVM(@NonNull Application app){ super(app); repo=new OrgRepository(app); }
    public LiveData<UiState> state(){ return state; }
    public void load(){
      state.postValue(UiState.loading());
      repo.beneficiaries(new OrgRepository.Cb<OrgModels.Beneficiaries>(){
        @Override public void ok(OrgModels.Beneficiaries t){ state.postValue(UiState.success(t.data)); }
        @Override public void err(Throwable e){ state.postValue(UiState.error(e.getMessage())); }
      });
    }
    public void create(OrgModels.BeneficiaryForm form){
      repo.createBeneficiary(form, new OrgRepository.Cb<OrgModels.OrganizationBeneficiary>(){
        @Override public void ok(OrgModels.OrganizationBeneficiary t){ load(); }
        @Override public void err(Throwable e){ state.postValue(UiState.error(e.getMessage())); }
      });
    }
  }

  public static class BeneficiaryDetailVM extends AndroidViewModel {
    private final OrgRepository repo;
    public final MutableLiveData<OrgModels.BeneficiaryDetail> detail=new MutableLiveData<>();
    public BeneficiaryDetailVM(@NonNull Application app){ super(app); repo=new OrgRepository(app); }
    public void load(int id){
      repo.beneficiaryDetail(id, new OrgRepository.Cb<OrgModels.BeneficiaryDetail>(){
        @Override public void ok(OrgModels.BeneficiaryDetail t){ detail.postValue(t); }
        @Override public void err(Throwable e){} });
    }
    public void assign(int id,int specialistId){
      repo.assignSpecialist(id, specialistId, new OrgRepository.Cb<OrgModels.OrganizationBeneficiary>(){
        @Override public void ok(OrgModels.OrganizationBeneficiary t){ load(id); }
        @Override public void err(Throwable e){} });
    }
  }

  public static class ReportsVM extends AndroidViewModel {
    public static class UiState {
      public final boolean loading;
      public final OrgModels.ReportSummary data;
      public final String error;
      UiState(boolean l, OrgModels.ReportSummary d, String e){ loading=l; data=d; error=e; }
      static UiState loading(){ return new UiState(true,null,null); }
      static UiState success(OrgModels.ReportSummary d){ return new UiState(false,d,null); }
      static UiState error(String e){ return new UiState(false,null, e==null?"تعذر تحميل التقارير":e); }
    }
    private final MutableLiveData<UiState> state=new MutableLiveData<>(UiState.loading());
    private final OrgRepository repo;
    public ReportsVM(@NonNull Application app){ super(app); repo=new OrgRepository(app); }
    public LiveData<UiState> state(){ return state; }
    public void load(){
      state.postValue(UiState.loading());
      repo.reports(new OrgRepository.Cb<OrgModels.ReportSummary>(){
        @Override public void ok(OrgModels.ReportSummary t){ state.postValue(UiState.success(t)); }
        @Override public void err(Throwable e){ state.postValue(UiState.error(e.getMessage())); }
      });
    }
  }

  public static class BillingVM extends AndroidViewModel {
    public static class UiState {
      public final boolean loading;
      public final OrgModels.BillingOverview data;
      public final String error;
      UiState(boolean l, OrgModels.BillingOverview d, String e){ loading=l; data=d; error=e; }
      static UiState loading(){ return new UiState(true,null,null); }
      static UiState success(OrgModels.BillingOverview d){ return new UiState(false,d,null); }
      static UiState error(String e){ return new UiState(false,null, e==null?"تعذر تحميل بيانات الفوترة":e); }
    }
    private final MutableLiveData<UiState> state=new MutableLiveData<>(UiState.loading());
    private final OrgRepository repo;
    public BillingVM(@NonNull Application app){ super(app); repo=new OrgRepository(app); }
    public LiveData<UiState> state(){ return state; }
    public void load(){
      state.postValue(UiState.loading());
      repo.billing(new OrgRepository.Cb<OrgModels.BillingOverview>(){
        @Override public void ok(OrgModels.BillingOverview t){ state.postValue(UiState.success(t)); }
        @Override public void err(Throwable e){ state.postValue(UiState.error(e.getMessage())); }
      });
    }
  }

  public static class SpecialistDetailVM extends AndroidViewModel {
    public static class UiState {
      public final boolean loading;
      public final OrgModels.SpecialistDetail data;
      public final String error;
      UiState(boolean l, OrgModels.SpecialistDetail d, String e){ loading=l; data=d; error=e; }
      static UiState loading(){ return new UiState(true,null,null); }
      static UiState success(OrgModels.SpecialistDetail d){ return new UiState(false,d,null); }
      static UiState error(String e){ return new UiState(false,null, e==null?"تعذر تحميل الملف":e); }
    }
    private final MutableLiveData<UiState> state=new MutableLiveData<>(UiState.loading());
    private final OrgRepository repo;
    public SpecialistDetailVM(@NonNull Application app){ super(app); repo=new OrgRepository(app); }
    public LiveData<UiState> state(){ return state; }
    public void load(int id){
      state.postValue(UiState.loading());
      repo.specialistDetail(id, new OrgRepository.Cb<OrgModels.SpecialistDetail>(){
        @Override public void ok(OrgModels.SpecialistDetail t){ state.postValue(UiState.success(t)); }
        @Override public void err(Throwable e){ state.postValue(UiState.error(e.getMessage())); }
      });
    }
  }
}
