package com.viranya.fintrack.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.viranya.fintrack.R;
import com.viranya.fintrack.adapter.BudgetAdapter;
import com.viranya.fintrack.model.Budget;

import java.util.ArrayList;
import java.util.List;

public class BudgetsFragment extends Fragment {

    // --- UI Elements ---
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private TextView emptyTextView;

    // --- Firebase & Adapter ---
    private BudgetAdapter adapter;
    private List<Budget> budgetList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budgets, container, false);

        // --- Initialize Firebase ---
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // --- Bind UI Elements ---
        recyclerView = view.findViewById(R.id.rv_budgets);
        fab = view.findViewById(R.id.fab_add_budget);
        emptyTextView = view.findViewById(R.id.tv_empty_budgets);

        // --- Setup RecyclerView ---
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        budgetList = new ArrayList<>();
        adapter = new BudgetAdapter(budgetList, getContext());
        recyclerView.setAdapter(adapter);

        // --- Setup Listeners ---
        fab.setOnClickListener(v -> {
            // Show the AddBudgetBottomSheetFragment
            AddBudgetBottomSheetFragment bottomSheet = new AddBudgetBottomSheetFragment();
            bottomSheet.show(getParentFragmentManager(), "AddBudgetBottomSheet");
        });

        // --- Fetch Data ---
        fetchBudgets();

        return view;
    }

    /**
     * Fetches budgets from Firestore and listens for real-time updates.
     */
    private void fetchBudgets() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("budgets")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Error fetching budgets.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    budgetList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Budget budget = doc.toObject(Budget.class);
                        budgetList.add(budget);
                    }
                    adapter.notifyDataSetChanged();
                    checkIfEmpty();
                });
    }

    /**
     * Checks if the budget list is empty and updates the UI.
     */
    private void checkIfEmpty() {
        if (budgetList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.GONE);
        }
    }
}