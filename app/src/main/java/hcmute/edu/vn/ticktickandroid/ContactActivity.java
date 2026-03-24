package hcmute.edu.vn.ticktickandroid;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.ticktickandroid.Adapter.ContactAdapter;
import hcmute.edu.vn.ticktickandroid.Contact.ContactEntity;
import hcmute.edu.vn.ticktickandroid.Service.SmsService;

public class ContactActivity extends AppCompatActivity {

    private RecyclerView rvContacts;
    private ContactAdapter adapter;
    private EditText etSearch;
    private Button btnSendSms;
    private FloatingActionButton fabAdd;
    private String taskMessage;

    private List<ContactEntity> allContacts = new ArrayList<>();
    private static final Uri CONTACTS_URI = Uri.parse("content://hcmute.edu.vn.contentprovider/contacts");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        Toolbar toolbar = findViewById(R.id.toolbar_contact);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        taskMessage = getIntent().getStringExtra("TASK_CONTENT");

        rvContacts = findViewById(R.id.rv_contacts);
        etSearch = findViewById(R.id.et_search_contact);
        btnSendSms = findViewById(R.id.btn_send_sms_selected);
        fabAdd = findViewById(R.id.fab_add_contact);

        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new ContactAdapter(new ContactAdapter.OnContactActionListener() {
            @Override
            public void onEdit(ContactEntity contact) {
                if (!contact.isNative()) {
                    showContactDialog(contact);
                }
            }

            @Override
            public void onDelete(ContactEntity contact) {
                if (!contact.isNative()) {
                    confirmDelete(contact);
                }
            }

            @Override
            public void onDoubleClick(ContactEntity contact) {
                if (!adapter.isSelectionMode()) {
                    showSendSmsDialog(contact);
                }
            }
        });

        rvContacts.setAdapter(adapter);

        if (taskMessage != null && !taskMessage.isEmpty()) {
            adapter.setSelectionMode(true);
            btnSendSms.setVisibility(View.VISIBLE);
        } else {
            adapter.setSelectionMode(false);
            btnSendSms.setVisibility(View.GONE);
        }

        loadContacts();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchContacts(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        fabAdd.setOnClickListener(v -> showContactDialog(null));

        btnSendSms.setOnClickListener(v -> {
            List<ContactEntity> selected = adapter.getSelectedContacts();
            if (selected.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một người", Toast.LENGTH_SHORT).show();
                return;
            }
            for (ContactEntity contact : selected) {
                sendTaskViaSms(contact.getPhoneNumber(), taskMessage);
            }
            Toast.makeText(this, "Đang gửi SMS...", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void loadContacts() {
        allContacts.clear();

        // 1. Load from Custom Content Provider (Internal DB)
        Cursor cursor = getContentResolver().query(CONTACTS_URI, null, null, null, null);
        if (cursor != null) {
            int idIndex = cursor.getColumnIndex("id");
            int nameIndex = cursor.getColumnIndex("name");
            int phoneIndex = cursor.getColumnIndex("phoneNumber");
            while (cursor.moveToNext()) {
                ContactEntity contact = new ContactEntity(
                        cursor.getString(nameIndex),
                        cursor.getString(phoneIndex)
                );
                contact.setId(cursor.getInt(idIndex));
                contact.setNative(false);
                allContacts.add(contact);
            }
            cursor.close();
        }

        // 2. Load from Native Phone Contacts (Read-Only Integration)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Cursor nativeCursor = getContentResolver().query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
            
            if (nativeCursor != null) {
                int nameIndex = nativeCursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int phoneIndex = nativeCursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                int fakeNativeId = 100000;
                while (nativeCursor.moveToNext()) {
                    if (nameIndex != -1 && phoneIndex != -1) {
                        ContactEntity contact = new ContactEntity(
                                nativeCursor.getString(nameIndex),
                                nativeCursor.getString(phoneIndex)
                        );
                        contact.setId(fakeNativeId++);
                        contact.setNative(true);
                        allContacts.add(contact);
                    }
                }
                nativeCursor.close();
            }
        }

        // Search text might already exist
        searchContacts(etSearch.getText().toString());
    }

    private void searchContacts(String query) {
        if (query.isEmpty()) {
            adapter.setContacts(allContacts);
        } else {
            query = query.toLowerCase();
            List<ContactEntity> filtered = new ArrayList<>();
            for (ContactEntity c : allContacts) {
                if (c.getName().toLowerCase().contains(query) || c.getPhoneNumber().contains(query)) {
                    filtered.add(c);
                }
            }
            adapter.setContacts(filtered);
        }
    }

    private void showContactDialog(ContactEntity contact) {
        boolean isEdit = contact != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEdit ? "Sửa danh bạ nội bộ" : "Thêm danh bạ nội bộ");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etName = new EditText(this);
        etName.setHint("Tên gốc");
        if (isEdit) etName.setText(contact.getName());
        layout.addView(etName);

        final EditText etPhone = new EditText(this);
        etPhone.setHint("Số điện thoại");
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        if (isEdit) etPhone.setText(contact.getPhoneNumber());
        layout.addView(etPhone);

        builder.setView(layout);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("phoneNumber", phone);

            if (isEdit) {
                Uri updateUri = Uri.withAppendedPath(CONTACTS_URI, String.valueOf(contact.getId()));
                getContentResolver().update(updateUri, values, null, null);
            } else {
                getContentResolver().insert(CONTACTS_URI, values);
            }
            loadContacts();
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void confirmDelete(ContactEntity contact) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa danh bạ nội bộ")
                .setMessage("Bạn có chắc muốn xóa " + contact.getName() + "?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    Uri deleteUri = Uri.withAppendedPath(CONTACTS_URI, String.valueOf(contact.getId()));
                    getContentResolver().delete(deleteUri, null, null);
                    loadContacts();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void sendTaskViaSms(String phoneNumber, String taskMessage) {
        Intent intent = new Intent(this, SmsService.class);
        intent.putExtra("PHONE_NUMBER", phoneNumber);
        intent.putExtra("MESSAGE", "Task từ TickTick: " + taskMessage);
        startService(intent);
    }

    private void showSendSmsDialog(ContactEntity contact) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Gửi SMS cho " + contact.getName());

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etMessage = new EditText(this);
        etMessage.setHint("Nhập nội dung tin nhắn...");
        layout.addView(etMessage);

        builder.setView(layout);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String message = etMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(this, "Nội dung tin nhắn trống", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, SmsService.class);
            intent.putExtra("PHONE_NUMBER", contact.getPhoneNumber());
            intent.putExtra("MESSAGE", message);
            startService(intent);
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
}
