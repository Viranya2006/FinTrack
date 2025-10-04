package com.viranya.fintrack.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.viranya.fintrack.R;
import com.viranya.fintrack.model.Account;
import com.viranya.fintrack.model.Transaction;

import java.util.Date;

public class AddAccountBottomSheetFragment extends BottomSheetDialogFragment {

    // --- UI Elements ---
    private TextInputEditText etAccountName, etInitialBalance;
    private Button btnSaveAccount;

    // --- Services ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase services
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Bind UI elements from the layout
        etAccountName = view.findViewById(R.id.et_account_name);
        etInitialBalance = view.findViewById(R.id.et_initial_balance);
        btnSaveAccount = view.findViewById(R.id.btn_save_account);

        // Set the listener for the save button
        btnSaveAccount.setOnClickListener(v -> saveAccount());
    }

    /**
     * Saves the new account to Firestore and creates an initial balance transaction.
     */
    private void saveAccount() {
        // Get user input
        String accountName = etAccountName.getText().toString().trim();
        String balanceStr = etInitialBalance.getText().toString().trim();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Validate input
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(accountName)) {
            etAccountName.setError("Account name cannot be empty.");
            return;
        }

        double initialBalance = 0;
        if (!TextUtils.isEmpty(balanceStr)) {
            initialBalance = Double.parseDouble(balanceStr);
        }

        String userId = currentUser.getUid();

        // Create a final copy for use in the background listener
        final double finalInitialBalance = initialBalance;

        // Step 1: Create the new Account object
        Account newAccount = new Account(accountName, finalInitialBalance);

        // Save the account to the 'accounts' sub-collection
        db.collection("users").document(userId).collection("accounts")
                .document(accountName) // Use the name as a unique ID
                .set(newAccount)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Account '" + accountName + "' created!", Toast.LENGTH_SHORT).show();

                    // Step 2: If there's an initial balance, create an "Income" transaction for it.
                    // This ensures the dashboard totals are immediately correct.
                    if (finalInitialBalance > 0) {
                        String title = "Initial Balance for " + accountName;
                        Transaction initialTransaction = new Transaction(title, "Initial Balance", finalInitialBalance, "Income", accountName, new Date());

                        db.collection("users").document(userId).collection("transactions")
                                .add(initialTransaction);
                    }

                    // Close the bottom sheet on success
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error creating account.", Toast.LENGTH_SHORT).show();
                });
    }
}

