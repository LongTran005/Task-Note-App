package hcmute.edu.vn.ticktickandroid.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.ticktickandroid.R;

public class CalendarEventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_TASK = 1;
    public static final int TYPE_REMINDER = 2;

    public static class CalendarEvent {
        public int id;
        public String title;
        public long timestamp;
        public int type;
        public Object data;

        public CalendarEvent(int id, String title, long timestamp, int type, Object data) {
            this.id = id;
            this.title = title;
            this.timestamp = timestamp;
            this.type = type;
            this.data = data;
        }
    }

    private final List<CalendarEvent> events;
    private final OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(CalendarEvent event);
    }

    public CalendarEventAdapter(List<CalendarEvent> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof EventViewHolder) {
            CalendarEvent event = events.get(position);
            EventViewHolder evHolder = (EventViewHolder) holder;

            evHolder.tvTitle.setText(event.title);

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String timeStr = timeFormat.format(new Date(event.timestamp));
            String typeStr = event.type == TYPE_TASK ? "Nhiệm vụ" : "Nhắc nhở";
            evHolder.tvTime.setText(timeStr + " • " + typeStr);

            // Change color indicator based on type
            if (event.type == TYPE_TASK) {
                evHolder.colorIndicator.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.PrimaryColor));
            } else {
                evHolder.colorIndicator.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.AccentColor));
            }

            evHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime;
        View colorIndicator;

        EventViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvTime = itemView.findViewById(R.id.tv_event_time);
            colorIndicator = itemView.findViewById(R.id.color_indicator);
        }
    }
}
