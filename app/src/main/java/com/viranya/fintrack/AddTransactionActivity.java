package com.viranya.fintrack;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.viranya.fintrack.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddTransactionActivity extends AppCompatActivity {

    // --- UI Elements ---
    private MaterialButtonToggleGroup toggleButtonGroup;
    private TextInputEditText etAmount, etDate, etDescription;
    private AutoCompleteTextView actCategory;
    private Button btnCancel, btnSave;

    // --- Firebase & Data ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String transactionType = "Expense"; // Default to expense
    private Calendar selectedDate = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // --- Initialize Firebase ---
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // --- Bind UI Elements ---
        toggleButtonGroup = findViewById(R.id.toggle_button_group);
        etAmount = findViewById(R.id.et_amount);
        etDate = findViewById(R.id.et_date);
        etDescription = findViewById(R.id.et_description);
        actCategory = findViewById(R.id.act_category);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);

        // --- Setup Initial State & Listeners ---
        setupCategoryDropdown();
        updateDateInView(); // Set today's date initially

        // Set default selection for the toggle group
        toggleButtonGroup.check(R.id.btn_expense);
        toggleButtonGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_income) {
                    transactionType = "Income";
                } else if (checkedId == R.id.btn_expense) {
                    transactionType = "Expense";
                }
            }
        });

        // Show date picker when date field is clicked
        etDate.setOnClickListener(v -> showDatePickerDialog());

        // Set listeners for save and cancel buttons
        btnSave.setOnClickListener(v -> saveTransaction());
        btnCancel.setOnClickListener(v -> finish()); // Close the activity
    }

    /**
     * Populates the category dropdown with a predefined list of categories.
     */
    private void setupCategoryDropdown() {
        String[] categories = new String[]{"Food", "Transport", "Housing", "Utilities", "Entertainment", "Shopping", "Health", "Salary", "Freelance"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        actCategory.setAdapter(adapter);
    }

    /**
     * Displays the DatePickerDialog to allow the user to select a date.
     */
    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateInView();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    /**
     * Updates the date EditText with the currently selected date.
     */
    private void updateDateInView() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDate.setText(sdf.format(selectedDate.getTime()));
    }

    /**
     * Validates user input and saves the transaction to Firestore.
     */
    private void saveTransaction() {
        // --- 1. Get User Input ---
        String amountStr = etAmount.getText().toString().trim();
        String category = actCategory.getText().toString().trim();
        // Use description field for the transaction title
        String title = etDescription.getText().toString().trim();
        Date date = selectedDate.getTime();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // --- 2. Validate Input ---
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to add a transaction.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(title)) {
            etDescription.setError("Description cannot be empty.");
            return;
        }
        if (TextUtils.isEmpty(amountStr) || Double.parseDouble(amountStr) <= 0) {
            etAmount.setError("Amount must be greater than zero.");
            return;
        }
        if (TextUtils.isEmpty(category)) {
            actCategory.setError("Category is required.");
            return;
        }

        // --- 3. Create Transaction Object ---
        double amount = Double.parseDouble(amountStr);
        Transaction newTransaction = new Transaction(title, category, amount, transactionType, date);

        // --- 4. Save to Firestore ---
        String userId = currentUser.getUid();
        db.collection("users").document(userId).collection("transactions")
                .add(newTransaction)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Transaction saved!", Toast.LENGTH_SHORT).show();

                    // --- 5. Update budget if it's an expense ---
                    if ("Expense".equals(transactionType)) {
                        updateBudgetSpentAmount(userId, category, amount);
                    }
                    finish(); // Close the activity and return to the list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving transaction: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Finds the budget corresponding to the expense category and increments the spent amount.
     * @param userId The current user's ID.
     * @param category The category of the expense.
     * @param expenseAmount The amount of the expense.
     */
    private void updateBudgetSpentAmount(String userId, String category, double expenseAmount) {
        // The budget document ID is the category name itself
        db.collection("users").document(userId).collection("budgets").document(category)
                .update("spentAmount", FieldValue.increment(expenseAmount))
                .addOnSuccessListener(aVoid -> {
                    // This is a background operation, so we just log the success
                    System.out.println("Budget updated successfully for category: " + category);
                })
                .addOnFailureListener(e -> {
                    // This can happen if no budget is set for this category, which is not an error.
                    // We just log it for debugging purposes.
                    System.out.println("No budget found for category: " + category + ". Error: " + e.getMessage());
                });
    }
}