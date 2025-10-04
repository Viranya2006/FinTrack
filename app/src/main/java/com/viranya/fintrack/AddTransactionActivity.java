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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.viranya.fintrack.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This Activity is the heart of the app's data entry.
 * It handles both creating a new transaction and editing an existing one.
 */
public class AddTransactionActivity extends AppCompatActivity {

    // --- UI Elements ---
    private MaterialButtonToggleGroup toggleButtonGroup;
    private TextInputEditText etAmount, etDate, etDescription;
    private AutoCompleteTextView actCategory, actAccount;
    private Button btnCancel, btnSave;
    private TextView tvTitle;

    // --- Firebase & Data ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String transactionType = "Expense"; // Default to expense
    private final Calendar selectedDate = Calendar.getInstance();

    // --- Dynamic Data for Dropdowns ---
    private final List<String> accountNames = new ArrayList<>();
    private ArrayAdapter<String> accountAdapter;
    private ArrayAdapter<String> categoryAdapter;
    // Default categories that are always available
    private final String[] defaultExpenseCategories = new String[]{"Food", "Transport", "Housing", "Utilities", "Entertainment", "Shopping", "Health", "Savings"};
    private final String[] defaultIncomeCategories = new String[]{"Salary", "Freelance", "Gift", "Initial Balance", "Other"};
    // Lists to hold custom categories fetched from Firestore
    private List<String> customIncomeCategories = new ArrayList<>();
    private List<String> customExpenseCategories = new ArrayList<>();

    // --- Edit Mode Variables ---
    private boolean isEditMode = false;
    private Transaction existingTransaction;
    // Store original values to correctly reverse calculations on edit
    private double originalAmount = 0;
    private String originalCategory = "";
    private String originalAccount = "";
    private String originalType = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // Initialize all services and UI components
        initializeServices();
        bindViews();
        fetchAccounts();
        fetchCustomCategories(); // Fetch custom categories when the screen starts

        // Check if the activity was started with an existing transaction to edit
        if (getIntent().hasExtra("EDIT_TRANSACTION")) {
            isEditMode = true;
            existingTransaction = (Transaction) getIntent().getSerializableExtra("EDIT_TRANSACTION");
            populateFieldsForEdit();
        } else {
            // If it's a new transaction, set the date to today and setup the default category list
            updateDateInView();
            updateCategoryAdapter();
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
        actAccount = findViewById(R.id.act_account);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        tvTitle = findViewById(R.id.tv_activity_title);
    }

