package com.brightpath.sanad.feature.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.brightpath.sanad.R;

/**
 * شاشة إدارة المحفظة للأدمن: إنشاء أكواد شحن وشحن يدوي للأدوار.
 */
public class AdminWalletFragment extends Fragment {
  private AdminViewModels.WalletVM vm;
  private ProgressBar progress;
  private EditText etCode, etPoints, etExpiry, etUserId, etUserPoints;

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_admin_wallet, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
    super.onViewCreated(v, s);
    progress = v.findViewById(R.id.walletProgress);
    etCode = v.findViewById(R.id.etCouponCode);
    etPoints = v.findViewById(R.id.etCouponPoints);
    etExpiry = v.findViewById(R.id.etCouponExpiry);
    etUserId = v.findViewById(R.id.etCreditUser);
    etUserPoints = v.findViewById(R.id.etCreditPoints);
    MaterialButton btnCreate = v.findViewById(R.id.btnCreateCoupon);
    MaterialButton btnCredit = v.findViewById(R.id.btnCredit);

    vm = new ViewModelProvider(this).get(AdminViewModels.WalletVM.class);
    vm.toast.observe(getViewLifecycleOwner(), msg -> {
      if (!TextUtils.isEmpty(msg)) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    });
    vm.loading.observe(getViewLifecycleOwner(), loading -> {
      progress.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
    });

    btnCreate.setOnClickListener(x -> createCoupon());
    btnCredit.setOnClickListener(x -> credit());
  }

  private void createCoupon(){
    String code = text(etCode);
    String pointsStr = text(etPoints);
    String expiry = text(etExpiry);
    if(code.isEmpty() || pointsStr.isEmpty()){
      Toast.makeText(requireContext(), R.string.error_required_fields, Toast.LENGTH_SHORT).show();
      return;
    }
    int points = safeInt(pointsStr);
    if(points<=0){
      Toast.makeText(requireContext(), R.string.error_required_fields, Toast.LENGTH_SHORT).show();
      return;
    }
    vm.createCoupon(code, points, expiry);
  }

  private void credit(){
    String userIdStr = text(etUserId);
    String pointsStr = text(etUserPoints);
    if(userIdStr.isEmpty() || pointsStr.isEmpty()){
      Toast.makeText(requireContext(), R.string.error_required_fields, Toast.LENGTH_SHORT).show();
      return;
    }
    int uid = safeInt(userIdStr);
    int pts = safeInt(pointsStr);
    if(uid<=0 || pts<=0){
      Toast.makeText(requireContext(), R.string.error_required_fields, Toast.LENGTH_SHORT).show();
      return;
    }
    vm.credit(uid, pts);
  }

  private String text(EditText et){
    return et.getText()!=null ? et.getText().toString().trim() : "";
  }
  private int safeInt(String s){
    try{ return Integer.parseInt(s); } catch (Exception e){ return 0; }
  }
}
