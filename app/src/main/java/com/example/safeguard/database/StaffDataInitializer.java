package com.example.safeguard.database;

import android.content.Context;
import android.util.Log;

public class StaffDataInitializer {
    
    private static final String TAG = "StaffDataInitializer";
    
    /**
     * Thêm dữ liệu staff vào database
     * @param context Context của ứng dụng
     * @return true nếu thêm thành công, false nếu thất bại
     */
    public static boolean addStaffData(Context context) {
        UserDatabaseHelper dbHelper = new UserDatabaseHelper(context);
        
        // Thông tin staff
        String staffFullName = "Staff Tuong Huy";
        String staffEmail = "tuonghuy7104@gmail.com";
        String staffPassword = "123456";
        
        try {
            // Kiểm tra xem email đã tồn tại chưa
            if (dbHelper.userExists(staffEmail)) {
                Log.d(TAG, "Staff with email " + staffEmail + " already exists");
                return true; // Đã tồn tại, coi như thành công
            }
            
            // Thêm staff vào database
            boolean isInserted = dbHelper.addUser(staffFullName, staffEmail, staffPassword);
            
            if (isInserted) {
                Log.d(TAG, "Staff data added successfully: " + staffEmail);
                return true;
            } else {
                Log.e(TAG, "Failed to add staff data: " + staffEmail);
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding staff data: " + e.getMessage());
            return false;
        } finally {
            dbHelper.close();
        }
    }
    
    /**
     * Kiểm tra xem staff có tồn tại trong database không
     * @param context Context của ứng dụng
     * @param email Email của staff
     * @param password Mật khẩu của staff
     * @return true nếu staff tồn tại và thông tin đăng nhập đúng
     */
    public static boolean checkStaffCredentials(Context context, String email, String password) {
        UserDatabaseHelper dbHelper = new UserDatabaseHelper(context);
        
        try {
            boolean isValid = dbHelper.checkUser(email, password);
            Log.d(TAG, "Staff credentials check for " + email + ": " + isValid);
            return isValid;
        } catch (Exception e) {
            Log.e(TAG, "Error checking staff credentials: " + e.getMessage());
            return false;
        } finally {
            dbHelper.close();
        }
    }
} 