package com.brightpath.sanad.feature.org;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.brightpath.sanad.R;

import java.util.ArrayList;
import java.util.List;

public class OrgBeneficiariesFragment extends Fragment {
    private OrgViewModels.BeneficiariesVM vm;
    private ProgressBar progress;
    private TextView empty;
    private BeneficiariesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_org_beneficiaries, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progress = view.findViewById(R.id.progress);
        empty = view.findViewById(R.id.tvEmpty);
        RecyclerView rv = view.findViewById(R.id.rvBeneficiaries);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BeneficiariesAdapter(item -> {
            Bundle args = new Bundle();
            args.putInt("beneficiaryId", item.id);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_orgBeneficiariesFragment_to_orgBeneficiaryDetailFragment, args);
        });
        rv.setAdapter(adapter);

        MaterialButton btnAdd = view.findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> showCreateDialog());

        vm = new ViewModelProvider(this).get(OrgViewModels.BeneficiariesVM.class);
        vm.state().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
            empty.setVisibility(state.data.isEmpty() && !state.loading ? View.VISIBLE : View.GONE);
            adapter.submit(state.data);
            if (state.error != null) {
                Toast.makeText(requireContext(), state.error, Toast.LENGTH_SHORT).show();
            }
        });
        vm.load();
        if (getArguments() != null && getArguments().getBoolean("openAdd", false)) {
            view.post(this::showCreateDialog);
        }
    }

    private void showCreateDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_beneficiary, null, false);
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        EditText etIssue = dialogView.findViewById(R.id.etIssue);
        new AlertDialog.Builder(requireContext())
                .setTitle("مستفيد جديد")
                .setView(dialogView)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    if (TextUtils.isEmpty(etName.getText())) {
                        Toast.makeText(requireContext(), "الاسم مطلوب", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    OrgModels.BeneficiaryForm form = new OrgModels.BeneficiaryForm();
                    form.name = etName.getText().toString().trim();
                    form.email = etEmail.getText().toString().trim();
                    form.primary_issue = etIssue.getText().toString().trim();
                    vm.create(form);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    static class BeneficiariesAdapter extends RecyclerView.Adapter<BeneficiariesAdapter.VH> {
        interface Listener { void onClick(OrgModels.Beneficiary item); }
        private final List<OrgModels.Beneficiary> data = new ArrayList<>();
        private final Listener listener;
        BeneficiariesAdapter(Listener l){ listener=l; }
        void submit(List<OrgModels.Beneficiary> list){
            data.clear();
            if(list!=null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
            View v=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_org_beneficiary,parent,false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH holder,int position){
            OrgModels.Beneficiary item=data.get(position);
            holder.name.setText(item.name);
            holder.meta.setText(item.primary_issue!=null?item.primary_issue:"");
            holder.risk.setText(item.risk_level!=null?"تصنيف: "+item.risk_level:"");
            holder.itemView.setOnClickListener(v->{ if(listener!=null) listener.onClick(item); });
        }
        @Override public int getItemCount(){ return data.size(); }
        static class VH extends RecyclerView.ViewHolder{
            final TextView name, meta, risk;
            VH(@NonNull View itemView){
                super(itemView);
                name=itemView.findViewById(R.id.tvName);
                meta=itemView.findViewById(R.id.tvMeta);
                risk=itemView.findViewById(R.id.tvRisk);
            }
        }
    }
}
