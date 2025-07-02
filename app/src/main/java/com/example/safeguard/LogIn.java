package com.example.safeguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.safeguard.database.UserDatabaseHelper;

public class LogIn extends AppCompatActivity {

    private EditText etEmail, etPassword; // Input fields for email and password
    private Button btnLogin; // Log In button
    private TextView tvSignUp, tvStaff; // Sign Up navigation text
    private UserDatabaseHelper dbHelper; // Helper for database operations

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize the UserDatabaseHelper
        dbHelper = new UserDatabaseHelper(this);

        // Link UI components using their respective IDs
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvStaff = findViewById(R.id.tvStaff);

        // Navigate to the Staff Login page when "Staff" text is clicked
        tvStaff.setOnClickListener(v -> {
            Intent intent = new Intent(LogIn.this, StaffLogIn.class);
            startActivity(intent); // Open the Staff Login activity
        });

        // Set click listener for the Log In button
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim(); // Get the entered email
            String password = etPassword.getText().toString().trim(); // Get the entered password

            // Validate that no fields are left empty
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LogIn.this, "All fields must be filled", Toast.LENGTH_SHORT).show();
                return; // Stop execution if validation fails
            }

            // Check user credentials in the database and get user info
            UserInfo userInfo = getUserInfo(email, password);

            if (userInfo != null) {
                // If valid, save login state and user info, then navigate to Home screen
                saveLoginState(userInfo); // Save the login state and user info in SharedPreferences
                Toast.makeText(LogIn.this, "Login successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LogIn.this, Home.class);
                startActivity(intent);
                finish(); // Close the Log In activity to prevent going back to it
            } else {
                // If invalid, show an error message
                Toast.makeText(LogIn.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
            }
        });

        // Navigate to the Sign Up page when "Sign Up" text is clicked
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LogIn.this, SignUp.class);
            startActivity(intent); // Open the Sign Up activity
        });
    }

    /**
     * Lấy thông tin user từ database
     */
    private UserInfo getUserInfo(String email, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM users WHERE email=? AND password=?";
        String[] selectionArgs = {email, password};

        Cursor cursor = db.rawQuery(query, selectionArgs);

        UserInfo userInfo = null;
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            String fullName = cursor.getString(cursor.getColumnIndexOrThrow("full_name"));
            String userEmail = cursor.getString(cursor.getColumnIndexOrThrow("email"));

            userInfo = new UserInfo(id, fullName, userEmail);
        }

        cursor.close();
        db.close();

        return userInfo;
    }

    // Save login state and user info in SharedPreferences
    private void saveLoginState(UserInfo userInfo) {
        SharedPreferences sharedPreferences = getSharedPreferences("AlertifyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true); // Set login state
        editor.putInt("userId", userInfo.getId()); // Save user ID
        editor.putString("userName", userInfo.getFullName()); // Save user name
        editor.putString("userEmail", userInfo.getEmail()); // Save user email
        editor.apply(); // Apply changes
    }

    /**
     * Class để lưu trữ thông tin user
     */
    private static class UserInfo {
        private int id;
        private String fullName;
        private String email;

        public UserInfo(int id, String fullName, String email) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
        }

        public int getId() { return id; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
    }
}
