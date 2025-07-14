package com.example.safeguard;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SafeGuardApplication extends Application {
    private static final String TAG = "SafeGuardApplication";
    private static final String DATABASE_URL = "https://safeguard-36ba7-default-rtdb.firebaseio.com/";
    private static boolean persistenceInitialized = false;
    private static boolean firebaseInitialized = false;

    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.i(TAG, "💫 SafeGuardApplication onCreate started");
        
        // Force initialize Firebase with manual configuration if standard approach fails
        initializeFirebase();
        
        // Check network connectivity
        checkNetworkConnectivity();
    }
    
    /**
     * Initialize Firebase with multiple fallback approaches
     */
    private void initializeFirebase() {
        // Approach 1: Standard initialization
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "✅ Firebase initialized with standard approach");
                firebaseInitialized = true;
            } else {
                Log.d(TAG, "✅ Firebase was already initialized");
                firebaseInitialized = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error with standard Firebase initialization: " + e.getMessage(), e);
            
            // Approach 2: Manual initialization with options
            try {
                // Only attempt if first approach failed
                if (!firebaseInitialized) {
                    FirebaseOptions options = new FirebaseOptions.Builder()
                        .setDatabaseUrl(DATABASE_URL)
                        .setApplicationId("1:882730931747:android:8f55e795bd8fe649798396") // From google-services.json
                        .setApiKey("AIzaSyB_MakDLSZoo6EKjBTHT6-ELHx5HY2CVQU") // From google-services.json
                        .build();
                    
                    FirebaseApp.initializeApp(this, options);
                    Log.d(TAG, "✅ Firebase initialized with manual options");
                    firebaseInitialized = true;
                }
            } catch (Exception e2) {
                Log.e(TAG, "❌ Error with manual Firebase initialization: " + e2.getMessage(), e2);
            }
        }
        
        // Try to enable persistence, regardless of which initialization approach worked
        try {
            if (!persistenceInitialized) {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                database.setPersistenceEnabled(true);
                persistenceInitialized = true;
                Log.d(TAG, "✅ Firebase persistence enabled successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error setting Firebase persistence: " + e.getMessage(), e);
        }
        
        // Verify database connection
        verifyDatabaseConnection();
    }
    
    /**
     * Check network connectivity
     */
    private void checkNetworkConnectivity() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        
        if (!isConnected) {
            Log.w(TAG, "⚠️ No network connectivity detected");
        } else {
            Log.d(TAG, "✅ Network connectivity available");
        }
    }
    
    /**
     * Verify Firebase database connection
     */
    private void verifyDatabaseConnection() {
        // Get Firebase instance with explicit URL
        FirebaseDatabase database;
        try {
            database = FirebaseDatabase.getInstance(DATABASE_URL);
            Log.d(TAG, "Using explicit database URL: " + DATABASE_URL);
        } catch (Exception e) {
            database = FirebaseDatabase.getInstance();
            Log.w(TAG, "Using default database URL: " + e.getMessage());
        }
        
        // Check connection status
        DatabaseReference connectedRef = database.getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null && connected) {
                    Log.d(TAG, "✅ Connected to Firebase Realtime Database successfully");
                } else {
                    Log.w(TAG, "❌ Not connected to Firebase Realtime Database");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Firebase Database connection check cancelled", error.toException());
            }
        });
        
        // Test database rules with write operation
        testDatabaseRules(database);
    }
    
    /**
     * Test database rules by attempting a write operation
     */
    private void testDatabaseRules(FirebaseDatabase database) {
        // Create test node with timestamp
        String testPath = "test_connection";
        DatabaseReference testRef = database.getReference(testPath);
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("timestamp", System.currentTimeMillis());
        testData.put("message", "Firebase connection test");
        
        // Try to write data
        testRef.setValue(testData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Successfully wrote test data to Firebase");
                
                // Now try to read it back
                testRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Log.d(TAG, "✅ Successfully read test data from Firebase");
                            
                            // Clean up test data
                            testRef.removeValue()
                                .addOnSuccessListener(unused -> Log.d(TAG, "✅ Test data cleaned up"))
                                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to clean up test data", e));
                        } else {
                            Log.e(TAG, "❌ Test data not found in Firebase");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "❌ Failed to read test data: " + error.getMessage(), error.toException());
                    }
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to write test data to Firebase: " + e.getMessage(), e);
                
                // Output database rules information if write fails
                Log.e(TAG, "FIREBASE DATABASE RULES INFORMATION:");
                Log.e(TAG, "Make sure your Firebase Realtime Database rules are set to allow reads and writes!");
                Log.e(TAG, "Example rules:");
                Log.e(TAG, "{");
                Log.e(TAG, "  \"rules\": {");
                Log.e(TAG, "    \".read\": true,");
                Log.e(TAG, "    \".write\": true");
                Log.e(TAG, "  }");
                Log.e(TAG, "}");
                
                // Also try to read some data
                try {
                    database.getReference(".info").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Log.d(TAG, "✅ Successfully read Firebase .info node");
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "❌ Failed to read Firebase .info node: " + error.getMessage());
                        }
                    });
                } catch (Exception readEx) {
                    Log.e(TAG, "❌ Error trying to read .info node: " + readEx.getMessage(), readEx);
                }
            });
    }
} 