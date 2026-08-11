
package com.brightpath.sanad.feature.calendar;
import android.os.Bundle; import android.view.*; import android.widget.TextView;
import androidx.annotation.*; import androidx.fragment.app.Fragment; import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager; import androidx.recyclerview.widget.RecyclerView;
import com.brightpath.sanad.R; import java.util.*;
public class AppointmentsListFragment extends Fragment {
  private CalendarViewModels.AppointmentsVM vm;
  @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s){ return i.inflate(R.layout.fragment_appointments_list, c, false); }
  @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s){
    super.onViewCreated(v,s);
    RecyclerView rv = v.findViewById(R.id.rvAppointments); rv.setLayoutManager(new LinearLayoutManager(requireContext()));
    Adapter ad = new Adapter(); rv.setAdapter(ad);
    vm = new ViewModelProvider(this).get(CalendarViewModels.AppointmentsVM.class);
    vm.list.observe(getViewLifecycleOwner(), ad::submit);
    vm.load("patient", null, null);
  }
  static class Adapter extends RecyclerView.Adapter<Adapter.VH> {
    private final java.util.List<CalendarModels.Appointment> data = new java.util.ArrayList<>();
    void submit(java.util.List<CalendarModels.Appointment> d){ data.clear(); if(d!=null) data.addAll(d); notifyDataSetChanged(); }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v){ return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_appointment, p, false)); }
    @Override public void onBindViewHolder(@NonNull VH h, int i){
      CalendarModels.Appointment a = data.get(i);
      h.title.setText("موعد · " + a.status);
      h.meta.setText(a.starts_at + " → " + a.ends_at);
    }
    @Override public int getItemCount(){ return data.size(); }
    static class VH extends RecyclerView.ViewHolder { TextView title, meta; VH(@NonNull View v){ super(v); title=v.findViewById(R.id.tvTitle); meta=v.findViewById(R.id.tvMeta); } }
  }
}
