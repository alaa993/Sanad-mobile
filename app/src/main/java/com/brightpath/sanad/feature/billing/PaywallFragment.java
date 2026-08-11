
package com.brightpath.sanad.feature.billing;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.brightpath.sanad.R;
import java.util.*;

public class PaywallFragment extends Fragment {
  private BillingRepository repo;
  private RecyclerView rv;
  private PlansAdapter ad;

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
    return i.inflate(R.layout.fragment_paywall, c, false);
  }

  @Override
  public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
    super.onViewCreated(v, s);
    rv = v.findViewById(R.id.rvPlans);
    rv.setLayoutManager(new LinearLayoutManager(requireContext()));
    repo = new BillingRepository(requireContext());
    ad = new PlansAdapter(plan -> repo.subscribe(plan.id, new BillingRepository.Cb<BillingApi.GenericResponse>() {
      public void ok(BillingApi.GenericResponse d) {
        Toast.makeText(getContext(), R.string.paywall_subscribed, Toast.LENGTH_SHORT).show();
      }
      public void err(Throwable e) {
        Toast.makeText(getContext(), R.string.paywall_error, Toast.LENGTH_SHORT).show();
      }
    }));
    rv.setAdapter(ad);
    repo.loadPlans(new BillingRepository.Cb<BillingApi.PlansResponse>() {
      public void ok(BillingApi.PlansResponse d) { ad.submit(d.data); }
      public void err(Throwable e) {}
    });
  }

  static class PlansAdapter extends RecyclerView.Adapter<PlansAdapter.VH> {
    interface OnClick { void select(BillingApi.Plan plan); }
    private final List<BillingApi.Plan> data = new ArrayList<>();
    private final OnClick on;
    PlansAdapter(OnClick on) { this.on = on; }
    void submit(List<BillingApi.Plan> d) { data.clear(); if (d != null) data.addAll(d); notifyDataSetChanged(); }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
      return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_plan, p, false));
    }
    @Override public void onBindViewHolder(@NonNull VH h, int i) {
      BillingApi.Plan p = data.get(i);
      h.title.setText(p.slug + " · " + p.cycle);
      h.price.setText(p.price + " " + p.currency);
      h.btn.setOnClickListener(x -> on.select(p));
    }
    @Override public int getItemCount() { return data.size(); }
    static class VH extends RecyclerView.ViewHolder {
      TextView title, price;
      Button btn;
      VH(@NonNull View v) {
        super(v);
        title = v.findViewById(R.id.tvTitle);
        price = v.findViewById(R.id.tvPrice);
        btn = v.findViewById(R.id.btnChoose);
      }
    }
  }
}
