package com.brightpath.sanad.feature.org;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brightpath.sanad.R;

import java.util.ArrayList;
import java.util.List;

public class OrgSpecialistDetailFragment extends Fragment {
    private OrgViewModels.SpecialistDetailVM vm;
    private ProgressBar progress;
    private View errorGroup;
    private TextView tvError;
    private View content;
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvPhone;
    private TextView tvSessions;
    private TextView tvCommitment;
    private TextView tvNextSession;
    private SessionsAdapter sessionsAdapter;
    private BeneficiariesAdapter beneficiariesAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_org_specialist_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progress = view.findViewById(R.id.progress);
        errorGroup = view.findViewById(R.id.errorGroup);
        tvError = view.findViewById(R.id.tvError);
        content = view.findViewById(R.id.contentGroup);
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvSessions = view.findViewById(R.id.tvSessionsCount);
        tvCommitment = view.findViewById(R.id.tvCommitment);
        tvNextSession = view.findViewById(R.id.tvNextSession);
        sessionsAdapter = new SessionsAdapter();
        RecyclerView rvSessions = view.findViewById(R.id.rvSessions);
        rvSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSessions.setAdapter(sessionsAdapter);
        beneficiariesAdapter = new BeneficiariesAdapter();
        RecyclerView rvBeneficiaries = view.findViewById(R.id.rvBeneficiaries);
        rvBeneficiaries.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBeneficiaries.setAdapter(beneficiariesAdapter);
        view.findViewById(R.id.btnBack).setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        int specialistId = requireArguments().getInt("specialistId", -1);
        vm = new ViewModelProvider(this).get(OrgViewModels.SpecialistDetailVM.class);
        vm.state().observe(getViewLifecycleOwner(), this::render);
        if (specialistId >= 0) {
            vm.load(specialistId);
            view.findViewById(R.id.btnRetry).setOnClickListener(v -> vm.load(specialistId));
        } else {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(R.string.error_fetch_data);
        }
    }

    private void render(OrgViewModels.SpecialistDetailVM.UiState state) {
        progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
        if (state.error != null) {
            errorGroup.setVisibility(View.VISIBLE);
            tvError.setText(state.error);
            content.setVisibility(View.GONE);
            return;
        } else {
            errorGroup.setVisibility(View.GONE);
        }
        if (state.data == null || state.data.data == null) {
            content.setVisibility(View.GONE);
            return;
        }
        content.setVisibility(View.VISIBLE);
        OrgModels.SpecialistDetail.Detail detail = state.data.data;
        if (detail.specialist != null) {
            tvName.setText(detail.specialist.name);
            tvEmail.setText(detail.specialist.email == null ? "--" : detail.specialist.email);
            tvPhone.setText(detail.specialist.phone == null ? "--" : detail.specialist.phone);
        }
        if (detail.stats != null) {
            tvSessions.setText(String.valueOf(detail.stats.sessions_count));
            tvCommitment.setText(String.format("%s%%", detail.stats.commitment_rate));
            tvNextSession.setText(detail.stats.next_session_at == null ? getString(R.string.org_reports_no_sessions) : detail.stats.next_session_at);
        }
        sessionsAdapter.submit(detail.sessions);
        beneficiariesAdapter.submit(detail.beneficiaries);
    }

    private static class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.VH> {
        private final List<OrgModels.Appointment> data = new ArrayList<>();
        void submit(List<OrgModels.Appointment> list) {
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull
        @Override public VH onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_row,parent,false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH holder,int position){
            OrgModels.Appointment ap = data.get(position);
            holder.title.setText(ap.starts_at == null ? "--" : ap.starts_at);
            holder.subtitle.setText(ap.status == null ? holder.subtitle.getContext().getString(R.string.session_status_upcoming) : ap.status);
        }
        @Override public int getItemCount(){ return data.size(); }
        static class VH extends RecyclerView.ViewHolder{
            final TextView title,subtitle;
            VH(@NonNull View itemView){
                super(itemView);
                title=itemView.findViewById(R.id.tvTitle);
                subtitle=itemView.findViewById(R.id.tvSubtitle);
            }
        }
    }

    private static class BeneficiariesAdapter extends RecyclerView.Adapter<BeneficiariesAdapter.VH>{
        private final List<OrgModels.SpecialistDetail.BeneficiaryMini> data=new ArrayList<>();
        void submit(List<OrgModels.SpecialistDetail.BeneficiaryMini> list){
            data.clear();
            if(list!=null) data.addAll(list);
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
            View v=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_org_report_beneficiary,parent,false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH holder,int position){
            OrgModels.SpecialistDetail.BeneficiaryMini item=data.get(position);
            holder.name.setText(item.name);
            holder.risk.setText(item.risk_level==null?"--":item.risk_level);
            holder.issue.setVisibility(View.GONE);
            holder.lastSession.setVisibility(View.GONE);
        }
        @Override public int getItemCount(){ return data.size(); }
        static class VH extends RecyclerView.ViewHolder{
            final TextView name,risk,issue,lastSession;
            VH(@NonNull View itemView){
                super(itemView);
                name=itemView.findViewById(R.id.tvName);
                risk=itemView.findViewById(R.id.tvRisk);
                issue=itemView.findViewById(R.id.tvIssue);
                lastSession=itemView.findViewById(R.id.tvLastSession);
            }
        }
    }
}
