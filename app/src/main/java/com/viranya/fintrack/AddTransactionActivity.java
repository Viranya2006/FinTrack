package com.viranya.fintrack;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
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

/**
 * This Activity handles both creating a new transaction and editing an existing one.
 * It determines its mode (add or edit) based on the Intent extras it receives.
 */
public class AddTransactionActivity extends AppCompatActivity {

    // --- UI Elements ---
    private MaterialButtonToggleGroup toggleButtonGroup;
    private TextInputEditText etAmount, etDate, etDescription;
    private AutoCompleteTextView actCategory;
    private Button btnCancel, btnSave;
    private TextView tvTitle;

    // --- Services ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // --- Data & State ---
    private String transactionType = "Expense"; // Default to expense
    private final Calendar selectedDate = Calendar.getInstance();

    // --- Edit Mode Variables ---
    private boolean isEditMode = false;
    private Transaction existingTransaction;
    private double originalAmount = 0;
    private String originalCategory = "";
    private String originalType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        initializeServices();
        bindViews();
        setupCategoryDropdown();

        // --- Check if we are in Edit Mode ---
        // The app enters Edit Mode if a "EDIT_TRANSACTION" extra is passed in the Intent.
        if (getIntent().hasExtra("EDIT_TRANSACTION")) {
            isEditMode = true;
            existingTransaction = (Transaction) getIntent().getSerializableExtra("EDIT_TRANSACTION");
            populateFieldsForEdit();
        } else {
            // New transaction mode: set the title and default date
            tvTitle.setText("Add Transaction");
            updateDateInView();
        }

        setupListeners();
    }

    private void initializeServices() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void bindViews() {
        toggleButtonGroup = findViewById(R.id.toggle_button_group);
        etAmount = findViewById(R.id.et_amount);
        etDate = findViewById(R.id.et_date);
        etDescription = findViewById(R.id.et_description);
        actCategory = findViewById(R.id.act_category);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        tvTitle = findViewById(R.id.tv_activity_title);
    }

    /**
     * If in edit mode, this method pre-fills all form fields with the existing transaction's data.
     */
    private void populateFieldsForEdit() {
        tvTitle.setText("Edit Transaction");
        etDescription.setText(existingTransaction.getTitle());
        etAmount.setText(String.valueOf(existingTransaction.getAmount()));
        // The 'false' argument prevents the dropdown from showing when we set the text
        actCategory.setText(existingTransaction.getCategory(), false);

        transactionType = existingTransaction.getType();
        if ("Income".equals(transactionType)) {
            toggleButtonGroup.check(R.id.btn_income);
        } else {
            toggleButtonGroup.check(R.id.btn_expense);
        }

        selectedDate.setTime(existingTransaction.getDate());
        updateDateInView();

        // Store the original values. This is crucial for correctly calculating budget changes.
        originalAmount = existingTransaction.getAmount();
        originalCategory = existingTransaction.getCategory();
        originalType = existingTransaction.getType();
    }

    private void setupListeners() {
        toggleButtonGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_income) {
                    transactionType = "Income";
                } else if (checkedId == R.id.btn_expense) {
                    transactionType = "Expense";
                }
            }
        });

        etDate.setOnClickListener(v -> showDatePickerDialog());
        btnSave.setOnClickListener(v -> saveTransaction());
        btnCancel.setOnClickListener(v -> finish());
    }

    /**
     * Handles the logic for both saving a new transaction and updating an existing one.
     */
    private void saveTransaction() {
        // --- 1. Get and Validate User Input ---
        String title = etDescription.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String category = actCategory.getText().toString().trim();
        Date date = selectedDate.getTime();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show();
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

        double amount = Double.parseDouble(amountStr);
        Transaction transaction = new Transaction(title, category, amount, transactionType, date);
        String userId = currentUser.getUid();

        // --- 2. Determine whether to Add or Update ---
        if (isEditMode) {
            // UPDATE EXISTING TRANSACTION: Overwrite the document with the new data.
            db.collection("users").document(userId).collection("transactions").document(existingTransaction.getDocumentId())
                    .set(transaction)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Transaction updated!", Toast.LENGTH_SHORT).show();
                        // Handle the complex logic of updating budgets
                        updateBudgetOnEdit(userId, category, amount);
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update transaction.", Toast.LENGTH_SHORT).show());
        } else {
            // ADD NEW TRANSACTION: Create a new document in the collection.
            db.collection("users").document(userId).collection("transactions")
                    .add(transaction)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Transaction saved!", Toast.LENGTH_SHORT).show();
                        // If it's an expense, update the budget
                        if ("Expense".equals(transactionType)) {
                            updateBudgetOnAdd(userId, category, amount);
                        }
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save transaction.", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Updates the budget when a new expense is added.
     */
    private void updateBudgetOnAdd(String userId, String category, double amount) {
        db.collection("users").document(userId).collection("budgets").document(category)
                .update("spentAmount", FieldValue.increment(amount));
    }

    /**
     * Handles the complex logic of updating budgets when an existing expense is edited.
     */
    private void updateBudgetOnEdit(String userId, String newCategory, double newAmount) {
        // --- Calculate the difference between the old and new expense values ---
        double difference = 0;
        // Case 1: Was an expense, still an expense.
        if ("Expense".equals(originalType) && "Expense".equals(transactionType)) {
            // If the category is the same, the difference is simply new amount - old amount.
            if (originalCategory.equals(newCategory)) {
                difference = newAmount - originalAmount;
                db.collection("users").document(userId).collection("budgets").document(newCategory)
                        .update("spentAmount", FieldValue.increment(difference));
            } else {
                // If the category changed, we must subtract from the old and add to the new.
                // Subtract from the original category's budget
                if (!originalCategory.isEmpty()) {
                    db.collection("users").document(userId).collection("budgets").document(originalCategory)
                            .update("spentAmount", FieldValue.increment(-originalAmount));
                }
                // Add to the new category's budget
                db.collection("users").document(userId).collection("budgets").document(newCategory)
                        .update("spentAmount", FieldValue.increment(newAmount));
            }
            // Case 2: Was an income, now an expense.
        } else if (!"Expense".equals(originalType) && "Expense".equals(transactionType)) {
            // We just need to add the new expense amount to the new category.
            updateBudgetOnAdd(userId, newCategory, newAmount);
            // Case 3: Was an expense, now an income.
        } else if ("Expense".equals(originalType) && !"Expense".equals(transactionType)) {
            // We need to subtract the original expense amount from the original category.
            if (!originalCategory.isEmpty()) {
                db.collection("users").document(userId).collection("budgets").document(originalCategory)
                        .update("spentAmount", FieldValue.increment(-originalAmount));
            }
        }
        // Case 4: Was income, still income. No budget change needed.
    }

    // --- UI Helper Methods ---

    private void setupCategoryDropdown() {
        String[] categories = new String[]{"Food", "Transport", "Housing", "Utilities", "Entertainment", "Shopping", "Health", "Salary", "Freelance", "Savings"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        actCategory.setAdapter(adapter);
    }

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

    private void updateDateInView() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDate.setText(sdf.format(selectedDate.getTime()));
    }
}
