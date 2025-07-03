package com.example.safeguard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.safeguard.database.SosAlertTestHelper;
import com.example.safeguard.database.StaffDataInitializer;

public class StaffTestActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnAddStaff, btnTestLogin, btnAddSampleAlerts, btnClearAlerts, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_test);

        // Initialize UI components
        tvStatus = findViewById(R.id.tvStatus);
        btnAddStaff = findViewById(R.id.btnAddStaff);
        btnTestLogin = findViewById(R.id.btnTestLogin);
        btnAddSampleAlerts = findViewById(R.id.btnAddSampleAlerts);
        btnClearAlerts = findViewById(R.id.btnClearAlerts);
        btnBack = findViewById(R.id.btnBack);

        // Button để thêm dữ liệu staff
        btnAddStaff.setOnClickListener(v -> {
            boolean success = StaffDataInitializer.addStaffData(this);
            if (success) {
                tvStatus.setText("✅ Staff data added successfully!\n\nEmail: tuonghuy7104@gmail.com\nPassword: 123456\nFull Name: Staff Tuong Huy");
                Toast.makeText(this, "Staff data added successfully!", Toast.LENGTH_LONG).show();
            } else {
                tvStatus.setText("❌ Failed to add staff data!\n\nPlease check the logs for more information.");
                Toast.makeText(this, "Failed to add staff data!", Toast.LENGTH_LONG).show();
            }
        });

        // Button để test đăng nhập
        btnTestLogin.setOnClickListener(v -> {
            boolean isValid = StaffDataInitializer.checkStaffCredentials(
                this, 
                "tuonghuy7104@gmail.com", 
                "123456"
            );
            
            if (isValid) {
                tvStatus.setText("✅ Staff login test: SUCCESS!\n\nEmail: tuonghuy7104@gmail.com\nPassword: 123456\nStatus: Valid credentials");
                Toast.makeText(this, "Staff login test: SUCCESS!", Toast.LENGTH_LONG).show();
            } else {
                tvStatus.setText("❌ Staff login test: FAILED!\n\nEmail: tuonghuy7104@gmail.com\nPassword: 123456\nStatus: Invalid credentials");
                Toast.makeText(this, "Staff login test: FAILED!", Toast.LENGTH_LONG).show();
            }
        });

        // Button để thêm SOS alerts mẫu
        btnAddSampleAlerts.setOnClickListener(v -> {
            SosAlertTestHelper.addSampleSosAlerts(this);
            tvStatus.setText("✅ Sample SOS alerts function called!\n\nNo sample data will be added.\n\nTo test StaffHome, users need to send actual SOS alerts.");
            Toast.makeText(this, "Sample SOS alerts function called!", Toast.LENGTH_LONG).show();
        });

        // Button để xóa tất cả alerts
        btnClearAlerts.setOnClickListener(v -> {
            SosAlertTestHelper.clearAllSosAlerts(this);
            tvStatus.setText("✅ All SOS alerts cleared successfully!\n\nAll active alerts have been removed from the database.\n\nStaffHome will now show 'No active SOS alerts'.");
            Toast.makeText(this, "All SOS alerts cleared successfully!", Toast.LENGTH_LONG).show();
        });

        // Button để quay lại StaffLogin
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(StaffTestActivity.this, StaffLogIn.class);
            startActivity(intent);
            finish();
        });

        // Hiển thị thông tin staff ban đầu
        displayStaffInfo();
    }

    private void displayStaffInfo() {
        String staffInfo = "📋 Staff Information:\n" +
                "Email: tuonghuy7104@gmail.com\n" +
                "Password: 123456\n" +
                "Full Name: Staff Tuong Huy\n\n" +
                "🔧 Test Functions:\n" +
                "• Add Staff Data: Add staff to database\n" +
                "• Test Login: Verify staff credentials\n" +
                "• Add Sample Alerts: Function call (no data added)\n" +
                "• Clear Alerts: Remove all alerts\n\n" +
                "Use the buttons below to test staff functionality.";
        
        tvStatus.setText(staffInfo);
    }
} 