package com.viranya.fintrack.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface OnCategoryListener {
        void onCategoryLongClick(String category);
    }

    private final List<String> categoryList;
    private final OnCategoryListener onCategoryListener;

    public CategoryAdapter(List<String> categoryList, OnCategoryListener onCategoryListener) {
        this.categoryList = categoryList;
        this.onCategoryListener = onCategoryListener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categoryList.get(position);
        holder.categoryName.setText(category);
        holder.itemView.setOnLongClickListener(v -> {
            if (onCategoryListener != null) {
                onCategoryListener.onCategoryLongClick(category);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName;
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(android.R.id.text1);
        }
    }
}