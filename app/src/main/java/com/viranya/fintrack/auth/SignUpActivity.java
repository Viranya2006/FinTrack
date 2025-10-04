package com.viranya.fintrack.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.viranya.fintrack.HomeActivity;
import com.viranya.fintrack.R;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    // --- UI Elements ---
    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private ImageButton btnGoogleSignUp;
    private TextView tvGoToLogin;

    // --- Firebase & Google Services ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // --- Initialize Firebase and Firestore ---
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- Bind UI elements from the XML layout ---
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmailSignUp);
        etPassword = findViewById(R.id.etPasswordSignUp);
        etConfirmPassword = findViewById(R.id.etConfirmPasswordSignUp);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // --- Configure and initialize Google Sign In (same as LoginActivity) ---
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // --- Set up Click Listeners ---
        btnSignUp.setOnClickListener(v -> createAccount());
        btnGoogleSignUp.setOnClickListener(v -> signInWithGoogle());
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
    }

    /**
     * Handles the email/password account creation flow.
     */
    private void createAccount() {
        // 1. Get user input from all fields.
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 2. Validate that all fields are filled.
        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Check if the entered passwords match.
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Use Firebase Auth to create a new user account.
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // 5. If the user is created successfully in Auth, save their details to Firestore.
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveUserData(user, fullName, email);
                    } else {
                        // If creation fails, show a detailed error message.
                        Toast.makeText(SignUpActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Authenticates with Firebase using a Google ID token.
     * This method is identical to the one in LoginActivity.
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                        if (isNewUser) {
                            // Since this is the sign-up screen, a Google sign-in will always be a new user.
                            // We save their data from their Google profile.
                            saveUserData(user, user.getDisplayName(), user.getEmail());
                        } else {
                            // This case is unlikely on the sign-up screen, but for safety,
                            // we navigate an existing user to the home screen.
                            startActivity(new Intent(SignUpActivity.this, HomeActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Firebase Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Starts the Google Sign In flow.
     * This method is identical to the one in LoginActivity.
     */
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    /**
     * Saves a new user's data to the 'users' collection in Firestore.
     * This method is identical to the one in LoginActivity.
     */
    private void saveUserData(FirebaseUser firebaseUser, String fullName, String email) {
        String userId = firebaseUser.getUid();
        Map<String, Object> user = new HashMap<>();
        user.put("name", fullName);
        user.put("email", email);
        user.put("currency", "LKR");

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignUpActivity.this, "Account created.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignUpActivity.this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignUpActivity.this, "Error saving user data.", Toast.LENGTH_SHORT).show();
                });
    }
}