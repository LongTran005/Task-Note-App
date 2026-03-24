package hcmute.edu.vn.ticktickandroid.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hcmute.edu.vn.ticktickandroid.Contact.ContactEntity;
import hcmute.edu.vn.ticktickandroid.R;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private List<ContactEntity> contactList = new ArrayList<>();
    private Set<Integer> selectedContactIds = new HashSet<>();
    private OnContactActionListener listener;
    private boolean isSelectionMode = false;

    public interface OnContactActionListener {
        void onEdit(ContactEntity contact);
        void onDelete(ContactEntity contact);
        void onDoubleClick(ContactEntity contact);
    }

    public ContactAdapter(OnContactActionListener listener) {
        this.listener = listener;
    }

    public void setContacts(List<ContactEntity> contacts) {
        this.contactList = contacts;
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean isSelectionMode) {
        this.isSelectionMode = isSelectionMode;
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public List<ContactEntity> getSelectedContacts() {
        List<ContactEntity> selected = new ArrayList<>();
        for (ContactEntity c : contactList) {
            if (selectedContactIds.contains(c.getId())) {
                selected.add(c);
            }
        }
        return selected;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ContactEntity contact = contactList.get(position);
        holder.tvName.setText(contact.getName());
        holder.tvPhone.setText(contact.getPhoneNumber());

        holder.cbSelect.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        holder.cbSelect.setOnCheckedChangeListener(null); // Clear previous listener
        holder.cbSelect.setChecked(selectedContactIds.contains(contact.getId()));

        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedContactIds.add(contact.getId());
            } else {
                selectedContactIds.remove(contact.getId());
            }
        });

        // Toggle selection on item click if in selection mode, with double click detection
        final long[] lastClickTime = {0};
        holder.itemView.setOnClickListener(v -> {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime[0] < 300) {
                // Double click
                if (listener != null) listener.onDoubleClick(contact);
                lastClickTime[0] = 0; // reset
            } else {
                // Single click
                if (isSelectionMode) {
                    holder.cbSelect.setChecked(!holder.cbSelect.isChecked());
                }
                lastClickTime[0] = clickTime;
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(contact);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(contact);
        });

        if (contact.isNative()) {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tvName, tvPhone;
        ImageButton btnEdit, btnDelete;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_select_contact);
            tvName = itemView.findViewById(R.id.tv_contact_name);
            tvPhone = itemView.findViewById(R.id.tv_contact_phone);
            btnEdit = itemView.findViewById(R.id.btn_edit_contact);
            btnDelete = itemView.findViewById(R.id.btn_delete_contact);
        }
    }
}
