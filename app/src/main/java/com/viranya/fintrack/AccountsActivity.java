package com.viranya.fintrack;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.viranya.fintrack.adapter.AccountAdapter;
import com.viranya.fintrack.fragment.AddAccountBottomSheetFragment;
import com.viranya.fintrack.model.Account;

import java.util.ArrayList;
import java.util.List;

// Implement the adapter's listener interface
public class AccountsActivity extends AppCompatActivity implements AccountAdapter.OnAccountListener {

    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private TextView emptyTextView;

    private AccountAdapter adapter;
    private List<Account> accountList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.rv_accounts);
        fab = findViewById(R.id.fab_add_account);
        emptyTextView = findViewById(R.id.tv_empty_accounts);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        accountList = new ArrayList<>();
        // Pass 'this' as the listener
        adapter = new AccountAdapter(accountList, this, this);
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v -> {
            AddAccountBottomSheetFragment bottomSheet = new AddAccountBottomSheetFragment();
            bottomSheet.show(getSupportFragmentManager(), "AddAccountBottomSheet");
        });

        fetchAccounts();
    }

    private void fetchAccounts() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("accounts")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error fetching accounts.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    accountList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Account account = doc.toObject(Account.class);
                        account.setDocumentId(doc.getId()); // Store the document ID
                        accountList.add(account);
                    }
                    adapter.notifyDataSetChanged();
                    checkIfEmpty();
                });
    }

    private void checkIfEmpty() {
        if (accountList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.GONE);
        }
    }

    /**
     * This method is called from the adapter when an account is long-pressed.
     */
    @Override
    public void onAccountLongClick(Account account) {
        // Show a dialog with "Edit" and "Delete" options
        final CharSequence[] options = {"Edit", "Delete", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Account: " + account.getName());
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Edit")) {
                showEditAccountDialog(account);
            } else if (options[item].equals("Delete")) {
                showDeleteAccountDialog(account);
            } else if (options[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    /**
     * Shows a dialog to edit an account's name and balance.
     */
    private void showEditAccountDialog(Account account) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_account, null);
        builder.setView(dialogView);

        final EditText etAccountName = dialogView.findViewById(R.id.et_edit_account_name);
        final EditText etAccountBalance = dialogView.findViewById(R.id.et_edit_account_balance);

        etAccountName.setText(account.getName());
        etAccountBalance.setText(String.valueOf(account.getBalance()));

        builder.setTitle("Edit Account")
                .setPositiveButton("Save", (dialog, id) -> {
                    String newName = etAccountName.getText().toString();
                    String newBalanceStr = etAccountBalance.getText().toString();
                    if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newBalanceStr)) {
                        Toast.makeText(this, "Fields cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // For simplicity, this example doesn't handle re-assigning transactions.
                    // We will just update the name and balance.
                    updateAccount(account, newName, Double.parseDouble(newBalanceStr));
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());
        builder.create().show();
    }

    /**
     * Shows a confirmation dialog before deleting an account.
     */
    private void showDeleteAccountDialog(Account account) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete the '" + account.getName() + "' account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount(account))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Updates an account's details in Firestore.
     */
    private void updateAccount(Account oldAccount, String newName, double newBalance) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // Create new account object
        Account updatedAccount = new Account(newName, newBalance);

        // Firestore does not allow renaming documents easily. The simplest way is to delete the old
        // document and create a new one. This flow assumes transactions do not need to be migrated.
        WriteBatch batch = db.batch();
        batch.delete(db.collection("users").document(currentUser.getUid()).collection("accounts").document(oldAccount.getName()));
        batch.set(db.collection("users").document(currentUser.getUid()).collection("accounts").document(newName), updatedAccount);

        batch.commit().addOnSuccessListener(aVoid -> Toast.makeText(this, "Account updated.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Deletes an account from Firestore.
     */
    private void deleteAccount(Account account) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).collection("accounts").document(account.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Account '" + account.getName() + "' deleted.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete account.", Toast.LENGTH_SHORT).show());
    }
}