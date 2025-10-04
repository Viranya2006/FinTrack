package com.viranya.fintrack.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.viranya.fintrack.AccountsActivity;
import com.viranya.fintrack.ChangePasswordActivity;
import com.viranya.fintrack.R;
import com.viranya.fintrack.auth.LoginActivity;
import com.viranya.fintrack.model.Transaction;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The ProfileFragment is responsible for displaying user settings and account management options.
 * It handles logging out, changing passwords, managing app lock, exporting data, and deleting the account.
 */
public class ProfileFragment extends Fragment {

    // --- Constants for SharedPreferences ---
    public static final String APP_PREFERENCES = "FinTrackPrefs";
    public static final String IS_APP_LOCK_ENABLED = "isAppLockEnabled";

    // --- UI Elements ---
    private TextView tvUserName, tvUserEmail, tvChangePassword, tvExportData, tvDeleteAccount, tvManageAccounts;
    private SwitchMaterial switchAppLock;
    private Button btnLogout;

    // --- Services ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;

    // --- ActivityResultLauncher for Export Data ---
    // This is the modern, recommended way to handle file creation. It opens a system file picker.
    private final ActivityResultLauncher<String> createFileLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri -> {
                if (uri != null) {
                    // If the user selects a location, this callback is triggered, and we write the data to the chosen file.
                    exportTransactionsToCSV(uri);
                } else {
                    // This is triggered if the user cancels the file picker.
                    Toast.makeText(getContext(), "Export canceled.", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Just inflate the XML layout. All logic is handled in onViewCreated.
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // This structured approach ensures all components are initialized in the correct order.
        initializeServices();
        bindViews(view);
        loadData();
        setupListeners();
    }

    /**
     * Initializes all necessary services like Firebase and SharedPreferences.
     */
    private void initializeServices() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // `requireActivity()` is a safe way to get the context, ensuring the fragment is attached.
        sharedPreferences = requireActivity().getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    /**
     * Binds all UI elements from the XML layout to their corresponding Java variables.
     * @param view The fragment's root view.
     */
    private void bindViews(View view) {
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        btnLogout = view.findViewById(R.id.btn_logout);
        tvChangePassword = view.findViewById(R.id.tv_change_password);
        switchAppLock = view.findViewById(R.id.switch_app_lock);
        tvExportData = view.findViewById(R.id.tv_export_data);
        tvDeleteAccount = view.findViewById(R.id.tv_delete_account);
        tvManageAccounts = view.findViewById(R.id.tv_manage_accounts);
    }

    /**
     * Loads dynamic data into the UI, such as user info and saved settings.
     */
    private void loadData() {
        // Load user name and email from Firebase
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            tvUserEmail.setText(currentUser.getEmail());

            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        // This check prevents a crash if the data returns after the user has left the screen.
                        if (isAdded() && documentSnapshot.exists() && documentSnapshot.getString("name") != null) {
                            tvUserName.setText(documentSnapshot.getString("name"));
                        }
                    });
        }

        // Load the saved state of the app lock switch from the device's storage
        boolean isAppLockEnabled = sharedPreferences.getBoolean(IS_APP_LOCK_ENABLED, false);
        switchAppLock.setChecked(isAppLockEnabled);
    }

    /**
     * Sets up all the click listeners for the interactive elements on the screen.
     */
    private void setupListeners() {
        btnLogout.setOnClickListener(v -> logoutUser());
        tvChangePassword.setOnClickListener(v -> startActivity(new Intent(requireActivity(), ChangePasswordActivity.class)));
        tvManageAccounts.setOnClickListener(v -> startActivity(new Intent(requireActivity(), AccountsActivity.class)));
        tvExportData.setOnClickListener(v -> handleExportClick());
        tvDeleteAccount.setOnClickListener(v -> showDeleteConfirmationDialog());

        switchAppLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(IS_APP_LOCK_ENABLED, isChecked).apply();
            Toast.makeText(getContext(), isChecked ? "App Lock Enabled" : "App Lock Disabled", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Handles the click on the "Export Data" button by launching the system file picker.
     */
    private void handleExportClick() {
        String fileName = "FinTrack_Export_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".csv";
        createFileLauncher.launch(fileName);
    }

    /**
     * Fetches all transactions and writes them to the file Uri provided by the system file picker.
     * @param uri The Uri of the file the user chose to save.
     */
    private void exportTransactionsToCSV(Uri uri) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).collection("transactions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return; // Safety check
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "No transactions to export.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Build the CSV content as a string
                    StringBuilder csvContent = new StringBuilder("Date,Type,Category,Title,Amount\n");
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction transaction = doc.toObject(Transaction.class);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        String dateStr = sdf.format(transaction.getDate());
                        // Format the line, escaping any quotes in the title
                        csvContent.append(String.format(Locale.US, "\"%s\",\"%s\",\"%s\",\"%s\",%.2f\n",
                                dateStr, transaction.getType(), transaction.getCategory(),
                                transaction.getTitle().replace("\"", "\"\""),
                                transaction.getAmount()));
                    }

                    // Write the string to the file using the modern Scoped Storage method
                    try (OutputStream outputStream = requireActivity().getContentResolver().openOutputStream(uri);
                         OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
                        writer.write(csvContent.toString());
                        Toast.makeText(getContext(), "Data exported successfully.", Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Toast.makeText(getContext(), "Failed to export data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to fetch transactions.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Shows a confirmation dialog to the user before deleting their account.
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account and all of your data? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes all user data from Firestore and then deletes the user's authentication account.
     */
    private void deleteUserAccount() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        // Step 1: Delete all sub-collections (transactions, budgets, etc.)
        deleteCollection(db.collection("users").document(userId).collection("transactions"), () ->
                deleteCollection(db.collection("users").document(userId).collection("budgets"), () ->
                        deleteCollection(db.collection("users").document(userId).collection("saving_goals"), () ->
                                deleteCollection(db.collection("users").document(userId).collection("accounts"), () -> {

                                    // Step 2: After all data is deleted, delete the main user document
                                    db.collection("users").document(userId).delete().addOnSuccessListener(aVoid -> {

                                        // Step 3: After the user document is deleted, delete the authentication account
                                        currentUser.delete().addOnCompleteListener(task -> {
                                            if (isAdded()) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(getContext(), "Account deleted successfully.", Toast.LENGTH_LONG).show();
                                                    logoutUser(); // Log out and go to the login screen
                                                } else {
                                                    Toast.makeText(getContext(), "Failed to delete account. Please log in again and retry.", Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                                    });
                                }))));
    }

    /**
     * A helper function to delete all documents within a Firestore collection.
     * @param collection The collection to delete.
     * @param onComplete A callback to run after the deletion is complete.
     */
    private void deleteCollection(CollectionReference collection, final Runnable onComplete) {
        collection.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) {
                onComplete.run();
                return;
            }
            WriteBatch batch = db.batch();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                batch.delete(doc.getReference());
            }
            batch.commit().addOnSuccessListener(aVoid -> onComplete.run());
        });
    }

    /**
     * Signs the user out of Firebase and navigates to the Login screen.
     */
    private void logoutUser() {
        mAuth.signOut();
        if (getActivity() != null) {
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
}

