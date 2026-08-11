
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

public class OrgSpecialistsFragment extends Fragment {
  private OrgViewModels.SpecialistsVM vm;
  private ProgressBar progress;
  private TextView tvEmpty;
  private TextView tvError;

  @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){ return i.inflate(R.layout.fragment_org_specialists, c, false); }
  @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){
    super.onViewCreated(v,s);
    progress=v.findViewById(R.id.progress);
    tvEmpty=v.findViewById(R.id.tvEmpty);
    tvError=v.findViewById(R.id.tvError);
    RecyclerView rv=v.findViewById(R.id.rv); rv.setLayoutManager(new LinearLayoutManager(requireContext()));
    Adapter ad=new Adapter(this::openDetail); rv.setAdapter(ad);
    vm=new ViewModelProvider(this).get(OrgViewModels.SpecialistsVM.class);
    vm.list.observe(getViewLifecycleOwner(), list -> {
      ad.submit(list);
      boolean isEmpty = list==null || list.isEmpty();
      tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    });
    vm.loading.observe(getViewLifecycleOwner(), loading -> progress.setVisibility(Boolean.TRUE.equals(loading)?View.VISIBLE:View.GONE));
    vm.error.observe(getViewLifecycleOwner(), err -> {
      if(err!=null){
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(err);
        tvEmpty.setVisibility(View.GONE);
      }else{
        tvError.setVisibility(View.GONE);
      }
    });
    vm.load();
  }

  private void openDetail(int id){
    Bundle args=new Bundle();
    args.putInt("specialistId", id);
    NavHostFragment.findNavController(this).navigate(R.id.orgSpecialistDetailFragment, args);
  }

  static class Adapter extends RecyclerView.Adapter<Adapter.VH>{
    interface OnClick { void onSpecialistClick(int id); }
    private final List<OrgModels.Specialist> data=new ArrayList<>();
    private final OnClick listener;
    Adapter(OnClick listener){ this.listener=listener; }
    void submit(List<OrgModels.Specialist> d){ data.clear(); if(d!=null) data.addAll(d); notifyDataSetChanged(); }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int v){ return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_org_specialist,p,false)); }
    @Override public void onBindViewHolder(@NonNull VH h,int i){
      OrgModels.Specialist s=data.get(i);
      h.title.setText(s.name);
      h.meta.setText(s.email);
      h.sessions.setText(String.valueOf(s.sessions_count));
      h.commitment.setText(String.format(java.util.Locale.getDefault(), "%.1f%%", s.commitment_rate));
      h.nextSession.setText(s.next_session_at==null ? h.nextSession.getContext().getString(R.string.org_reports_no_sessions) : s.next_session_at);
      h.itemView.setOnClickListener(v -> {
        if(listener!=null) listener.onSpecialistClick(s.id);
      });
    }
    @Override public int getItemCount(){ return data.size(); }
    static class VH extends RecyclerView.ViewHolder{
      final TextView title,meta,sessions,commitment,nextSession;
      VH(@NonNull View v){
        super(v);
        title=v.findViewById(R.id.tvTitle);
        meta=v.findViewById(R.id.tvMeta);
        sessions=v.findViewById(R.id.tvSessions);
        commitment=v.findViewById(R.id.tvCommitment);
        nextSession=v.findViewById(R.id.tvNextSession);
      }
    }
  }
}
