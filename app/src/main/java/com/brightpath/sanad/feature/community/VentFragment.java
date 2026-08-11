package com.brightpath.sanad.feature.community;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.brightpath.sanad.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

public class VentFragment extends Fragment {
    private VentRepository repo;
    private RecyclerView rv;
    private ProgressBar progress;
    private TextView empty;
    private EditText composer;
    private SwipeRefreshLayout swipeRefresh;
    private Adapter adapter;
    private boolean posting;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        if (!com.brightpath.sanad.ui.PatientOnlyGuard.allowOrLeave(this)) return;
        repo = new VentRepository(requireContext());
        rv = v.findViewById(R.id.rvVent);
        progress = v.findViewById(R.id.progress);
        empty = v.findViewById(R.id.tvEmpty);
        composer = v.findViewById(R.id.etComposer);
        swipeRefresh = v.findViewById(R.id.swipeRefresh);
        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        MaterialButton btnPost = v.findViewById(R.id.btnPost);

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(x -> NavHostFragment.findNavController(this).popBackStack());
        }

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new Adapter(repo, this::load);
        rv.setAdapter(adapter);

        if (btnPost != null) {
            btnPost.setOnClickListener(x -> postVent());
        }

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::load);
        }

        load();
    }

    private void load() {
        if (progress != null && (swipeRefresh == null || !swipeRefresh.isRefreshing())) {
            progress.setVisibility(View.VISIBLE);
        }
        repo.list(new VentRepository.Cb<VentModels.VentList>() {
            @Override
            public void ok(VentModels.VentList list) {
                if (!isAdded()) return;
                List<VentModels.VentPost> posts = list != null && list.data != null ? list.data : new ArrayList<>();
                adapter.submit(posts);
                if (progress != null) progress.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (empty != null) empty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void err(Throwable e) {
                if (!isAdded()) return;
                if (progress != null) progress.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), R.string.vent_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void postVent() {
        if (posting || composer == null) return;
        String body = composer.getText() != null ? composer.getText().toString().trim() : "";
        if (TextUtils.isEmpty(body)) {
            Toast.makeText(requireContext(), R.string.vent_post_hint, Toast.LENGTH_SHORT).show();
            return;
        }
        posting = true;
        repo.create(body, new VentRepository.Cb<VentModels.VentPost>() {
            @Override
            public void ok(VentModels.VentPost post) {
                if (!isAdded()) return;
                posting = false;
                composer.setText("");
                Toast.makeText(requireContext(), R.string.vent_post_success, Toast.LENGTH_SHORT).show();
                load();
            }

            @Override
            public void err(Throwable e) {
                if (!isAdded()) return;
                posting = false;
                Toast.makeText(requireContext(), R.string.vent_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    static class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        private final List<VentModels.VentPost> data = new ArrayList<>();
        private final VentRepository repo;
        private final Runnable onChanged;

        Adapter(VentRepository repo, Runnable onChanged) {
            this.repo = repo;
            this.onChanged = onChanged;
        }

        void submit(List<VentModels.VentPost> posts) {
            data.clear();
            if (posts != null) data.addAll(posts);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vent_post, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            VentModels.VentPost post = data.get(position);
            String alias = post.alias != null && !post.alias.isEmpty()
                    ? post.alias
                    : holder.itemView.getContext().getString(R.string.community_vent_alias, post.id);
            holder.alias.setText(alias);
            holder.body.setText(post.body != null ? post.body : "");
            styleReactionButton(
                    holder.btnEmpathy,
                    R.string.community_vent_empathy_count,
                    Math.max(0, post.empathy_count),
                    post.user_empathy);
            styleReactionButton(
                    holder.btnSupport,
                    R.string.community_vent_support_count,
                    Math.max(0, post.support_count),
                    post.user_support);
            holder.btnEmpathy.setOnClickListener(x -> react(post, "empathy", holder));
            holder.btnSupport.setOnClickListener(x -> react(post, "support", holder));
            holder.btnReport.setOnClickListener(x -> report(post, holder));
        }

        private void styleReactionButton(MaterialButton btn, int labelRes, int count, boolean active) {
            btn.setText(btn.getContext().getString(labelRes, count));
            btn.setIcon(null);
            int primary = MaterialColors.getColor(btn, com.google.android.material.R.attr.colorPrimary);
            int onPrimary = ContextCompat.getColor(btn.getContext(), R.color.sanad_on_primary);
            int softBg = ContextCompat.getColor(btn.getContext(), R.color.sanad_button_bg);
            if (active) {
                btn.setBackgroundTintList(ColorStateList.valueOf(primary));
                btn.setTextColor(onPrimary);
            } else {
                btn.setBackgroundTintList(ColorStateList.valueOf(softBg));
                btn.setTextColor(primary);
            }
        }

        private void react(VentModels.VentPost post, String type, VH holder) {
            repo.react(post.id, type, new VentRepository.Cb<VentModels.ReactResponse>() {
                @Override
                public void ok(VentModels.ReactResponse res) {
                    if (res != null) {
                        if ("empathy".equals(type)) {
                            post.user_empathy = res.active;
                            post.empathy_count = res.count;
                        } else {
                            post.user_support = res.active;
                            post.support_count = res.count;
                        }
                        int pos = data.indexOf(post);
                        if (pos >= 0) {
                            notifyItemChanged(pos);
                        }
                    } else if (onChanged != null) {
                        onChanged.run();
                    }
                }

                @Override
                public void err(Throwable e) {
                    Toast.makeText(holder.itemView.getContext(), R.string.vent_error, Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void report(VentModels.VentPost post, VH holder) {
            repo.report(post.id, null, new VentRepository.Cb<VentModels.ReportResponse>() {
                @Override
                public void ok(VentModels.ReportResponse res) {
                    Toast.makeText(holder.itemView.getContext(), R.string.community_vent_report_sent, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void err(Throwable e) {
                    Toast.makeText(holder.itemView.getContext(), R.string.vent_error, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView alias, body;
            final com.google.android.material.button.MaterialButton btnEmpathy, btnSupport, btnReport;

            VH(@NonNull View itemView) {
                super(itemView);
                alias = itemView.findViewById(R.id.tvAlias);
                body = itemView.findViewById(R.id.tvBody);
                btnEmpathy = itemView.findViewById(R.id.btnEmpathy);
                btnSupport = itemView.findViewById(R.id.btnSupport);
                btnReport = itemView.findViewById(R.id.btnReport);
            }
        }
    }
}
