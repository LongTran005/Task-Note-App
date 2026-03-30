package hcmute.edu.vn.ticktickandroid.Fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.ticktickandroid.Adapter.CalendarDayAdapter;
import hcmute.edu.vn.ticktickandroid.Adapter.CalendarEventAdapter;
import hcmute.edu.vn.ticktickandroid.Category.CategoryDao;
import hcmute.edu.vn.ticktickandroid.Database.AppDatabase;
import hcmute.edu.vn.ticktickandroid.Dialog.TaskDialogHelper;
import hcmute.edu.vn.ticktickandroid.R;
import hcmute.edu.vn.ticktickandroid.Receiver.AlarmScheduler;
import hcmute.edu.vn.ticktickandroid.Reminder.ReminderDao;
import hcmute.edu.vn.ticktickandroid.Reminder.ReminderEntity;
import hcmute.edu.vn.ticktickandroid.Task.TaskDao;
import hcmute.edu.vn.ticktickandroid.Task.TaskEntity;

public class CalendarFragment extends Fragment {

    private TextView tvMonthYear;
    private ImageButton btnPrevMonth, btnNextMonth;
    private RecyclerView rvCalendarGrid, rvCalendarEvents;
    private TextView tvSelectedDateHeader;
    private View fabAddReminder;

    private Calendar currentDisplayDate;
    private Calendar selectedDate;

    private CalendarDayAdapter dayAdapter;
    private CalendarEventAdapter eventAdapter;

    private List<Calendar> currentDaysInDisplay = new ArrayList<>();
    private List<Calendar> eventDays = new ArrayList<>();
    private List<CalendarEventAdapter.CalendarEvent> dailyEvents = new ArrayList<>();

    private TaskDao taskDao;
    private ReminderDao reminderDao;
    private CategoryDao categoryDao;

    public CalendarFragment() {
        // Required empty constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        AppDatabase db = AppDatabase.getInstance(context);
        taskDao = db.taskDao();
        reminderDao = db.reminderDao();
        categoryDao = db.categoryDao();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        
        tvMonthYear = view.findViewById(R.id.tv_month_year);
        btnPrevMonth = view.findViewById(R.id.btn_prev_month);
        btnNextMonth = view.findViewById(R.id.btn_next_month);
        rvCalendarGrid = view.findViewById(R.id.rv_calendar_grid);
        rvCalendarEvents = view.findViewById(R.id.rv_calendar_events);
        tvSelectedDateHeader = view.findViewById(R.id.tv_selected_date_header);
        fabAddReminder = view.findViewById(R.id.fab_add_reminder);

        currentDisplayDate = Calendar.getInstance();
        currentDisplayDate.set(Calendar.DAY_OF_MONTH, 1);
        selectedDate = Calendar.getInstance();

        rvCalendarGrid.setLayoutManager(new GridLayoutManager(getContext(), 7));
        rvCalendarEvents.setLayoutManager(new LinearLayoutManager(getContext()));

        btnPrevMonth.setOnClickListener(v -> {
            currentDisplayDate.add(Calendar.MONTH, -1);
            setupCalendarGrid();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentDisplayDate.add(Calendar.MONTH, 1);
            setupCalendarGrid();
        });

        fabAddReminder.setOnClickListener(v -> showAddReminderDialog());

        setupCalendarGrid();
        return view;
    }

    private void setupCalendarGrid() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN"));
        tvMonthYear.setText(sdf.format(currentDisplayDate.getTime()));

