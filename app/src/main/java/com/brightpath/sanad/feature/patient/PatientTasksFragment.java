package com.brightpath.sanad.feature.patient;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.brightpath.sanad.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PatientTasksFragment extends Fragment {
    private PatientTaskViewModel vm;
    private TaskAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_tasks, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        RecyclerView rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TaskAdapter(this::promptCompleteTask);
        rv.setAdapter(adapter);

        vm = new ViewModelProvider(this).get(PatientTaskViewModel.class);
        vm.getTasks().observe(getViewLifecycleOwner(), tasks -> adapter.submit(tasks));

        MaterialButton btnAdd = v.findViewById(R.id.btnAddTask);
        btnAdd.setOnClickListener(x -> openDialog());
    }

    private void openDialog(){
        View dialog = LayoutInflater.from(requireContext()).inflate(R.layout.view_task_dialog, null, false);
        TextInputEditText etTitle = dialog.findViewById(R.id.etTaskTitle);
        TextInputEditText etBody = dialog.findViewById(R.id.etTaskBody);
        TextInputEditText etSession = dialog.findViewById(R.id.etSessionId);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("واجب للجلسة")
                .setView(dialog)
                .setPositiveButton("حفظ", (d, w) -> {
                    PatientTask task = new PatientTask();
                    task.title = textOf(etTitle);
                    task.description = textOf(etBody);
                    task.sessionId = textOf(etSession);
                    task.dueAt = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000;
                    vm.addTask(task);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void promptCompleteTask(PatientTask task){
        View dialog = LayoutInflater.from(requireContext()).inflate(R.layout.view_task_complete, null, false);
        TextInputEditText etNote = dialog.findViewById(R.id.etCompletionNote);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.task_completion_dialog_title)
                .setView(dialog)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String note = textOf(etNote);
                    vm.completeTask(task, note);
                    android.widget.Toast.makeText(requireContext(), R.string.task_completion_dialog_success, android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String textOf(@Nullable TextInputEditText field){
        return field!=null && field.getText()!=null ? field.getText().toString().trim() : null;
    }

    static class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {
        interface Listener { void onComplete(PatientTask task); }
        private List<PatientTask> items;
        private final Listener listener;

        TaskAdapter(Listener listener){
            this.listener = listener;
        }

        void submit(List<PatientTask> list){
            items = list;
            notifyDataSetChanged();
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_task, parent, false), listener);
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int position){
            holder.bind(items.get(position));
        }
        @Override public int getItemCount(){ return items!=null?items.size():0; }

        static class VH extends RecyclerView.ViewHolder {
            private final android.widget.TextView tvTitle, tvDesc, tvDue, tvStatus, tvNote, tvCompletedAt;
            private final MaterialButton btnComplete;
            private final Listener listener;
            VH(@NonNull View itemView, Listener listener){
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTaskTitle);
                tvDesc = itemView.findViewById(R.id.tvTaskDesc);
                tvDue = itemView.findViewById(R.id.tvTaskDue);
                tvStatus = itemView.findViewById(R.id.tvTaskStatus);
                tvNote = itemView.findViewById(R.id.tvTaskNote);
                tvCompletedAt = itemView.findViewById(R.id.tvTaskCompletedAt);
                btnComplete = itemView.findViewById(R.id.btnCompleteTask);
                this.listener = listener;
            }
            void bind(PatientTask task){
                tvTitle.setText(!TextUtils.isEmpty(task.title) ? task.title : "واجب");
                if (!TextUtils.isEmpty(task.description)){
                    tvDesc.setVisibility(View.VISIBLE);
                    tvDesc.setText(task.description);
                } else {
                    tvDesc.setVisibility(View.GONE);
                }
                SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                tvDue.setText(itemView.getResources().getString(R.string.task_due_at, df.format(new Date(task.dueAt))));
                boolean completed = task.status == PatientTask.Status.COMPLETED;
                tvStatus.setText(itemView.getResources().getString(
                        completed ? R.string.task_status_completed : R.string.task_status_pending));
                if (completed && !TextUtils.isEmpty(task.completionNote)){
                    tvNote.setText(itemView.getResources().getString(R.string.task_completion_note_label, task.completionNote));
                    tvNote.setVisibility(View.VISIBLE);
                } else {
                    tvNote.setVisibility(View.GONE);
                }
                if (completed && task.completedAt > 0){
                    SimpleDateFormat finishFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                    tvCompletedAt.setText(itemView.getResources().getString(R.string.task_completed_at, finishFormat.format(new Date(task.completedAt))));
                    tvCompletedAt.setVisibility(View.VISIBLE);
                } else {
                    tvCompletedAt.setVisibility(View.GONE);
                }
                if (completed){
                    btnComplete.setVisibility(View.GONE);
                } else {
                    btnComplete.setVisibility(View.VISIBLE);
                    btnComplete.setOnClickListener(v -> {
                        if (listener != null) listener.onComplete(task);
                    });
                }
            }
        }
    }
}
