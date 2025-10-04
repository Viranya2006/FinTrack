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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This Activity handles both creating a new transaction and editing an existing one.
 */
public class AddTransactionActivity extends AppCompatActivity {

    // --- UI & Data Declarations ---
    private MaterialButtonToggleGroup toggleButtonGroup;
    private TextInputEditText etAmount, etDate, etDescription;
    private AutoCompleteTextView actCategory, actAccount;
    private Button btnCancel, btnSave;
    private TextView tvTitle;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String transactionType = "Expense";
    private final Calendar selectedDate = Calendar.getInstance();

    // --- Dynamic Data for Dropdowns ---
    private final List<String> accountNames = new ArrayList<>();
    private ArrayAdapter<String> accountAdapter;
    private ArrayAdapter<String> categoryAdapter;
    private final String[] expenseCategories = new String[]{"Food", "Transport", "Housing", "Utilities", "Entertainment", "Shopping", "Health", "Savings"};
    private final String[] incomeCategories = new String[]{"Salary", "Freelance", "Gift", "Other"};

    // --- Edit Mode Variables ---
    private boolean isEditMode = false;
    private Transaction existingTransaction;
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
        fetchAccounts();

        if (getIntent().hasExtra("EDIT_TRANSACTION")) {
            isEditMode = true;
            existingTransaction = (Transaction) getIntent().getSerializableExtra("EDIT_TRANSACTION");
            populateFieldsForEdit();
        } else {
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

    private void updateCategoryAdapter() {
        if ("Income".equals(transactionType)) {
            categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, incomeCategories);
        } else {
            categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, expenseCategories);
        }
        actCategory.setAdapter(categoryAdapter);
    }

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

    private void setupListeners() {
        toggleButtonGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_income) {
                    transactionType = "Income";
                } else if (checkedId == R.id.btn_expense) {
                    transactionType = "Expense";
                }
                actCategory.setText("", false);
                updateCategoryAdapter();
            }
        });

        etDate.setOnClickListener(v -> showDatePickerDialog());
        btnSave.setOnClickListener(v -> saveTransaction());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveTransaction() {
        String title = etDescription.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String category = actCategory.getText().toString().trim();
        String accountName = actAccount.getText().toString().trim();
        Date date = selectedDate.getTime();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) { Toast.makeText(this, "Authentication error.", Toast.LENGTH_SHORT).show(); return; }
        if (TextUtils.isEmpty(title)) { etDescription.setError("Description is required."); return; }
        if (TextUtils.isEmpty(amountStr) || Double.parseDouble(amountStr) <= 0) { etAmount.setError("Amount must be greater than zero."); return; }
        if (TextUtils.isEmpty(accountName)) { actAccount.setError("Account is required."); return; }
        if (TextUtils.isEmpty(category)) { actCategory.setError("Category is required."); return; }

        double amount = Double.parseDouble(amountStr);
        String userId = currentUser.getUid();

        Transaction transaction = new Transaction(title, category, amount, transactionType, accountName, date);

        if (isEditMode) {
            db.collection("users").document(userId).collection("transactions").document(existingTransaction.getDocumentId())
                    .set(transaction)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Transaction updated successfully!", Toast.LENGTH_SHORT).show();
                        updateAccountBalanceOnEdit(userId, amount);
                        updateBudgetOnEdit(userId, category, amount);
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update transaction.", Toast.LENGTH_SHORT).show());
        } else {
            db.collection("users").document(userId).collection("transactions")
                    .add(transaction)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Transaction saved successfully!", Toast.LENGTH_SHORT).show();
                        updateAccountBalance(userId, accountName, amount, transactionType);
                        if ("Expense".equals(transactionType)) {
                            updateBudgetOnAdd(userId, category, amount);
                        }
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save transaction.", Toast.LENGTH_SHORT).show());
        }
    }

    private void updateAccountBalance(String userId, String accountName, double amount, String type) {
        double amountToUpdate = "Income".equals(type) ? amount : -amount;
        db.collection("users").document(userId).collection("accounts").document(accountName)
                .update("balance", FieldValue.increment(amountToUpdate));
    }

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

    private void updateBudgetOnAdd(String userId, String category, double amount) {
        if ("Expense".equals(transactionType)) {
            db.collection("users").document(userId).collection("budgets").document(category)
                    .update("spentAmount", FieldValue.increment(amount));
        }
    }

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

    private void updateDateInView() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDate.setText(sdf.format(selectedDate.getTime()));
    }
}