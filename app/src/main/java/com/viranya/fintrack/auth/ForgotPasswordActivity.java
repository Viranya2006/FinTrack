package com.viranya.fintrack.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.viranya.fintrack.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    // --- UI Elements ---
    private EditText etEmailForgot;
    private Button btnSendResetLink;
    private TextView tvBackToLogin;

    // --- Firebase ---
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // --- Initialize Firebase ---
        mAuth = FirebaseAuth.getInstance();

        // --- Bind UI elements ---
        etEmailForgot = findViewById(R.id.etEmailForgot);
        btnSendResetLink = findViewById(R.id.btnSendResetLink);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // --- Set up Click Listeners ---
        btnSendResetLink.setOnClickListener(v -> sendResetLink());
        tvBackToLogin.setOnClickListener(v -> finish()); // Go back to the previous screen (Login)
    }

    /**
     * Handles the password reset email flow.
     */
    private void sendResetLink() {
        String email = etEmailForgot.getText().toString().trim();

        // 1. Validate the email input.
        if (TextUtils.isEmpty(email)) {
            etEmailForgot.setError("Email is required.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailForgot.setError("Please enter a valid email.");
            return;
        }

        // 2. Use Firebase Auth to send the password reset email.
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // If the email is sent, inform the user and close the screen.
                        Toast.makeText(ForgotPasswordActivity.this, "Password reset link sent to your email.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        // If it fails (e.g., email not found), show an error.
                        Toast.makeText(ForgotPasswordActivity.this, "Failed to send reset link. Please check the email address.", Toast.LENGTH_LONG).show();
                    }
                });
    }
}