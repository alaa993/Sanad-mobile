package com.brightpath.sanad.feature.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.brightpath.sanad.R;
import com.brightpath.sanad.feature.billing.BillingApi;
import com.brightpath.sanad.feature.billing.BillingRepository;
import androidx.navigation.fragment.NavHostFragment;
import com.brightpath.sanad.ui.tour.CoachMarkManager;
import com.brightpath.sanad.ui.tour.CoachMarkStep;
import java.util.ArrayList;
import java.util.List;

public class WalletFragment extends Fragment {
    private WalletViewModel vm;
    private View content, errorContainer;
    private ProgressBar progress;
    private TextView tvBalance;
    private TextView tvEmpty;
    private TextView tvInvoices, tvInvoicesEmpty;
    private RecyclerView rvHistory;
    private TxAdapter adapter;
    private TextInputEditText etCode;
    private TextInputEditText etMtnAmount, etMtnTransactionId, etTopupPhone;
    private TextView tvMtnInstructions;
    private MaterialButton btnMtnInit, btnMtnConfirm;
    private com.google.android.material.chip.ChipGroup chipTopupPresets, chipTopupMethod;
    private String mtnReference;
    private boolean useSyriatel = true;
    private BillingRepository billingRepo;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wallet, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s){
        super.onViewCreated(v, s);
        content = v.findViewById(R.id.content);
        errorContainer = v.findViewById(R.id.errorContainer);
        progress = v.findViewById(R.id.progress);
        tvBalance = v.findViewById(R.id.tvBalance);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        tvInvoices = v.findViewById(R.id.tvInvoices);
        tvInvoicesEmpty = v.findViewById(R.id.tvInvoicesEmpty);
        etCode = v.findViewById(R.id.etCode);
        etMtnAmount = v.findViewById(R.id.etMtnAmount);
        etMtnTransactionId = v.findViewById(R.id.etMtnTransactionId);
        etTopupPhone = v.findViewById(R.id.etTopupPhone);
        tvMtnInstructions = v.findViewById(R.id.tvMtnInstructions);
        btnMtnInit = v.findViewById(R.id.btnMtnInit);
        btnMtnConfirm = v.findViewById(R.id.btnMtnConfirm);
        chipTopupPresets = v.findViewById(R.id.chipTopupPresets);
        chipTopupMethod = v.findViewById(R.id.chipTopupMethod);
        billingRepo = new BillingRepository(requireContext());
        rvHistory = v.findViewById(R.id.rvHistory);
        adapter = new TxAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(adapter);

        MaterialButton btnRetry = v.findViewById(R.id.btnRetry);
        MaterialButton btnRedeem = v.findViewById(R.id.btnRedeem);
        MaterialButton btnViewPlans = v.findViewById(R.id.btnViewPlans);

        vm = new ViewModelProvider(this).get(WalletViewModel.class);
        vm.getState().observe(getViewLifecycleOwner(), this::render);
        vm.load();
        loadInvoices();
        wireTopupChips(v);

        btnRetry.setOnClickListener(x -> { vm.load(); loadInvoices(); });
        btnRedeem.setOnClickListener(x -> applyCode());
        if (btnViewPlans != null) {
            btnViewPlans.setOnClickListener(x -> NavHostFragment.findNavController(this).navigate(R.id.paywallFragment));
        }
        if (btnMtnInit != null) {
            btnMtnInit.setOnClickListener(x -> startMobileTopup());
        }
        if (btnMtnConfirm != null) {
            btnMtnConfirm.setOnClickListener(x -> confirmMobileTopup());
        }

        // Patient-only recharge surfaces (specialists keep balance / redeem / history).
        com.brightpath.sanad.feature.community.CommunityRolePolicy rolePolicy =
                new com.brightpath.sanad.feature.community.CommunityRolePolicy(
                        new com.brightpath.sanad.data.auth.TokenStore(requireContext()).getRole());
        if (!rolePolicy.isPatient()) {
            View mtn = v.findViewById(R.id.cardMtnTopup);
            if (mtn != null) mtn.setVisibility(View.GONE);
            if (btnViewPlans != null) btnViewPlans.setVisibility(View.GONE);
        }

        v.post(() -> {
            java.util.List<CoachMarkStep> steps = new java.util.ArrayList<>();
            if (tvBalance != null) steps.add(CoachMarkManager.step(tvBalance, R.string.tour_wallet_balance_title, R.string.tour_wallet_balance_desc));
            if (etCode != null) steps.add(CoachMarkManager.step(etCode, R.string.tour_wallet_code_title, R.string.tour_wallet_code_desc));
            if (btnRedeem != null) steps.add(CoachMarkManager.step(btnRedeem, R.string.tour_wallet_redeem_title, R.string.tour_wallet_redeem_desc));
            if (rvHistory != null) steps.add(CoachMarkManager.step(rvHistory, R.string.tour_wallet_history_title, R.string.tour_wallet_history_desc));
            CoachMarkManager.showIfNeeded(WalletFragment.this, "tour_wallet", steps);
        });
    }

    private void render(WalletViewModel.UIState state){
        if (state == null) return;
        if (state.loading){ show(progress); return; }
        if (state.error != null){ show(errorContainer); return; }
        if (state.data != null){
            show(content);
            tvBalance.setText(String.valueOf(state.data.points));
            List<BillingApi.Tx> txs = state.data.transactions != null ? state.data.transactions : new ArrayList<>();
            adapter.setData(txs);
            tvEmpty.setVisibility(txs.isEmpty()?View.VISIBLE:View.GONE);
        }
    }

    private void show(View v){
        if (content != null) content.setVisibility(v==content?View.VISIBLE:View.GONE);
        if (errorContainer != null) errorContainer.setVisibility(v==errorContainer?View.VISIBLE:View.GONE);
        if (progress != null) progress.setVisibility(v==progress?View.VISIBLE:View.GONE);
    }

    private void applyCode(){
        String code = etCode.getText()!=null ? etCode.getText().toString().trim() : "";
        if (code.isEmpty()){
            Toast.makeText(requireContext(), R.string.wallet_redeem_hint, Toast.LENGTH_SHORT).show();
            return;
        }
        vm.redeem(code, new BillingRepository.Cb<BillingApi.CouponResponse>() {
            @Override public void ok(BillingApi.CouponResponse d) {
                Toast.makeText(requireContext(), R.string.wallet_redeem_success, Toast.LENGTH_SHORT).show();
                etCode.setText("");
                vm.load();
                loadInvoices();
            }

            @Override public void err(Throwable e) {
                Toast.makeText(requireContext(), R.string.wallet_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void wireTopupChips(View v) {
        if (chipTopupPresets != null) {
            chipTopupPresets.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds == null || checkedIds.isEmpty() || etMtnAmount == null) return;
                int id = checkedIds.get(0);
                if (id == R.id.chipTopup50) etMtnAmount.setText("50");
                else if (id == R.id.chipTopup100) etMtnAmount.setText("100");
                else if (id == R.id.chipTopup300) etMtnAmount.setText("300");
            });
        }
        if (chipTopupMethod != null) {
            chipTopupMethod.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds == null || checkedIds.isEmpty()) return;
                useSyriatel = checkedIds.get(0) == R.id.chipMethodSyriatel;
                mtnReference = null;
                if (btnMtnConfirm != null) btnMtnConfirm.setVisibility(View.GONE);
                if (tvMtnInstructions != null) {
                    tvMtnInstructions.setText(useSyriatel ? R.string.wallet_syriatel_desc : R.string.wallet_mtn_desc);
                }
            });
        }
    }

    private void startMobileTopup() {
        String amountRaw = etMtnAmount != null && etMtnAmount.getText() != null ? etMtnAmount.getText().toString().trim() : "";
        int amount;
        try { amount = Integer.parseInt(amountRaw); } catch (NumberFormatException e) { amount = 0; }
        if (amount <= 0) {
            Toast.makeText(requireContext(), R.string.wallet_topup_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        String phone = etTopupPhone != null && etTopupPhone.getText() != null ? etTopupPhone.getText().toString().trim() : "";
        BillingRepository.Cb<BillingApi.MtnInitResponse> cb = new BillingRepository.Cb<BillingApi.MtnInitResponse>() {
            @Override public void ok(BillingApi.MtnInitResponse d) {
                if (!isAdded() || d == null) return;
                mtnReference = d.reference;
                if (tvMtnInstructions != null && d.instructions != null) {
                    tvMtnInstructions.setText(d.instructions);
                }
                if (btnMtnConfirm != null) btnMtnConfirm.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), R.string.wallet_mtn_reference_ready, Toast.LENGTH_LONG).show();
            }
            @Override public void err(Throwable e) {
                if (isAdded()) Toast.makeText(requireContext(), R.string.wallet_mtn_failed, Toast.LENGTH_SHORT).show();
            }
        };
        if (useSyriatel) billingRepo.syriatelInit(amount, phone, cb);
        else billingRepo.mtnInit(amount, phone, cb);
    }

    private void confirmMobileTopup() {
        if (mtnReference == null || mtnReference.isEmpty()) {
            Toast.makeText(requireContext(), R.string.wallet_mtn_reference_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        String tx = etMtnTransactionId != null && etMtnTransactionId.getText() != null
                ? etMtnTransactionId.getText().toString().trim() : "";
        if (tx.isEmpty()) {
            Toast.makeText(requireContext(), R.string.wallet_mtn_transaction_hint, Toast.LENGTH_SHORT).show();
            return;
        }
        BillingRepository.Cb<BillingApi.MtnConfirmResponse> cb = new BillingRepository.Cb<BillingApi.MtnConfirmResponse>() {
            @Override public void ok(BillingApi.MtnConfirmResponse d) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.wallet_mtn_success, Toast.LENGTH_SHORT).show();
                mtnReference = null;
                if (etMtnTransactionId != null) etMtnTransactionId.setText("");
                if (btnMtnConfirm != null) btnMtnConfirm.setVisibility(View.GONE);
                vm.load();
                loadInvoices();
            }
            @Override public void err(Throwable e) {
                if (isAdded()) Toast.makeText(requireContext(), R.string.wallet_mtn_failed, Toast.LENGTH_SHORT).show();
            }
        };
        if (useSyriatel) billingRepo.syriatelConfirm(mtnReference, tx, cb);
        else billingRepo.mtnConfirm(mtnReference, tx, cb);
    }

    private void startMtnTopup() { startMobileTopup(); }

    private void confirmMtnTopup() { confirmMobileTopup(); }

    private void loadInvoices() {
        BillingRepository billingRepo = new BillingRepository(requireContext());
        billingRepo.invoices(new BillingRepository.Cb<BillingApi.InvoicesResponse>() {
            @Override public void ok(BillingApi.InvoicesResponse d) {
                if (!isAdded()) return;
                java.util.List<BillingApi.Invoice> list = d != null && d.data != null ? d.data : new ArrayList<>();
                boolean empty = list.isEmpty();
                if (tvInvoicesEmpty != null) tvInvoicesEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (tvInvoices == null) return;
                if (empty) {
                    tvInvoices.setText("");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                int limit = Math.min(10, list.size());
                for (int i = 0; i < limit; i++) {
                    BillingApi.Invoice inv = list.get(i);
                    if (inv == null) continue;
                    if (sb.length() > 0) sb.append("\n");
                    sb.append("#").append(inv.id).append(" — ")
                        .append(inv.total != null ? inv.total : 0).append(" ")
                        .append(inv.currency != null ? inv.currency : "")
                        .append(" (").append(inv.status != null ? inv.status : "—").append(")");
                }
                tvInvoices.setText(sb.toString());
            }
            @Override public void err(Throwable e) {
                if (isAdded() && tvInvoicesEmpty != null) tvInvoicesEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private static class TxAdapter extends RecyclerView.Adapter<TxAdapter.VH>{
        private final List<BillingApi.Tx> data = new ArrayList<>();
        void setData(List<BillingApi.Tx> list){
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallet_tx, parent, false);
            return new VH(view);
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int position) { holder.bind(data.get(position)); }
        @Override public int getItemCount(){ return data.size(); }
        static class VH extends RecyclerView.ViewHolder{
            TextView tvType, tvMeta, tvAmount;
            VH(@NonNull View itemView){
                super(itemView);
                tvType = itemView.findViewById(R.id.tvType);
                tvMeta = itemView.findViewById(R.id.tvMeta);
                tvAmount = itemView.findViewById(R.id.tvAmount);
            }
            void bind(BillingApi.Tx tx){
                tvType.setText(tx.type != null ? tx.type : "");
                tvMeta.setText(tx.created_at != null ? tx.created_at : (tx.currency!=null?tx.currency:""));
                boolean pointTx = tx.points != 0
                        || "point_credit".equalsIgnoreCase(tx.type)
                        || "point_debit".equalsIgnoreCase(tx.type)
                        || "balance_to_points".equalsIgnoreCase(tx.type);
                int signed = pointTx ? tx.points : tx.amount;
                // Debit types with positive amount still count as money out
                if (!pointTx && tx.type != null) {
                    String t = tx.type.toLowerCase();
                    if ((t.contains("debit") || t.contains("charge") || t.contains("hold")) && signed > 0) {
                        signed = -signed;
                    }
                }
                String prefix = pointTx ? "PTS " : (tx.currency != null ? tx.currency + " " : "");
                tvAmount.setText(prefix + (signed > 0 ? "+" : "") + signed);
                tvAmount.setTextColor(itemView.getResources().getColor(
                        signed >=0 ? R.color.sanad_primary : R.color.sanad_error));
            }
        }
    }

    @Override
    public void onDestroyView() {
        try { CoachMarkManager.dismissActive(); } catch (Throwable ignored) {}
        super.onDestroyView();
    }

}
