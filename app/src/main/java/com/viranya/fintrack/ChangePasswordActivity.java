package com.viranya.fintrack;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText etNewPassword, etConfirmNewPassword;
    private Button btnUpdatePassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();

        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmNewPassword = findViewById(R.id.et_confirm_new_password);
        btnUpdatePassword = findViewById(R.id.btn_update_password);

        btnUpdatePassword.setOnClickListener(v -> updatePassword());
    }

    private void updatePassword() {
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmNewPassword.getText().toString().trim();

        // --- 1. Validate Input ---
        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 6) {
            etNewPassword.setError("Password must be at least 6 characters.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            etConfirmNewPassword.setError("Passwords do not match.");
            return;
        }

        // --- 2. Get Current User and Update Password ---
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ChangePasswordActivity.this, "Password updated successfully.", Toast.LENGTH_SHORT).show();
                            finish(); // Go back to the profile screen
                        } else {
                            // This can fail if the user's login session is old.
                            // Firebase requires recent authentication for security.
                            Toast.makeText(ChangePasswordActivity.this, "Failed to update password. Please log out and log in again.", Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }
}