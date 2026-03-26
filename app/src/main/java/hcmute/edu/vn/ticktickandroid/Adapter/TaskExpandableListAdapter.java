package hcmute.edu.vn.ticktickandroid.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hcmute.edu.vn.ticktickandroid.ContactActivity;
import hcmute.edu.vn.ticktickandroid.R;
import hcmute.edu.vn.ticktickandroid.Task.TaskEntity;

public class TaskExpandableListAdapter extends BaseExpandableListAdapter {

    public interface OnTaskActionListener {
        void onTaskCheckedChanged(TaskEntity task, boolean isChecked);
        void onTaskLongClick(TaskEntity task);
        void onSelectionModeChanged(boolean enabled);
    }

    private static final SimpleDateFormat DUE_FORMAT =
            new SimpleDateFormat("dd/MM  HH:mm:ss", Locale.getDefault());

    private Context context;
    private List<String> groupList;
    private Map<String, List<TaskEntity>> taskMap;
    private OnTaskActionListener listener;

    private boolean isSelectionMode = false;
    private List<TaskEntity> selectedTasks = new ArrayList<>();

    public TaskExpandableListAdapter(Context context, List<String> groupList,
                                     Map<String, List<TaskEntity>> taskMap,
                                     OnTaskActionListener listener) {
        this.context = context;
        this.groupList = groupList;
        this.taskMap = taskMap;
        this.listener = listener;
        sortAllInternal();
    }

    public boolean isSelectionMode() { return isSelectionMode; }
    
    public void setSelectionMode(boolean enabled) {
        this.isSelectionMode = enabled;
        if (!enabled) selectedTasks.clear();
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionModeChanged(enabled);
    }

    public List<TaskEntity> getSelectedTasks() { return selectedTasks; }

    @Override
    public int getGroupCount() { return groupList.size(); }

    @Override
    public int getChildrenCount(int groupPosition) {
        String group = groupList.get(groupPosition);
        List<TaskEntity> tasks = taskMap.get(group);
        return tasks != null ? tasks.size() : 0;
    }

    @Override
    public Object getGroup(int groupPosition) { return groupList.get(groupPosition); }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        String group = groupList.get(groupPosition);
        return taskMap.get(group).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) { return groupPosition; }

    @Override
    public long getChildId(int groupPosition, int childPosition) { return childPosition; }

    @Override
    public boolean hasStableIds() { return false; }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_task_group, parent, false);
        }
        String group = groupList.get(groupPosition);
        int count = getChildrenCount(groupPosition);

        TextView tvGroupName = convertView.findViewById(R.id.tv_group_name);
        TextView tvGroupCount = convertView.findViewById(R.id.tv_group_count);
        TextView tvArrow = convertView.findViewById(R.id.tv_arrow);

        tvGroupName.setText(group);
        tvGroupCount.setText(String.valueOf(count));
        tvArrow.setText(isExpanded ? "▾" : "▸");

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        }
        TaskEntity task = (TaskEntity) getChild(groupPosition, childPosition);

        CheckBox checkBox = convertView.findViewById(R.id.cb_task);
        TextView tvTitle = convertView.findViewById(R.id.tv_task_title);
        TextView tvDue = convertView.findViewById(R.id.tv_task_due);
        ImageView btnEdit = convertView.findViewById(R.id.btn_edit_task);
        View btnShareSms = convertView.findViewById(R.id.btn_share_sms);

        tvTitle.setText(task.getTitle());
        
        if (isSelectionMode) {
            btnEdit.setVisibility(View.GONE);
            btnShareSms.setVisibility(View.GONE);
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(selectedTasks.contains(task));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedTasks.contains(task)) selectedTasks.add(task);
                } else {
                    selectedTasks.remove(task);
                }
                if (listener != null) listener.onSelectionModeChanged(true);
            });
            
            convertView.setOnClickListener(v -> checkBox.setChecked(!checkBox.isChecked()));
            convertView.setOnLongClickListener(null);
        } else {
            btnEdit.setVisibility(View.VISIBLE);
            btnShareSms.setVisibility(View.VISIBLE);
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(task.isCompleted());
            
            btnEdit.setOnClickListener(v -> { if (listener != null) listener.onTaskLongClick(task); });
            btnShareSms.setOnClickListener(v -> {
                Intent intent = new Intent(context, ContactActivity.class);
                intent.putExtra("TASK_CONTENT", task.getTitle());
                context.startActivity(intent);
            });

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) listener.onTaskCheckedChanged(task, isChecked);
            });

            convertView.setOnClickListener(v -> {
                // If user clicks item but not checkbox in normal mode, toggle checkbox
                checkBox.setChecked(!checkBox.isChecked());
            });
            
            convertView.setOnLongClickListener(v -> {
                setSelectionMode(true);
                if (!selectedTasks.contains(task)) selectedTasks.add(task);
                notifyDataSetChanged();
                return true;
            });
        }

        // Apply strike-through consistently
        if (task.isCompleted()) {
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvTitle.setAlpha(0.5f);
        } else {
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            tvTitle.setAlpha(1.0f);
        }

        if (task.getDueDate() > 0) {
            tvDue.setVisibility(View.VISIBLE);
            tvDue.setText(DUE_FORMAT.format(new Date(task.getDueDate())));
        } else {
            tvDue.setVisibility(View.GONE);
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) { return true; }

    private void sortAllInternal() {
        for (String key : taskMap.keySet()) {
            List<TaskEntity> tasks = taskMap.get(key);
            if (tasks != null) {
                Collections.sort(tasks, (t1, t2) -> {
                    long d1 = t1.getDueDate();
                    long d2 = t2.getDueDate();
                    if (d1 == 0 && d2 == 0) return 0;
                    if (d1 == 0) return 1;
                    if (d2 == 0) return -1;
                    return Long.compare(d1, d2);
                });
            }
        }
    }

    public void sortAllTasks() {
        sortAllInternal();
        notifyDataSetChanged();
    }
}
