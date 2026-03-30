package hcmute.edu.vn.ticktickandroid.Reminder;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY reminderTime ASC")
    List<ReminderEntity> getAll();

    @Query("SELECT * FROM reminders WHERE reminderTime BETWEEN :start AND :end ORDER BY reminderTime ASC")
    List<ReminderEntity> getByDateRange(long start, long end);

    @Insert
    long insert(ReminderEntity reminder);

    @Update
    void update(ReminderEntity reminder);

    @Delete
    void delete(ReminderEntity reminder);

    @Query("SELECT * FROM reminders WHERE id = :id")
    ReminderEntity getById(int id);
}
