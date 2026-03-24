package hcmute.edu.vn.ticktickandroid.Contact;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ContactDao {
    @Insert
    void insert(ContactEntity contact);

    @Update
    void update(ContactEntity contact);

    @Delete
    void delete(ContactEntity contact);

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    List<ContactEntity> getAll();

    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :searchQuery || '%' OR phoneNumber LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    List<ContactEntity> search(String searchQuery);

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    android.database.Cursor getContactsCursor();

    @Query("SELECT * FROM contacts WHERE id = :id")
    android.database.Cursor getContactByIdCursor(long id);
}
