package com.viranya.fintrack.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.viranya.fintrack.model.SavingGoal;

public class AddGoalBottomSheetFragment extends BottomSheetDialogFragment {

    private TextInputEditText etGoalName, etTargetAmount;
    private Button btnSaveGoal;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_goal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etGoalName = view.findViewById(R.id.et_goal_name);
        etTargetAmount = view.findViewById(R.id.et_target_amount);
        btnSaveGoal = view.findViewById(R.id.btn_save_goal);

        btnSaveGoal.setOnClickListener(v -> saveGoal());
    }

    private void saveGoal() {
        String goalName = etGoalName.getText().toString().trim();
        String targetAmountStr = etTargetAmount.getText().toString().trim();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Validate input
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(goalName)) {
            etGoalName.setError("Goal name cannot be empty.");
            return;
        }
        if (TextUtils.isEmpty(targetAmountStr) || Double.parseDouble(targetAmountStr) <= 0) {
            etTargetAmount.setError("Target amount must be greater than zero.");
            return;
        }

        double targetAmount = Double.parseDouble(targetAmountStr);

        // Create new SavingGoal object
        SavingGoal newGoal = new SavingGoal(goalName, targetAmount, 0); // savedAmount is initially 0

        // Save to Firestore using the goal name as the document ID
        String userId = currentUser.getUid();
        db.collection("users").document(userId).collection("saving_goals")
                .document(goalName)
                .set(newGoal)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Savings goal created!", Toast.LENGTH_SHORT).show();
                    dismiss(); // Close the bottom sheet
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error creating goal.", Toast.LENGTH_SHORT).show();
                });
    }
}