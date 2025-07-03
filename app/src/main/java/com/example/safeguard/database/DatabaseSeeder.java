package com.example.safeguard.database;

import android.content.Context;
import android.util.Log;

/**
 * Class để seed dữ liệu ban đầu vào database
 * Sử dụng để thêm dữ liệu mẫu khi ứng dụng được khởi chạy lần đầu
 */
public class DatabaseSeeder {
    
    private static final String TAG = "DatabaseSeeder";
    
    /**
     * Seed dữ liệu ban đầu vào database
     * @param context Context của ứng dụng
     */
    public static void seedInitialData(Context context) {
        Log.d(TAG, "Starting database seeding...");
        
        // Thêm dữ liệu staff
        boolean staffAdded = addStaffData(context);
        
        if (staffAdded) {
            Log.d(TAG, "Database seeding completed successfully");
        } else {
            Log.e(TAG, "Database seeding failed");
        }
    }
    
    /**
     * Thêm dữ liệu staff vào database
     * @param context Context của ứng dụng
     * @return true nếu thành công, false nếu thất bại
     */
    private static boolean addStaffData(Context context) {
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
     * Kiểm tra xem có cần seed dữ liệu không
     * @param context Context của ứng dụng
     * @return true nếu cần seed, false nếu không cần
     */
    public static boolean needsSeeding(Context context) {
        UserDatabaseHelper dbHelper = new UserDatabaseHelper(context);
        
        try {
            // Kiểm tra xem đã có staff data chưa
            boolean hasStaff = dbHelper.userExists("tuonghuy7104@gmail.com");
            Log.d(TAG, "Database needs seeding: " + !hasStaff);
            return !hasStaff;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if seeding is needed: " + e.getMessage());
            return true; // Nếu có lỗi, coi như cần seed
        } finally {
            dbHelper.close();
        }
    }
} 