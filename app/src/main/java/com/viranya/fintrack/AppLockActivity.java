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
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                // Handle unrecoverable errors, like sensor not available
                Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                finishAffinity(); // Close the app
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // Authentication was successful, proceed to the main app
                Toast.makeText(getApplicationContext(), "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(AppLockActivity.this, HomeActivity.class));
                finish(); // Finish this lock screen activity
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                // User's fingerprint was not recognized
                Toast.makeText(getApplicationContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
            }
        });

        // --- 2. Configure the Prompt Dialog ---
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Lock")
                .setSubtitle("Unlock FinTrack using your fingerprint or face")
                .setNegativeButtonText("Cancel")
                .build();

        // --- 3. Set Click Listener and Show Prompt ---
        btnAuthenticate.setOnClickListener(v -> showBiometricPrompt());

        // --- 4. Automatically show the prompt on start ---
        showBiometricPrompt();
    }

    private void showBiometricPrompt() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo);
        } else {
            // No biometrics available on this device
            Toast.makeText(this, "Biometric authentication is not available on this device.", Toast.LENGTH_LONG).show();
            // In a real app, you might fall back to a PIN here. For now, we'll just allow access.
            startActivity(new Intent(AppLockActivity.this, HomeActivity.class));
            finish();
        }
    }
}