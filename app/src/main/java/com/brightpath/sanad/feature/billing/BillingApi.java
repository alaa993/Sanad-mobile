
package com.brightpath.sanad.feature.billing;
import retrofit2.*; import retrofit2.http.*; import java.util.*;
public interface BillingApi {
  @GET("api/v1/billing/plans") Call<PlansResponse> plans();
  @POST("api/v1/billing/subscribe") @FormUrlEncoded Call<GenericResponse> subscribe(@Field("plan_id") int planId);
  @POST("api/v1/billing/cancel") Call<GenericResponse> cancel();
  @GET("api/v1/wallet/me") Call<WalletResponse> wallet();
  @POST("api/v1/wallet/topup/intent") @FormUrlEncoded Call<IntentResponse> createIntent(@Field("amount") int amount);
  @POST("api/v1/wallet/mtn/init") @FormUrlEncoded Call<MtnInitResponse> mtnInit(@Field("amount") int amount, @Field("phone") String phone);
  @POST("api/v1/wallet/mtn/confirm") @FormUrlEncoded Call<MtnConfirmResponse> mtnConfirm(@Field("reference") String reference, @Field("transaction_id") String transactionId);
  @POST("api/v1/wallet/syriatel/init") @FormUrlEncoded Call<MtnInitResponse> syriatelInit(@Field("amount") int amount, @Field("phone") String phone);
  @POST("api/v1/wallet/syriatel/confirm") @FormUrlEncoded Call<MtnConfirmResponse> syriatelConfirm(@Field("reference") String reference, @Field("transaction_id") String transactionId);
  @POST("api/v1/wallet/apply-coupon") @FormUrlEncoded Call<CouponResponse> applyCoupon(@Field("code") String code);
  @GET("api/v1/billing/payment-methods") Call<PaymentMethodsResponse> paymentMethods();
  @POST("api/v1/sessions/{id}/confirm-payment") @FormUrlEncoded Call<GenericResponse> confirmSession(@Path("id") int id, @Field("method") String method, @Field("coupon") String coupon);
  @GET("api/v1/billing/transactions") Call<TransactionsResponse> transactions();
  @GET("api/v1/billing/invoices") Call<InvoicesResponse> invoices();
  class GenericResponse { public Boolean ok; public String msg; public Integer paid; }
  class PlansResponse { public java.util.List<Plan> data; }
  class Plan { public int id; public String slug,type,cycle,currency; public int price; public String features; }
  class WalletResponse { public int balance; public int points; public java.util.List<Tx> transactions; }
  class Tx { public int id, amount, points; public String type,status,currency; public String created_at; }
  class IntentResponse { public Boolean ok; public String client_secret; public Integer balance; public Integer points; public String msg; }
  class MtnInitResponse { public Boolean ok; public String reference, instructions, currency, ussd, expires_at, provider; public Integer amount, id; public Boolean sandbox; }
  class MtnConfirmResponse { public Boolean ok; public Integer balance, points; public String msg; }
  class PaymentMethodsResponse {
    public java.util.List<String> methods;
    public Boolean mtn_enabled;
    public Boolean syriatel_enabled;
    public java.util.List<Integer> topup_presets;
  }
  class CouponResponse { public Boolean ok; public Integer points; public Coupon coupon; public String msg; }
  class Coupon { public String code; public Integer percent_off; public Integer amount_off; public String expires_at; }
  class TransactionsResponse { public java.util.List<Tx> data; }
  class InvoicesResponse { public java.util.List<Invoice> data; }
  class Invoice { public int id; public Integer total; public String currency, status, created_at; }
}
