package hcmute.edu.vn.ticktickandroid.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(selectedContactIds.contains(contact.getId()));

        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedContactIds.add(contact.getId());
            } else {
                selectedContactIds.remove(contact.getId());
            }
        });

        final long[] lastClickTime = {0};
        holder.itemView.setOnClickListener(v -> {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime[0] < 300) {
                if (listener != null) listener.onDoubleClick(contact);
                lastClickTime[0] = 0;
            } else {
                if (isSelectionMode) {
                    holder.cbSelect.setChecked(!holder.cbSelect.isChecked());
                }
                lastClickTime[0] = clickTime;
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tvName, tvPhone;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_select_contact);
            tvName = itemView.findViewById(R.id.tv_contact_name);
            tvPhone = itemView.findViewById(R.id.tv_contact_phone);
        }
    }
}
