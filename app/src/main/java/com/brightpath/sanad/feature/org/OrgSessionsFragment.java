
package com.brightpath.sanad.feature.org;
import android.os.Bundle; import android.view.*; import android.widget.TextView;
import androidx.annotation.*; import androidx.fragment.app.Fragment; import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager; import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.brightpath.sanad.R;
public class OrgSessionsFragment extends Fragment {
  private OrgViewModels.SessionsVM vm;
  @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){ return i.inflate(R.layout.fragment_org_sessions, c, false); }
  @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){
    super.onViewCreated(v,s);
    RecyclerView rv=v.findViewById(R.id.rv); rv.setLayoutManager(new LinearLayoutManager(requireContext())); Adapter ad=new Adapter(); rv.setAdapter(ad);
    SwipeRefreshLayout swipe=v.findViewById(R.id.swipeRefresh);
    vm=new ViewModelProvider(this).get(OrgViewModels.SessionsVM.class);
    vm.list.observe(getViewLifecycleOwner(), list -> {
      ad.submit(list);
      if (swipe != null) swipe.setRefreshing(false);
    });
    if (swipe != null) swipe.setOnRefreshListener(() -> vm.load());
    vm.load();
  }
  static class Adapter extends RecyclerView.Adapter<Adapter.VH>{
    private final java.util.List<OrgModels.Appointment> data=new java.util.ArrayList<>();
    void submit(java.util.List<OrgModels.Appointment> d){ data.clear(); if(d!=null) data.addAll(d); notifyDataSetChanged(); }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int v){ return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_org_session,p,false)); }
    @Override public void onBindViewHolder(@NonNull VH h,int i){ var a=data.get(i); h.title.setText("جلسة · "+a.status); h.meta.setText(a.starts_at+" → "+a.ends_at); }
    @Override public int getItemCount(){ return data.size(); }
    static class VH extends RecyclerView.ViewHolder{ TextView title,meta; VH(@NonNull View v){ super(v); title=v.findViewById(R.id.tvTitle); meta=v.findViewById(R.id.tvMeta);} }
  }
}
