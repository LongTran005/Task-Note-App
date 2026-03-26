package hcmute.edu.vn.ticktickandroid;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hcmute.edu.vn.ticktickandroid.Adapter.DrawerCategoryAdapter;
import hcmute.edu.vn.ticktickandroid.Adapter.TaskExpandableListAdapter;
import hcmute.edu.vn.ticktickandroid.Category.Category;
import hcmute.edu.vn.ticktickandroid.Category.CategoryDao;
import hcmute.edu.vn.ticktickandroid.Database.AppDatabase;
import hcmute.edu.vn.ticktickandroid.Dialog.TaskDialogHelper;
import hcmute.edu.vn.ticktickandroid.Fragment.ContactFragment;
import hcmute.edu.vn.ticktickandroid.Fragment.MusicPickerFragment;
import hcmute.edu.vn.ticktickandroid.Fragment.NotificationFragment;
import hcmute.edu.vn.ticktickandroid.Fragment.TimerFragment;
import hcmute.edu.vn.ticktickandroid.Notification.NotificationDao;
import hcmute.edu.vn.ticktickandroid.Service.MusicService;
import hcmute.edu.vn.ticktickandroid.Service.TaskReminderService;
import hcmute.edu.vn.ticktickandroid.Task.TaskDao;
import hcmute.edu.vn.ticktickandroid.Task.TaskEntity;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private TextView toolbarTitle;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAdd;
    private ExpandableListView expandableListView;
    private LinearLayout emptyState;

    private View btnNotification;
    private ImageView ivNotificationIcon;
    private TextView tvNotificationBadge;

    private CategoryDao categoryDao;
    private TaskDao taskDao;
    private NotificationDao notificationDao;

    private List<Category> categories = new ArrayList<>();
    private Category currentCategory = null;

    private List<String> groupList = new ArrayList<>();
    private Map<String, List<TaskEntity>> taskMap = new LinkedHashMap<>();

    private DrawerCategoryAdapter drawerAdapter;
    private TaskExpandableListAdapter taskAdapter;

    private TimerFragment timerFragment;
    private ContactFragment contactFragment;
    private MusicPickerFragment musicPickerFragment;
    private NotificationFragment notificationFragment;

    private int lastUiState = R.id.nav_tasks;

    private TaskReminderService taskReminderService;
    private boolean isBound = false;
    private MusicService musicService;
    private boolean isMusicBound = false;

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean contactsGranted = result.getOrDefault(Manifest.permission.READ_CONTACTS, false);
                Boolean smsGranted = result.getOrDefault(Manifest.permission.SEND_SMS, false);
                if (contactsGranted != null && contactsGranted && smsGranted != null && smsGranted) {
                    Toast.makeText(this, "Đã cấp quyền danh bạ và SMS", Toast.LENGTH_SHORT).show();
                }
            });

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TaskReminderService.LocalBinder binder = (TaskReminderService.LocalBinder) service;
            taskReminderService = binder.getService();
            isBound = true;
            taskReminderService.setNotificationListener(MainActivity.this::updateNotificationBadge);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) { isBound = false; }
    };

    private ServiceConnection musicServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isMusicBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isMusicBound = false;
            musicService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
            return windowInsets;
        });

        checkPermissions();
        initDatabase();
        bindViews();
        setupToolbar();
        setupBottomNav();
        setupDrawer();

        fabAdd.setOnClickListener(v ->
                TaskDialogHelper.showAddDialog(this, taskDao, categoryDao, currentCategory, this::refreshAll));

        btnNotification.setOnClickListener(v -> showNotificationUi());

        findViewById(R.id.fragment_container).setOnClickListener(v -> {
            if (taskAdapter != null && taskAdapter.isSelectionMode()) {
                taskAdapter.setSelectionMode(false);
            }
        });

        refreshTaskList();
        updateFabVisibility();
        updateNotificationBadge();

        Intent intent = new Intent(this, TaskReminderService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        Intent musicIntent = new Intent(this, MusicService.class);
        bindService(musicIntent, musicServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_CONTACTS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS);
        }
        if (!permissions.isEmpty()) {
            requestPermissionsLauncher.launch(permissions.toArray(new String[0]));
        }
    }

    private void initDatabase() {
        AppDatabase db = AppDatabase.getInstance(this);
        categoryDao = db.categoryDao();
        taskDao = db.taskDao();
        notificationDao = db.notificationDao();
    }

    private void bindViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);
        fabAdd = findViewById(R.id.fab_add);
        expandableListView = findViewById(R.id.expandable_task_list);
        emptyState = findViewById(R.id.empty_state);

        btnNotification = findViewById(R.id.btn_notification);
        ivNotificationIcon = findViewById(R.id.iv_notification_icon);
        tvNotificationBadge = findViewById(R.id.tv_notification_badge);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupBottomNav() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            lastUiState = itemId;
            if (itemId == R.id.nav_tasks) {
                showTasksUi();
                return true;
            } else if (itemId == R.id.nav_contacts) {
                showContactsUi();
                return true;
            } else if (itemId == R.id.nav_timer) {
                showTimerUi();
                return true;
            }
            return false;
        });
    }

    private void showTasksUi() {
        toolbarTitle.setText(currentCategory != null ? currentCategory.getName() : "Công việc");
        hideAllFragments();
        refreshTaskList(); 
        updateFabVisibility();
    }

    private void showContactsUi() {
        emptyState.setVisibility(View.GONE);
        expandableListView.setVisibility(View.GONE);
        fabAdd.hide();
        toolbarTitle.setText("Danh bạ");

        hideAllFragments();
        if (contactFragment == null) {
            contactFragment = new ContactFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, contactFragment).commit();
        } else {
            getSupportFragmentManager().beginTransaction().show(contactFragment).commit();
        }
    }

    private void showTimerUi() {
        emptyState.setVisibility(View.GONE);
        expandableListView.setVisibility(View.GONE);
        fabAdd.hide();
        toolbarTitle.setText("Tập trung");

        hideAllFragments();
        if (timerFragment == null) {
            timerFragment = new TimerFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, timerFragment).commit();
        } else {
            getSupportFragmentManager().beginTransaction().show(timerFragment).commit();
        }
    }

    private void showMusicPickerUi() {
        emptyState.setVisibility(View.GONE);
        expandableListView.setVisibility(View.GONE);
        fabAdd.hide();
        toolbarTitle.setText("Chọn nhạc nền");

        hideAllFragments();
        if (musicPickerFragment == null) {
            musicPickerFragment = new MusicPickerFragment();
            musicPickerFragment.setListener(new MusicPickerFragment.OnMusicSelectedListener() {
                @Override
                public void onMusicSelected(int resId, String name) {
                    checkOverlayPermission();
                    Intent intent = new Intent(MainActivity.this, hcmute.edu.vn.ticktickandroid.Service.MusicService.class);
                    intent.setAction(hcmute.edu.vn.ticktickandroid.Service.MusicService.ACTION_PLAY);
                    intent.putExtra(hcmute.edu.vn.ticktickandroid.Service.MusicService.EXTRA_RES_ID, resId);
                    intent.putExtra(hcmute.edu.vn.ticktickandroid.Service.MusicService.EXTRA_MUSIC_NAME, name);
                    startMusicService(intent);
                }

                @Override
                public void onFileSelected(Uri uri, String name) {
                    Intent intent = new Intent(MainActivity.this, hcmute.edu.vn.ticktickandroid.Service.MusicService.class);
                    intent.setAction(hcmute.edu.vn.ticktickandroid.Service.MusicService.ACTION_PLAY);
                    intent.putExtra(hcmute.edu.vn.ticktickandroid.Service.MusicService.EXTRA_URI, uri.toString());
                    intent.putExtra(hcmute.edu.vn.ticktickandroid.Service.MusicService.EXTRA_MUSIC_NAME, name);
                    startMusicService(intent);
                }

                @Override
                public void onPauseMusic() {
                    Intent intent = new Intent(MainActivity.this, hcmute.edu.vn.ticktickandroid.Service.MusicService.class);
                    intent.setAction(hcmute.edu.vn.ticktickandroid.Service.MusicService.ACTION_PAUSE);
                    startMusicService(intent);
                }

                @Override
                public void onResumeMusic() {
                    Intent intent = new Intent(MainActivity.this, hcmute.edu.vn.ticktickandroid.Service.MusicService.class);
                    intent.setAction(hcmute.edu.vn.ticktickandroid.Service.MusicService.ACTION_RESUME);
                    startMusicService(intent);
                }

                @Override
                public void onStopMusic() {
                    Intent intent = new Intent(MainActivity.this, hcmute.edu.vn.ticktickandroid.Service.MusicService.class);
                    intent.setAction(hcmute.edu.vn.ticktickandroid.Service.MusicService.ACTION_STOP);
                    startMusicService(intent);
                }

                @Override
                public void onBack() {
                    if (lastUiState == R.id.nav_tasks) showTasksUi();
                    else if (lastUiState == R.id.nav_contacts) showContactsUi();
                    else if (lastUiState == R.id.nav_timer) showTimerUi();
                    else showTasksUi();
                }
            });
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, musicPickerFragment).commit();
        } else {
            getSupportFragmentManager().beginTransaction().show(musicPickerFragment).commit();
        }
    }

    private void showNotificationUi() {
        emptyState.setVisibility(View.GONE);
        expandableListView.setVisibility(View.GONE);
        fabAdd.hide();
        toolbarTitle.setText("Thông báo");

        hideAllFragments();
        if (notificationFragment == null) {
            notificationFragment = new NotificationFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, notificationFragment).commit();
        } else {
            getSupportFragmentManager().beginTransaction().show(notificationFragment).commit();
        }
        updateNotificationBadge();
    }

    private void hideAllFragments() {
        Fragment[] fragments = {timerFragment, contactFragment, musicPickerFragment, notificationFragment};
        for (Fragment f : fragments) {
            if (f != null && f.isAdded()) {
                getSupportFragmentManager().beginTransaction().hide(f).commit();
            }
        }
    }

    private void setupDrawer() {
        RecyclerView rvDrawerCategories = findViewById(R.id.rv_drawer_categories);
        rvDrawerCategories.setLayoutManager(new LinearLayoutManager(this));
        drawerAdapter = new DrawerCategoryAdapter(new ArrayList<>(), new LinkedHashMap<>(),
                new DrawerCategoryAdapter.OnCategoryActionListener() {
                    @Override
                    public void onClick(Category category) {
                        currentCategory = category;
                        toolbarTitle.setText(category.getName());
                        drawerLayout.closeDrawer(GravityCompat.START);
                        showTasksUi();
                    }
                    @Override public void onEdit(Category category) {}
                    @Override public void onDelete(Category category) {}
                });
        rvDrawerCategories.setAdapter(drawerAdapter);
        refreshDrawerCategories();
    }

    private void refreshAll() {
        refreshDrawerCategories();
        refreshTaskList();
    }

    private void refreshDrawerCategories() {
        categories = categoryDao.getAll();
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (Category c : categories) counts.put(c.getId(), taskDao.getByCategoryId(c.getId()).size());
        drawerAdapter.updateData(categories, counts);
    }

    private void updateFabVisibility() {
        if (bottomNavigationView.getSelectedItemId() == R.id.nav_tasks && currentCategory != null) {
            fabAdd.show();
        } else {
            fabAdd.hide();
        }
    }

    private void refreshTaskList() {
        groupList.clear();
        taskMap.clear();
        List<TaskEntity> tasks = (currentCategory != null) ? taskDao.getByCategoryId(currentCategory.getId()) : taskDao.getAll();
        for (TaskEntity t : tasks) {
            String catName = "Công việc";
            if (!taskMap.containsKey(catName)) {
                taskMap.put(catName, new ArrayList<>());
                groupList.add(catName);
            }
            List<TaskEntity> list = taskMap.get(catName);
            if (list != null) list.add(t);
        }

        if (taskMap.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            expandableListView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            expandableListView.setVisibility(View.VISIBLE);
        }

        taskAdapter = new TaskExpandableListAdapter(this, groupList, taskMap, new TaskExpandableListAdapter.OnTaskActionListener() {
            @Override public void onTaskCheckedChanged(TaskEntity task, boolean isChecked) {
                task.setCompleted(isChecked);
                taskDao.update(task);
                refreshDrawerCategories();
                refreshTaskList();
            }
            @Override public void onTaskLongClick(TaskEntity task) {
                TaskDialogHelper.showEditDialog(MainActivity.this, task, taskDao, categoryDao, MainActivity.this::refreshAll);
            }
            @Override public void onSelectionModeChanged(boolean enabled) {
                if (enabled) {
                    toolbarTitle.setText(String.valueOf(taskAdapter.getSelectedTasks().size()) + " đã chọn");
                    fabAdd.hide();
                } else {
                    toolbarTitle.setText(currentCategory != null ? currentCategory.getName() : "Công việc");
                    updateFabVisibility();
                }
                invalidateOptionsMenu();
            }
        });
        expandableListView.setAdapter(taskAdapter);
        for (int i = 0; i < groupList.size(); i++) expandableListView.expandGroup(i);
        
        if (taskAdapter == null || !taskAdapter.isSelectionMode()) {
            updateFabVisibility();
        }
    }

    private void updateNotificationBadge() {
        new Thread(() -> {
            int unreadCount = notificationDao.getUnreadCount();
            runOnUiThread(() -> {
                if (unreadCount > 0) {
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadge.setText(String.valueOf(unreadCount));
                    ivNotificationIcon.setImageResource(R.drawable.ic_notification_on);
                } else {
                    tvNotificationBadge.setVisibility(View.GONE);
                    ivNotificationIcon.setImageResource(R.drawable.ic_notification_off);
                }
            });
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.extra_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isSelection = taskAdapter != null && taskAdapter.isSelectionMode();
        MenuItem deleteItem = menu.findItem(R.id.action_delete_selected);
        MenuItem shareItem = menu.findItem(R.id.action_share_selected);
        MenuItem musicItem = menu.findItem(R.id.action_pick_music);
        
        if (deleteItem != null) deleteItem.setVisible(isSelection);
        if (shareItem != null) shareItem.setVisible(isSelection);
        if (musicItem != null) musicItem.setVisible(!isSelection);
        
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_pick_music) {
            showMusicPickerUi();
            return true;
        } else if (id == R.id.action_delete_selected) {
            deleteSelectedTasks();
            return true;
        } else if (id == R.id.action_share_selected) {
            shareSelectedTasks();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteSelectedTasks() {
        List<TaskEntity> selected = taskAdapter.getSelectedTasks();
        if (selected.isEmpty()) return;
        
        new Thread(() -> {
            for (TaskEntity task : selected) {
                taskDao.delete(task);
            }
            runOnUiThread(() -> {
                taskAdapter.setSelectionMode(false);
                refreshAll();
                Toast.makeText(this, "Đã xóa " + selected.size() + " nhiệm vụ", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void shareSelectedTasks() {
        List<TaskEntity> selected = taskAdapter.getSelectedTasks();
        if (selected.isEmpty()) return;
        
        StringBuilder sb = new StringBuilder("Nhiệm vụ của tôi:\n");
        for (TaskEntity t : selected) {
            sb.append("- ").append(t.getTitle()).append(t.isCompleted() ? " (X)" : "").append("\n");
        }
        
        Intent intent = new Intent(this, ContactActivity.class);
        intent.putExtra("TASK_CONTENT", sb.toString());
        startActivity(intent);
        taskAdapter.setSelectionMode(false);
    }

    private void startMusicService(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "Vui lòng cấp quyền hiển thị trên ứng dụng khác để hiện miniplayer", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (isMusicBound && musicService != null && musicService.getCurrentState() != MusicService.STATE_IDLE) {
            musicService.showFloatingPlayer();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isMusicBound && musicService != null) {
            musicService.hideFloatingPlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        if (isMusicBound) {
            unbindService(musicServiceConnection);
            isMusicBound = false;
        }
    }
}
