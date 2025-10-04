package com.viranya.fintrack;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.viranya.fintrack.adapter.CategoryAdapter;
import java.util.ArrayList;
import java.util.List;

public class CategoriesActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryListener {

    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private TextView emptyTextView;
    private CategoryAdapter adapter;
    private List<String> customCategoryList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Store both types of categories
    private List<String> incomeCategories;
    private List<String> expenseCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.rv_categories);
        fab = findViewById(R.id.fab_add_category);
        emptyTextView = findViewById(R.id.tv_empty_categories);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        customCategoryList = new ArrayList<>();
        incomeCategories = new ArrayList<>();
        expenseCategories = new ArrayList<>();
        adapter = new CategoryAdapter(customCategoryList, this);
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v -> showCategoryDialog(null, null));
        fetchCategories();
    }

    private void fetchCategories() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).collection("categories").document("user_defined")
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error fetching categories.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    customCategoryList.clear();
                    incomeCategories.clear();
                    expenseCategories.clear();

                    if (doc != null && doc.exists()) {
                        if (doc.contains("income")) incomeCategories = (List<String>) doc.get("income");
                        if (doc.contains("expense")) expenseCategories = (List<String>) doc.get("expense");

                        // Combine for display
                        customCategoryList.addAll(incomeCategories);
                        customCategoryList.addAll(expenseCategories);
                    }
                    adapter.notifyDataSetChanged();
                    checkIfEmpty();
                });
    }

    private void checkIfEmpty() {
        emptyTextView.setVisibility(customCategoryList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCategoryLongClick(String category) {
        // Determine the type of the clicked category
        String type = expenseCategories.contains(category) ? "expense" : "income";

        final CharSequence[] options = {"Edit", "Delete", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("Manage Category: " + category)
                .setItems(options, (dialog, item) -> {
                    if (options[item].equals("Edit")) {
                        showCategoryDialog(category, type);
                    } else if (options[item].equals("Delete")) {
                        deleteCategory(category, type);
                    } else {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void showCategoryDialog(String oldCategory, String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(oldCategory == null ? "Add New Category" : "Edit Category");

        // Create a layout for the dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        final EditText input = new EditText(this);
        input.setHint("Category Name");
        if (oldCategory != null) input.setText(oldCategory);
        layout.addView(input);

        // Add RadioButtons to select type
        RadioGroup radioGroup = new RadioGroup(this);
        RadioButton rbExpense = new RadioButton(this);
        rbExpense.setText("Expense");
        RadioButton rbIncome = new RadioButton(this);
        rbIncome.setText("Income");
        radioGroup.addView(rbExpense);
        radioGroup.addView(rbIncome);
        // Set default or existing type
        if ("income".equals(type)) {
            rbIncome.setChecked(true);
        } else {
            rbExpense.setChecked(true); // Default to expense
        }
        layout.addView(radioGroup);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newCategory = input.getText().toString().trim();
            if (newCategory.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            String selectedType = rbIncome.isChecked() ? "income" : "expense";
            saveCategory(oldCategory, newCategory, selectedType);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveCategory(String oldCategory, String newCategory, String type) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // If renaming, we must first remove the old category name from its array
        if (oldCategory != null && !oldCategory.equals(newCategory)) {
            String oldType = expenseCategories.contains(oldCategory) ? "expense" : "income";
            db.collection("users").document(currentUser.getUid()).collection("categories").document("user_defined")
                    .update(oldType, FieldValue.arrayRemove(oldCategory));
        }

        // Add the new/updated category to the correct array in Firestore
        db.collection("users").document(currentUser.getUid()).collection("categories").document("user_defined")
                .update(type, FieldValue.arrayUnion(newCategory))
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Category saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    // This might fail if the document doesn't exist, so we create it
                    java.util.Map<String, Object> docData = new java.util.HashMap<>();
                    docData.put(type, java.util.Collections.singletonList(newCategory));
                    db.collection("users").document(currentUser.getUid()).collection("categories").document("user_defined").set(docData);
                });
    }

    private void deleteCategory(String category, String type) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete '" + category + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("users").document(currentUser.getUid()).collection("categories").document("user_defined")
                            .update(type, FieldValue.arrayRemove(category))
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Category deleted.", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}