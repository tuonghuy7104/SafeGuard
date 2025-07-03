package com.example.safeguard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.safeguard.database.SosAlertDatabaseHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class StaffHome extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private LinearLayout notificationList;
    private Button btnLogout;
    private SosAlertDatabaseHelper sosAlertDbHelper;
    private TextView tvNoAlerts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_home);

        // Initialize database helper
        sosAlertDbHelper = new SosAlertDatabaseHelper(this);

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

        // Load SOS alerts
        loadSosAlerts();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        // Future: Add markers when an alert is received
    }

    /**
     * Load và hiển thị danh sách SOS alerts
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
                makePhoneCall("911");
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
        new AlertDialog.Builder(this)
                .setTitle("Dismiss Alert")
                .setMessage("Are you sure you want to dismiss this alert? This action cannot be undone.")
                .setPositiveButton("Dismiss", (dialog, which) -> {
                    boolean success = sosAlertDbHelper.deleteSosAlert(alert.getId());
                    if (success) {
                        Toast.makeText(this, "Alert dismissed successfully", Toast.LENGTH_SHORT).show();
                        loadSosAlerts(); // Reload alerts
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
        new AlertDialog.Builder(this)
                .setTitle("Mark as Assisted")
                .setMessage("Are you sure you want to mark this alert as assisted? This will resolve the case.")
                .setPositiveButton("Mark Assisted", (dialog, which) -> {
                    boolean success = sosAlertDbHelper.resolveSosAlert(alert.getId());
                    if (success) {
                        Toast.makeText(this, "Alert marked as assisted", Toast.LENGTH_SHORT).show();
                        loadSosAlerts(); // Reload alerts
                    } else {
                        Toast.makeText(this, "Failed to mark alert as assisted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Hiển thị vị trí trên map
     */
    private void showLocationOnMap(SosAlertDatabaseHelper.SosAlert alert) {
        if (googleMap != null) {
            LatLng location = new LatLng(alert.getLatitude(), alert.getLongitude());
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(alert.getUserName())
                    .snippet("Emergency Location"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
            
            Toast.makeText(this, "Location displayed on map", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Thực hiện cuộc gọi
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
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
               == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Yêu cầu quyền gọi điện
     */
    private void requestCallPermission() {
        ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CALL_PHONE}, 
                100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Call permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Call permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        // Reload alerts when returning to the activity
        loadSosAlerts();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (sosAlertDbHelper != null) {
            sosAlertDbHelper.close();
        }
    }
}
