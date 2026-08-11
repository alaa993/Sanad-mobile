package com.brightpath.sanad.feature.wallet;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.brightpath.sanad.feature.billing.BillingApi;
import com.brightpath.sanad.feature.billing.BillingRepository;

/**
 * Patient wallet screen state: load /wallet/me, Stripe top-up intent, coupon redeem.
 * Fragment also loads invoices in parallel via BillingRepository.
 */
public class WalletViewModel extends AndroidViewModel {
    private final BillingRepository repo;
    private final MutableLiveData<UIState> state = new MutableLiveData<>(UIState.loading());

    public WalletViewModel(@NonNull Application app){
        super(app);
        repo = new BillingRepository(app);
    }

    public LiveData<UIState> getState(){ return state; }

    public void load(){
        state.postValue(UIState.loading());
        repo.wallet(new BillingRepository.Cb<BillingApi.WalletResponse>() {
            @Override public void ok(BillingApi.WalletResponse d){ state.postValue(UIState.data(d)); }
            @Override public void err(Throwable e){ state.postValue(UIState.error(e.getMessage())); }
        });
    }

    public void topup(int amount, BillingRepository.Cb<BillingApi.IntentResponse> cb){
        repo.createIntent(amount, cb);
    }

    public void redeem(String code, BillingRepository.Cb<BillingApi.CouponResponse> cb){
        repo.applyCoupon(code, cb);
    }

    static class UIState {
        boolean loading;
        String error;
        BillingApi.WalletResponse data;
        static UIState loading(){ UIState s = new UIState(); s.loading = true; return s; }
        static UIState error(String e){ UIState s = new UIState(); s.error = e; return s; }
        static UIState data(BillingApi.WalletResponse d){ UIState s = new UIState(); s.data = d; return s; }
    }
}
