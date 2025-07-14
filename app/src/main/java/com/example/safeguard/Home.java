package com.example.safeguard;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.safeguard.database.ContactDatabaseHelper;
import com.example.safeguard.database.SosAlertDatabaseHelper;
import com.example.safeguard.database.FirebaseManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Home extends AppCompatActivity implements OnMapReadyCallback {

    // Sidebar layout and buttons
    private View sidebarLayout;
    private View backgroundOverlay;
    private ImageButton closeButton;

    // Sidebar buttons
    private Button btnHome;
    private Button btnSosContacts;
    private Button btnSafetyTips;
    private Button btnAboutUs;
    private Button btnTermsConditions;
    private Button btnLogOut;

    // Slider elements for SOS functionality
    private FrameLayout sliderButton;
    private TextView sliderInstruction;
    private boolean isSliderActive = false;

    private boolean isHomeSelected = true; // Flag to track if the "Home" button is selected

    private ArrayList<Contact> pinnedContacts = new ArrayList<>(); // List of pinned contacts

    // Placeholders for pinned contacts in the UI
    private ArrayList<TextView> placeholders;

    // Database helper for retrieving contacts
    private ContactDatabaseHelper contactDatabaseHelper;

    // Database helper for SOS alerts
    private SosAlertDatabaseHelper sosAlertDbHelper;
    
    // Firebase manager
    private FirebaseManager firebaseManager;

    // Google Map variables
    private MapView mapView;
    private GoogleMap googleMap;

    // Location client for accessing the user's location
    private FusedLocationProviderClient fusedLocationClient;

    private LocationCallback locationCallback;

    // Interface để callback kết quả
    public interface LocationResultListener {
        void onLocationResult(android.location.Location location);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Request permissions (combined for location and SMS)
        checkAndRequestPermissions();

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize the MapView
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Initialize database helpers
        contactDatabaseHelper = new ContactDatabaseHelper(this);
        sosAlertDbHelper = new SosAlertDatabaseHelper(this);
        firebaseManager = new FirebaseManager(this);

        // Initialize sidebar buttons
        btnHome = findViewById(R.id.btn_home);
        btnSosContacts = findViewById(R.id.btn_sos_contacts);
        btnSafetyTips = findViewById(R.id.btn_safety_tips);
        btnAboutUs = findViewById(R.id.btn_about_us);
        btnTermsConditions = findViewById(R.id.btn_terms_conditions);
        btnLogOut = findViewById(R.id.btn_logout);

        // Initialize slider elements for SOS activation
        sliderButton = findViewById(R.id.sliderButton);
        sliderInstruction = findViewById(R.id.sliderInstruction);

        sliderButton.setOnTouchListener(this::handleSliderMovement);

        // Initialize placeholders for pinned contacts in the UI
        placeholders = new ArrayList<>();
        placeholders.add(findViewById(R.id.contact_text_1));
        placeholders.add(findViewById(R.id.contact_text_2));
        placeholders.add(findViewById(R.id.contact_text_3));
        placeholders.add(findViewById(R.id.contact_text_4));

        // Load pinned contacts from the database
        loadPinnedContacts();

        // Initialize sidebar layout and elements
        sidebarLayout = findViewById(R.id.sidebar_layout);
        backgroundOverlay = findViewById(R.id.background_overlay);
        ImageButton openSettingsButton = findViewById(R.id.open_settings_button);
        closeButton = findViewById(R.id.close_button);

        sidebarLayout.setVisibility(View.INVISIBLE);
        sidebarLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                sidebarLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                sidebarLayout.setTranslationX(sidebarLayout.getWidth());
                sidebarLayout.setVisibility(View.GONE);
            }
        });

        // Set listeners for sidebar buttons
        openSettingsButton.setOnClickListener(v -> openSidebar());
        backgroundOverlay.setOnClickListener(v -> closeSidebar());
        closeButton.setOnClickListener(v -> closeSidebar());

        // Set actions for sidebar menu buttons
        btnHome.setOnClickListener(v -> {
            if (isHomeSelected) {
                closeSidebar();
            } else {
                setSelectedButton(btnHome);
                isHomeSelected = true;
            }
        });

        btnSosContacts.setOnClickListener(v -> {
            setSelectedButton(btnSosContacts);
            isHomeSelected = false;
            startActivity(new Intent(Home.this, SosContacts.class));
        });

        btnSafetyTips.setOnClickListener(v -> {
            setSelectedButton(btnSafetyTips);
            isHomeSelected = false;
            // TODO: Navigate to Safety Tips page
            startActivity(new Intent(Home.this, SafetyTips.class));
        });

        btnAboutUs.setOnClickListener(v -> {
            setSelectedButton(btnAboutUs);
            isHomeSelected = false;
            // TODO: Navigate to About Us page
            startActivity(new Intent(Home.this, AboutUs.class));
        });

        btnTermsConditions.setOnClickListener(v -> {
            setSelectedButton(btnTermsConditions);
            isHomeSelected = false;
            // TODO: Navigate to Terms & Conditions page
            startActivity(new Intent(Home.this, TermCondition.class));
        });

        btnLogOut.setOnClickListener(v -> showLogoutDialog());
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Configure the map if permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupMap();

            // Enable My Location layer
            googleMap.setMyLocationEnabled(true);
            // Move the location button to the bottom-right corner
            if (mapView != null) {
                View locationButton = ((View) mapView.findViewById(Integer.parseInt("1"))
                        .getParent()).findViewById(Integer.parseInt("2"));
                // Update layout parameters to move the button
                if (locationButton != null) {
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0); // Remove from top
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE); // Align to bottom
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE); // Align to right
                    layoutParams.setMargins(0, 0, 30, 30); // Set margins (adjust as needed)
                }
            }
        }
    }

    // Sets up the Google Map with the user's location
    private void setupMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap.setMyLocationEnabled(true);
        getCurrentLocation(location -> {
            if (location != null) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                googleMap.addMarker(new MarkerOptions().position(userLocation).title("You are here"));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload pinned contacts when Home is resumed
        loadPinnedContacts();

        // Always select the Home button
        setSelectedButton(btnHome);
        isHomeSelected = true;

        // Resume the map view
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Pause the map view
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Destroy the map view
        if (mapView != null) {
            mapView.onDestroy();
        }
        
        // Close database helpers
        if (contactDatabaseHelper != null) {
            contactDatabaseHelper.close();
        }
        if (sosAlertDbHelper != null) {
            sosAlertDbHelper.close();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the map view state
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    // Loads pinned contacts from the database and updates the UI
    private void loadPinnedContacts() {
        SQLiteDatabase database = contactDatabaseHelper.getReadableDatabase();

        // Query pinned contacts, ordered by pinned_order
        Cursor cursor = database.query(
                "contacts",
                new String[]{"name", "number"}, // Include the phone number in the query
                "isPinned = ?",
                new String[]{"1"},
                null,
                null,
                "pinned_order ASC"
        );

        // Clear the list of pinned contacts
        pinnedContacts.clear();

        // Reset placeholders to "N/A"
        for (TextView placeholder : placeholders) {
            placeholder.setText("N/A");
            placeholder.setOnClickListener(null); // Clear any existing click listeners
        }

        // Populate placeholders with contact names and set click listeners
        int index = 0;
        while (cursor.moveToNext()) {
            String contactName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String contactNumber = cursor.getString(cursor.getColumnIndexOrThrow("number"));

            // Add the contact to the pinned contacts list
            pinnedContacts.add(new Contact(contactName, contactNumber));

            // Update placeholders up to the maximum limit
            if (index < placeholders.size()) {
                TextView placeholder = placeholders.get(index);
                placeholder.setText(contactName);

                // Set click listener for the placeholder with feedback
                placeholder.setOnClickListener(v -> {
                    // Visual feedback: Change the placeholder's background color temporarily
                    placeholder.setBackgroundResource(R.color.orange);

                    // Reset the background color after a short delay
                    new Handler().postDelayed(() -> {
                        placeholder.setBackgroundResource(R.color.light_purple); // Reset to the default color
                    }, 200); // 200ms delay for visual effect

                    // Add vibration feedback
                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)); // 100ms vibration
                    }

                    // Send the safe message
                    sendSafeMessage(contactName, contactNumber);
                });
                index++;
            }
        }
        cursor.close();
    }

    private void fetchNearbyPlaces(double latitude, double longitude) throws PackageManager.NameNotFoundException {
        ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
        String apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY");

        int radius = 1000; // Radius in meters

        // Google Places API URL
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=" + latitude + "," + longitude +
                "&radius=" + radius +
                "&type=restaurant|bar|cafe|store|establishment" + // Add more types here
                "&opennow=true" +
                "&key=" + apiKey;

        // Make a network request
        new Thread(() -> {
            try {
                URL apiUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
                connection.setRequestMethod("GET");

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                parseNearbyPlaces(response.toString(), latitude, longitude);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void parseNearbyPlaces(String jsonResponse, double userLat, double userLng) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray results = jsonObject.getJSONArray("results");

            LatLng closestLocation = null;
            double shortestDistance = Double.MAX_VALUE;

            for (int i = 0; i < results.length(); i++) {
                JSONObject place = results.getJSONObject(i);
                JSONObject geometry = place.getJSONObject("geometry").getJSONObject("location");

                double placeLat = geometry.getDouble("lat");
                double placeLng = geometry.getDouble("lng");

                // Calculate distance to user
                double distance = calculateDistance(userLat, userLng, placeLat, placeLng);

                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    closestLocation = new LatLng(placeLat, placeLng);
                }
            }

            if (closestLocation != null) {
                showPathToDestination(closestLocation);
            } else {
                runOnUiThread(() -> Toast.makeText(this, "No open places nearby.", Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of Earth in kilometers
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // Convert to meters
    }

    private void showPathToDestination(LatLng destination) {
        if (googleMap == null) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // Permissions not granted, exit the method
        }
        getCurrentLocation(location -> {
            if (location != null) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                try {
                    ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
                    String apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY");
                    String url = "https://maps.googleapis.com/maps/api/directions/json" +
                            "?origin=" + userLocation.latitude + "," + userLocation.longitude +
                            "&destination=" + destination.latitude + "," + destination.longitude +
                            "&key=" + apiKey;
                    new Thread(() -> {
                        try {
                            URL apiUrl = new URL(url);
                            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
                            connection.setRequestMethod("GET");
                            InputStream inputStream = connection.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }
                            reader.close();
                            drawRouteOnMap(response.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void drawRouteOnMap(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray routes = jsonObject.getJSONArray("routes");
            if (routes.length() > 0) {
                JSONObject route = routes.getJSONObject(0);
                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                String encodedPolyline = overviewPolyline.getString("points");

                List<LatLng> points = PolyUtil.decode(encodedPolyline);

                runOnUiThread(() -> {
                    googleMap.addPolyline(new PolylineOptions()
                            .addAll(points)
                            .width(10)
                            .color(ContextCompat.getColor(this, R.color.primary_purple)));
                });
            } else {
                runOnUiThread(() -> Toast.makeText(this, "No routes found.", Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Failed to draw route.", Toast.LENGTH_SHORT).show());
        }
    }

    // Sends a "safe message" to a specific contact
    private void sendSafeMessage(String contactName, String contactNumber) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        getCurrentLocation(location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                String locationUrl = "https://www.google.com/maps?q=" + latitude + "," + longitude;
                String message = "I don't feel safe. Please monitor my location: " + locationUrl;
                for (Contact contact : pinnedContacts) {
                    SmsManager smsManager = SmsManager.getDefault();
                    try {
                        String normalizedNumber = normalizePhoneNumber(contact.getNumber());
                        smsManager.sendTextMessage(normalizedNumber, null, message, null, null);
                        Log.d("SOSApp", "Contact number: " + normalizedNumber);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to send SMS to " + contact.getNumber(), Toast.LENGTH_SHORT).show();
                    }
                }
                Toast.makeText(this, "Monitoring request sent.", Toast.LENGTH_SHORT).show();
                try {
                    fetchNearbyPlaces(latitude, longitude);
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else {
                Toast.makeText(this, "Location not available. Cannot send monitoring request.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Opens the sidebar with an animation
    private void openSidebar() {
        if (sidebarLayout.getWidth() == 0) {
            sidebarLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    sidebarLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    animateSidebarOpen();
                }
            });
        } else {
            animateSidebarOpen();
        }
    }
    private void animateSidebarOpen() {
        sidebarLayout.setVisibility(View.VISIBLE);
        backgroundOverlay.setVisibility(View.VISIBLE);
        sidebarLayout.setTranslationX(sidebarLayout.getWidth());
        sidebarLayout.animate().translationX(0).setDuration(300).start();
    }

    // Closes the sidebar with an animation
    private void closeSidebar() {
        sidebarLayout.animate()
                .translationX(sidebarLayout.getWidth())
                .setDuration(300)
                .withEndAction(() -> {
                    sidebarLayout.setVisibility(View.GONE);
                    backgroundOverlay.setVisibility(View.GONE);
                }).start();
    }

    // Updates the styles of the selected sidebar button
    private void setSelectedButton(Button selectedButton) {
        resetButtonStyles();

        // Apply selected style
        selectedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_purple));
        selectedButton.setTextColor(ContextCompat.getColor(this, R.color.orange));

        isHomeSelected = selectedButton == btnHome;
    }

    // Resets the styles of all sidebar buttons to default
    private void resetButtonStyles() {
        Button[] buttons = {btnHome, btnSosContacts, btnSafetyTips, btnAboutUs, btnTermsConditions};
        for (Button button : buttons) {
            button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            button.setTextColor(ContextCompat.getColor(this, R.color.dark_grey));
        }
    }

    // Shows a logout confirmation dialog
    private void showLogoutDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog, null);

        AlertDialog customDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (customDialog.getWindow() != null) {
            customDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        customDialog.show();

        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        // Confirm logout
        btnYes.setOnClickListener(v -> {
            performLogout();
            customDialog.dismiss();
        });

        // Cancel logout
        btnNo.setOnClickListener(v -> customDialog.dismiss());
    }

    // Logs the user out and redirects to the login screen
    private void performLogout() {
        // Reset the logged-in state in SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("AlertifyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false); // Set login state to false
        editor.apply(); // Apply changes

        // Redirect to the LogIn screen
        Intent intent = new Intent(Home.this, LogIn.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Finish the current activity
    }

    // Handles slider movement for SOS activation
    private boolean handleSliderMovement(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                float x = event.getRawX();
                float parentStart = sliderInstruction.getX();
                float parentEnd = parentStart + sliderInstruction.getWidth();
                float maxTranslation = sliderInstruction.getWidth() - sliderButton.getWidth();

                if (x >= parentStart && x <= parentEnd) {
                    float translationX = x - parentStart - sliderButton.getWidth() / 2;
                    sliderButton.setTranslationX(Math.min(Math.max(translationX, 0), maxTranslation));
                }
                return true;
            case MotionEvent.ACTION_UP:
                float finalPosition = sliderButton.getTranslationX() + sliderButton.getWidth();
                if (finalPosition >= sliderInstruction.getWidth() * 0.85) {
                    lockSliderAtEnd();
                } else {
                    sliderButton.animate().translationX(0).setDuration(200).start();
                }
                return true;
        }
        return false;
    }

    // Locks the slider at the end position and starts the countdown
    private void lockSliderAtEnd() {
        sliderButton.setTranslationX(sliderInstruction.getWidth() - sliderButton.getWidth());
        sliderInstruction.setText("Click to abort - Sending SOS in 5...");
        sliderInstruction.setTextColor(getResources().getColor(R.color.dark_grey)); // Change text color
        isSliderActive = true;

        // Change colors when countdown starts
        View sosSlider = findViewById(R.id.sosSlider);
        sosSlider.setBackgroundResource(R.drawable.slider_bg_active);

        View sliderButton = findViewById(R.id.sliderButton);
        sliderButton.setBackgroundResource(R.drawable.slider_handle_active);

        ImageView sliderIcon = findViewById(R.id.sliderIcon);
        sliderIcon.setImageResource(R.drawable.slider_handle_arrow_active);

        // Allow slider to be clickable to cancel
        sosSlider.setOnClickListener(v -> cancelSOS());

        // Start countdown
        startCountdown(5);
    }

    // Start a countdown with the ability to cancel
    private void startCountdown(int seconds) {
        Handler handler = new Handler();
        Runnable countdownRunnable = new Runnable() {
            int timeLeft = seconds;

            @Override
            public void run() {
                if (!isSliderActive) return; // Exit if cancelled

                if (timeLeft > 0) {
                    sliderInstruction.setText("Click to abort (" + timeLeft + ")...");
                    timeLeft--;
                    handler.postDelayed(this, 1000);
                } else {
                    sendSosMessage(); // Only send if not cancelled
                    resetSliderWithDelay(2000);
                }
            }
        };

        handler.post(countdownRunnable);
    }

    // Cancels the SOS process and resets the slider
    private void cancelSOS() {
        isSliderActive = false; // Stop the countdown
        sliderInstruction.setText("Slide to Send SOS");
        sliderInstruction.setTextColor(getResources().getColor(R.color.primary_yellow)); // Reset text color

        // Reset the slider position
        sliderButton.animate().translationX(0).setDuration(200).start();

        // Reset colors when cancelled
        View sosSlider = findViewById(R.id.sosSlider);
        sosSlider.setBackgroundResource(R.drawable.slider_bg);

        View sliderButton = findViewById(R.id.sliderButton);
        sliderButton.setBackgroundResource(R.drawable.slider_handle);

        ImageView sliderIcon = findViewById(R.id.sliderIcon);
        sliderIcon.setImageResource(R.drawable.slider_handle_arrow);

        // Remove the click listener after cancellation
        sosSlider.setOnClickListener(null);

        // Show feedback
        Toast.makeText(this, "SOS cancelled", Toast.LENGTH_SHORT).show();
    }

    // Reset the slider to its initial position with a delay
    private void resetSliderWithDelay(int delay) {
        new Handler().postDelayed(() -> {
            sliderButton.animate().translationX(0).setDuration(200).start();
            sliderInstruction.setText("Slide to Send SOS");
            sliderInstruction.setTextColor(getResources().getColor(R.color.primary_yellow)); // Reset text color
            isSliderActive = false;

            // Reset colors to default when countdown ends
            View sosSlider = findViewById(R.id.sosSlider);
            sosSlider.setBackgroundResource(R.drawable.slider_bg);

            View sliderButton = findViewById(R.id.sliderButton);
            sliderButton.setBackgroundResource(R.drawable.slider_handle);

            ImageView sliderIcon = findViewById(R.id.sliderIcon);
            sliderIcon.setImageResource(R.drawable.slider_handle_arrow);

            // Remove the click listener after reset
            sosSlider.setOnClickListener(null);
        }, delay);
    }

    // Sends an SOS message to pinned contacts
    private void sendSosMessage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission not granted. Cannot send SOS.", Toast.LENGTH_SHORT).show();
            return;
        }
        getCurrentLocation(location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                String locationUrl = "https://www.google.com/maps?q=" + latitude + "," + longitude;
                
                // Get user info from SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("AlertifyPrefs", MODE_PRIVATE);
                String userEmail = sharedPreferences.getString("userEmail", "");
                String userName = sharedPreferences.getString("userName", "Unknown User");
                int userId = sharedPreferences.getInt("userId", 0);
                
                // Get emergency contacts
                SQLiteDatabase database = contactDatabaseHelper.getReadableDatabase();
                Cursor cursor = database.query(
                        "contacts",
                        new String[]{"name", "number"},
                        "isPinned = ?",
                        new String[]{"1"},
                        null, null, "pinned_order ASC"
                );
                
                // Build emergency contacts string
                StringBuilder emergencyContacts = new StringBuilder();
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    String number = cursor.getString(cursor.getColumnIndexOrThrow("number"));
                    if (number != null && !number.isEmpty()) {
                        if (emergencyContacts.length() > 0) {
                            emergencyContacts.append(",");
                        }
                        emergencyContacts.append(name).append(":").append(number);
                    }
                }
                cursor.close();
                
                // Save SOS alert to database for staff
                long localAlertId = sosAlertDbHelper.addSosAlert(
                    userId, 
                    userName, 
                    userEmail, 
                    latitude, 
                    longitude, 
                    emergencyContacts.toString()
                );
                
                if (localAlertId != -1) {
                    Log.d("SOSApp", "SOS Alert saved to local database with ID: " + localAlertId);
                    
                    // Lấy alert vừa tạo và gửi đến Firebase
                    List<SosAlertDatabaseHelper.SosAlert> unsyncedAlerts = sosAlertDbHelper.getUnsyncedAlerts();
                    for (SosAlertDatabaseHelper.SosAlert alert : unsyncedAlerts) {
                        if (alert.getId() == localAlertId) {
                            // Gửi alert lên Firebase
                            firebaseManager.addSosAlert(alert);
                            break;
                        }
                    }
                } else {
                    Log.e("SOSApp", "Failed to save SOS Alert to local database");
                }
                
                // Send SMS to emergency contacts
                SmsManager smsManager = SmsManager.getDefault();
                cursor = database.query(
                        "contacts",
                        new String[]{"name", "number"},
                        "isPinned = ?",
                        new String[]{"1"},
                        null, null, "pinned_order ASC"
                );
                
                while (cursor.moveToNext()) {
                    String number = cursor.getString(cursor.getColumnIndexOrThrow("number"));
                    if (number != null && !number.isEmpty()) {
                        String message = "SOS! I need help. Here's my location: " + locationUrl;
                        try {
                            String normalizedNumber = normalizePhoneNumber(number);
                            smsManager.sendTextMessage(normalizedNumber, null, message, null, null);
                            Log.d("SOSApp", "Contact number: " + normalizedNumber);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed to send SMS to " + number, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                cursor.close();
                Toast.makeText(this, "SOS sent with location!", Toast.LENGTH_SHORT).show();
                try {
                    fetchNearbyPlaces(latitude, longitude);
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else {
                Toast.makeText(this, "Location not available. SOS sent without location.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Checks and requests SMS permissions if not already granted
    private void checkAndRequestPermissions() {
        // List of permissions to request
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.SEND_SMS
        };

        // Check if any of the permissions are not granted
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        // Request all necessary permissions
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            boolean locationGranted = false;
            boolean smsGranted = false;

            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    locationGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                }
                if (permissions[i].equals(Manifest.permission.SEND_SMS)) {
                    smsGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                }
            }

            // Handle location permission
            if (locationGranted) {
                setupMap();
            } else {
                Toast.makeText(this, "Location permission is required for maps.", Toast.LENGTH_SHORT).show();
            }

            // Handle SMS permission
            if (!smsGranted) {
                Toast.makeText(this, "SMS permission is required to send alerts.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Hàm lấy vị trí hiện tại, fallback sang requestLocationUpdates nếu getLastLocation trả về null
    private void getCurrentLocation(LocationResultListener listener) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listener.onLocationResult(null);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                listener.onLocationResult(location);
            } else {
                LocationRequest locationRequest = LocationRequest.create()
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setInterval(1000)
                        .setNumUpdates(1);
                locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                            listener.onLocationResult(locationResult.getLastLocation());
                        } else {
                            listener.onLocationResult(null);
                        }
                        fusedLocationClient.removeLocationUpdates(this);
                    }
                };
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            }
        });
    }

    // Contact class to store information about contacts
    public class Contact {
        private String name;
        private String number;

        public Contact(String name, String number) {
            this.name = name;
            this.number = number;
        }

        public String getName() {
            return name;
        }

        public String getNumber() {
            return number;
        }
    }

    private String normalizePhoneNumber(String number) {
        number = number.replaceAll("[^\\d+]", ""); // Loại bỏ ký tự không phải số hoặc +
        if (number.startsWith("0")) {
            number = "+84" + number.substring(1);
        }
        return number;
    }
}