package hcmute.edu.vn.ticktickandroid;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
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
import hcmute.edu.vn.ticktickandroid.Dialog.CategoryDialogHelper;
import hcmute.edu.vn.ticktickandroid.Dialog.TaskDialogHelper;
import hcmute.edu.vn.ticktickandroid.Fragment.ContactFragment;
import hcmute.edu.vn.ticktickandroid.Fragment.MusicPickerFragment;
import hcmute.edu.vn.ticktickandroid.Fragment.TimerFragment;
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

    private CategoryDao categoryDao;
    private TaskDao taskDao;

    private List<Category> categories = new ArrayList<>();
    private Category currentCategory = null;

    private List<String> groupList = new ArrayList<>();
    private Map<String, List<TaskEntity>> taskMap = new LinkedHashMap<>();

    private RecyclerView rvDrawerCategories;
    private DrawerCategoryAdapter drawerAdapter;
    private TaskExpandableListAdapter taskAdapter;

    private TimerFragment timerFragment;
    private ContactFragment contactFragment;
    private MusicPickerFragment musicPickerFragment;

    private TaskReminderService taskReminderService;
    private boolean isBound = false;

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean contactsGranted = result.getOrDefault(Manifest.permission.READ_CONTACTS, false);
                Boolean smsGranted = result.getOrDefault(Manifest.permission.SEND_SMS, false);
                if (contactsGranted && smsGranted) {
                    Toast.makeText(this, "Đã cấp quyền danh bạ và SMS", Toast.LENGTH_SHORT).show();
                }
            });

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TaskReminderService.LocalBinder binder = (TaskReminderService.LocalBinder) service;
            taskReminderService = binder.getService();
            isBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) { isBound = false; }
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

        refreshTaskList();
        updateFabVisibility();

        Intent intent = new Intent(this, TaskReminderService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
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
    }

    private void bindViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);
        fabAdd = findViewById(R.id.fab_add);
        expandableListView = findViewById(R.id.expandable_task_list);
        emptyState = findViewById(R.id.empty_state);
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
        emptyState.setVisibility(taskMap.isEmpty() ? View.VISIBLE : View.GONE);
        expandableListView.setVisibility(taskMap.isEmpty() ? View.GONE : View.VISIBLE);
        toolbarTitle.setText(currentCategory != null ? currentCategory.getName() : "Công việc");
        updateFabVisibility();
        
        hideAllFragments();
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
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, musicPickerFragment).commit();
        } else {
            getSupportFragmentManager().beginTransaction().show(musicPickerFragment).commit();
        }
    }

    private void hideAllFragments() {
        Fragment[] fragments = {timerFragment, contactFragment, musicPickerFragment};
        for (Fragment f : fragments) {
            if (f != null && f.isAdded()) {
                getSupportFragmentManager().beginTransaction().hide(f).commit();
            }
        }
    }

    private void setupDrawer() {
        rvDrawerCategories = findViewById(R.id.rv_drawer_categories);
        rvDrawerCategories.setLayoutManager(new LinearLayoutManager(this));
        drawerAdapter = new DrawerCategoryAdapter(new ArrayList<>(), new LinkedHashMap<>(),
                new DrawerCategoryAdapter.OnCategoryActionListener() {
                    @Override
                    public void onClick(Category category) {
                        currentCategory = category;
                        toolbarTitle.setText(category.getName());
                        drawerLayout.closeDrawer(GravityCompat.START);
                        showTasksUi();
                        refreshTaskList();
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

    private void updateFabVisibility() {
        if (bottomNavigationView.getSelectedItemId() == R.id.nav_tasks && currentCategory != null) {
            fabAdd.show();
        } else {
            fabAdd.hide();
        }
    }

    private void refreshDrawerCategories() {
        categories = categoryDao.getAll();
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (Category c : categories) counts.put(c.getId(), taskDao.getByCategoryId(c.getId()).size());
        drawerAdapter.updateData(categories, counts);
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
            taskMap.get(catName).add(t);
        }
        taskAdapter = new TaskExpandableListAdapter(this, groupList, taskMap, new TaskExpandableListAdapter.OnTaskActionListener() {
            @Override public void onTaskCheckedChanged(TaskEntity task, boolean isChecked) {
                task.setCompleted(isChecked);
                taskDao.update(task);
            }
            @Override public void onTaskLongClick(TaskEntity task) {}
            @Override public void onSelectionModeChanged(boolean enabled) {}
        });
        expandableListView.setAdapter(taskAdapter);
        for (int i = 0; i < groupList.size(); i++) expandableListView.expandGroup(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.extra_menu, menu);
        MenuItem manageContacts = menu.findItem(R.id.action_manage_contacts);
        if (manageContacts != null) manageContacts.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_pick_music) {
            showMusicPickerUi();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
