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
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
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
import java.util.stream.Collectors;

public class TransactionsFragment extends Fragment implements TransactionAdapter.OnTransactionListener {

    // --- UI Elements ---
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private TextView emptyTextView;
    private TabLayout tabLayout;
    private SearchView searchView;

    // --- Firebase & Adapter ---
    private TransactionAdapter adapter;
    private List<Transaction> allTransactionsList; // Holds all transactions from Firestore
    private List<Transaction> filteredTransactionList; // Holds the list currently being displayed
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentFilter = "All"; // To keep track of the selected tab

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.rv_transactions);
        fab = view.findViewById(R.id.fab_add_transaction);
        emptyTextView = view.findViewById(R.id.tv_empty_transactions);
        tabLayout = view.findViewById(R.id.tab_layout);
        searchView = view.findViewById(R.id.search_view);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        allTransactionsList = new ArrayList<>();
        filteredTransactionList = new ArrayList<>();
        adapter = new TransactionAdapter(filteredTransactionList, getContext(), this);
        recyclerView.setAdapter(adapter);

        // Setup Listeners
        fab.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddTransactionActivity.class)));
        setupTabListener();
        setupSearchListener();

        // Fetch initial data
        fetchTransactions();

        return view;
    }

    private void setupTabListener() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // When a tab is selected, update the filter and apply it
                currentFilter = tab.getText().toString();
                applyFilters();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSearchListener() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // When the search text changes, apply all filters again
                applyFilters();
                return true;
            }
        });
    }

    /**
     * Fetches ALL transactions from Firestore and stores them in `allTransactionsList`.
     */
    private void fetchTransactions() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Error fetching transactions.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value == null) return;

                    allTransactionsList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Transaction transaction = doc.toObject(Transaction.class);
                        transaction.setDocumentId(doc.getId());
                        allTransactionsList.add(transaction);
                    }
                    // After fetching, apply the current filters
                    applyFilters();
                });
    }

    /**
     * A central method to apply both the tab filter and the search filter.
     */
    private void applyFilters() {
        // Start with a fresh copy of all transactions
        List<Transaction> tempFilteredList = new ArrayList<>(allTransactionsList);

        // 1. Apply the Tab Filter (All, Income, Expense)
        if (!"All".equals(currentFilter)) {
            // This is the corrected logic to filter the list based on the selected tab
            ArrayList<Transaction> filteredByType = new ArrayList<>();
            for (Transaction t : tempFilteredList) {
                if (t.getType().equals(currentFilter)) {
                    filteredByType.add(t);
                }
            }
            tempFilteredList = filteredByType;
        }

        // 2. Apply the Search Filter
        String searchQuery = searchView.getQuery().toString().toLowerCase().trim();
        if (!searchQuery.isEmpty()) {
            ArrayList<Transaction> searchedList = new ArrayList<>();
            for (Transaction t : tempFilteredList) {
                if (t.getTitle().toLowerCase().contains(searchQuery)) {
                    searchedList.add(t);
                }
            }
            tempFilteredList = searchedList;
        }

        // Update the list that the adapter uses
        filteredTransactionList.clear();
        filteredTransactionList.addAll(tempFilteredList);
        adapter.notifyDataSetChanged();

        // Show or hide the "empty" message
        checkIfEmpty();
    }

    private void checkIfEmpty() {
        if (filteredTransactionList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.GONE);
        }
    }

    // --- Edit and Delete Logic (remains the same) ---
    @Override
    public void onTransactionClick(Transaction transaction) {
        Intent intent = new Intent(getActivity(), AddTransactionActivity.class);
        intent.putExtra("EDIT_TRANSACTION", transaction);
        startActivity(intent);
    }
    @Override
    public void onTransactionLongClick(Transaction transaction) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTransaction(transaction))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTransaction(Transaction transaction) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || transaction.getDocumentId() == null) return;
        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("transactions").document(transaction.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Transaction deleted.", Toast.LENGTH_SHORT).show();
                    if ("Expense".equals(transaction.getType())) {
                        updateBudgetOnDelete(userId, transaction.getCategory(), transaction.getAmount());
                    }
                });
    }
    private void updateBudgetOnDelete(String userId, String category, double expenseAmount) {
        db.collection("users").document(userId).collection("budgets").document(category)
                .update("spentAmount", FieldValue.increment(-expenseAmount));
    }
}