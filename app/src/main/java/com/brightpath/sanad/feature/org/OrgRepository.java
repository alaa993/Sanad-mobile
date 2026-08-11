
package com.brightpath.sanad.feature.org;
import android.content.Context; import com.brightpath.sanad.data.ApiClient;
import retrofit2.*;
public class OrgRepository {
  private final OrgApi api;
  public OrgRepository(Context ctx){ api = ApiClient.get(ctx).create(OrgApi.class); }
  public interface Cb<T>{ void ok(T t); void err(Throwable e); }
  private static <T> Callback<T> wrap(Cb<T> cb){ return new Callback<T>(){ @Override public void onResponse(Call<T> c, Response<T> r){ if(r.isSuccessful()&&r.body()!=null) cb.ok(r.body()); else cb.err(new Exception("HTTP "+r.code())); } @Override public void onFailure(Call<T> c, Throwable t){ cb.err(t); } }; }
  public void dashboard(Cb<OrgModels.Dashboard> cb){ api.dashboard().enqueue(wrap(cb)); }
  public void supportRoom(Cb<OrgModels.SupportRoom> cb){ api.supportRoom().enqueue(wrap(cb)); }
  public void specialists(Cb<OrgModels.Specialists> cb){ api.specialists().enqueue(wrap(cb)); }
  public void sessions(Cb<OrgModels.Appointments> cb){ api.sessions().enqueue(wrap(cb)); }
  public void beneficiaries(Cb<OrgModels.Beneficiaries> cb){ api.beneficiaries().enqueue(wrap(cb)); }
  public void createBeneficiary(OrgModels.BeneficiaryForm body, Cb<OrgModels.OrganizationBeneficiary> cb){ api.createBeneficiary(body).enqueue(wrap(cb)); }
  public void beneficiaryDetail(int id, Cb<OrgModels.BeneficiaryDetail> cb){ api.beneficiaryDetail(id).enqueue(wrap(cb)); }
  public void assignSpecialist(int id, int specialistId, Cb<OrgModels.OrganizationBeneficiary> cb){
    java.util.Map<String,Integer> payload=new java.util.HashMap<>(); payload.put("specialist_id", specialistId);
    api.assignSpecialist(id, payload).enqueue(wrap(cb));
  }
  public void reports(Cb<OrgModels.ReportSummary> cb){ api.reportsSummary().enqueue(wrap(cb)); }
  public void billing(Cb<OrgModels.BillingOverview> cb){ api.billingOverview().enqueue(wrap(cb)); }
  public void specialistDetail(int id, Cb<OrgModels.SpecialistDetail> cb){ api.specialistDetail(id).enqueue(wrap(cb)); }
}
