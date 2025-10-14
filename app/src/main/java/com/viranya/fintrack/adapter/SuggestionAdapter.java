package com.viranya.fintrack.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.viranya.fintrack.R;
import com.viranya.fintrack.model.Suggestion;
import java.util.List;

// SuggestionAdapter is a RecyclerView.Adapter for displaying Suggestion items
public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder> {

    // Holds the list of suggestions to display
    private final List<Suggestion> suggestionList;

    // Constructor initializes the adapter with a list of suggestions
    public SuggestionAdapter(List<Suggestion> suggestionList) {
        this.suggestionList = suggestionList;
    }

    // Called when RecyclerView needs a new ViewHolder for a suggestion item
    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single suggestion item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.suggestion_item, parent, false);
        return new SuggestionViewHolder(view);
    }

    // Binds the data from a Suggestion object to the ViewHolder
    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        Suggestion suggestion = suggestionList.get(position);
        holder.title.setText(suggestion.getTitle());
        holder.description.setText(suggestion.getDescription());
    }

    // Returns the total number of suggestion items
    @Override
    public int getItemCount() {
        return suggestionList.size();
    }

    // ViewHolder class for holding the views of a suggestion item
    public static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        TextView title, description;
        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find and assign the TextViews from the layout
            title = itemView.findViewById(R.id.tv_suggestion_title);
            description = itemView.findViewById(R.id.tv_suggestion_description);
        }
    }
}