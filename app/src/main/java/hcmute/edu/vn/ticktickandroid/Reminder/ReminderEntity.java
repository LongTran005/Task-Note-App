package hcmute.edu.vn.ticktickandroid.Reminder;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminders")
public class ReminderEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private long reminderTime;
    private long createdAt;

    public ReminderEntity(String name, long reminderTime) {
        this.name = name;
        this.reminderTime = reminderTime;
        this.createdAt = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getReminderTime() { return reminderTime; }
    public void setReminderTime(long reminderTime) { this.reminderTime = reminderTime; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
