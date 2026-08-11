package com.brightpath.sanad.feature.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.List;

/**
 * شاشة مراقبة الجلسات لجميع المستخدمين.
 */
public class AdminSessionsFragment extends Fragment {
  private AdminViewModels.SessionsVM vm;
  private ProgressBar progress;
  private TextView tvError,tvEmpty,tvFilter;
  private SwipeRefreshLayout swipe;
  private SessionsAdapter adapter;
  private int specialistId;
  private String specialistName;

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_admin_sessions, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    progress=view.findViewById(R.id.progress);
    tvError=view.findViewById(R.id.tvError);
    tvEmpty=view.findViewById(R.id.tvEmpty);
    tvFilter=view.findViewById(R.id.tvFilter);
    swipe=view.findViewById(R.id.swipe);
    RecyclerView rv=view.findViewById(R.id.rv);
    rv.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter=new SessionsAdapter();
    rv.setAdapter(adapter);
    if(getArguments()!=null){
      specialistId = getArguments().getInt("specialistId", 0);
      specialistName = getArguments().getString("specialistName");
      if(specialistId>0 && !TextUtils.isEmpty(specialistName)){
        tvFilter.setVisibility(View.VISIBLE);
        tvFilter.setText(getString(R.string.admin_sessions_filtered, specialistName));
      }
    }
    vm=new ViewModelProvider(this).get(AdminViewModels.SessionsVM.class);
    vm.state().observe(getViewLifecycleOwner(), this::render);
    swipe.setOnRefreshListener(() -> vm.load());
    vm.load();
  }

  private void render(AdminViewModels.ListState<AdminModels.Appointments.Appointment> state){
    if(state==null) return;
    progress.setVisibility(state.loading?View.VISIBLE:View.GONE);
    swipe.setRefreshing(false);
    if(!TextUtils.isEmpty(state.error)){
      tvError.setText(state.error);
      tvError.setVisibility(View.VISIBLE);
    } else tvError.setVisibility(View.GONE);
    List<AdminModels.Appointments.Appointment> list = state.data;
    if(specialistId>0 && list!=null){
      List<AdminModels.Appointments.Appointment> filtered = new ArrayList<>();
      for(AdminModels.Appointments.Appointment appt:list){
        if(appt!=null && appt.specialist_id==specialistId){
          filtered.add(appt);
        }
      }
      list = filtered;
    }
    adapter.submit(list);
    boolean empty=!state.loading&&(list==null||list.isEmpty());
    tvEmpty.setVisibility(empty?View.VISIBLE:View.GONE);
  }

  private static class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.VH>{
    private final List<AdminModels.Appointments.Appointment> data=new ArrayList<>();
    void submit(List<AdminModels.Appointments.Appointment> list){
      data.clear();
      if(list!=null) data.addAll(list);
      notifyDataSetChanged();
    }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
      View v=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_session,parent,false);
      return new VH(v);
    }
    @Override public void onBindViewHolder(@NonNull VH holder,int position){
      AdminModels.Appointments.Appointment appt=data.get(position);
      holder.title.setText(holder.itemView.getContext().getString(
          R.string.admin_session_title,
          safe(appt.patient_name),
          safe(appt.specialist_name)));
      holder.meta.setText(holder.itemView.getContext().getString(
          R.string.admin_session_meta,
          safe(appt.status),
          safe(appt.type)));
      holder.time.setText(holder.itemView.getContext().getString(
          R.string.admin_session_time,
          safe(appt.starts_at),
          safe(appt.ends_at)));
      holder.org.setText(safe(appt.organization_name));
    }
    private String safe(String src){ return TextUtils.isEmpty(src)?"-":src; }
    @Override public int getItemCount(){ return data.size(); }
    static class VH extends RecyclerView.ViewHolder{
      final TextView title,meta,time,org;
      VH(@NonNull View itemView){
        super(itemView);
        title=itemView.findViewById(R.id.tvTitle);
        meta=itemView.findViewById(R.id.tvMeta);
        time=itemView.findViewById(R.id.tvTime);
        org=itemView.findViewById(R.id.tvOrg);
      }
    }
  }
}
