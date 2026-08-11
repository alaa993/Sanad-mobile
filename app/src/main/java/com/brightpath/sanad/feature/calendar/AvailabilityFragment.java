
package com.brightpath.sanad.feature.calendar;
import android.os.Bundle; import android.view.*; import android.widget.*;
import androidx.annotation.*; import androidx.fragment.app.Fragment; import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager; import androidx.recyclerview.widget.RecyclerView;
import com.brightpath.sanad.R; import java.util.*;
public class AvailabilityFragment extends Fragment {
  private CalendarViewModels.AvailabilityVM vm;
  @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s){ return i.inflate(R.layout.fragment_availability, c, false); }
  @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s){
    super.onViewCreated(v,s);
    RecyclerView rv = v.findViewById(R.id.rvSlots); rv.setLayoutManager(new LinearLayoutManager(requireContext()));
    Adapter ad = new Adapter(); rv.setAdapter(ad);
    vm = new ViewModelProvider(this).get(CalendarViewModels.AvailabilityVM.class);
    vm.state.observe(getViewLifecycleOwner(), a -> { if(a!=null) ad.submit(a.slots); });
    vm.load();

    v.findViewById(R.id.btnAddSlot).setOnClickListener(x -> {
      EditText w=v.findViewById(R.id.etWeekday), sT=v.findViewById(R.id.etStart), eT=v.findViewById(R.id.etEnd);
      int wd=Integer.parseInt(w.getText().toString().trim()); vm.addSlot(wd, sT.getText().toString().trim(), eT.getText().toString().trim());
    });
  }
  static class Adapter extends RecyclerView.Adapter<Adapter.VH>{
    private final java.util.List<CalendarModels.Slot> data=new java.util.ArrayList<>();
    void submit(java.util.List<CalendarModels.Slot> d){ data.clear(); if(d!=null) data.addAll(d); notifyDataSetChanged(); }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int v){ return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_availability_slot,p,false)); }
    @Override public void onBindViewHolder(@NonNull VH h,int i){ CalendarModels.Slot s=data.get(i); h.title.setText("Day "+s.weekday+" · "+s.start_time+"-"+s.end_time); }
    @Override public int getItemCount(){ return data.size(); }
    static class VH extends RecyclerView.ViewHolder{ TextView title; VH(@NonNull View v){ super(v); title=v.findViewById(R.id.tvTitle);} }
  }
}
