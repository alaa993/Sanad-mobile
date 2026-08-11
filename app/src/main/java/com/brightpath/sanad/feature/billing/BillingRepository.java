
package com.brightpath.sanad.feature.billing;
import android.content.Context; import com.brightpath.sanad.data.ApiClient; import retrofit2.*;

/**
 * Billing/wallet Retrofit facade: plans, Stripe intent, MTN/Syriatel top-up, coupons, invoices, session confirm.
 */
public class BillingRepository {
  private final BillingApi api; public BillingRepository(Context ctx){ api = ApiClient.get(ctx).create(BillingApi.class); }
  public interface Cb<T>{ void ok(T d); void err(Throwable e); }
  private static <T> Callback<T> wrap(Cb<T> cb){ return new Callback<T>(){ public void onResponse(Call<T> c, retrofit2.Response<T> r){ if(r.isSuccessful()&&r.body()!=null) cb.ok(r.body()); else cb.err(new Exception("HTTP "+r.code())); } public void onFailure(Call<T> c, Throwable t){ cb.err(t);} }; }
  public void loadPlans(Cb<BillingApi.PlansResponse> cb){ api.plans().enqueue(wrap(cb)); }
  public void subscribe(int planId, Cb<BillingApi.GenericResponse> cb){ api.subscribe(planId).enqueue(wrap(cb)); }
  public void cancel(Cb<BillingApi.GenericResponse> cb){ api.cancel().enqueue(wrap(cb)); }
  public void wallet(Cb<BillingApi.WalletResponse> cb){ api.wallet().enqueue(wrap(cb)); }
  public void createIntent(int amount, Cb<BillingApi.IntentResponse> cb){ api.createIntent(amount).enqueue(wrap(cb)); }
  public void mtnInit(int amount, String phone, Cb<BillingApi.MtnInitResponse> cb){ api.mtnInit(amount, phone).enqueue(wrap(cb)); }
  public void mtnConfirm(String reference, String transactionId, Cb<BillingApi.MtnConfirmResponse> cb){ api.mtnConfirm(reference, transactionId).enqueue(wrap(cb)); }
  public void syriatelInit(int amount, String phone, Cb<BillingApi.MtnInitResponse> cb){ api.syriatelInit(amount, phone).enqueue(wrap(cb)); }
  public void syriatelConfirm(String reference, String transactionId, Cb<BillingApi.MtnConfirmResponse> cb){ api.syriatelConfirm(reference, transactionId).enqueue(wrap(cb)); }
  public void paymentMethods(Cb<BillingApi.PaymentMethodsResponse> cb){ api.paymentMethods().enqueue(wrap(cb)); }
  public void applyCoupon(String code, Cb<BillingApi.CouponResponse> cb){ api.applyCoupon(code).enqueue(wrap(cb)); }
  public void confirmSession(int id, String method, String coupon, Cb<BillingApi.GenericResponse> cb){ api.confirmSession(id, method, coupon).enqueue(wrap(cb)); }
  public void transactions(Cb<BillingApi.TransactionsResponse> cb){ api.transactions().enqueue(wrap(cb)); }
  public void invoices(Cb<BillingApi.InvoicesResponse> cb){ api.invoices().enqueue(wrap(cb)); }
}
