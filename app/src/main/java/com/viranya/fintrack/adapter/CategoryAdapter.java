package com.viranya.fintrack.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// Adapter class for displaying a list of categories in a RecyclerView
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    // Listener interface for handling long click events on categories
    public interface OnCategoryListener {
        void onCategoryLongClick(String category);
    }

    private final List<String> categoryList; // List of category names
    private final OnCategoryListener onCategoryListener; // Listener for long click events

    // Constructor to initialize the adapter with category list and listener
    public CategoryAdapter(List<String> categoryList, OnCategoryListener onCategoryListener) {
        this.categoryList = categoryList;
        this.onCategoryListener = onCategoryListener;
    }

    // Creates a new ViewHolder for a category item
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single list item
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new CategoryViewHolder(view);
    }

    // Binds data to the ViewHolder at the specified position
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categoryList.get(position); // Get category name
        holder.categoryName.setText(category); // Set category name to TextView
        // Set long click listener for the item
        holder.itemView.setOnLongClickListener(v -> {
            if (onCategoryListener != null) {
                onCategoryListener.onCategoryLongClick(category);
                return true;
            }
            return false;
        });
    }

    // Returns the total number of items in the list
    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    // ViewHolder class for category items
    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName; // TextView to display category name

        // Constructor to initialize the TextView
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(android.R.id.text1);
        }
    }
}