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
    private List<Transaction> allTransactionsList;
    private List<Transaction> filteredTransactionList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentFilter = "All";

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

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        allTransactionsList = new ArrayList<>();
        filteredTransactionList = new ArrayList<>();
        adapter = new TransactionAdapter(filteredTransactionList, getContext(), this);
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddTransactionActivity.class)));
        setupTabListener();
        setupSearchListener();

        fetchTransactions();

        return view;
    }

    private void setupTabListener() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
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
                applyFilters();
                return true;
            }
        });
    }

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
                    applyFilters();
                });
    }

    private void applyFilters() {
        List<Transaction> tempFilteredList = new ArrayList<>(allTransactionsList);

        if (!"All".equals(currentFilter)) {
            tempFilteredList = tempFilteredList.stream()
                    .filter(t -> t.getType().equals(currentFilter))
                    .collect(Collectors.toList());
        }

        String searchQuery = searchView.getQuery().toString().toLowerCase().trim();
        if (!searchQuery.isEmpty()) {
            tempFilteredList = tempFilteredList.stream()
                    .filter(t -> t.getTitle().toLowerCase().contains(searchQuery))
                    .collect(Collectors.toList());
        }

        filteredTransactionList.clear();
        filteredTransactionList.addAll(tempFilteredList);
        adapter.notifyDataSetChanged();
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

        String userId = currentUser.getUid(); // Get the userId here

        db.collection("users").document(userId).collection("transactions").document(transaction.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Transaction deleted.", Toast.LENGTH_SHORT).show();
                    if ("Expense".equals(transaction.getType())) {
                        // Pass the userId to the helper method
                        updateBudgetOnDelete(userId, transaction.getCategory(), transaction.getAmount());
                    }
                });
    }

    // It now accepts the 'userId' as an argument.
    private void updateBudgetOnDelete(String userId, String category, double expenseAmount) {
        db.collection("users").document(userId).collection("budgets").document(category)
                .update("spentAmount", FieldValue.increment(-expenseAmount))
                .addOnSuccessListener(aVoid -> System.out.println("Budget updated after deletion."))
                .addOnFailureListener(e -> System.out.println("No budget to update or error: " + e.getMessage()));
    }
}