        currentDaysInDisplay.clear();
        Calendar monthCal = (Calendar) currentDisplayDate.clone();
        monthCal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK);
        int prevMonthDays = (firstDayOfWeek == Calendar.SUNDAY) ? 6 : firstDayOfWeek - 2;

        monthCal.add(Calendar.DAY_OF_MONTH, -prevMonthDays);
        int maxDays = 42; // 6 rows * 7 days
        for (int i = 0; i < maxDays; i++) {
            currentDaysInDisplay.add((Calendar) monthCal.clone());
            monthCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        loadEventDays();

        dayAdapter = new CalendarDayAdapter(currentDaysInDisplay, selectedDate, eventDays, date -> {
            selectedDate = (Calendar) date.clone();
            if (selectedDate.get(Calendar.MONTH) != currentDisplayDate.get(Calendar.MONTH)) {
                currentDisplayDate = (Calendar) selectedDate.clone();
                currentDisplayDate.set(Calendar.DAY_OF_MONTH, 1);
                setupCalendarGrid();
            } else {
                dayAdapter.notifyDataSetChanged();
                loadEventsForSelectedDate();
            }
        });
        rvCalendarGrid.setAdapter(dayAdapter);

        loadEventsForSelectedDate();
    }

    private void loadEventDays() {
        eventDays.clear();
        
        Calendar startOfMonth = (Calendar) currentDaysInDisplay.get(0).clone();
        Calendar endOfMonth = (Calendar) currentDaysInDisplay.get(currentDaysInDisplay.size() - 1).clone();
        endOfMonth.set(Calendar.HOUR_OF_DAY, 23);
        endOfMonth.set(Calendar.MINUTE, 59);

        // Load tasks with dueDate
        List<TaskEntity> tasks = taskDao.getByDateRange(startOfMonth.getTimeInMillis(), endOfMonth.getTimeInMillis());
        for (TaskEntity task : tasks) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(task.getDueDate());
            eventDays.add(cal);
        }

        // Load reminders
        List<ReminderEntity> reminders = reminderDao.getByDateRange(startOfMonth.getTimeInMillis(), endOfMonth.getTimeInMillis());
        for (ReminderEntity r : reminders) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(r.getReminderTime());
            eventDays.add(cal);
        }
    }

    private void loadEventsForSelectedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
        tvSelectedDateHeader.setText(sdf.format(selectedDate.getTime()));

        dailyEvents.clear();

        Calendar startOfDay = (Calendar) selectedDate.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) startOfDay.clone();
        endOfDay.add(Calendar.DAY_OF_MONTH, 1);
        endOfDay.add(Calendar.MILLISECOND, -1);

        List<TaskEntity> tasks = taskDao.getByDateRange(startOfDay.getTimeInMillis(), endOfDay.getTimeInMillis());
        for (TaskEntity task : tasks) {
            dailyEvents.add(new CalendarEventAdapter.CalendarEvent(
                    task.getId(), task.getTitle(), task.getDueDate(),
                    CalendarEventAdapter.TYPE_TASK, task));
        }

        List<ReminderEntity> reminders = reminderDao.getByDateRange(startOfDay.getTimeInMillis(), endOfDay.getTimeInMillis());
        for (ReminderEntity r : reminders) {
            dailyEvents.add(new CalendarEventAdapter.CalendarEvent(
                    r.getId(), r.getName(), r.getReminderTime(),
                    CalendarEventAdapter.TYPE_REMINDER, r));
        }

        // Sort by time
        dailyEvents.sort((e1, e2) -> Long.compare(e1.timestamp, e2.timestamp));

        eventAdapter = new CalendarEventAdapter(dailyEvents, event -> {
            if (event.type == CalendarEventAdapter.TYPE_TASK) {
                TaskEntity t = (TaskEntity) event.data;
                TaskDialogHelper.showEditDialog(getActivity(), t, taskDao, categoryDao, () -> {
                    if (t.getDueDate() > 0) {
                        AlarmScheduler.scheduleAlarm(getContext(), t.getId(), t.getTitle(), t.getDueDate());
                    } else {
                        AlarmScheduler.cancelAlarm(getContext(), t.getId());
                    }
                    setupCalendarGrid(); // Refresh
                });
            } else {
                showEditReminderDialog((ReminderEntity) event.data);
            }
        });
        rvCalendarEvents.setAdapter(eventAdapter);
    }

    private void showAddReminderDialog() {
        showReminderDialog(null);
    }

    private void showEditReminderDialog(ReminderEntity reminder) {
        showReminderDialog(reminder);
    }

    private void showReminderDialog(ReminderEntity existingReminder) {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_category, null);
        EditText etName = view.findViewById(R.id.et_category_name);
        etName.setHint("Tên lời nhắc tùy ý");

        final Calendar cal;
        if (existingReminder != null) {
            cal = Calendar.getInstance();
            etName.setText(existingReminder.getName());
            cal.setTimeInMillis(existingReminder.getReminderTime());
        } else {
            cal = (Calendar) selectedDate.clone();
            cal.set(Calendar.HOUR_OF_DAY, 12); // Default to noon
            cal.set(Calendar.MINUTE, 0);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle(existingReminder == null ? "Thêm lời nhắc ngày" : "Sửa lời nhắc ngày")
                .setView(view)
                .setPositiveButton("Lưu", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng nhập tên", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    new TimePickerDialog(getContext(), (tview, hourOfDay, minute) -> {
                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        cal.set(Calendar.MINUTE, minute);
                        cal.set(Calendar.SECOND, 0);

                        if (existingReminder == null) {
                            ReminderEntity r = new ReminderEntity(name, cal.getTimeInMillis());
                            long id = reminderDao.insert(r);
                            AlarmScheduler.scheduleAlarm(getContext(), (int) id + 100000, name, cal.getTimeInMillis());
                        } else {
                            existingReminder.setName(name);
                            existingReminder.setReminderTime(cal.getTimeInMillis());
                            reminderDao.update(existingReminder);
                            AlarmScheduler.scheduleAlarm(getContext(), existingReminder.getId() + 100000, name, cal.getTimeInMillis());
                        }
                        setupCalendarGrid();
                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
                })
                .setNegativeButton("Hủy", null);
                
        if (existingReminder != null) {
            builder.setNeutralButton("Xóa", (d, w) -> {
                reminderDao.delete(existingReminder);
                AlarmScheduler.cancelAlarm(getContext(), existingReminder.getId() + 100000);
                setupCalendarGrid();
            });
        }
        
        builder.show();
    }
}
