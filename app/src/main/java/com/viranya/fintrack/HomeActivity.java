package com.viranya.fintrack;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.viranya.fintrack.fragment.BudgetsFragment;
import com.viranya.fintrack.fragment.HomeFragment;
import com.viranya.fintrack.fragment.ProfileFragment;
import com.viranya.fintrack.fragment.TransactionsFragment;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Load the default fragment when the app starts
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // Set up the listener for bottom navigation
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_transactions) {
                selectedFragment = new TransactionsFragment();
            } else if (itemId == R.id.nav_budgets) {
                selectedFragment = new BudgetsFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    /**
     * Helper method to completely replace the fragment in the container.
     * @param fragment The fragment to display.
     */
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        // The replace() method removes the current fragment and adds the new one.
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }
}