    /**
     * Fetches the list of user-created accounts from Firestore to populate the account dropdown.
     */
    private void fetchAccounts() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        accountAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, accountNames);
        actAccount.setAdapter(accountAdapter);

        db.collection("users").document(currentUser.getUid()).collection("accounts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    accountNames.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        accountNames.add(doc.getId());
                    }
                    accountAdapter.notifyDataSetChanged();
                });
    }

    /**
     * Fetches the user's custom income and expense categories from Firestore.
     */
    private void fetchCustomCategories() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).collection("categories").document("user_defined")
                .addSnapshotListener((doc, error) -> {
                    if (error != null) return;

                    customIncomeCategories.clear();
                    customExpenseCategories.clear();

                    if (doc != null && doc.exists()) {
                        if (doc.contains("income")) customIncomeCategories = (List<String>) doc.get("income");
                        if (doc.contains("expense")) customExpenseCategories = (List<String>) doc.get("expense");
                    }
                    // Refresh the category adapter with the newly fetched data
                    updateCategoryAdapter();
                });
    }

    /**
     * Updates the category dropdown, combining default and custom categories based on the selected type (Income/Expense).
     */
    private void updateCategoryAdapter() {
        List<String> combinedList = new ArrayList<>();
        if ("Income".equals(transactionType)) {
            combinedList.addAll(Arrays.asList(defaultIncomeCategories));
            combinedList.addAll(customIncomeCategories);
        } else {
            combinedList.addAll(Arrays.asList(defaultExpenseCategories));
            combinedList.addAll(customExpenseCategories);
        }
        Collections.sort(combinedList); // Sort the final list alphabetically

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, combinedList);
        actCategory.setAdapter(categoryAdapter);
    }

    /**
     * If in edit mode, this method pre-fills all form fields with the data from the existing transaction.
     */
    private void populateFieldsForEdit() {
        tvTitle.setText("Edit Transaction");
        etDescription.setText(existingTransaction.getTitle());
        etAmount.setText(String.valueOf(existingTransaction.getAmount()));
        actAccount.setText(existingTransaction.getAccountName(), false); // 'false' prevents the dropdown from showing automatically

        transactionType = existingTransaction.getType();
        updateCategoryAdapter(); // Update adapter to show correct category type list
        actCategory.setText(existingTransaction.getCategory(), false);


        if ("Income".equals(transactionType)) {
            toggleButtonGroup.check(R.id.btn_income);
        } else {
            toggleButtonGroup.check(R.id.btn_expense);
        }

        selectedDate.setTime(existingTransaction.getDate());
        updateDateInView();

        // Store original values to correctly calculate balance/budget changes on save
        originalAmount = existingTransaction.getAmount();
        originalCategory = existingTransaction.getCategory();
        originalAccount = existingTransaction.getAccountName();
        originalType = existingTransaction.getType();
    }

    /**
     * Sets up all click listeners for the interactive elements on the screen.
     */
    private void setupListeners() {
        // When the Income/Expense toggle is changed, update the category list
        toggleButtonGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_income) {
                    transactionType = "Income";
                } else if (checkedId == R.id.btn_expense) {
                    transactionType = "Expense";
                }
                actCategory.setText("", false); // Clear the selected category
                updateCategoryAdapter(); // Refresh the dropdown with the correct list
            }
        });

        etDate.setOnClickListener(v -> showDatePickerDialog());
        btnSave.setOnClickListener(v -> saveTransaction());
        btnCancel.setOnClickListener(v -> finish()); // Close the activity
    }

    /**
     * The main logic for saving data. It validates input, determines if it's an edit or a new entry,
     * and writes the data to Firestore.
     */
    private void saveTransaction() {
        // --- 1. Get User Input ---
        String title = etDescription.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String category = actCategory.getText().toString().trim();
        String accountName = actAccount.getText().toString().trim();
        Date date = selectedDate.getTime();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // --- 2. Validate All Fields ---
        if (currentUser == null) { Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show(); return; }
        if (TextUtils.isEmpty(title)) { etDescription.setError("Description is required."); return; }
        if (TextUtils.isEmpty(amountStr) || Double.parseDouble(amountStr) <= 0) { etAmount.setError("Amount must be greater than zero."); return; }
        if (TextUtils.isEmpty(accountName)) { actAccount.setError("Account is required."); return; }
        if (TextUtils.isEmpty(category)) { actCategory.setError("Category is required."); return; }

        double amount = Double.parseDouble(amountStr);
        String userId = currentUser.getUid();

        // --- 3. Create Transaction Object ---
        Transaction transaction = new Transaction(title, category, amount, transactionType, accountName, date);

        // --- 4. Determine Action: Edit or Add New ---
        if (isEditMode) {
            // UPDATE an existing transaction document in Firestore
            db.collection("users").document(userId).collection("transactions").document(existingTransaction.getDocumentId())
                    .set(transaction)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Transaction updated successfully!", Toast.LENGTH_SHORT).show();
                        // Correctly update balances and budgets based on the changes made
                        updateAccountBalanceOnEdit(userId, amount);
                        updateBudgetOnEdit(userId, category, amount);
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update transaction.", Toast.LENGTH_SHORT).show());
        } else {
            // ADD a new transaction document to Firestore
            db.collection("users").document(userId).collection("transactions")
                    .add(transaction)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Transaction saved successfully!", Toast.LENGTH_SHORT).show();
                        // Update the balance of the chosen account
                        updateAccountBalance(userId, accountName, amount, transactionType);
                        // If it's an expense, update the corresponding budget
                        if ("Expense".equals(transactionType)) {
                            updateBudgetOnAdd(userId, category, amount);
                        }
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save transaction.", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Updates an account's balance when a NEW transaction is added.
     */
    private void updateAccountBalance(String userId, String accountName, double amount, String type) {
        // Income adds to the balance, Expense subtracts from it
        double amountToUpdate = "Income".equals(type) ? amount : -amount;
        db.collection("users").document(userId).collection("accounts").document(accountName)
                .update("balance", FieldValue.increment(amountToUpdate));
    }

    /**
     * Handles the complex logic of updating account balances when a transaction is EDITED.
     */
    private void updateAccountBalanceOnEdit(String userId, double newAmount) {
        double newAmountToUpdate = "Income".equals(transactionType) ? newAmount : -newAmount;
        double oldAmountToRevert = "Income".equals(originalType) ? -originalAmount : originalAmount; // Reverse the original transaction
        String newAccount = actAccount.getText().toString();

        if (originalAccount.equals(newAccount)) {
            // Case 1: The account did not change. We just need to apply the difference.
            double totalChange = oldAmountToRevert + newAmountToUpdate;
            db.collection("users").document(userId).collection("accounts").document(originalAccount)
                    .update("balance", FieldValue.increment(totalChange));
        } else {
            // Case 2: The account changed. Revert the old account and apply to the new one.
            // Revert the balance of the original account
            db.collection("users").document(userId).collection("accounts").document(originalAccount)
                    .update("balance", FieldValue.increment(oldAmountToRevert));
            // Apply the new balance to the new account
            db.collection("users").document(userId).collection("accounts").document(newAccount)
                    .update("balance", FieldValue.increment(newAmountToUpdate));
        }
    }

    /**
     * Updates a budget's spent amount when a NEW expense is added.
     */
    private void updateBudgetOnAdd(String userId, String category, double amount) {
        // This only applies to expenses
        if ("Expense".equals(transactionType)) {
            db.collection("users").document(userId).collection("budgets").document(category)
                    .update("spentAmount", FieldValue.increment(amount));
        }
    }

    /**
     * Handles the complex logic of updating budgets when an expense is EDITED.
     */
    private void updateBudgetOnEdit(String userId, String newCategory, double newAmount) {
        // If neither the old nor new transaction is an expense, do nothing
        if (!"Expense".equals(transactionType) && !"Expense".equals(originalType)) return;

        if (originalCategory.equals(newCategory)) {
            // Case 1: Category is the same. Just apply the difference in amount.
            if ("Expense".equals(originalType)) {
                double difference = newAmount - originalAmount;
                db.collection("users").document(userId).collection("budgets").document(newCategory)
                        .update("spentAmount", FieldValue.increment(difference));
            }
        } else {
            // Case 2: Category changed.
            // Revert the amount from the old budget
            if (!originalCategory.isEmpty() && "Expense".equals(originalType)) {
                db.collection("users").document(userId).collection("budgets").document(originalCategory)
                        .update("spentAmount", FieldValue.increment(-originalAmount));
            }
            // Add the new amount to the new budget
            if ("Expense".equals(transactionType)) {
                db.collection("users").document(userId).collection("budgets").document(newCategory)
                        .update("spentAmount", FieldValue.increment(newAmount));
            }
        }
    }

    /**
     * Shows the DatePickerDialog to allow the user to select a date.
     */
    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this, (view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateInView();
        },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    /**
     * Updates the date EditText with the currently selected date in DD/MM/YYYY format.
     */
    private void updateDateInView() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDate.setText(sdf.format(selectedDate.getTime()));
    }
}