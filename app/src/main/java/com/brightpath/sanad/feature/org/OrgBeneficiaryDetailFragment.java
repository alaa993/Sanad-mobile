package com.brightpath.sanad.feature.org;

import android.os.Bundle;
import android.text.InputType;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.brightpath.sanad.R;

import java.util.ArrayList;
import java.util.List;

public class OrgBeneficiaryDetailFragment extends Fragment {
    private OrgViewModels.BeneficiaryDetailVM vm;
    private TextView tvName, tvEmail, tvSpecialist;
    private RecyclerView rvSessions;
    private ProgressBar progress;
    private SessionsAdapter adapter;
    private int beneficiaryId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_org_beneficiary_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvName = view.findViewById(R.id.tvBeneficiaryName);
        tvEmail = view.findViewById(R.id.tvBeneficiaryEmail);
        tvSpecialist = view.findViewById(R.id.tvAssignedSpecialist);
        progress = view.findViewById(R.id.detailProgress);
        rvSessions = view.findViewById(R.id.rvSessions);
        rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SessionsAdapter();
        rvSessions.setAdapter(adapter);
        MaterialButton btnAssign = view.findViewById(R.id.btnAssign);
        btnAssign.setOnClickListener(v -> showAssignDialog());

        if (getArguments() != null) {
            beneficiaryId = getArguments().getInt("beneficiaryId", -1);
        }
        vm = new ViewModelProvider(this).get(OrgViewModels.BeneficiaryDetailVM.class);
        vm.detail.observe(getViewLifecycleOwner(), detail -> {
            progress.setVisibility(View.GONE);
            if (detail == null || detail.data == null) return;
            OrgModels.Patient patient = detail.data.patient;
            if (patient != null) {
                tvName.setText(patient.name);
                tvEmail.setText(patient.email);
            }
            if (detail.data.assigned_specialist != null) {
                tvSpecialist.setText("الأخصائي: " + detail.data.assigned_specialist.name);
            } else {
                tvSpecialist.setText("لم يتم تعيين أخصائي");
            }
            adapter.submit(detail.data.sessions);
        });
        if (beneficiaryId > 0) {
            progress.setVisibility(View.VISIBLE);
            vm.load(beneficiaryId);
        }
    }

    private void showAssignDialog() {
        if (beneficiaryId <= 0) return;
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("معرّف الأخصائي");
        new AlertDialog.Builder(requireContext())
                .setTitle("تعيين الأخصائي")
                .setView(input)
                .setPositiveButton("حفظ", (d, which) -> {
                    try {
                        int specId = Integer.parseInt(input.getText().toString().trim());
                        vm.assign(beneficiaryId, specId);
                        Toast.makeText(requireContext(), "تم التعيين", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "رقم غير صالح", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    static class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.VH> {
        private final List<OrgModels.Appointment> data = new ArrayList<>();
        void submit(List<OrgModels.Appointment> list){
            data.clear();
            if(list!=null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
            View v=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_org_session,parent,false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH holder,int position){
            OrgModels.Appointment a=data.get(position);
            holder.title.setText("جلسة #"+a.id+" · "+(a.status!=null?a.status:""));
            holder.meta.setText(a.starts_at+" - "+a.ends_at);
        }
        @Override public int getItemCount(){ return data.size(); }
        static class VH extends RecyclerView.ViewHolder{
            final TextView title,meta;
            VH(@NonNull View itemView){
                super(itemView);
                title=itemView.findViewById(R.id.tvTitle);
                meta=itemView.findViewById(R.id.tvMeta);
            }
        }
    }
}
