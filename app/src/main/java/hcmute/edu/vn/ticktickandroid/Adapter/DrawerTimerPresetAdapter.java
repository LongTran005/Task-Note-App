package hcmute.edu.vn.ticktickandroid.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.ticktickandroid.R;

public class DrawerTimerPresetAdapter extends RecyclerView.Adapter<DrawerTimerPresetAdapter.ViewHolder> {

    public interface OnPresetClickListener {
        void onClick(int totalSeconds);
    }

    public interface OnPresetLongClickListener {
        void onLongClick(int position, int totalSeconds);
    }

    private final List<Integer> presets;
    private int selectedIndex;
    private final OnPresetClickListener clickListener;
    private OnPresetLongClickListener longClickListener;

    public DrawerTimerPresetAdapter(List<Integer> presets, int defaultIndex, OnPresetClickListener clickListener) {
        this.presets = presets;
        this.selectedIndex = defaultIndex;
        this.clickListener = clickListener;
    }

    public void setOnPresetLongClickListener(OnPresetLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void addPreset(int totalSeconds) {
        presets.add(totalSeconds);
        notifyItemInserted(presets.size() - 1);
    }

    public void removePreset(int position) {
        if (position >= 0 && position < presets.size()) {
            presets.remove(position);
            if (selectedIndex == position) {
                selectedIndex = -1;
            } else if (selectedIndex > position) {
                selectedIndex--;
            }
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_drawer_timer_preset, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int totalSeconds = presets.get(position);
        holder.tvLabel.setText(formatDuration(totalSeconds));
        holder.ivCheck.setVisibility(position == selectedIndex ? View.VISIBLE : View.GONE);

        if (position == selectedIndex) {
            holder.tvLabel.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.PrimaryColor));
        } else {
            holder.tvLabel.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.PrimaryText));
        }

        holder.itemView.setOnClickListener(v -> {
            int old = selectedIndex;
            selectedIndex = holder.getAdapterPosition();
            if (old >= 0 && old < presets.size()) notifyItemChanged(old);
            notifyItemChanged(selectedIndex);
            clickListener.onClick(totalSeconds);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(holder.getAdapterPosition(), totalSeconds);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return presets.size();
    }

    private String formatDuration(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0 && seconds > 0) {
            return String.format(Locale.getDefault(), "%d giờ %d phút %d giây", hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format(Locale.getDefault(), "%d giờ %d phút", hours, minutes);
        } else if (seconds > 0) {
            return String.format(Locale.getDefault(), "%d phút %d giây", minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%d phút", minutes);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLabel;
        ImageView ivCheck;

        ViewHolder(View itemView) {
            super(itemView);
            tvLabel = itemView.findViewById(R.id.tv_preset_label);
            ivCheck = itemView.findViewById(R.id.iv_check);
        }
    }
}
