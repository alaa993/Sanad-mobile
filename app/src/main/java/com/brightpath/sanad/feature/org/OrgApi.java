
package com.brightpath.sanad.feature.org;
import retrofit2.*; import retrofit2.http.*;
public interface OrgApi {
  @GET("api/v1/org/dashboard") Call<OrgModels.Dashboard> dashboard();
  @GET("api/v1/org/support-room") Call<OrgModels.SupportRoom> supportRoom();
  @GET("api/v1/org/specialists") Call<OrgModels.Specialists> specialists();
  @GET("api/v1/org/sessions") Call<OrgModels.Appointments> sessions();
  @GET("api/v1/org/beneficiaries") Call<OrgModels.Beneficiaries> beneficiaries();
  @POST("api/v1/org/beneficiaries") Call<OrgModels.OrganizationBeneficiary> createBeneficiary(@Body OrgModels.BeneficiaryForm body);
  @GET("api/v1/org/beneficiaries/{id}") Call<OrgModels.BeneficiaryDetail> beneficiaryDetail(@Path("id") int id);
  @POST("api/v1/org/beneficiaries/{id}/assign-specialist") Call<OrgModels.OrganizationBeneficiary> assignSpecialist(@Path("id") int id, @Body java.util.Map<String,Integer> payload);
  @GET("api/v1/org/reports/summary") Call<OrgModels.ReportSummary> reportsSummary();
  @GET("api/v1/org/billing/overview") Call<OrgModels.BillingOverview> billingOverview();
  @GET("api/v1/org/specialists/{id}") Call<OrgModels.SpecialistDetail> specialistDetail(@Path("id") int id);
}
