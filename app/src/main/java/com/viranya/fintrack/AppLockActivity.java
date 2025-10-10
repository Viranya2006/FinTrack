package com.viranya.fintrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

/**
 * This Activity acts as a security gate for the app.
 * It uses the modern BiometricPrompt API, which automatically handles fingerprint, face,
 * and the device's PIN/pattern/password as a fallback.
 */
public class AppLockActivity extends AppCompatActivity {

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock);

        ImageButton btnAuthenticate = findViewById(R.id.btn_authenticate);

        // --- 1. Initialize Biometric Components ---
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            // This callback handles the results of the authentication attempt.

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                // This is called for unrecoverable errors, like the user canceling the prompt.
                // We close the app to prevent access.
                Toast.makeText(getApplicationContext(), "Authentication required.", Toast.LENGTH_SHORT).show();
                finishAffinity();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // Authentication was successful (either by biometric or device PIN).
                proceedToApp();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                // This is called when a biometric is not recognized (e.g., wrong fingerprint).
                Toast.makeText(getApplicationContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
            }
        });

        // --- 2. Configure the Prompt Dialog ---
        // This is where we build the dialog that the user sees.
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("FinTrack is Locked")
                .setSubtitle("Unlock using your biometric or device credential")
                // --- THIS IS THE KEY CHANGE ---
                // This allows the user to use their phone's PIN, pattern, or password if
                // biometrics fail or if they choose to use the fallback option.
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        // Set a listener on the button as a manual way to trigger the prompt.
        btnAuthenticate.setOnClickListener(v -> showBiometricPrompt());

        // Automatically show the prompt when the activity starts.
        showBiometricPrompt();
    }

    /**
     * Checks if the device can authenticate and then shows the prompt.
     */
    private void showBiometricPrompt() {
        biometricPrompt.authenticate(promptInfo);
    }

    /**
     * Navigates to the main part of the app after successful authentication.
     */
    private void proceedToApp() {
        Toast.makeText(getApplicationContext(), "Unlocked!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(AppLockActivity.this, HomeActivity.class));
        finish(); // Finish the lock screen so the user can't go back to it.
    }
}