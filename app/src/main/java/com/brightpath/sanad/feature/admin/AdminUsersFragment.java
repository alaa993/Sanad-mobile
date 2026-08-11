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
 * شاشة مراجعة مستخدمي النظام من قبل الأدمن.
 */
public class AdminUsersFragment extends Fragment {
  private AdminViewModels.UsersVM vm;
  private ProgressBar progress;
  private TextView tvError, tvEmpty;
  private SwipeRefreshLayout swipe;
  private UsersAdapter adapter;

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_admin_users, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    progress = view.findViewById(R.id.progress);
    tvError = view.findViewById(R.id.tvError);
    tvEmpty = view.findViewById(R.id.tvEmpty);
    swipe = view.findViewById(R.id.swipe);
    RecyclerView rv = view.findViewById(R.id.rv);
    rv.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter = new UsersAdapter();
    rv.setAdapter(adapter);

    vm = new ViewModelProvider(this).get(AdminViewModels.UsersVM.class);
    vm.state().observe(getViewLifecycleOwner(), this::render);
    swipe.setOnRefreshListener(() -> vm.load());
    vm.load();
  }

  private void render(AdminViewModels.ListState<AdminModels.Users.User> state){
    if(state==null) return;
    progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
    swipe.setRefreshing(false);
    if(!TextUtils.isEmpty(state.error)){
      tvError.setText(state.error);
      tvError.setVisibility(View.VISIBLE);
    } else {
      tvError.setVisibility(View.GONE);
    }
    adapter.submit(state.data);
    boolean empty = !state.loading && (state.data==null || state.data.isEmpty());
    tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
  }

  private static class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.VH>{
    private final List<AdminModels.Users.User> data = new ArrayList<>();
    void submit(List<AdminModels.Users.User> list){
      data.clear();
      if(list!=null) data.addAll(list);
      notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
      View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user,parent,false);
      return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH holder,int position){
      AdminModels.Users.User user=data.get(position);
      holder.name.setText(user.name!=null?user.name:"-");
      holder.email.setText(user.email!=null?user.email:"-");
      holder.meta.setText(holder.itemView.getContext().getString(
          R.string.admin_user_meta,
          safe(user.role),
          safe(user.status)));
      holder.created.setText(safe(user.created_at));
    }

    @Override public int getItemCount(){ return data.size(); }

    private String safe(String src){ return TextUtils.isEmpty(src)?"-":src; }

    static class VH extends RecyclerView.ViewHolder{
      final TextView name,email,meta,created;
      VH(@NonNull View itemView){
        super(itemView);
        name=itemView.findViewById(R.id.tvName);
        email=itemView.findViewById(R.id.tvEmail);
        meta=itemView.findViewById(R.id.tvMeta);
        created=itemView.findViewById(R.id.tvCreated);
      }
    }
  }
}
