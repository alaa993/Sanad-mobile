package com.brightpath.sanad.feature.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.brightpath.sanad.R;
import com.brightpath.sanad.feature.admin.AdminModels.OrganizationDetail;

public class AdminOrganizationDetailFragment extends Fragment {
    private AdminViewModels.OrgDetailVM vm;
    private TextView tvName, tvStatus, tvStats;
    private View progress;
    private TextView error;
    private int orgId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_organization_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvName = view.findViewById(R.id.tvOrgName);
        tvStatus = view.findViewById(R.id.tvOrgStatus);
        tvStats = view.findViewById(R.id.tvOrgStats);
        progress = view.findViewById(R.id.detailProgress);
        error = view.findViewById(R.id.detailError);
        MaterialButton btnApprove = view.findViewById(R.id.btnApproveOrg);
        MaterialButton btnReject = view.findViewById(R.id.btnRejectOrg);

        if (getArguments() != null) orgId = getArguments().getInt("organizationId", -1);

        vm = new ViewModelProvider(this).get(AdminViewModels.OrgDetailVM.class);
        vm.detail.observe(getViewLifecycleOwner(), detail -> {
            progress.setVisibility(View.GONE);
            if (detail == null || detail.data == null) {
                error.setVisibility(View.VISIBLE);
                error.setText("تعذر تحميل بيانات المنظمة");
                return;
            } else {
                error.setVisibility(View.GONE);
            }
            AdminModels.Organizations.Organization org = detail.data.organization;
            if (org != null) {
                tvName.setText(org.name);
                tvStatus.setText("الحالة: " + org.status);
            }
            if (detail.data.stats != null) {
                AdminModels.OrganizationDetail.Stats stats = detail.data.stats;
                String summary = "الأعضاء: " + stats.members +
                        "\nالأخصائيون: " + stats.specialists +
                        "\nالمستفيدون: " + stats.beneficiaries +
                        "\nالجلسات المنفذة: " + stats.sessions_total +
                        "\nالجلسات القادمة: " + stats.upcoming;
                tvStats.setText(summary);
            }
        });

        btnApprove.setOnClickListener(v -> {
            if (orgId > 0) {
                vm.approve(orgId);
                Toast.makeText(requireContext(), "تم التفعيل", Toast.LENGTH_SHORT).show();
            }
        });
        btnReject.setOnClickListener(v -> {
            if (orgId > 0) {
                AdminRejectDialog.show(requireContext(), reason -> {
                    vm.reject(orgId, reason);
                    Toast.makeText(requireContext(), R.string.admin_review_complete, Toast.LENGTH_SHORT).show();
                });
            }
        });

        if (orgId > 0) {
            progress.setVisibility(View.VISIBLE);
            vm.load(orgId);
        }
    }
}
