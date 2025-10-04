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

public class ProfileFragment extends Fragment {

    // --- Constants for SharedPreferences ---
    public static final String APP_PREFERENCES = "FinTrackPrefs";
    public static final String IS_APP_LOCK_ENABLED = "isAppLockEnabled";

    // --- UI Elements ---
    private TextView tvUserName, tvUserEmail, tvChangePassword, tvExportData, tvDeleteAccount;
    private SwitchMaterial switchAppLock;
    private Button btnLogout;

    // --- Services ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;

    // --- NEW: ActivityResultLauncher for creating a file ---
    private final ActivityResultLauncher<String> createFileLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri -> {
                if (uri != null) {
                    // The user has selected a location and file name. Now, we write the data.
                    exportTransactionsToCSV(uri);
                } else {
                    // User canceled the file picker
                    Toast.makeText(getContext(), "Export canceled.", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeServices();
        bindViews(view);
        loadData();
        setupListeners();
    }

    // --- Initialization Methods ---

    private void initializeServices() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = requireActivity().getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    private void bindViews(View view) {
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        btnLogout = view.findViewById(R.id.btn_logout);
        tvChangePassword = view.findViewById(R.id.tv_change_password);
        switchAppLock = view.findViewById(R.id.switch_app_lock);
        tvExportData = view.findViewById(R.id.tv_export_data);
        tvDeleteAccount = view.findViewById(R.id.tv_delete_account);
    }


    private void loadData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            tvUserEmail.setText(currentUser.getEmail());
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (isAdded() && documentSnapshot.exists() && documentSnapshot.getString("name") != null) {
                            tvUserName.setText(documentSnapshot.getString("name"));
                        }
                    });
        }
        boolean isAppLockEnabled = sharedPreferences.getBoolean(IS_APP_LOCK_ENABLED, false);
        switchAppLock.setChecked(isAppLockEnabled);
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> logoutUser());
        tvChangePassword.setOnClickListener(v -> startActivity(new Intent(requireActivity(), ChangePasswordActivity.class)));
        switchAppLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(IS_APP_LOCK_ENABLED, isChecked).apply();
            Toast.makeText(getContext(), isChecked ? "App Lock Enabled" : "App Lock Disabled", Toast.LENGTH_SHORT).show();
        });

        // --- UPDATED: The click now launches the system file picker ---
        tvExportData.setOnClickListener(v -> handleExportClick());
        tvDeleteAccount.setOnClickListener(v -> showDeleteConfirmationDialog());
    }
    // --- Feature Implementations ---

    private void handleExportClick() {
        // Create a default file name
        String fileName = "FinTrack_Export_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".csv";
        // Launch the file picker. The user can change the name if they want.
        createFileLauncher.launch(fileName);
    }
    /**
     * Fetches transactions and writes them to the Uri provided by the file picker.
     */
    private void exportTransactionsToCSV(Uri uri) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).collection("transactions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "No transactions to export.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    StringBuilder csvContent = new StringBuilder("Date,Type,Category,Title,Amount\n");
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction transaction = doc.toObject(Transaction.class);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        String dateStr = sdf.format(transaction.getDate());
                        csvContent.append(String.format(Locale.US, "\"%s\",\"%s\",\"%s\",\"%s\",%.2f\n",
                                dateStr, transaction.getType(), transaction.getCategory(),
                                transaction.getTitle().replace("\"", "\"\""), // Escape quotes in title
                                transaction.getAmount()));
                    }

                    // --- UPDATED: Write to the Uri's OutputStream ---
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


    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure? All your data will be permanently lost.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteUserAccount() {
        final FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        final String userId = currentUser.getUid();

        deleteCollection(db.collection("users").document(userId).collection("transactions"), () ->
                deleteCollection(db.collection("users").document(userId).collection("budgets"), () ->
                        deleteCollection(db.collection("users").document(userId).collection("saving_goals"), () -> {
                            db.collection("users").document(userId).delete()
                                    .addOnSuccessListener(aVoid -> currentUser.delete()
                                            .addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(getContext(), "Account deleted successfully.", Toast.LENGTH_LONG).show();
                                                    logoutUser();
                                                } else {
                                                    Toast.makeText(getContext(), "Failed to delete account. Please log in again and retry.", Toast.LENGTH_LONG).show();
                                                }
                                            }));
                        })
                )
        );
    }

    private void deleteCollection(CollectionReference collection, final Runnable onComplete) {
        collection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WriteBatch batch = db.batch();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    batch.delete(document.getReference());
                }
                batch.commit().addOnCompleteListener(batchTask -> onComplete.run());
            } else {
                onComplete.run();
            }
        });
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

