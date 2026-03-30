package hcmute.edu.vn.ticktickandroid.Adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.List;

import hcmute.edu.vn.ticktickandroid.R;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.ViewHolder> {

    private final List<Calendar> days;
    private final Calendar selectedDate;
    private final Calendar today;
    private final List<Calendar> daysWithEvents;
    private final OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(Calendar date);
    }

    public CalendarDayAdapter(List<Calendar> days, Calendar selectedDate, List<Calendar> daysWithEvents, OnDayClickListener listener) {
        this.days = days;
        this.selectedDate = selectedDate;
        this.daysWithEvents = daysWithEvents;
        this.listener = listener;
        this.today = Calendar.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Calendar day = days.get(position);
        holder.tvDayNumber.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));

        // Check if day is today
        boolean isToday = isSameDay(day, today);
        // Check if day is selected
        boolean isSelected = isSameDay(day, selectedDate);
        // Check if day has event
        boolean hasEvent = false;
        for (Calendar eventDay : daysWithEvents) {
            if (isSameDay(day, eventDay)) {
                hasEvent = true;
                break;
            }
        }

        holder.dotIndicator.setVisibility(hasEvent ? View.VISIBLE : View.GONE);

        if (isSelected) {
            holder.tvDayNumber.setTextColor(Color.WHITE);
            holder.tvDayNumber.setBackgroundResource(R.drawable.bg_calendar_today);
        } else if (isToday) {
            holder.tvDayNumber.setTextColor(Color.parseColor("#009688")); // PrimaryColor
            holder.tvDayNumber.setBackgroundResource(0);
        } else {
            // Check if day is from current month (fade out if not)
            boolean isCurrentMonth = day.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH);
            holder.tvDayNumber.setTextColor(isCurrentMonth ? Color.parseColor("#212121") : Color.parseColor("#BDBDBD"));
            holder.tvDayNumber.setBackgroundResource(0);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDayClick(day);
            }
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) return false;
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNumber;
        View dotIndicator;

        ViewHolder(View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tv_day_number);
            dotIndicator = itemView.findViewById(R.id.dot_indicator);
        }
    }
}
