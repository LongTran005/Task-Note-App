package hcmute.edu.vn.ticktickandroid.Fragment;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.ticktickandroid.R;
import hcmute.edu.vn.ticktickandroid.Service.SmsService;

public class ContactFragment extends Fragment {

    private RecyclerView rvContacts;
    private List<ContactModel> contactList = new ArrayList<>();
    private ContactAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact, container, false);

        rvContacts = view.findViewById(R.id.rv_contacts_fragment);
        rvContacts.setLayoutManager(new LinearLayoutManager(getContext()));

        loadContacts();

        adapter = new ContactAdapter(contactList, contact -> showChatBox(contact.name, contact.phone));
        rvContacts.setAdapter(adapter);

        return view;
    }

    private void loadContacts() {
        contactList.clear();
        ContentResolver contentResolver = getContext().getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                if (nameIndex != -1 && phoneIndex != -1) {
                    contactList.add(new ContactModel(cursor.getString(nameIndex), cursor.getString(phoneIndex)));
                }
            }
            cursor.close();
        }
    }

    private void showChatBox(String name, String phoneNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_chat_box, null);
        builder.setView(dialogView);

        TextView tvRecipient = dialogView.findViewById(R.id.tv_chat_recipient);
        EditText etMessage = dialogView.findViewById(R.id.et_chat_message);
        View btnSend = dialogView.findViewById(R.id.btn_chat_send);

        tvRecipient.setText("Gửi đến: " + name);
        etMessage.requestFocus();
        
        AlertDialog dialog = builder.create();
        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                Intent intent = new Intent(getContext(), SmsService.class);
                intent.putExtra("PHONE_NUMBER", phoneNumber);
                intent.putExtra("MESSAGE", message);
                getContext().startService(intent);
                dialog.dismiss();
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.show();
    }

    private static class ContactModel {
        String name, phone;
        ContactModel(String name, String phone) { this.name = name; this.phone = phone; }
    }

    private interface OnContactClickListener { void onContactClick(ContactModel contact); }

    private static class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {
        private final List<ContactModel> contacts;
        private final OnContactClickListener listener;

        ContactAdapter(List<ContactModel> contacts, OnContactClickListener listener) {
            this.contacts = contacts;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ContactModel contact = contacts.get(position);
            holder.tvName.setText(contact.name);
            holder.tvPhone.setText(contact.phone);
            holder.itemView.setOnClickListener(v -> listener.onContactClick(contact));
        }

        @Override
        public int getItemCount() { return contacts.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_contact_name);
                tvPhone = v.findViewById(R.id.tv_contact_phone);
            }
        }
    }
}
