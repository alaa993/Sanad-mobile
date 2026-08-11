package com.brightpath.sanad.feature.admin;

import android.os.Bundle;
import android.text.TextUtils;
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

import com.google.android.material.snackbar.Snackbar;
import com.brightpath.sanad.R;

import java.util.ArrayList;
import java.util.List;

/**
 * واجهة إدارة المقالات والمحتوى من قبل الأدمن.
 */
public class AdminLibraryFragment extends Fragment {
  private AdminViewModels.LibraryVM vm;
  private ProgressBar progress;
  private TextView tvError,tvEmpty;
  private SwipeRefreshLayout swipe;
  private PostsAdapter adapter;

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_admin_library, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    progress=view.findViewById(R.id.progress);
    tvError=view.findViewById(R.id.tvError);
    tvEmpty=view.findViewById(R.id.tvEmpty);
    swipe=view.findViewById(R.id.swipe);
    RecyclerView rv=view.findViewById(R.id.rv);
    rv.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter=new PostsAdapter(id -> {
      Snackbar.make(requireView(), R.string.admin_library_toggling, Snackbar.LENGTH_SHORT).show();
      vm.toggle(id);
    });
    rv.setAdapter(adapter);
    vm=new ViewModelProvider(this).get(AdminViewModels.LibraryVM.class);
    vm.state().observe(getViewLifecycleOwner(), this::render);
    swipe.setOnRefreshListener(() -> vm.load());
    vm.load();
  }

  private void render(AdminViewModels.ListState<AdminModels.Posts.Post> state){
    if(state==null) return;
    progress.setVisibility(state.loading?View.VISIBLE:View.GONE);
    swipe.setRefreshing(false);
    if(!TextUtils.isEmpty(state.error)){
      tvError.setText(state.error);
      tvError.setVisibility(View.VISIBLE);
    } else tvError.setVisibility(View.GONE);
    adapter.submit(state.data);
    boolean empty=!state.loading&&(state.data==null||state.data.isEmpty());
    tvEmpty.setVisibility(empty?View.VISIBLE:View.GONE);
  }

  private static class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.VH>{
    interface Listener{ void onToggle(int id); }
    private final List<AdminModels.Posts.Post> data=new ArrayList<>();
    private final Listener listener;
    PostsAdapter(Listener l){ listener=l; }
    void submit(List<AdminModels.Posts.Post> list){
      data.clear();
      if(list!=null) data.addAll(list);
      notifyDataSetChanged();
    }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
      View v=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_post,parent,false);
      return new VH(v);
    }
    @Override public void onBindViewHolder(@NonNull VH holder,int position){
      AdminModels.Posts.Post post=data.get(position);
      holder.title.setText(post.title!=null?post.title:"-");
      holder.meta.setText(holder.itemView.getContext().getString(
          R.string.admin_post_meta,
          safe(post.status),
          safe(post.type)));
      holder.author.setText(holder.itemView.getContext().getString(
          R.string.admin_post_author,
          safe(post.author),
          safe(post.created_at)));
      holder.stats.setText(holder.itemView.getContext().getString(
          R.string.admin_post_stats,
          post.comments,
          post.likes));
      holder.btnToggle.setText(post != null && "published".equalsIgnoreCase(post.status)
          ? R.string.admin_library_hide
          : R.string.admin_library_publish);
      holder.btnToggle.setOnClickListener(v->{
        if(listener!=null) listener.onToggle(post.id);
      });
    }
    private String safe(String src){ return TextUtils.isEmpty(src)?"-":src; }
    @Override public int getItemCount(){ return data.size(); }
    static class VH extends RecyclerView.ViewHolder{
      final TextView title,meta,author,stats;
      final Button btnToggle;
      VH(@NonNull View itemView){
        super(itemView);
        title=itemView.findViewById(R.id.tvTitle);
        meta=itemView.findViewById(R.id.tvMeta);
        author=itemView.findViewById(R.id.tvAuthor);
        stats=itemView.findViewById(R.id.tvStats);
        btnToggle=itemView.findViewById(R.id.btnToggle);
      }
    }
  }
}
