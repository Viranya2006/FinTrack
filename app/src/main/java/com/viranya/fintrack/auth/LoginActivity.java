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

public class LoginActivity extends AppCompatActivity {

    // --- UI Elements ---
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ImageButton btnGoogleLogin;
    private TextView tvGoToSignUp, tvForgotPassword;
    // --- Firebase & Google Services ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // --- Initialize Firebase and Firestore ---
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- Bind UI elements from the XML layout ---
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        tvGoToSignUp = findViewById(R.id.tvGoToSignUp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // --- Configure Google Sign In ---
        // We configure the sign-in to request the user's ID token and basic profile.
        // The ID token is essential for authenticating with the Firebase backend.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // --- Initialize the launcher for the Google Sign In result ---
        // This modern approach replaces the deprecated onActivityResult method.
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // This callback is executed when the Google Sign-In activity returns a result.
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            // Google Sign In was successful, authenticate with Firebase
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            // Google Sign In failed
                            Toast.makeText(this, "Google sign in failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // --- Set up Click Listeners ---
        btnLogin.setOnClickListener(v -> loginUser());
        btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());
        tvGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            finish();
        });
        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
    }

    /**
     * Handles the standard email and password login flow.
     */
    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 1. Validate that the fields are not empty.
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Email and password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Use Firebase Auth to sign in the user.
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // If login is successful, navigate to the main part of the app.
                        Toast.makeText(LoginActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        finish();
                    } else {
                        // If login fails, display a message to the user.
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Starts the Google Sign In flow by launching the intent from the Google Sign In Client.
     */
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    /**
     * Authenticates the user with Firebase using the Google ID token.
     * @param idToken The Google ID token obtained from the Google Sign In intent.
     */
    private void firebaseAuthWithGoogle(String idToken) {
        // 1. Get the credential from the Google ID token.
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        // 2. Use the credential to sign in to Firebase.
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        // 3. Check if this is a brand new user.
                        // If so, we need to save their info to our Firestore database.
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                        if (isNewUser) {
                            saveUserData(user, user.getDisplayName(), user.getEmail());
                        } else {
                            // If it's an existing user, just navigate to the Home screen.
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Firebase Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Saves a new user's data to the 'users' collection in Firestore.
     */
    private void saveUserData(FirebaseUser firebaseUser, String fullName, String email) {
        String userId = firebaseUser.getUid();
        Map<String, Object> user = new HashMap<>();
        user.put("name", fullName);
        user.put("email", email);
        user.put("currency", "LKR"); // Default currency for new users

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    // Once data is saved, navigate to the Home screen.
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Error saving user data.", Toast.LENGTH_SHORT).show());
    }
}