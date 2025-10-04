package com.viranya.fintrack;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.viranya.fintrack.adapter.AccountAdapter;
import com.viranya.fintrack.fragment.AddAccountBottomSheetFragment;
import com.viranya.fintrack.model.Account;

import java.util.ArrayList;
import java.util.List;

public class AccountsActivity extends AppCompatActivity {

    // --- UI Elements ---
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private TextView emptyTextView;

    // --- Firebase & Adapter ---
    private AccountAdapter adapter;
    private List<Account> accountList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);

        // --- Initialize Firebase ---
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // --- Bind UI Elements ---
        recyclerView = findViewById(R.id.rv_accounts);
        fab = findViewById(R.id.fab_add_account);
        emptyTextView = findViewById(R.id.tv_empty_accounts);

        // --- Setup RecyclerView ---
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        accountList = new ArrayList<>();
        adapter = new AccountAdapter(accountList, this);
        recyclerView.setAdapter(adapter);

        // --- Setup Listeners ---
        fab.setOnClickListener(v -> {
            // Show the AddAccountBottomSheetFragment when the FAB is clicked
            AddAccountBottomSheetFragment bottomSheet = new AddAccountBottomSheetFragment();
            bottomSheet.show(getSupportFragmentManager(), "AddAccountBottomSheet");
        });

        // --- Fetch initial data ---
        fetchAccounts();
    }

    /**
     * Fetches the list of accounts from Firestore and listens for real-time updates.
     */
    private void fetchAccounts() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("accounts")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error fetching accounts.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    accountList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Account account = doc.toObject(Account.class);
                        accountList.add(account);
                    }
                    adapter.notifyDataSetChanged();
                    checkIfEmpty();
                });
    }

    /**
     * Toggles the visibility of the "empty" message based on the list size.
     */
    private void checkIfEmpty() {
        if (accountList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.GONE);
        }
    }
}
