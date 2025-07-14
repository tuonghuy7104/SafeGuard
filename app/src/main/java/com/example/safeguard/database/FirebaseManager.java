package com.example.safeguard.database;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
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
    private static final String DATABASE_URL = "https://safeguard-36ba7-default-rtdb.firebaseio.com/";

    private final DatabaseReference sosAlertsRef;
    private final SosAlertDatabaseHelper localDbHelper;
    private final Context context;

    public FirebaseManager(Context context) {
        this.context = context;
        
        // Get Firebase instance
        FirebaseDatabase database;
        try {
            database = FirebaseDatabase.getInstance(DATABASE_URL);
            Log.d(TAG, "Using explicit database URL: " + DATABASE_URL);
        } catch (Exception e) {
            Log.w(TAG, "Error using explicit URL, falling back to default: " + e.getMessage());
            database = FirebaseDatabase.getInstance();
        }
        
        // Get reference to SOS alerts
        sosAlertsRef = database.getReference(SOS_ALERTS_PATH);
        localDbHelper = new SosAlertDatabaseHelper(context);
        
        // Keep cached data if disconnected
        sosAlertsRef.keepSynced(true);
        
        // Check connectivity and notify user if offline
        checkConnectivity();
    }
    
    /**
     * Check if device has network connectivity
     */
    private void checkConnectivity() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        
        if (!isConnected) {
            Log.w(TAG, "No network connectivity - Firebase will work in offline mode only");
            Toast.makeText(context, "No internet connection. Working in offline mode.", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "Network connectivity available");
            // Test database connection
            testDatabaseConnection();
        }
    }
    
    /**
     * Test connection to Firebase database
     */
    private void testDatabaseConnection() {
        // Get Firebase instance with explicit URL
        FirebaseDatabase database;
        try {
            database = FirebaseDatabase.getInstance(DATABASE_URL);
        } catch (Exception e) {
            database = FirebaseDatabase.getInstance();
        }
        
        DatabaseReference testRef = database.getReference(".info/connected");
        testRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null && connected) {
                    Log.d(TAG, "Successfully connected to Firebase");
                } else {
                    Log.w(TAG, "Failed to connect to Firebase");
                    Toast.makeText(context, "Could not connect to Firebase. Some features may not work.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase connection test cancelled: " + error.getMessage());
                Toast.makeText(context, "Firebase connection error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Interface to notify when alerts are updated
     */
    public interface AlertsListener {
        void onAlertsUpdated(List<SosAlertDatabaseHelper.SosAlert> alerts);
    }

    /**
     * Add listener to monitor alerts changes
     */
    public void listenForAlerts(final AlertsListener listener) {
        Log.d(TAG, "Setting up Firebase alerts listener");
        
        sosAlertsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<SosAlertDatabaseHelper.SosAlert> alerts = new ArrayList<>();
                
                Log.d(TAG, "Firebase data changed, snapshot has " + dataSnapshot.getChildrenCount() + " alerts");
                
                for (DataSnapshot alertSnapshot : dataSnapshot.getChildren()) {
                    try {
                        // Create a new SOS alert from Firebase data
                        SosAlertDatabaseHelper.SosAlert alert = new SosAlertDatabaseHelper.SosAlert();
                        
                        // Set the Firebase ID
                        alert.setFirebaseId(alertSnapshot.getKey());
                        Log.d(TAG, "Processing alert with Firebase ID: " + alertSnapshot.getKey());
                        
                        // Get all the alert data
                        if (alertSnapshot.hasChild("userId"))
                            alert.setUserId(Integer.parseInt(alertSnapshot.child("userId").getValue().toString()));
                        
                        if (alertSnapshot.hasChild("userName")) {
                            alert.setUserName(alertSnapshot.child("userName").getValue().toString());
                            Log.d(TAG, "Alert from user: " + alert.getUserName());
                        }
                        
                        if (alertSnapshot.hasChild("userEmail"))
                            alert.setUserEmail(alertSnapshot.child("userEmail").getValue().toString());
                        
                        if (alertSnapshot.hasChild("latitude"))
                            alert.setLatitude(Double.parseDouble(alertSnapshot.child("latitude").getValue().toString()));
                        
                        if (alertSnapshot.hasChild("longitude"))
                            alert.setLongitude(Double.parseDouble(alertSnapshot.child("longitude").getValue().toString()));
                        
                        if (alertSnapshot.hasChild("emergencyContacts"))
                            alert.setEmergencyContacts(alertSnapshot.child("emergencyContacts").getValue().toString());
                        
                        if (alertSnapshot.hasChild("timestamp"))
                            alert.setTimestamp(alertSnapshot.child("timestamp").getValue().toString());
                        
                        if (alertSnapshot.hasChild("status")) {
                            alert.setStatus(alertSnapshot.child("status").getValue().toString());
                            Log.d(TAG, "Alert status: " + alert.getStatus());
                        } else {
                            alert.setStatus("active");
                        }
                        
                        // Only add active alerts to the list
                        if ("active".equals(alert.getStatus())) {
                            alerts.add(alert);
                            Log.d(TAG, "Added active alert from Firebase: " + alert.getUserName());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing alert: " + e.getMessage(), e);
                    }
                }
                
                // Update local database with alerts from Firebase
                syncLocalDatabase(alerts);
                
                // Call the callback
                if (listener != null) {
                    listener.onAlertsUpdated(alerts);
                    Log.d(TAG, "Notified listener with " + alerts.size() + " alerts");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Firebase Database error: " + databaseError.getMessage());
                Toast.makeText(context, "Firebase error: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Add a new alert to Firebase
     */
    public void addSosAlert(SosAlertDatabaseHelper.SosAlert alert) {
        // Create a new key for the alert
        String alertKey = sosAlertsRef.push().getKey();
        if (alertKey == null) {
            Log.e(TAG, "Failed to create new Firebase key for SOS alert");
            return;
        }

        // Create a map from the alert to save to Firebase
        Map<String, Object> alertValues = alertToMap(alert);
        
        Log.d(TAG, "Adding alert to Firebase with key: " + alertKey);

        // Update Firebase
        sosAlertsRef.child(alertKey).setValue(alertValues)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "SOS Alert added to Firebase successfully");
                // Update the local database with the Firebase ID
                localDbHelper.updateFirebaseId(alert.getId(), alertKey);
                Toast.makeText(context, "SOS alert sent successfully!", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to add SOS Alert to Firebase: " + e.getMessage(), e);
                Toast.makeText(context, "Failed to send SOS alert: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    /**
     * Update the status of an alert
     */
    public void updateAlertStatus(String firebaseId, String status) {
        if (firebaseId != null && !firebaseId.isEmpty()) {
            Log.d(TAG, "Updating alert status in Firebase: " + firebaseId + " to " + status);
            
            sosAlertsRef.child(firebaseId).child("status").setValue(status)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "SOS Alert status updated in Firebase");
                    Toast.makeText(context, "Alert status updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update SOS Alert status in Firebase: " + e.getMessage(), e);
                    Toast.makeText(context, "Failed to update alert status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        }
    }

    /**
     * Sync local database with data from Firebase
     */
    private void syncLocalDatabase(List<SosAlertDatabaseHelper.SosAlert> firebaseAlerts) {
        // Run in a separate thread to avoid blocking UI
        new Thread(() -> {
            try {
                for (SosAlertDatabaseHelper.SosAlert alert : firebaseAlerts) {
                    String firebaseId = alert.getFirebaseId();
                    
                    // Check if this alert already exists in local database
                    boolean exists = localDbHelper.checkIfFirebaseAlertExists(firebaseId);
                    
                    if (!exists) {
                        // Add the alert to local database
                        localDbHelper.addSosAlertFromFirebase(
                            firebaseId,
                            alert.getUserId(),
                            alert.getUserName(),
                            alert.getUserEmail(),
                            alert.getLatitude(),
                            alert.getLongitude(),
                            alert.getEmergencyContacts(),
                            alert.getTimestamp(),
                            alert.getStatus()
                        );
                        Log.d(TAG, "Added new alert from Firebase to local database: " + alert.getUserName());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing with local database: " + e.getMessage(), e);
            }
        }).start();
    }

    /**
     * Convert SosAlert object to Map
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