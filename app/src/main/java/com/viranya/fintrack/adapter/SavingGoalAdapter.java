package com.viranya.fintrack.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.viranya.fintrack.R;
import com.viranya.fintrack.model.SavingGoal;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SavingGoalAdapter extends RecyclerView.Adapter<SavingGoalAdapter.SavingGoalViewHolder> {

    // Interface to handle click events on items
    public interface OnGoalListener {
        void onAddMoneyClick(SavingGoal goal);
    }

    private final List<SavingGoal> goalList;
    private final Context context;
    private final OnGoalListener onGoalListener;

    /**
     * Constructor for the adapter.
     * @param goalList The list of savings goals to display.
     * @param context The context of the calling activity/fragment.
     * @param onGoalListener The listener to handle clicks.
     */
    public SavingGoalAdapter(List<SavingGoal> goalList, Context context, OnGoalListener onGoalListener) {
        this.goalList = goalList;
        this.context = context;
        this.onGoalListener = onGoalListener;
    }

    @NonNull
    @Override
    public SavingGoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single savings goal item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.saving_goal_item, parent, false);
        return new SavingGoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavingGoalViewHolder holder, int position) {
        // Get the data model for this position
        SavingGoal goal = goalList.get(position);

        // Bind the data to the views in the ViewHolder
        holder.goalName.setText(goal.getGoalName());

        // Format currency amounts for Sri Lanka (LKR)
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("si", "LK"));
        String savedFormatted = format.format(goal.getSavedAmount());
        String targetFormatted = format.format(goal.getTargetAmount());

        // Set the progress text
        holder.progressText.setText(String.format("Saved %s of %s", savedFormatted, targetFormatted));

        // Calculate and set the progress for the ProgressBar
        int progress = 0;
        if (goal.getTargetAmount() > 0) {
            progress = (int) ((goal.getSavedAmount() / goal.getTargetAmount()) * 100);
        }
        holder.progressBar.setProgress(progress);

        // Set the click listener for the "Add" button, passing the specific goal object
        holder.addMoneyButton.setOnClickListener(v -> onGoalListener.onAddMoneyClick(goal));
    }

    @Override
    public int getItemCount() {
        // Return the total number of items in the list
        return goalList.size();
    }

    /**
     * ViewHolder class that holds the UI elements for a single item in the RecyclerView.
     */
    public static class SavingGoalViewHolder extends RecyclerView.ViewHolder {
        TextView goalName, progressText;
        ProgressBar progressBar;
        MaterialButton addMoneyButton;

        public SavingGoalViewHolder(@NonNull View itemView) {
            super(itemView);
            goalName = itemView.findViewById(R.id.tv_goal_name);
            progressText = itemView.findViewById(R.id.tv_goal_progress_text);
            progressBar = itemView.findViewById(R.id.progress_bar_goal);
            addMoneyButton = itemView.findViewById(R.id.btn_add_money);
        }
    }
}