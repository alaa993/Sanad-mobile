
package com.brightpath.sanad.feature.admin;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.brightpath.sanad.R;

import java.util.*;
public class AdminOrganizationsFragment extends Fragment {
  private AdminViewModels.OrgListVM vm;
  private ProgressBar progress;
  private TextView tvError,tvEmpty;
  private SwipeRefreshLayout swipe;
  @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){ return i.inflate(R.layout.fragment_admin_organizations, c, false); }
  @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){
    super.onViewCreated(v,s);
    progress=v.findViewById(R.id.progress);
    tvError=v.findViewById(R.id.tvError);
    tvEmpty=v.findViewById(R.id.tvEmpty);
    swipe=v.findViewById(R.id.swipe);
    RecyclerView rv=v.findViewById(R.id.rv); rv.setLayoutManager(new LinearLayoutManager(requireContext()));
    OrgAdapter ad=new OrgAdapter(new OrgAdapter.Listener(){
      @Override public void onApprove(int id){ vm.approve(id); }
      @Override public void onReject (int id){
        AdminRejectDialog.show(requireContext(), reason -> vm.reject(id, reason));
      }
      @Override public void onView(int id){
        Bundle args=new Bundle(); args.putInt("organizationId", id);
        androidx.navigation.fragment.NavHostFragment.findNavController(AdminOrganizationsFragment.this)
            .navigate(R.id.action_adminOrganizationsFragment_to_adminOrganizationDetailFragment, args);
      }
    }); rv.setAdapter(ad);
    vm=new ViewModelProvider(this).get(AdminViewModels.OrgListVM.class);
    vm.state().observe(getViewLifecycleOwner(), state -> {
      if(state==null) return;
      progress.setVisibility(state.loading?View.VISIBLE:View.GONE);
      swipe.setRefreshing(false);
      if(state.error!=null){
        tvError.setText(state.error);
        tvError.setVisibility(View.VISIBLE);
      } else tvError.setVisibility(View.GONE);
      ad.submit(state.data);
      boolean empty=!state.loading&&(state.data==null||state.data.isEmpty());
      tvEmpty.setVisibility(empty?View.VISIBLE:View.GONE);
    });
    swipe.setOnRefreshListener(() -> vm.load());
    vm.load();
  }
  static class OrgAdapter extends RecyclerView.Adapter<OrgAdapter.VH> {
    interface Listener{ void onApprove(int id); void onReject(int id); void onView(int id); }
    private final List<AdminModels.Organizations.Organization> data=new ArrayList<>(); private final Listener ls;
    OrgAdapter(Listener l){ this.ls=l; }
    void submit(List<AdminModels.Organizations.Organization> d){ data.clear(); if(d!=null) data.addAll(d); notifyDataSetChanged(); }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int v){ return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_admin_org,p,false)); }
    @Override public void onBindViewHolder(@NonNull VH h,int i){
      AdminModels.Organizations.Organization o=data.get(i);
      h.title.setText(o.name);
      h.meta.setText("status: "+(o.status!=null?o.status:"-"));
      h.btnApprove.setOnClickListener(x-> ls.onApprove(o.id));
      h.btnReject .setOnClickListener(x-> ls.onReject (o.id));
      h.itemView.setOnClickListener(v-> ls.onView(o.id));
    }
    @Override public int getItemCount(){ return data.size(); }
    static class VH extends RecyclerView.ViewHolder{
      TextView title, meta; Button btnApprove, btnReject;
      VH(@NonNull View v){ super(v); title=v.findViewById(R.id.tvTitle); meta=v.findViewById(R.id.tvMeta); btnApprove=v.findViewById(R.id.btnApprove); btnReject=v.findViewById(R.id.btnReject); }
    }
  }
}
