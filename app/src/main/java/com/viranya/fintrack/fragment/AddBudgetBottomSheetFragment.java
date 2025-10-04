package com.viranya.fintrack.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.viranya.fintrack.model.Budget;

public class AddBudgetBottomSheetFragment extends BottomSheetDialogFragment {

    private AutoCompleteTextView actCategory;
    private TextInputEditText etLimit;
    private Button btnAddBudget;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.bottom_sheet_add_budget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Bind views
        actCategory = view.findViewById(R.id.act_budget_category);
        etLimit = view.findViewById(R.id.et_budget_limit);
        btnAddBudget = view.findViewById(R.id.btn_add_budget);

        // Populate the category dropdown
        setupCategoryDropdown();

        // Set click listener for the button
        btnAddBudget.setOnClickListener(v -> saveBudget());
    }

    private void setupCategoryDropdown() {
        // We'll use a simple list of expense categories for budgeting
        String[] categories = new String[]{"Food", "Transport", "Housing", "Utilities", "Entertainment", "Shopping", "Health"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, categories);
        actCategory.setAdapter(adapter);
    }

    private void saveBudget() {
        String category = actCategory.getText().toString().trim();
        String limitStr = etLimit.getText().toString().trim();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Validate input
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(category)) {
            actCategory.setError("Category cannot be empty.");
            return;
        }
        if (TextUtils.isEmpty(limitStr) || Double.parseDouble(limitStr) <= 0) {
            etLimit.setError("Limit must be greater than zero.");
            return;
        }

        double limitAmount = Double.parseDouble(limitStr);

        // Create a new budget object
        Budget newBudget = new Budget(category, limitAmount, 0); // spentAmount is initially 0

        // Save to Firestore. We use the category name as the document ID
        // to prevent creating duplicate budgets for the same category.
        String userId = currentUser.getUid();
        db.collection("users").document(userId).collection("budgets")
                .document(category) // Using category as the ID
                .set(newBudget)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Budget for " + category + " saved!", Toast.LENGTH_SHORT).show();
                    dismiss(); // Close the bottom sheet on success
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error saving budget.", Toast.LENGTH_SHORT).show();
                });
    }
}











