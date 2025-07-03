package com.example.safeguard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.safeguard.database.StaffDataInitializer;

public class StaffLogIn extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_login);

        // Khởi tạo dữ liệu staff khi activity được tạo
        StaffDataInitializer.addStaffData(this);

        // Link UI components
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnBack = findViewById(R.id.btnBack);

        // Set click listener for back button
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(StaffLogIn.this, LogIn.class);
            startActivity(intent);
            finish();
        });

        // Set click listener for login button
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Validate input fields
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(StaffLogIn.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(StaffLogIn.this, "Invalid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check staff credentials
            boolean isValid = StaffDataInitializer.checkStaffCredentials(this, email, password);

            if (isValid) {
                Toast.makeText(StaffLogIn.this, "Login successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(StaffLogIn.this, StaffHome.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(StaffLogIn.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
