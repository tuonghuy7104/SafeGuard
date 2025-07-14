package com.example.safeguard.database;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static final String SOS_ALERTS_PATH = "sos_alerts";

    private final DatabaseReference sosAlertsRef;
    private final SosAlertDatabaseHelper localDbHelper;

    public FirebaseManager(Context context) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        sosAlertsRef = database.getReference(SOS_ALERTS_PATH);
        localDbHelper = new SosAlertDatabaseHelper(context);
    }

    /**
     * Interface để thông báo khi có alert mới
     */
    public interface AlertsListener {
        void onAlertsUpdated(List<SosAlertDatabaseHelper.SosAlert> alerts);
    }

    /**
     * Thêm listener theo dõi thay đổi alerts
     */
    public void listenForAlerts(final AlertsListener listener) {
        sosAlertsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<SosAlertDatabaseHelper.SosAlert> alerts = new ArrayList<>();
                
                for (DataSnapshot alertSnapshot : dataSnapshot.getChildren()) {
                    try {
                        SosAlertDatabaseHelper.SosAlert alert = alertSnapshot.getValue(SosAlertDatabaseHelper.SosAlert.class);
                        if (alert != null && "active".equals(alert.getStatus())) {
                            alerts.add(alert);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing alert: " + e.getMessage());
                    }
                }
                
                // Cập nhật local database với alerts từ Firebase
                syncLocalDatabase(alerts);
                
                // Gọi callback
                if (listener != null) {
                    listener.onAlertsUpdated(alerts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Firebase Database error: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Thêm một alert mới vào Firebase
     */
    public void addSosAlert(SosAlertDatabaseHelper.SosAlert alert) {
        // Tạo một key mới cho alert
        String alertKey = sosAlertsRef.push().getKey();
        if (alertKey == null) {
            Log.e(TAG, "Failed to create new Firebase key for SOS alert");
            return;
        }

        // Tạo map từ alert để lưu vào Firebase
        Map<String, Object> alertValues = alertToMap(alert);
        alertValues.put("firebaseId", alertKey); // Thêm firebase ID

        // Cập nhật vào Firebase
        sosAlertsRef.child(alertKey).setValue(alertValues)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "SOS Alert added to Firebase successfully"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to add SOS Alert to Firebase: " + e.getMessage()));
    }

    /**
     * Cập nhật trạng thái của alert
     */
    public void updateAlertStatus(String firebaseId, String status) {
        if (firebaseId != null && !firebaseId.isEmpty()) {
            sosAlertsRef.child(firebaseId).child("status").setValue(status)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "SOS Alert status updated in Firebase"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update SOS Alert status in Firebase: " + e.getMessage()));
        }
    }

    /**
     * Đồng bộ database local với dữ liệu từ Firebase
     */
    private void syncLocalDatabase(List<SosAlertDatabaseHelper.SosAlert> firebaseAlerts) {
        // Thực hiện trong một luồng riêng để không block UI
        new Thread(() -> {
            try {
                for (SosAlertDatabaseHelper.SosAlert alert : firebaseAlerts) {
                    // Kiểm tra xem alert đã có trong local database chưa
                    // Nếu alert này chưa có trong local database, thêm vào
                    // Logic đồng bộ chi tiết sẽ phụ thuộc vào yêu cầu cụ thể
                    // (Đây chỉ là ví dụ đơn giản)
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing with local database: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Chuyển đổi SosAlert object thành Map
     */
    private Map<String, Object> alertToMap(SosAlertDatabaseHelper.SosAlert alert) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", alert.getUserId());
        result.put("userName", alert.getUserName());
        result.put("userEmail", alert.getUserEmail());
        result.put("latitude", alert.getLatitude());
        result.put("longitude", alert.getLongitude());
        result.put("emergencyContacts", alert.getEmergencyContacts());
        result.put("timestamp", alert.getTimestamp());
        result.put("status", alert.getStatus());
        return result;
    }
} 