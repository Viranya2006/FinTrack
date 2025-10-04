package com.viranya.fintrack;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.viranya.fintrack.adapter.SavingGoalAdapter;
import com.viranya.fintrack.fragment.AddGoalBottomSheetFragment;
import com.viranya.fintrack.model.SavingGoal;
import com.viranya.fintrack.model.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SavingsGoalsActivity extends AppCompatActivity implements SavingGoalAdapter.OnGoalListener {

    // --- UI Elements ---
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private TextView emptyTextView;

    // --- Firebase, Adapter & Data ---
    private SavingGoalAdapter adapter;
    private List<SavingGoal> goalList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savings_goals);

        // --- Initialize Firebase ---
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // --- Bind UI Elements ---
        recyclerView = findViewById(R.id.rv_savings_goals);
        fab = findViewById(R.id.fab_add_goal);
        emptyTextView = findViewById(R.id.tv_empty_goals);

        // --- Setup RecyclerView ---
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        goalList = new ArrayList<>();
        // Pass 'this' as the OnGoalListener because this activity implements the interface
        adapter = new SavingGoalAdapter(goalList, this, this);
        recyclerView.setAdapter(adapter);

        // --- Setup Listeners ---
        fab.setOnClickListener(v -> {
            // Create and show the bottom sheet for adding a new goal
            AddGoalBottomSheetFragment bottomSheet = new AddGoalBottomSheetFragment();
            bottomSheet.show(getSupportFragmentManager(), "AddGoalBottomSheet");
        });

        // --- Fetch initial data ---
        fetchGoals();
    }

    /**
     * Fetches the list of savings goals from Firestore and listens for real-time updates.
     */
    private void fetchGoals() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("saving_goals")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error fetching goals.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    goalList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        SavingGoal goal = doc.toObject(SavingGoal.class);
                        goalList.add(goal);
                    }
                    adapter.notifyDataSetChanged();
                    checkIfEmpty();
                });
    }

    /**
     * Toggles the visibility of the "empty" message based on the list size.
     */
    private void checkIfEmpty() {
        if (goalList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.GONE);
        }
    }

    /**
     * This method is called from the adapter when the "Add" button on a goal item is clicked.
     * @param goal The specific goal that was clicked.
     */
    @Override
    public void onAddMoneyClick(SavingGoal goal) {
        // Create an AlertDialog to get the amount from the user
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_money, null);
        builder.setView(dialogView);

        final EditText etAmount = dialogView.findViewById(R.id.et_dialog_amount);

        builder.setTitle("Add to " + goal.getGoalName())
                .setPositiveButton("Add", (dialog, id) -> {
                    String amountStr = etAmount.getText().toString();
                    if (TextUtils.isEmpty(amountStr) || Double.parseDouble(amountStr) <= 0) {
                        Toast.makeText(this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double amountToAdd = Double.parseDouble(amountStr);
                    addMoneyToGoal(goal, amountToAdd);
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        builder.create().show();
    }

    /**
     * Updates the goal in Firestore and creates a corresponding expense transaction.
     * @param goal The goal to update.
     * @param amount The amount to add.
     */
    private void addMoneyToGoal(SavingGoal goal, double amount) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        // --- Step 1: Update the saved amount in the saving_goals collection ---
        // We use FieldValue.increment() for safe, atomic updates.
        db.collection("users").document(userId).collection("saving_goals").document(goal.getGoalName())
                .update("savedAmount", FieldValue.increment(amount));

        // --- Step 2: Create a corresponding expense transaction for record keeping ---
        String title = "Contribution to " + goal.getGoalName();
        // This transaction is categorized as "Savings" to keep the user's budget accurate.
        Transaction transaction = new Transaction(title, "Savings", amount, "Expense", new Date());

        db.collection("users").document(userId).collection("transactions")
                .add(transaction)
                .addOnSuccessListener(documentReference -> Toast.makeText(this, "Successfully added money to goal!", Toast.LENGTH_SHORT).show());
    }
}