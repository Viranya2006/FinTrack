package com.viranya.fintrack.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.viranya.fintrack.R;
import com.viranya.fintrack.model.Account;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    private final List<Account> accountList;
    private final Context context;

    public AccountAdapter(List<Account> accountList, Context context) {
        this.accountList = accountList;
        this.context = context;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.account_item, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        Account account = accountList.get(position);
        holder.accountName.setText(account.getName());

        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("si", "LK"));
        holder.accountBalance.setText(format.format(account.getBalance()));
    }

    @Override
    public int getItemCount() {
        return accountList.size();
    }

    public static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView accountName, accountBalance;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            accountName = itemView.findViewById(R.id.tv_account_name);
            accountBalance = itemView.findViewById(R.id.tv_account_balance);
        }
    }
}
