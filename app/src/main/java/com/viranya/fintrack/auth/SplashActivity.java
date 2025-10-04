package com.viranya.fintrack.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.viranya.fintrack.AppLockActivity;
import com.viranya.fintrack.HomeActivity;
import com.viranya.fintrack.R;
import com.viranya.fintrack.fragment.ProfileFragment;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2-second delay for branding

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // A Handler is used to delay the execution of the navigation logic,
        // allowing the splash screen to be visible for a short period.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // --- 1. Check if App Lock is Enabled ---
            SharedPreferences sharedPreferences = getSharedPreferences(ProfileFragment.APP_PREFERENCES, MODE_PRIVATE);
            boolean isAppLockEnabled = sharedPreferences.getBoolean(ProfileFragment.IS_APP_LOCK_ENABLED, false);

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            // --- 2. Decide the Next Screen ---
            if (currentUser != null && isAppLockEnabled) {
                // CASE A: User is logged in AND app lock is on.
                // Go to the AppLockActivity first.
                startActivity(new Intent(SplashActivity.this, AppLockActivity.class));
            } else if (currentUser != null) {
                // CASE B: User is logged in BUT app lock is off.
                // Go directly to the HomeActivity.
                startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            } else {
                // CASE C: User is not logged in.
                // Go to the LoginActivity.
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }

            // Finish the SplashActivity so the user cannot navigate back to it.
            finish();

        }, SPLASH_DELAY);
    }
}