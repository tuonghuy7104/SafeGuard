package com.example.safeguard;

import android.os.Bundle;
import android.widget.ImageButton;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutUs extends AppCompatActivity {
    private TextView checkUpdatesTitle;
    private TextView checkUpdatesContent;
    private TextView aboutTitle;
    private ImageView checkUpdateIcon;
    private ImageView aboutIcon;
    private LinearLayout aboutContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        checkUpdatesTitle = findViewById(R.id.check_updates_title);
        checkUpdatesContent = findViewById(R.id.check_updates_content);
        aboutTitle = findViewById(R.id.about_title);
        aboutContent = findViewById(R.id.about_content);
        checkUpdateIcon = findViewById(R.id.check_updates_icon);
        aboutIcon = findViewById(R.id.about_icon);

        checkUpdatesContent.setVisibility(View.GONE);
        aboutContent.setVisibility(View.GONE);

        // Accordion: Check for Updates
        checkUpdatesTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVisibility(checkUpdatesContent);
            }
        });

        // Accordion: About
        aboutTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVisibility(aboutContent);
            }
        });

        checkUpdateIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVisibility(checkUpdatesContent);
            }
        });

        aboutIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVisibility(aboutContent);
            }
        });

        // Back button functionality
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

    private void toggleVisibility(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
        }
    }


}
