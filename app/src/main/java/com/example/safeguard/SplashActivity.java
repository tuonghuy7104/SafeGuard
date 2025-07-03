package com.example.safeguard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.safeguard.database.DatabaseSeeder;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Forces light theme, since we don't have a dark theme yet
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        // Set the splash screen layout
        setContentView(R.layout.activity_splash);

        // Seed initial data (including staff data) if needed
        if (DatabaseSeeder.needsSeeding(this)) {
            DatabaseSeeder.seedInitialData(this);
        }

        // Add a delay of 3 seconds before deciding the next screen
        new Handler().postDelayed(() -> {
            // Check if the user is logged in
            SharedPreferences sharedPreferences = getSharedPreferences("AlertifyPrefs", MODE_PRIVATE);
            boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false); // Default is false

            Intent intent;
            if (isLoggedIn) {
                // Navigate to the Home Activity if user is logged in
                intent = new Intent(SplashActivity.this, Home.class);
            } else {
                // Navigate to the Login Activity if user is not logged in
                intent = new Intent(SplashActivity.this, LogIn.class);
            }

            startActivity(intent);

            // Finish SplashActivity to remove it from the back stack
            finish();
        }, 3000); // 3000 milliseconds = 3 seconds
    }
}
