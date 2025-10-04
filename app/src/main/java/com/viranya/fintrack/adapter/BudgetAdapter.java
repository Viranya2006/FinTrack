package com.viranya.fintrack.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.viranya.fintrack.R;
import com.viranya.fintrack.model.Budget;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private final List<Budget> budgetList;
    private final Context context;

    public BudgetAdapter(List<Budget> budgetList, Context context) {
        this.budgetList = budgetList;
        this.context = context;
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single budget item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.budget_item, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        // Get the budget for the current position
        Budget budget = budgetList.get(position);

        // Set the category name
        holder.category.setText(budget.getCategory());

        // Format currency for Sri Lanka (LKR)
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("si", "LK"));
        String spentFormatted = format.format(budget.getSpentAmount());
        String limitFormatted = format.format(budget.getLimitAmount());

        // Set the spending info text
        holder.info.setText(String.format("%s spent of %s limit", spentFormatted, limitFormatted));

        // Calculate and set the progress for the ProgressBar
        int progress = 0;
        if (budget.getLimitAmount() > 0) {
            progress = (int) ((budget.getSpentAmount() / budget.getLimitAmount()) * 100);
        }
        holder.progressBar.setProgress(progress);

        // Change progress bar color to red if budget is exceeded
        if (progress > 100) {
            holder.progressBar.setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.custom_progress_bar_warning));
        } else {
            holder.progressBar.setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.custom_progress_bar));
        }
    }

    @Override
    public int getItemCount() {
        return budgetList.size();
    }

    /**
     * ViewHolder class to hold the UI elements for each budget item.
     */
    public static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView category, info;
        ProgressBar progressBar;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            category = itemView.findViewById(R.id.tv_budget_category);
            info = itemView.findViewById(R.id.tv_budget_info);
            progressBar = itemView.findViewById(R.id.progress_bar_budget);
        }
    }
}