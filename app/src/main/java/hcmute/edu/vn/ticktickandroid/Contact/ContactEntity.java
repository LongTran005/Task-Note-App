package hcmute.edu.vn.ticktickandroid.Contact;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contacts")
public class ContactEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String phoneNumber;

    @androidx.room.Ignore
    private boolean isNative = false;

    public ContactEntity(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isNative() { return isNative; }
    public void setNative(boolean aNative) { isNative = aNative; }
}
