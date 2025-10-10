package com.viranya.fintrack;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.viranya.fintrack.model.Account;
import com.viranya.fintrack.model.Budget;
import com.viranya.fintrack.model.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This Activity is the core of data entry for the app. It handles a wide range of responsibilities:
 * - Creating new transactions and editing existing ones.
 * - Dynamically loading accounts and categories from Firestore.
 * - Validating user input, including checking for sufficient funds in an account.
 * - Handling advanced budget management, such as borrowing from another budget if a limit is exceeded.
 * - Performing all necessary database updates for transactions, accounts, and budgets.
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
    private String transactionType = "Expense"; // Default transaction type
    private final Calendar selectedDate = Calendar.getInstance();

    // --- Dynamic Data for Dropdowns ---
    private final List<String> accountNames = new ArrayList<>();
    private ArrayAdapter<String> accountAdapter;
    private ArrayAdapter<String> categoryAdapter;
    private final String[] defaultExpenseCategories = new String[]{"Food", "Transport", "Housing", "Utilities", "Entertainment", "Shopping", "Health", "Savings", "Internal Transfer"};
    private final String[] defaultIncomeCategories = new String[]{"Salary", "Freelance", "Gift", "Initial Balance", "Other"};
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

        initializeServices();
        bindViews();

        // --- Corrected Initialization Order ---
        // 1. Setup adapters with empty lists first. This prevents race conditions where the UI tries to display before data is ready.
        accountAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, accountNames);
        actAccount.setAdapter(accountAdapter);
        updateCategoryAdapter(); // This sets up the category adapter with default values.

        // 2. Asynchronously fetch data. The adapters will be notified and update the UI when data arrives.
        fetchAccounts();
        fetchCustomCategories();

        // 3. Check if we are in "Edit Mode"
        if (getIntent().hasExtra("EDIT_TRANSACTION")) {
            isEditMode = true;
            existingTransaction = (Transaction) getIntent().getSerializableExtra("EDIT_TRANSACTION");
            populateFieldsForEdit();
        } else {
            // For new transactions, just set the date to today.
            updateDateInView();
        }

        setupListeners();
    }

    // --- Initialization & Setup Methods ---

    /**
     * Initializes all necessary services like Firebase.
     */
    private void initializeServices() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Binds all UI elements from the XML layout to their corresponding Java variables.
     */
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
     * Fetches the user-created account names from Firestore and updates the dropdown.
     */
    private void fetchAccounts() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid()).collection("accounts").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    accountNames.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        accountNames.add(doc.getId()); // The document ID is the account name
                    }
                    accountAdapter.notifyDataSetChanged(); // Refresh the dropdown with the new data
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
                    updateCategoryAdapter(); // Refresh the category dropdown
                });
    }

    /**
     * Rebuilds the category dropdown list by combining default and custom categories
     * based on the currently selected transaction type (Income/Expense).
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
        actAccount.setText(existingTransaction.getAccountName(), false);

        transactionType = existingTransaction.getType();
        updateCategoryAdapter();
        actCategory.setText(existingTransaction.getCategory(), false);

        if ("Income".equals(transactionType)) {
            toggleButtonGroup.check(R.id.btn_income);
        } else {
            toggleButtonGroup.check(R.id.btn_expense);
        }

        selectedDate.setTime(existingTransaction.getDate());
        updateDateInView();

        originalAmount = existingTransaction.getAmount();
        originalCategory = existingTransaction.getCategory();
        originalAccount = existingTransaction.getAccountName();
        originalType = existingTransaction.getType();
    }

    /**
     * Sets up all the click listeners for the interactive elements on the screen.
     */
    private void setupListeners() {
        toggleButtonGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                transactionType = (checkedId == R.id.btn_income) ? "Income" : "Expense";
                actCategory.setText("", false);
                updateCategoryAdapter();
            }
        });

        etDate.setOnClickListener(v -> showDatePickerDialog());
        btnSave.setOnClickListener(v -> saveTransaction());
        btnCancel.setOnClickListener(v -> finish());
    }

    // --- Main Save Logic & Business Rule Validation ---

    /**
     * The entry point for saving a transaction. It validates all user input
     * and then proceeds to check business rules like sufficient funds.
     */
    private void saveTransaction() {
        String title = etDescription.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String category = actCategory.getText().toString().trim();
        String accountName = actAccount.getText().toString().trim();
        Date date = selectedDate.getTime();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null || TextUtils.isEmpty(title) || TextUtils.isEmpty(amountStr) ||
                Double.parseDouble(amountStr) <= 0 || TextUtils.isEmpty(accountName) || TextUtils.isEmpty(category)) {
            Toast.makeText(this, "Please fill all required fields correctly.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        Transaction transaction = new Transaction(title, category, amount, transactionType, accountName, date);

        if ("Expense".equals(transactionType)) {
            // For expenses, we must first check if the user has enough money in the selected account.
            checkSufficientFunds(transaction);
        } else {
            // For income, no balance check is needed, so we save directly.
            proceedWithSave(transaction);
        }
    }

    /**
     * Checks if the selected account has enough balance to cover the new expense.
     * This is a critical validation step to prevent negative balances.
     * @param transaction The new expense transaction to be saved.
     */
    private void checkSufficientFunds(Transaction transaction) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).collection("accounts").document(transaction.getAccountName())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Account account = documentSnapshot.toObject(Account.class);
                        if (account.getBalance() >= transaction.getAmount()) {
                            // Sufficient funds: Now check for budget overages (for new transactions only).
                            if (!isEditMode) {
                                checkBudgetOverage(transaction);
                            } else {
                                proceedWithSave(transaction); // Edits don't trigger the borrow flow for simplicity.
                            }
                        } else {
                            // Insufficient funds: Block the transaction and show an error.
                            new AlertDialog.Builder(this)
                                    .setTitle("Insufficient Funds")
                                    .setMessage("You do not have enough money in the '" + transaction.getAccountName() + "' account to cover this expense.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    } else {
                        Toast.makeText(this, "Selected account not found.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * This method contains the final save/update logic, called only after all checks have passed.
     * @param transaction The final transaction object to be saved or updated.
     */
    private void proceedWithSave(Transaction transaction) {
        String userId = mAuth.getCurrentUser().getUid();

        if (isEditMode) {
            db.collection("users").document(userId).collection("transactions").document(existingTransaction.getDocumentId())
                    .set(transaction)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Transaction updated successfully!", Toast.LENGTH_SHORT).show();
                        updateAccountBalanceOnEdit(userId, transaction.getAmount());
                        updateBudgetOnEdit(userId, transaction.getCategory(), transaction.getAmount());
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update transaction.", Toast.LENGTH_SHORT).show());
        } else {
            db.collection("users").document(userId).collection("transactions")
                    .add(transaction)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Transaction saved successfully!", Toast.LENGTH_SHORT).show();
                        updateAccountBalance(userId, transaction.getAccountName(), transaction.getAmount(), transaction.getType());
                        if ("Expense".equals(transaction.getType())) {
                            updateBudgetOnAdd(userId, transaction.getCategory(), transaction.getAmount());
                        }
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save transaction.", Toast.LENGTH_SHORT).show());
        }
    }

    // --- Budget Borrowing Feature Logic ---

    /**
     * Checks if the new expense will exceed the budget for its category.
     * If so, it triggers the borrow flow. Otherwise, it saves the transaction.
     */
    private void checkBudgetOverage(Transaction transaction) {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId).collection("budgets").document(transaction.getCategory())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Budget budget = documentSnapshot.toObject(Budget.class);
                        double overageAmount = (budget.getSpentAmount() + transaction.getAmount()) - budget.getLimitAmount();

                        if (overageAmount > 0) {
                            showBorrowDialog(transaction, overageAmount);
                        } else {
                            proceedWithSave(transaction);
                        }
                    } else {
                        proceedWithSave(transaction);
                    }
                });
    }

    /**
     * Shows a dialog asking the user to borrow funds from another budget.
     */
    private void showBorrowDialog(Transaction transaction, double overageAmount) {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId).collection("budgets")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> availableBudgets = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Budget budget = doc.toObject(Budget.class);
                        if (!doc.getId().equals(transaction.getCategory()) && (budget.getLimitAmount() - budget.getSpentAmount()) >= overageAmount) {
                            availableBudgets.add(doc.getId());
                        }
                    }

                    if (availableBudgets.isEmpty()) {
                        new AlertDialog.Builder(this)
                                .setTitle("Budget Exceeded")
                                .setMessage("This transaction will exceed your budget, and no other budgets have enough funds to cover the difference.")
                                .setPositiveButton("OK", null)
                                .show();
                        return;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_borrow_budget, null);
                    builder.setView(dialogView);

                    TextView message = dialogView.findViewById(R.id.tv_borrow_message);
                    Spinner spinner = dialogView.findViewById(R.id.spinner_borrow_from);

                    NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("si", "LK"));
                    message.setText(String.format("Your '%s' budget will be exceeded by %s. Borrow from another budget?", transaction.getCategory(), format.format(overageAmount)));

                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, availableBudgets);
                    spinner.setAdapter(spinnerAdapter);

                    builder.setTitle("Budget Overage")
                            .setPositiveButton("Confirm & Borrow", (dialog, which) -> {
                                String sourceBudget = spinner.getSelectedItem().toString();
                                performBudgetBorrow(transaction, sourceBudget, overageAmount);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

                    builder.create().show();
                });
    }

    /**
     * Performs all database operations for the budget transfer using a secure WriteBatch.
     */
    private void performBudgetBorrow(Transaction originalTransaction, String sourceBudget, double borrowAmount) {
        String userId = mAuth.getCurrentUser().getUid();

        WriteBatch batch = db.batch();

        DocumentReference sourceRef = db.collection("users").document(userId).collection("budgets").document(sourceBudget);
        batch.update(sourceRef, "limitAmount", FieldValue.increment(-borrowAmount));

        DocumentReference destRef = db.collection("users").document(userId).collection("budgets").document(originalTransaction.getCategory());
        batch.update(destRef, "limitAmount", FieldValue.increment(borrowAmount));

        DocumentReference newTransactionRef = db.collection("users").document(userId).collection("transactions").document();
        batch.set(newTransactionRef, originalTransaction);

        String transferTitle = String.format("Budget transfer from '%s'", sourceBudget);
        Transaction transferTransaction = new Transaction(transferTitle, "Internal Transfer", borrowAmount, "Expense", originalTransaction.getAccountName(), new Date());
        DocumentReference transferTransactionRef = db.collection("users").document(userId).collection("transactions").document();
        batch.set(transferTransactionRef, transferTransaction);

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Transaction saved and budget adjusted.", Toast.LENGTH_LONG).show();
            updateBudgetOnAdd(userId, originalTransaction.getCategory(), originalTransaction.getAmount());
            updateBudgetOnAdd(userId, "Internal Transfer", borrowAmount);
            updateAccountBalance(userId, originalTransaction.getAccountName(), originalTransaction.getAmount() + borrowAmount, "Expense");
            finish();
        }).addOnFailureListener(e -> Toast.makeText(this, "An error occurred during the transfer.", Toast.LENGTH_SHORT).show());
    }

    // --- Helper Methods ---

    /**
     * Updates an account's balance when a NEW transaction is added.
     */
    private void updateAccountBalance(String userId, String accountName, double amount, String type) {
        double amountToUpdate = "Income".equals(type) ? amount : -amount;
        db.collection("users").document(userId).collection("accounts").document(accountName)
                .update("balance", FieldValue.increment(amountToUpdate));
    }

    /**
     * Handles the complex logic of updating account balances when a transaction is EDITED.
     */
    private void updateAccountBalanceOnEdit(String userId, double newAmount) {
        double newAmountToUpdate = "Income".equals(transactionType) ? newAmount : -newAmount;
        double oldAmountToRevert = "Income".equals(originalType) ? -originalAmount : originalAmount;
        String newAccount = actAccount.getText().toString();

        if (originalAccount.equals(newAccount)) {
            double totalChange = oldAmountToRevert + newAmountToUpdate;
            db.collection("users").document(userId).collection("accounts").document(originalAccount)
                    .update("balance", FieldValue.increment(totalChange));
        } else {
            db.collection("users").document(userId).collection("accounts").document(originalAccount)
                    .update("balance", FieldValue.increment(oldAmountToRevert));
            db.collection("users").document(userId).collection("accounts").document(newAccount)
                    .update("balance", FieldValue.increment(newAmountToUpdate));
        }
    }

    /**
     * Updates a budget's spent amount when a NEW expense is added.
     */
    private void updateBudgetOnAdd(String userId, String category, double amount) {
        if ("Expense".equals(transactionType)) {
            db.collection("users").document(userId).collection("budgets").document(category)
                    .update("spentAmount", FieldValue.increment(amount));
        }
    }

    /**
     * Handles the complex logic of updating budgets when an expense is EDITED.
     */
    private void updateBudgetOnEdit(String userId, String newCategory, double newAmount) {
        if (!"Expense".equals(transactionType) && !"Expense".equals(originalType)) return;

        if (originalCategory.equals(newCategory)) {
            if ("Expense".equals(originalType)) {
                double difference = newAmount - originalAmount;
                db.collection("users").document(userId).collection("budgets").document(newCategory)
                        .update("spentAmount", FieldValue.increment(difference));
            }
        } else {
            if (!originalCategory.isEmpty() && "Expense".equals(originalType)) {
                db.collection("users").document(userId).collection("budgets").document(originalCategory)
                        .update("spentAmount", FieldValue.increment(-originalAmount));
            }
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