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

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder> {

    private final List<Suggestion> suggestionList;

    public SuggestionAdapter(List<Suggestion> suggestionList) {
        this.suggestionList = suggestionList;
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.suggestion_item, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        Suggestion suggestion = suggestionList.get(position);
        holder.title.setText(suggestion.getTitle());
        holder.description.setText(suggestion.getDescription());
    }

    @Override
    public int getItemCount() {
        return suggestionList.size();
    }

    public static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        TextView title, description;
        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_suggestion_title);
            description = itemView.findViewById(R.id.tv_suggestion_description);
        }
    }
}