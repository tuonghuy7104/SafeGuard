package com.example.safeguard;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

public class SafeGuardApplication extends Application {
    private static final String TAG = "SafeGuardApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase only once
        try {
            // Initialize Firebase only if not already initialized
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialized successfully");
            } else {
                Log.d(TAG, "Firebase was already initialized");
            }
            
            // Enable disk persistence (allow offline capabilities)
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage(), e);
        }
    }
} 