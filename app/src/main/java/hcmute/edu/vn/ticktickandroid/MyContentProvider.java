package hcmute.edu.vn.ticktickandroid;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import androidx.sqlite.db.SupportSQLiteDatabase;
import hcmute.edu.vn.ticktickandroid.Database.AppDatabase;

public class MyContentProvider extends ContentProvider {
    public MyContentProvider() {
    }

    static final String PROVIDER_NAME = "hcmute.edu.vn.contentprovider";
    static final String URL = "content://" + PROVIDER_NAME + "/tasks";
    public static final Uri CONTENT_URI = Uri.parse(URL);
    static final int TASKS = 1;
    static final int TASK_ID = 2;
    static final int CONTACTS = 3;
    static final int CONTACT_ID = 4;
    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "tasks", TASKS);
        uriMatcher.addURI(PROVIDER_NAME, "tasks/#", TASK_ID);
        uriMatcher.addURI(PROVIDER_NAME, "contacts", CONTACTS);
        uriMatcher.addURI(PROVIDER_NAME, "contacts/#", CONTACT_ID);
    }
    private AppDatabase appDatabase;

    @Override
    public boolean onCreate() {
        if (getContext() != null) {
            appDatabase = AppDatabase.getInstance(getContext());
            return true;
        }
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor cursor;
        switch (uriMatcher.match(uri)) {
            case TASKS:
                cursor = appDatabase.taskDao().getTasksCursor();
                break;
            case TASK_ID:
                long id = android.content.ContentUris.parseId(uri);
                cursor = appDatabase.taskDao().getTaskByIdCursor(id);
                break;
            case CONTACTS:
                cursor = appDatabase.contactDao().getContactsCursor();
                break;
            case CONTACT_ID:
                long contactId = android.content.ContentUris.parseId(uri);
                cursor = appDatabase.contactDao().getContactByIdCursor(contactId);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SupportSQLiteDatabase db = appDatabase.getOpenHelper().getWritableDatabase();
        long id;
        Uri resultUri;
        switch (uriMatcher.match(uri)) {
            case TASKS:
                id = db.insert("tasks", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, values);
                if (id > 0) {
                    resultUri = android.content.ContentUris.withAppendedId(CONTENT_URI, id);
                    if (getContext() != null) getContext().getContentResolver().notifyChange(resultUri, null);
                    return resultUri;
                }
                break;
            case CONTACTS:
                id = db.insert("contacts", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, values);
                if (id > 0) {
                    resultUri = android.content.ContentUris.withAppendedId(uri, id);
                    if (getContext() != null) getContext().getContentResolver().notifyChange(resultUri, null);
                    return resultUri;
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI for insert: " + uri);
        }
        throw new android.database.SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SupportSQLiteDatabase db = appDatabase.getOpenHelper().getWritableDatabase();
        int count;
        switch (uriMatcher.match(uri)) {
            case TASKS:
                count = db.delete("tasks", selection, selectionArgs);
                break;
            case TASK_ID:
                String taskId = uri.getPathSegments().get(1);
                String taskSelection = "id = " + taskId + (android.text.TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")");
                count = db.delete("tasks", taskSelection, selectionArgs);
                break;
            case CONTACTS:
                count = db.delete("contacts", selection, selectionArgs);
                break;
            case CONTACT_ID:
                String contactId = uri.getPathSegments().get(1);
                String contactSelection = "id = " + contactId + (android.text.TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")");
                count = db.delete("contacts", contactSelection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI for delete: " + uri);
        }
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        SupportSQLiteDatabase db = appDatabase.getOpenHelper().getWritableDatabase();
        int count;
        switch (uriMatcher.match(uri)) {
            case TASKS:
                count = db.update("tasks", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, values, selection, selectionArgs);
                break;
            case TASK_ID:
                String taskId = uri.getLastPathSegment();
                String taskSelection = "id = " + taskId + (android.text.TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")");
                count = db.update("tasks", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, values, taskSelection, selectionArgs);
                break;
            case CONTACTS:
                count = db.update("contacts", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, values, selection, selectionArgs);
                break;
            case CONTACT_ID:
                String contactId = uri.getLastPathSegment();
                String contactSelection = "id = " + contactId + (android.text.TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")");
                count = db.update("contacts", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, values, contactSelection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI for update: " + uri);
        }
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case TASKS:
                return "vnd.android.cursor.dir/vnd.hcmute.edu.vn.ticktickandroid.tasks";
            case TASK_ID:
                return "vnd.android.cursor.item/vnd.hcmute.edu.vn.ticktickandroid.tasks";
            case CONTACTS:
                return "vnd.android.cursor.dir/vnd.hcmute.edu.vn.ticktickandroid.contacts";
            case CONTACT_ID:
                return "vnd.android.cursor.item/vnd.hcmute.edu.vn.ticktickandroid.contacts";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }
}