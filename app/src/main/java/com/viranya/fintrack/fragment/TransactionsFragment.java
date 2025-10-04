package com.viranya.fintrack.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.viranya.fintrack.AddTransactionActivity;
import com.viranya.fintrack.R;
import com.viranya.fintrack.adapter.TransactionAdapter;
import com.viranya.fintrack.model.Transaction;

import java.util.ArrayList;
import java.util.List;

// Implement the adapter's listener interface
public class TransactionsFragment extends Fragment implements TransactionAdapter.OnTransactionListener {

    // --- UI Elements ---
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private TextView emptyTextView;

    // --- Firebase & Adapter ---
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.rv_transactions);
        fab = view.findViewById(R.id.fab_add_transaction);
        emptyTextView = view.findViewById(R.id.tv_empty_transactions);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionList = new ArrayList<>();
        // Initialize the adapter, passing 'this' as the listener
        adapter = new TransactionAdapter(transactionList, getContext(), this);
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddTransactionActivity.class);
            startActivity(intent);
        });

        fetchTransactions();

        return view;
    }

    /**
     * Fetches transactions from Firestore and listens for real-time updates.
     */
    private void fetchTransactions() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Error fetching transactions.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (queryDocumentSnapshots == null) return;

                    transactionList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction transaction = doc.toObject(Transaction.class);
                        // Get the document ID from Firestore and set it in our model
                        transaction.setDocumentId(doc.getId());
                        transactionList.add(transaction);
                    }
                    adapter.notifyDataSetChanged();
                    checkIfEmpty();
                });
    }

    /**
     * Checks if the transaction list is empty and updates the UI accordingly.
     */
    private void checkIfEmpty() {
        if (transactionList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.GONE);
        }
    }
    /**
     * This method is called from the adapter when an item is normal-clicked.
     * @param transaction The transaction that was normal-clicked.
     */    @Override
    public void onTransactionClick(Transaction transaction) {
        // When a transaction is clicked, open the AddTransactionActivity
        Intent intent = new Intent(getActivity(), AddTransactionActivity.class);
        // Pass the selected transaction object to the activity
        intent.putExtra("EDIT_TRANSACTION", transaction);
        startActivity(intent);
    }


    /**
     * This method is called from the adapter when an item is long-clicked.
     * @param transaction The transaction that was long-clicked.
     */
    @Override
    public void onTransactionLongClick(Transaction transaction) {
        // Show a confirmation dialog to prevent accidental deletion
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTransaction(transaction))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the specified transaction from Firestore.
     * @param transaction The transaction object to delete.
     */
    private void deleteTransaction(Transaction transaction) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || transaction.getDocumentId() == null) {
            Toast.makeText(getContext(), "Error: Could not delete transaction.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        // Use the document ID stored in the transaction object to delete it
        db.collection("users").document(userId).collection("transactions").document(transaction.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Transaction deleted.", Toast.LENGTH_SHORT).show();
                    // If the deleted item was an expense, we need to update the corresponding budget
                    if ("Expense".equals(transaction.getType())) {
                        updateBudgetOnDelete(userId, transaction.getCategory(), transaction.getAmount());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete transaction.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Reverses the budget calculation after an expense is deleted.
     * @param userId The ID of the current user.
     * @param category The category of the deleted expense.
     * @param expenseAmount The amount of the deleted expense.
     */
    private void updateBudgetOnDelete(String userId, String category, double expenseAmount) {
        // Find the budget for the category and DECREMENT the spentAmount by the expense amount
        db.collection("users").document(userId).collection("budgets").document(category)
                .update("spentAmount", FieldValue.increment(-expenseAmount))
                .addOnSuccessListener(aVoid -> System.out.println("Budget updated after deletion."))
                .addOnFailureListener(e -> System.out.println("No budget to update or error: " + e.getMessage()));
    }


}