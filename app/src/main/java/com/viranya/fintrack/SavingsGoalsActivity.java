package com.viranya.fintrack;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.viranya.fintrack.adapter.SuggestionAdapter;
import com.viranya.fintrack.fragment.AddGoalBottomSheetFragment;
import com.viranya.fintrack.model.SavingGoal;
import com.viranya.fintrack.model.Suggestion;
import com.viranya.fintrack.model.Transaction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SavingsGoalsActivity extends AppCompatActivity implements SavingGoalAdapter.OnGoalListener {

    // --- UI Elements ---
    private RecyclerView goalsRecyclerView, suggestionsRecyclerView;
    private FloatingActionButton fab;
    private TextView emptyTextView, suggestionHeaderTextView;
    private LinearLayout suggestionLayout;

    // --- Adapters & Data Lists ---
    private SavingGoalAdapter goalAdapter;
    private SuggestionAdapter suggestionAdapter;
    private List<SavingGoal> goalList;
    private List<Suggestion> allSuggestions;

    // --- Firebase Services ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savings_goals);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        goalsRecyclerView = findViewById(R.id.rv_savings_goals);
        suggestionsRecyclerView = findViewById(R.id.rv_suggestions);
        fab = findViewById(R.id.fab_add_goal);
        emptyTextView = findViewById(R.id.tv_empty_goals);
        suggestionHeaderTextView = findViewById(R.id.tv_suggestion_header);
        suggestionLayout = findViewById(R.id.suggestion_layout);

        // --- Setup for Goals List ---
        goalsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        goalList = new ArrayList<>();
        goalAdapter = new SavingGoalAdapter(goalList, this, this);
        goalsRecyclerView.setAdapter(goalAdapter);

        // --- Setup for Suggestions List ---
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        createAllSuggestions();

        fab.setOnClickListener(v -> {
            AddGoalBottomSheetFragment bottomSheet = new AddGoalBottomSheetFragment();
            bottomSheet.show(getSupportFragmentManager(), "AddGoalBottomSheet");
        });

        fetchGoals();
    }

    private void createAllSuggestions() {
        allSuggestions = new ArrayList<>();

        // Original Suggestions
        allSuggestions.add(new Suggestion("A Nice Dinner", "Treat yourself to a meal at a fancy restaurant.", 5000));
        allSuggestions.add(new Suggestion("New Pair of Shoes", "Upgrade your footwear.", 10000));
        allSuggestions.add(new Suggestion("Weekend Getaway to Ella", "Explore the scenic beauty of Ella.", 20000));
        allSuggestions.add(new Suggestion("Budget Smartphone", "Get a new modern smartphone.", 40000));
        allSuggestions.add(new Suggestion("Trip to Galle", "Enjoy the historic Galle Fort and beautiful beaches.", 50000));

        // --- 15 New Suggestions ---

        // Smaller Treats (Under 10,000 LKR)
        allSuggestions.add(new Suggestion("High-Tea Buffet", "Enjoy an evening high-tea experience at a Colombo hotel.", 7500));
        allSuggestions.add(new Suggestion("New Cricket Bat", "Get a quality bat for your evening matches.", 8000));
        allSuggestions.add(new Suggestion("Bluetooth Speaker", "Listen to your favorite music anywhere.", 9000));

        // Mid-Range Goals (10,000 - 50,000 LKR)
        allSuggestions.add(new Suggestion("Branded Watch", "Get a stylish new watch.", 15000));
        allSuggestions.add(new Suggestion("Day Trip to Sigiriya", "Visit and climb the historic Sigiriya rock fortress.", 25000));
        allSuggestions.add(new Suggestion("Designer Saree or Suit", "Purchase a high-quality outfit for a special occasion.", 30000));
        allSuggestions.add(new Suggestion("Gaming Headset", "Upgrade your gaming experience.", 35000));

        // Significant Purchases (50,000 - 150,000 LKR)
        allSuggestions.add(new Suggestion("Down Payment for a Scooter", "Start saving for your own two-wheeler.", 60000));
        allSuggestions.add(new Suggestion("New Television", "Upgrade your home entertainment system.", 75000));
        allSuggestions.add(new Suggestion("Gold Jewellery", "Invest in a timeless piece of gold.", 85000));
        allSuggestions.add(new Suggestion("Professional Camera Lens", "Enhance your photography skills.", 100000));
        allSuggestions.add(new Suggestion("High-End Laptop", "Get a powerful new computer for work or study.", 150000));

        // Major Goals (Over 150,000 LKR)
        allSuggestions.add(new Suggestion("International Trip to Thailand", "Save up for a vacation abroad.", 200000));
        allSuggestions.add(new Suggestion("Down Payment for a Motorbike", "Save for a more powerful bike.", 250000));
        allSuggestions.add(new Suggestion("Invest in a Tuk-Tuk", "Start a small business or generate extra income.", 500000));
    }

    private void fetchGoals() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("saving_goals")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error fetching goals.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value == null) return;

                    goalList.clear();
                    double totalSaved = 0;
                    for (QueryDocumentSnapshot doc : value) {
                        SavingGoal goal = doc.toObject(SavingGoal.class);
                        goalList.add(goal);
                        totalSaved += goal.getSavedAmount();
                    }
                    goalAdapter.notifyDataSetChanged();
                    checkIfEmpty();

                    updateSuggestions(totalSaved);
                });
    }

    private void updateSuggestions(double totalSaved) {
        List<Suggestion> affordableSuggestions = allSuggestions.stream()
                .filter(s -> totalSaved >= s.getAmountRequired())
                .collect(Collectors.toList());

        if (affordableSuggestions.isEmpty()) {
            suggestionLayout.setVisibility(View.GONE);
        } else {
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("si", "LK"));
            suggestionHeaderTextView.setText("With " + format.format(totalSaved) + ", you can afford:");

            suggestionAdapter = new SuggestionAdapter(affordableSuggestions);
            suggestionsRecyclerView.setAdapter(suggestionAdapter);
            suggestionLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAddMoneyClick(SavingGoal goal) {
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

    private void addMoneyToGoal(SavingGoal goal, double amount) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("saving_goals").document(goal.getGoalName())
                .update("savedAmount", FieldValue.increment(amount));

        String title = "Contribution to " + goal.getGoalName();
        Transaction transaction = new Transaction(title, "Savings", amount, "Expense", "Default Account", new Date());
        db.collection("users").document(userId).collection("transactions")
                .add(transaction)
                .addOnSuccessListener(documentReference -> Toast.makeText(this, "Successfully added money to goal!", Toast.LENGTH_SHORT).show());
    }

    private void checkIfEmpty() {
        if (goalList.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
        } else {
            emptyTextView.setVisibility(View.GONE);
        }
    }
}
