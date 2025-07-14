package com.example.safeguard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.safeguard.database.FirebaseManager;
import com.example.safeguard.database.SosAlertDatabaseHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

public class StaffHome extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private LinearLayout notificationList;
    private Button btnLogout;
    private SosAlertDatabaseHelper sosAlertDbHelper;
    private FirebaseManager firebaseManager;
    private TextView tvNoAlerts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_home);

        // Initialize database helpers
        sosAlertDbHelper = new SosAlertDatabaseHelper(this);
        firebaseManager = new FirebaseManager(this);

        // Initialize UI Elements
        mapView = findViewById(R.id.mapView);
        notificationList = findViewById(R.id.notificationList);
        btnLogout = findViewById(R.id.btn_logout);
        tvNoAlerts = findViewById(R.id.tvNoAlerts);

        // MapView Setup
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Logout Button Click -> Redirect to Login
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(StaffHome.this, StaffLogIn.class);
            startActivity(intent);
            finish(); // Close current activity
        });

        // Set up Firebase listener to receive new alerts
        setupFirebaseListener();
        
        // Directly load alerts from Firebase (don't just wait for listener)
        queryFirebaseAlerts();
        
        // Load SOS alerts from local database as a fallback (for offline support)
        loadSosAlerts();
        
        // Add a refresh button
        Button btnRefresh = findViewById(R.id.btnRefresh);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                Toast.makeText(this, "Refreshing alerts...", Toast.LENGTH_SHORT).show();
                queryFirebaseAlerts();
            });
        }
        
        // Add test alert button
        Button btnTestAlert = findViewById(R.id.btnTestAlert);
        if (btnTestAlert != null) {
            btnTestAlert.setOnClickListener(v -> {
                createTestAlert();
            });
        }
    }

    /**
     * Set up Firebase listener to receive new alerts
     */
    private void setupFirebaseListener() {
        Log.d("StaffHome", "Setting up Firebase alerts listener");
        
        firebaseManager.listenForAlerts(alerts -> {
            // Update UI when changes come from Firebase
            runOnUiThread(() -> {
                Log.d("StaffHome", "Received " + alerts.size() + " alerts from Firebase");
                
                // Clear existing views
                notificationList.removeAllViews();
                
                if (alerts.isEmpty()) {
                    // Show no alerts message
                    if (tvNoAlerts != null) {
                        tvNoAlerts.setVisibility(View.VISIBLE);
                    }
                    return;
                }
                
                // Hide no alerts message
                if (tvNoAlerts != null) {
                    tvNoAlerts.setVisibility(View.GONE);
                }

                // Create view for each alert
                for (SosAlertDatabaseHelper.SosAlert alert : alerts) {
                    View alertView = createSosAlertView(alert);
                    notificationList.addView(alertView);
                    Log.d("StaffHome", "Added alert to UI: " + alert.getUserName());
                }
                
                // Show all points on the map
                if (googleMap != null) {
                    showAlertsOnMap(alerts);
                }
            });
        });
    }

    /**
     * Directly query Firebase for alerts instead of waiting for listener
     */
    private void queryFirebaseAlerts() {
        Log.d("StaffHome", "Directly querying Firebase for alerts");
        
        // Show a loading message
        if (tvNoAlerts != null) {
            tvNoAlerts.setText("Loading alerts from server...");
            tvNoAlerts.setVisibility(View.VISIBLE);
        }
        
        // Get the Firebase database reference
        FirebaseDatabase database;
        try {
            String DATABASE_URL = "https://safeguard-36ba7-default-rtdb.asia-southeast1.firebasedatabase.app/";
            database = FirebaseDatabase.getInstance(DATABASE_URL);
            Log.d("StaffHome", "Using explicit database URL: " + DATABASE_URL);
        } catch (Exception e) {
            database = FirebaseDatabase.getInstance();
            Log.e("StaffHome", "Error using explicit URL, falling back to default: " + e.getMessage());
        }
        
        Log.d("StaffHome", "Firebase database instance created: " + database);
        DatabaseReference alertsRef = database.getReference("sos_alerts");
        Log.d("StaffHome", "Reference path: " + alertsRef.toString());
        
        // First test if we can read data at all
        DatabaseReference testRef = database.getReference(".info/connected");
        testRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                Log.d("StaffHome", "Firebase connection test result: " + connected);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StaffHome", "Firebase connection test failed: " + error.getMessage());
            }
        });
        
        // Create a test alert to verify write permissions
        try {
            DatabaseReference testAlertRef = database.getReference("test_alert");
            Map<String, Object> testAlert = new HashMap<>();
            testAlert.put("test", "value");
            testAlert.put("timestamp", System.currentTimeMillis());
            
            testAlertRef.setValue(testAlert)
                .addOnSuccessListener(aVoid -> {
                    Log.d("StaffHome", "Test alert written successfully");
                    testAlertRef.removeValue(); // Clean up test data
                })
                .addOnFailureListener(e -> {
                    Log.e("StaffHome", "Test alert write failed: " + e.getMessage());
                });
        } catch (Exception e) {
            Log.e("StaffHome", "Error creating test alert: " + e.getMessage());
        }
        
        // Now query the actual alerts
        alertsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("StaffHome", "Firebase direct query successful");
                DataSnapshot dataSnapshot = task.getResult();
                
                if (dataSnapshot != null) {
                    Log.d("StaffHome", "DataSnapshot is not null, children count: " + dataSnapshot.getChildrenCount());
                    List<SosAlertDatabaseHelper.SosAlert> alerts = new ArrayList<>();
                    
                    for (DataSnapshot alertSnapshot : dataSnapshot.getChildren()) {
                        Log.d("StaffHome", "Processing alert with key: " + alertSnapshot.getKey());
                        try {
                            // Create alert from Firebase data
                            SosAlertDatabaseHelper.SosAlert alert = new SosAlertDatabaseHelper.SosAlert();
                            
                            // Set Firebase ID
                            alert.setFirebaseId(alertSnapshot.getKey());
                            
                            // Extract alert data
                            if (alertSnapshot.hasChild("userId")) {
                                alert.setUserId(Integer.parseInt(alertSnapshot.child("userId").getValue().toString()));
                                Log.d("StaffHome", "Alert userId: " + alert.getUserId());
                            }
                            
                            if (alertSnapshot.hasChild("userName")) {
                                alert.setUserName(alertSnapshot.child("userName").getValue().toString());
                                Log.d("StaffHome", "Alert userName: " + alert.getUserName());
                            }
                            
                            if (alertSnapshot.hasChild("userEmail")) {
                                alert.setUserEmail(alertSnapshot.child("userEmail").getValue().toString());
                                Log.d("StaffHome", "Alert userEmail: " + alert.getUserEmail());
                            }
                            
                            if (alertSnapshot.hasChild("latitude")) {
                                alert.setLatitude(Double.parseDouble(alertSnapshot.child("latitude").getValue().toString()));
                                Log.d("StaffHome", "Alert latitude: " + alert.getLatitude());
                            }
                            
                            if (alertSnapshot.hasChild("longitude")) {
                                alert.setLongitude(Double.parseDouble(alertSnapshot.child("longitude").getValue().toString()));
                                Log.d("StaffHome", "Alert longitude: " + alert.getLongitude());
                            }
                            
                            if (alertSnapshot.hasChild("emergencyContacts")) {
                                alert.setEmergencyContacts(alertSnapshot.child("emergencyContacts").getValue().toString());
                                Log.d("StaffHome", "Alert has emergency contacts");
                            }
                            
                            if (alertSnapshot.hasChild("timestamp")) {
                                alert.setTimestamp(alertSnapshot.child("timestamp").getValue().toString());
                                Log.d("StaffHome", "Alert timestamp: " + alert.getTimestamp());
                            }
                            
                            if (alertSnapshot.hasChild("status")) {
                                alert.setStatus(alertSnapshot.child("status").getValue().toString());
                                Log.d("StaffHome", "Alert status: " + alert.getStatus());
                            } else {
                                alert.setStatus("active");
                                Log.d("StaffHome", "Alert has no status, setting to active");
                            }
                            
                            // Only add active alerts
                            if ("active".equals(alert.getStatus())) {
                                alerts.add(alert);
                                Log.d("StaffHome", "Added active alert: " + alert.getUserName());
                            } else {
                                Log.d("StaffHome", "Skipping non-active alert with status: " + alert.getStatus());
                            }
                        } catch (Exception e) {
                            Log.e("StaffHome", "Error parsing alert: " + e.getMessage(), e);
                        }
                    }
                    
                    // Update UI with alerts
                    Log.d("StaffHome", "Processed " + alerts.size() + " active alerts, updating UI");
                    updateAlertsUI(alerts);
                } else {
                    Log.e("StaffHome", "DataSnapshot is null");
                    runOnUiThread(() -> {
                        if (tvNoAlerts != null) {
                            tvNoAlerts.setText("No data found on server");
                            tvNoAlerts.setVisibility(View.VISIBLE);
                        }
                        Toast.makeText(StaffHome.this, "No alerts data found on server", Toast.LENGTH_LONG).show();
                    });
                }
            } else {
                Log.e("StaffHome", "Error getting alerts from Firebase", task.getException());
                runOnUiThread(() -> {
                    if (tvNoAlerts != null) {
                        tvNoAlerts.setText("Error loading alerts: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        tvNoAlerts.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(StaffHome.this, "Failed to load alerts: " + 
                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"), 
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * Update UI with alerts
     */
    private void updateAlertsUI(List<SosAlertDatabaseHelper.SosAlert> alerts) {
        runOnUiThread(() -> {
            // Clear existing views
            notificationList.removeAllViews();
            
            if (alerts.isEmpty()) {
                // Show no alerts message
                if (tvNoAlerts != null) {
                    tvNoAlerts.setText("No active alerts");
                    tvNoAlerts.setVisibility(View.VISIBLE);
                }
                return;
            }
            
            // Hide no alerts message
            if (tvNoAlerts != null) {
                tvNoAlerts.setVisibility(View.GONE);
            }

            // Create view for each alert
            for (SosAlertDatabaseHelper.SosAlert alert : alerts) {
                View alertView = createSosAlertView(alert);
                notificationList.addView(alertView);
                Log.d("StaffHome", "Added alert to UI: " + alert.getUserName());
            }
            
            // Show all points on the map
            if (googleMap != null) {
                showAlertsOnMap(alerts);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        // Sau khi map sẵn sàng, hiển thị các alerts lên map
        loadMapMarkers();
    }

    /**
     * Hiển thị tất cả alerts lên map
     */
    private void loadMapMarkers() {
        List<SosAlertDatabaseHelper.SosAlert> alerts = sosAlertDbHelper.getActiveSosAlerts();
        showAlertsOnMap(alerts);
    }

    /**
     * Hiển thị các điểm cảnh báo lên map
     */
    private void showAlertsOnMap(List<SosAlertDatabaseHelper.SosAlert> alerts) {
        if (googleMap == null) return;
        
        googleMap.clear(); // Xóa các markers hiện tại
        
        for (SosAlertDatabaseHelper.SosAlert alert : alerts) {
            LatLng location = new LatLng(alert.getLatitude(), alert.getLongitude());
            googleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(alert.getUserName())
                    .snippet("SOS Alert: " + alert.getTimestamp()));
            
            // Zoom đến alert gần nhất
            if (alerts.indexOf(alert) == 0) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
            }
        }
    }

    /**
     * Load và hiển thị danh sách SOS alerts từ local database
     */
    private void loadSosAlerts() {
        List<SosAlertDatabaseHelper.SosAlert> alerts = sosAlertDbHelper.getActiveSosAlerts();
        
        // Clear existing views
        notificationList.removeAllViews();
        
        if (alerts.isEmpty()) {
            // Hiển thị thông báo không có alerts
            if (tvNoAlerts != null) {
                tvNoAlerts.setVisibility(View.VISIBLE);
            }
            return;
        }
        
        // Ẩn thông báo không có alerts
        if (tvNoAlerts != null) {
            tvNoAlerts.setVisibility(View.GONE);
        }

        // Tạo view cho từng alert
        for (SosAlertDatabaseHelper.SosAlert alert : alerts) {
            View alertView = createSosAlertView(alert);
            notificationList.addView(alertView);
        }
    }

    /**
     * Tạo view cho một SOS alert
     */
    private View createSosAlertView(SosAlertDatabaseHelper.SosAlert alert) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View alertView = inflater.inflate(R.layout.item_sos_alert, notificationList, false);

        // Set user info
        TextView tvUserName = alertView.findViewById(R.id.tvUserName);
        TextView tvUserEmail = alertView.findViewById(R.id.tvUserEmail);
        TextView tvTimestamp = alertView.findViewById(R.id.tvTimestamp);
        TextView tvLocation = alertView.findViewById(R.id.tvLocation);

        tvUserName.setText(alert.getUserName());
        tvUserEmail.setText(alert.getUserEmail());
        tvTimestamp.setText(alert.getTimestamp());
        tvLocation.setText(String.format("%.6f, %.6f", alert.getLatitude(), alert.getLongitude()));

        // Set up emergency contacts
        setupEmergencyContacts(alertView, alert);

        // Set up buttons
        setupButtons(alertView, alert);

        return alertView;
    }

    /**
     * Thiết lập emergency contacts có thể gọi được
     */
    private void setupEmergencyContacts(View alertView, SosAlertDatabaseHelper.SosAlert alert) {
        LinearLayout contactsContainer = alertView.findViewById(R.id.emergencyContactsContainer);
        TextView tvNoContacts = alertView.findViewById(R.id.tvNoContacts);

        // Parse emergency contacts string (format: "name1:phone1,name2:phone2")
        String contactsStr = alert.getEmergencyContacts();
        if (contactsStr == null || contactsStr.isEmpty()) {
            tvNoContacts.setVisibility(View.VISIBLE);
            return;
        }

        tvNoContacts.setVisibility(View.GONE);
        String[] contacts = contactsStr.split(",");
        
        for (String contact : contacts) {
            String[] parts = contact.split(":");
            if (parts.length == 2) {
                String name = parts[0].trim();
                String phone = parts[1].trim();
                
                // Tạo button để gọi
                Button callButton = new Button(this);
                callButton.setText(String.format("📞 %s (%s)", name, phone));
                callButton.setTextSize(12);
                callButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.orange));
                callButton.setTextColor(ContextCompat.getColor(this, R.color.light_white));
                callButton.setPadding(8, 4, 8, 4);
                callButton.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                
                // Set click listener để gọi
                callButton.setOnClickListener(v -> {
                    if (checkCallPermission()) {
                        makePhoneCall(phone);
                    } else {
                        requestCallPermission();
                    }
                });
                
                contactsContainer.addView(callButton);
            }
        }
    }

    /**
     * Thiết lập các buttons cho alert
     */
    private void setupButtons(View alertView, SosAlertDatabaseHelper.SosAlert alert) {
        Button btnDismiss = alertView.findViewById(R.id.btnDismiss);
        Button btnAssist = alertView.findViewById(R.id.btnAssist);
        Button btnCallEmergency = alertView.findViewById(R.id.btnCallEmergency);
        Button btnViewLocation = alertView.findViewById(R.id.btnViewLocation);

        // Dismiss button - xóa alert
        btnDismiss.setOnClickListener(v -> {
            showDismissDialog(alert);
        });

        // Assist button - đánh dấu đã hỗ trợ
        btnAssist.setOnClickListener(v -> {
            showAssistDialog(alert);
        });

        // Call emergency button - gọi 911
        btnCallEmergency.setOnClickListener(v -> {
            if (checkCallPermission()) {
                makePhoneCall("113");
            } else {
                requestCallPermission();
            }
        });

        // View location button - xem vị trí trên map
        btnViewLocation.setOnClickListener(v -> {
            showLocationOnMap(alert);
        });
    }

    /**
     * Hiển thị dialog xác nhận xóa alert
     */
    private void showDismissDialog(SosAlertDatabaseHelper.SosAlert alert) {
        new AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setTitle("Dismiss Alert")
                .setMessage("Are you sure you want to dismiss this alert? This action cannot be undone.")
                .setPositiveButton("Dismiss", (dialog, which) -> {
                    boolean success = sosAlertDbHelper.deleteSosAlert(alert.getId());
                    
                    // Cập nhật trạng thái trên Firebase
                    if (alert.getFirebaseId() != null) {
                        firebaseManager.updateAlertStatus(alert.getFirebaseId(), "dismissed");
                    }
                    
                    if (success) {
                        Toast.makeText(this, "Alert dismissed successfully", Toast.LENGTH_SHORT).show();
                        queryFirebaseAlerts(); // Reload alerts
                    } else {
                        Toast.makeText(this, "Failed to dismiss alert", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Hiển thị dialog xác nhận hỗ trợ
     */
    private void showAssistDialog(SosAlertDatabaseHelper.SosAlert alert) {
        new AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setTitle("Mark as Assisted")
                .setMessage("Are you sure you want to mark this alert as assisted? This will resolve the case.")
                .setPositiveButton("Mark Assisted", (dialog, which) -> {
                    boolean success = sosAlertDbHelper.resolveSosAlert(alert.getId());
                    
                    // Cập nhật trạng thái trên Firebase
                    if (alert.getFirebaseId() != null) {
                        firebaseManager.updateAlertStatus(alert.getFirebaseId(), "resolved");
                    }
                    
                    if (success) {
                        Toast.makeText(this, "Alert marked as assisted", Toast.LENGTH_SHORT).show();
                        queryFirebaseAlerts(); // Reload alerts
                    } else {
                        Toast.makeText(this, "Failed to mark alert as assisted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Hiển thị vị trí trên bản đồ
     */
    private void showLocationOnMap(SosAlertDatabaseHelper.SosAlert alert) {
        LatLng location = new LatLng(alert.getLatitude(), alert.getLongitude());
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        
        // Hiển thị marker nếu chưa có
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions()
                .position(location)
                .title(alert.getUserName())
                .snippet("SOS Alert: " + alert.getTimestamp()));
    }

    /**
     * Thực hiện cuộc gọi đến số điện thoại
     */
    private void makePhoneCall(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }

    /**
     * Kiểm tra quyền gọi điện
     */
    private boolean checkCallPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Yêu cầu quyền gọi điện
     */
    private void requestCallPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Call permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Call permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Create a test alert and send it directly to Firebase
     */
    private void createTestAlert() {
        try {
            Log.d("StaffHome", "Creating test alert");
            Toast.makeText(this, "Sending test alert to Firebase...", Toast.LENGTH_SHORT).show();
            
            // Get Firebase database reference
            FirebaseDatabase database;
            try {
                String DATABASE_URL = "https://safeguard-36ba7-default-rtdb.asia-southeast1.firebasedatabase.app/";
                database = FirebaseDatabase.getInstance(DATABASE_URL);
            } catch (Exception e) {
                database = FirebaseDatabase.getInstance();
            }
            
            DatabaseReference alertsRef = database.getReference("sos_alerts");
            
            // Create a new key for the alert
            String alertKey = alertsRef.push().getKey();
            if (alertKey == null) {
                Log.e("StaffHome", "Failed to create Firebase key for test alert");
                Toast.makeText(this, "Failed to create test alert key", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Create test alert data
            Map<String, Object> testAlert = new HashMap<>();
            testAlert.put("userId", 999);
            testAlert.put("userName", "Test User");
            testAlert.put("userEmail", "test@example.com");
            testAlert.put("latitude", 10.777651);
            testAlert.put("longitude", 106.694712);
            testAlert.put("emergencyContacts", "Test Contact:0123456789");
            testAlert.put("timestamp", java.text.DateFormat.getDateTimeInstance().format(new Date()));
            testAlert.put("status", "active");
            
            // Send to Firebase
            alertsRef.child(alertKey).setValue(testAlert)
                .addOnSuccessListener(aVoid -> {
                    Log.d("StaffHome", "Test alert created successfully with key: " + alertKey);
                    Toast.makeText(this, "Test alert sent successfully! Key: " + alertKey, Toast.LENGTH_LONG).show();
                    
                    // Refresh the alerts list
                    queryFirebaseAlerts();
                })
                .addOnFailureListener(e -> {
                    Log.e("StaffHome", "Failed to create test alert: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to send test alert: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        } catch (Exception e) {
            Log.e("StaffHome", "Error in createTestAlert: " + e.getMessage(), e);
            Toast.makeText(this, "Error creating test alert: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        // Reload alerts when activity resumes
        queryFirebaseAlerts();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }
}
