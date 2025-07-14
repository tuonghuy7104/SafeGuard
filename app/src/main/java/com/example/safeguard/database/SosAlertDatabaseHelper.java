package com.example.safeguard.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class SosAlertDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "SosAlertDatabaseHelper";
    private static final String DATABASE_NAME = "SosAlertDatabase";
    private static final int DATABASE_VERSION = 1;

    // Table and columns
    private static final String TABLE_SOS_ALERTS = "sos_alerts";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_FIREBASE_ID = "firebase_id"; // Added for Firebase sync
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_USER_NAME = "user_name";
    private static final String COLUMN_USER_EMAIL = "user_email";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";
    private static final String COLUMN_EMERGENCY_CONTACTS = "emergency_contacts";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_STATUS = "status"; // "active", "resolved"
    private static final String COLUMN_IS_SYNCED = "is_synced"; // Added for tracking sync status

    // Create table SQL query
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_SOS_ALERTS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_FIREBASE_ID + " TEXT, " +
                    COLUMN_USER_ID + " INTEGER, " +
                    COLUMN_USER_NAME + " TEXT, " +
                    COLUMN_USER_EMAIL + " TEXT, " +
                    COLUMN_LATITUDE + " REAL, " +
                    COLUMN_LONGITUDE + " REAL, " +
                    COLUMN_EMERGENCY_CONTACTS + " TEXT, " +
                    COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    COLUMN_STATUS + " TEXT DEFAULT 'active', " +
                    COLUMN_IS_SYNCED + " INTEGER DEFAULT 0)";

    public SosAlertDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        Log.d(TAG, "SOS Alerts table created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Trong trường hợp cần cập nhật schema, ta sẽ thêm các cột mới thay vì xóa hết
        if (oldVersion < 2) {
            // Thêm cột FIREBASE_ID nếu chưa có
            try {
                db.execSQL("ALTER TABLE " + TABLE_SOS_ALERTS + 
                          " ADD COLUMN " + COLUMN_FIREBASE_ID + " TEXT");
            } catch (Exception e) {
                Log.e(TAG, "Error adding column FIREBASE_ID: " + e.getMessage());
            }
            
            // Thêm cột IS_SYNCED nếu chưa có
            try {
                db.execSQL("ALTER TABLE " + TABLE_SOS_ALERTS + 
                          " ADD COLUMN " + COLUMN_IS_SYNCED + " INTEGER DEFAULT 0");
            } catch (Exception e) {
                Log.e(TAG, "Error adding column IS_SYNCED: " + e.getMessage());
            }
        }
    }

    /**
     * Thêm một SOS alert mới
     */
    public long addSosAlert(int userId, String userName, String userEmail, 
                           double latitude, double longitude, String emergencyContacts) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_USER_NAME, userName);
        values.put(COLUMN_USER_EMAIL, userEmail);
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_LONGITUDE, longitude);
        values.put(COLUMN_EMERGENCY_CONTACTS, emergencyContacts);
        values.put(COLUMN_STATUS, "active");
        values.put(COLUMN_IS_SYNCED, 0); // Chưa đồng bộ

        long result = db.insert(TABLE_SOS_ALERTS, null, values);
        db.close();
        
        if (result != -1) {
            Log.d(TAG, "SOS Alert added successfully for user: " + userName);
        } else {
            Log.e(TAG, "Failed to add SOS Alert for user: " + userName);
        }
        
        return result;
    }

    /**
     * Thêm một SOS alert từ Firebase
     */
    public long addSosAlertFromFirebase(String firebaseId, int userId, String userName, String userEmail, 
                           double latitude, double longitude, String emergencyContacts,
                           String timestamp, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_FIREBASE_ID, firebaseId);
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_USER_NAME, userName);
        values.put(COLUMN_USER_EMAIL, userEmail);
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_LONGITUDE, longitude);
        values.put(COLUMN_EMERGENCY_CONTACTS, emergencyContacts);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_STATUS, status);
        values.put(COLUMN_IS_SYNCED, 1); // Đã đồng bộ

        long result = db.insert(TABLE_SOS_ALERTS, null, values);
        db.close();
        
        if (result != -1) {
            Log.d(TAG, "SOS Alert from Firebase added successfully for user: " + userName);
        } else {
            Log.e(TAG, "Failed to add SOS Alert from Firebase for user: " + userName);
        }
        
        return result;
    }

    /**
     * Cập nhật Firebase ID cho một alert
     */
    public boolean updateFirebaseId(long localId, String firebaseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FIREBASE_ID, firebaseId);
        values.put(COLUMN_IS_SYNCED, 1); // Đã đồng bộ
        
        int result = db.update(TABLE_SOS_ALERTS, values, 
                              COLUMN_ID + " = ?", 
                              new String[]{String.valueOf(localId)});
        
        db.close();
        
        if (result > 0) {
            Log.d(TAG, "Firebase ID updated for SOS Alert " + localId);
            return true;
        } else {
            Log.e(TAG, "Failed to update Firebase ID for SOS Alert " + localId);
            return false;
        }
    }

    /**
     * Lấy tất cả SOS alerts đang active
     */
    public List<SosAlert> getActiveSosAlerts() {
        List<SosAlert> alerts = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query = "SELECT * FROM " + TABLE_SOS_ALERTS + 
                      " WHERE " + COLUMN_STATUS + " = 'active' " +
                      " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                SosAlert alert = new SosAlert();
                alert.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                
                // Thêm Firebase ID nếu có
                int firebaseIdIndex = cursor.getColumnIndex(COLUMN_FIREBASE_ID);
                if (firebaseIdIndex != -1) {
                    alert.setFirebaseId(cursor.getString(firebaseIdIndex));
                }
                
                alert.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
                alert.setUserName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME)));
                alert.setUserEmail(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_EMAIL)));
                alert.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)));
                alert.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)));
                alert.setEmergencyContacts(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACTS)));
                alert.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
                alert.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)));
                
                // Thêm trạng thái đồng bộ nếu có
                int isSyncedIndex = cursor.getColumnIndex(COLUMN_IS_SYNCED);
                if (isSyncedIndex != -1) {
                    alert.setSynced(cursor.getInt(isSyncedIndex) == 1);
                }
                
                alerts.add(alert);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        Log.d(TAG, "Retrieved " + alerts.size() + " active SOS alerts");
        return alerts;
    }

    /**
     * Lấy các alerts chưa đồng bộ
     */
    public List<SosAlert> getUnsyncedAlerts() {
        List<SosAlert> alerts = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query = "SELECT * FROM " + TABLE_SOS_ALERTS + 
                      " WHERE " + COLUMN_IS_SYNCED + " = 0";
        
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                SosAlert alert = new SosAlert();
                alert.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                alert.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
                alert.setUserName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME)));
                alert.setUserEmail(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_EMAIL)));
                alert.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)));
                alert.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)));
                alert.setEmergencyContacts(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACTS)));
                alert.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
                alert.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)));
                alert.setSynced(false);
                
                alerts.add(alert);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        return alerts;
    }

    /**
     * Kiểm tra xem một alert từ Firebase đã tồn tại trong local database chưa
     */
    public boolean checkIfFirebaseAlertExists(String firebaseId) {
        if (firebaseId == null || firebaseId.isEmpty()) {
            return false;
        }
        
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_SOS_ALERTS + 
                      " WHERE " + COLUMN_FIREBASE_ID + " = ?";
        
        Cursor cursor = db.rawQuery(query, new String[]{firebaseId});
        int count = 0;
        
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        db.close();
        
        return count > 0;
    }

    /**
     * Đánh dấu SOS alert đã được giải quyết
     */
    public boolean resolveSosAlert(int alertId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, "resolved");
        
        int result = db.update(TABLE_SOS_ALERTS, values, 
                              COLUMN_ID + " = ?", 
                              new String[]{String.valueOf(alertId)});
        
        db.close();
        
        if (result > 0) {
            Log.d(TAG, "SOS Alert " + alertId + " marked as resolved");
            return true;
        } else {
            Log.e(TAG, "Failed to resolve SOS Alert " + alertId);
            return false;
        }
    }

    /**
     * Xóa SOS alert
     */
    public boolean deleteSosAlert(int alertId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_SOS_ALERTS, 
                              COLUMN_ID + " = ?", 
                              new String[]{String.valueOf(alertId)});
        
        db.close();
        
        if (result > 0) {
            Log.d(TAG, "SOS Alert " + alertId + " deleted");
            return true;
        } else {
            Log.e(TAG, "Failed to delete SOS Alert " + alertId);
            return false;
        }
    }

    /**
     * Class để đại diện cho một SOS Alert
     */
    public static class SosAlert {
        private int id;
        private String firebaseId; // ID trong Firebase database
        private int userId;
        private String userName;
        private String userEmail;
        private double latitude;
        private double longitude;
        private String emergencyContacts;
        private String timestamp;
        private String status;
        private boolean isSynced; // Trạng thái đồng bộ

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getFirebaseId() { return firebaseId; }
        public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }
        
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        
        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
        
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        
        public String getEmergencyContacts() { return emergencyContacts; }
        public void setEmergencyContacts(String emergencyContacts) { this.emergencyContacts = emergencyContacts; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public boolean isSynced() { return isSynced; }
        public void setSynced(boolean synced) { isSynced = synced; }
    }
} 