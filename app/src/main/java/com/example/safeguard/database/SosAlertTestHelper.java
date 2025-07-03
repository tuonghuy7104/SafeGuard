package com.example.safeguard.database;

import android.content.Context;
import android.util.Log;

/**
 * Class utility để test việc thêm SOS alerts mẫu
 * Sử dụng để tạo dữ liệu test cho StaffHome
 */
public class SosAlertTestHelper {
    
    private static final String TAG = "SosAlertTestHelper";
    
    /**
     * Thêm SOS alerts mẫu để test
     * @param context Context của ứng dụng
     */
    public static void addSampleSosAlerts(Context context) {
        SosAlertDatabaseHelper dbHelper = new SosAlertDatabaseHelper(context);
        
        try {
            Log.d(TAG, "Sample SOS alerts function called - no sample data added");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in addSampleSosAlerts: " + e.getMessage());
        } finally {
            dbHelper.close();
        }
    }
    
    /**
     * Xóa tất cả SOS alerts (dùng để reset)
     * @param context Context của ứng dụng
     */
    public static void clearAllSosAlerts(Context context) {
        SosAlertDatabaseHelper dbHelper = new SosAlertDatabaseHelper(context);
        
        try {
            // Lấy tất cả alerts
            java.util.List<SosAlertDatabaseHelper.SosAlert> alerts = dbHelper.getActiveSosAlerts();
            
            // Xóa từng alert
            for (SosAlertDatabaseHelper.SosAlert alert : alerts) {
                dbHelper.deleteSosAlert(alert.getId());
            }
            
            Log.d(TAG, "Cleared " + alerts.size() + " SOS alerts");
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing SOS alerts: " + e.getMessage());
        } finally {
            dbHelper.close();
        }
    }
    
    /**
     * Kiểm tra xem có SOS alerts nào không
     * @param context Context của ứng dụng
     * @return true nếu có alerts, false nếu không có
     */
    public static boolean hasSosAlerts(Context context) {
        SosAlertDatabaseHelper dbHelper = new SosAlertDatabaseHelper(context);
        
        try {
            java.util.List<SosAlertDatabaseHelper.SosAlert> alerts = dbHelper.getActiveSosAlerts();
            boolean hasAlerts = !alerts.isEmpty();
            Log.d(TAG, "Has SOS alerts: " + hasAlerts + " (count: " + alerts.size() + ")");
            return hasAlerts;
        } catch (Exception e) {
            Log.e(TAG, "Error checking SOS alerts: " + e.getMessage());
            return false;
        } finally {
            dbHelper.close();
        }
    }
} 