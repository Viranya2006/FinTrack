package com.viranya.fintrack.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.viranya.fintrack.R;
import com.viranya.fintrack.model.Transaction;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    // --- Interface to handle click events on items ---
    public interface OnTransactionListener {
        void onTransactionClick(Transaction transaction); // For editing
        void onTransactionLongClick(Transaction transaction); // For deleting
    }

    private final List<Transaction> transactionList;
    private final Context context;
    private final OnTransactionListener onTransactionListener;

    /**
     * Constructor for the adapter.
     * @param transactionList The list of transactions to display.
     * @param context The context of the calling fragment.
     * @param onTransactionListener The listener to handle clicks.
     */
    public TransactionAdapter(List<Transaction> transactionList, Context context, OnTransactionListener onTransactionListener) {
        this.transactionList = transactionList;
        this.context = context;
        this.onTransactionListener = onTransactionListener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single transaction item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_item, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        // Get the data model for this position
        Transaction transaction = transactionList.get(position);

        // Bind the data to the views in the ViewHolder
        holder.title.setText(transaction.getTitle());
        holder.category.setText(transaction.getCategory());

        // Format currency for Sri Lanka (LKR)
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("si", "LK"));
        String formattedAmount = format.format(transaction.getAmount());

        // Check the transaction type and set the amount text and color accordingly
        if ("Expense".equals(transaction.getType())) {
            holder.amount.setText("- " + formattedAmount);
            holder.amount.setTextColor(ContextCompat.getColor(context, R.color.vibrant_coral));
        } else {
            holder.amount.setText("+ " + formattedAmount);
            holder.amount.setTextColor(ContextCompat.getColor(context, R.color.teal_green));
        }
        // --- Set the regular click listener for editing---

        holder.itemView.setOnClickListener(v -> {
            if (onTransactionListener != null) {
                onTransactionListener.onTransactionClick(transaction);
            }
        });

        // --- Set the long-click listener for deletion ---
        holder.itemView.setOnLongClickListener(v -> {
            if (onTransactionListener != null) {
                // Notify the fragment that an item was long-clicked
                onTransactionListener.onTransactionLongClick(transaction);
                return true; // Return true to indicate the event was handled
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        // Return the total number of items in the list
        return transactionList.size();
    }

    /**
     * ViewHolder class that holds the UI elements for a single item in the RecyclerView.
     */
    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView title, category, amount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_transaction_title);
            category = itemView.findViewById(R.id.tv_transaction_category);
            amount = itemView.findViewById(R.id.tv_transaction_amount);
        }
    }
}