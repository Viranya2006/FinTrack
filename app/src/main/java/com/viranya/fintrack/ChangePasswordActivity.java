package com.viranya.fintrack;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Pattern;

/**
 * This activity allows a logged-in user to change their password after verifying their current password.
 * It also enforces a strong password policy.
 */
public class ChangePasswordActivity extends AppCompatActivity {

    private static final String TAG = "ChangePasswordActivity";

    // --- UI Elements ---
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private Button btnUpdatePassword;

    // --- Firebase Services ---
    private FirebaseAuth mAuth;

    // --- Password Validation Pattern ---
    // This pattern checks for:
    // - at least 8 characters
    // - at least one number
    // - at least one uppercase letter
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    "(?=.*[0-9])" +         // at least 1 digit
                    "(?=.*[A-Z])" +         // at least 1 upper case letter
                    ".{8,}" +               // at least 8 characters
                    "$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Bind UI elements
        etCurrentPassword = findViewById(R.id.et_current_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnUpdatePassword = findViewById(R.id.btn_update_password);

        // Set the click listener for the update button
        btnUpdatePassword.setOnClickListener(v -> changePassword());
    }

    /**
     * The main method to handle the password change process.
     */
    private void changePassword() {
        // Get the current user from Firebase
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the text from all input fields
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // --- 1. Validate User Input ---
        if (TextUtils.isEmpty(currentPassword)) {
            etCurrentPassword.setError("Current password is required.");
            etCurrentPassword.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("New password is required.");
            etNewPassword.requestFocus();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match.");
            etConfirmPassword.requestFocus();
            return;
        }

        // --- 2. Validate New Password Strength ---
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            etNewPassword.setError("Password must be at least 8 chars, include an uppercase letter and a number.");
            etNewPassword.requestFocus();
            return;
        }

        // --- 3. Re-authenticate the User (Security Check) ---
        // This is a crucial step to ensure it's the real user changing the password.
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Re-authentication successful, now we can update the password.
                    Log.d(TAG, "User re-authenticated successfully.");

                    // --- 4. Update the Password in Firebase Auth ---
                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(ChangePasswordActivity.this, "Password updated successfully!", Toast.LENGTH_LONG).show();
                                finish(); // Close the activity on success
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ChangePasswordActivity.this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Password update failed", e);
                            });
                })
                .addOnFailureListener(e -> {
                    // Re-authentication failed, likely due to an incorrect current password.
                    etCurrentPassword.setError("Current password is incorrect.");
                    etCurrentPassword.requestFocus();
                    Log.w(TAG, "User re-authentication failed", e);
                });
    }
}