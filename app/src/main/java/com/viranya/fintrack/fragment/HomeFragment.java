package com.viranya.fintrack.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.viranya.fintrack.R;
import com.viranya.fintrack.SavingsGoalsActivity;
import com.viranya.fintrack.adapter.TransactionAdapter;
import com.viranya.fintrack.model.Transaction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class HomeFragment extends Fragment {

    // --- UI Elements ---
    private TextView tvTotalBalance, tvMonthlyIncome, tvMonthlyExpense;
    private PieChart pieChart;
    private BarChart barChart;
    private RecyclerView rvRecentTransactions;

    // --- Firebase & Data ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TransactionAdapter recentTransactionsAdapter;
    private final List<Transaction> recentTransactionList = new ArrayList<>();

    // --- Data Holders for Calculation ---
    // We use AtomicReference to safely share values between the asynchronous listeners.
    private final AtomicReference<Double> totalAccountBalance = new AtomicReference<>(0.0);
    private final AtomicReference<Double> currentMonthIncome = new AtomicReference<>(0.0);
    private final AtomicReference<Double> currentMonthExpense = new AtomicReference<>(0.0);


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        tvMonthlyIncome = view.findViewById(R.id.tv_monthly_income);
        tvMonthlyExpense = view.findViewById(R.id.tv_monthly_expense);
        pieChart = view.findViewById(R.id.pie_chart);
        barChart = view.findViewById(R.id.bar_chart);
        rvRecentTransactions = view.findViewById(R.id.rv_recent_transactions);
        MaterialCardView savingsCard = view.findViewById(R.id.card_savings_goals);

        setupRecentTransactionsList();
        savingsCard.setOnClickListener(v -> startActivity(new Intent(requireActivity(), SavingsGoalsActivity.class)));

        fetchDashboardData();
    }
    private void setupRecentTransactionsList() {
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        recentTransactionsAdapter = new TransactionAdapter(recentTransactionList, getContext(), null);
        rvRecentTransactions.setAdapter(recentTransactionsAdapter);
    }

    private void fetchDashboardData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        // Listener 1: Get the sum of all account balances
        db.collection("users").document(userId).collection("accounts")
                .addSnapshotListener((value, error) -> {
                    if (getContext() == null || error != null) return;

                    double balanceSum = 0;
                    for (QueryDocumentSnapshot doc : value) {
                        if (doc.contains("balance")) {
                            balanceSum += doc.getDouble("balance");
                        }
                    }
                    totalAccountBalance.set(balanceSum);
                    updateTotalBalance();
                });

        // Calculate start and end of the current month
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date startOfMonth = calendar.getTime();
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date endOfMonth = calendar.getTime();

        // Listener 2: Get all stats for the current month
        db.collection("users").document(userId).collection("transactions")
                .whereGreaterThanOrEqualTo("date", startOfMonth)
                .whereLessThanOrEqualTo("date", endOfMonth)
                .addSnapshotListener((value, error) -> {
                    if (getContext() == null || error != null) return;

                    double monthlyIncome = 0;
                    double monthlyExpense = 0;
                    Map<String, Double> expenseByCategory = new HashMap<>();

                    for (QueryDocumentSnapshot doc : value) {
                        Transaction transaction = doc.toObject(Transaction.class);
                        if ("Income".equals(transaction.getType())) {
                            monthlyIncome += transaction.getAmount();
                        } else {
                            monthlyExpense += transaction.getAmount();
                            String category = transaction.getCategory();
                            expenseByCategory.put(category, expenseByCategory.getOrDefault(category, 0.0) + transaction.getAmount());
                        }
                    }

                    NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("si", "LK"));
                    tvMonthlyIncome.setText(format.format(monthlyIncome));
                    tvMonthlyExpense.setText(format.format(monthlyExpense));

                    // Store monthly values for the total balance calculation
                    currentMonthIncome.set(monthlyIncome);
                    currentMonthExpense.set(monthlyExpense);
                    updateTotalBalance();

                    setupPieChart(expenseByCategory);
                    setupBarChart(monthlyIncome, monthlyExpense);
                });

        // Listener 3: Get the 5 most recent transactions
        db.collection("users").document(userId).collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener((value, error) -> {
                    if (getContext() == null || error != null) return;
                    recentTransactionList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        recentTransactionList.add(doc.toObject(Transaction.class));
                    }
                    recentTransactionsAdapter.notifyDataSetChanged();
                });
    }

    /**
     * A helper method to calculate and display the final total balance based on your new formula.
     */
    private void updateTotalBalance() {
        // --- THIS IS THE CORRECTED LOGIC ---
        // Final Total = (Sum of All Account Balances) + (This Month's Income) - (This Month's Expense)
        double finalTotal = totalAccountBalance.get() + currentMonthIncome.get() - currentMonthExpense.get();
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("si", "LK"));
        tvTotalBalance.setText(format.format(finalTotal));
    }
    // --- Chart Setup Methods
    private void setupPieChart(Map<String, Double> expenseData) {
        if (getContext() == null || expenseData == null || expenseData.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("No expense data for this month.");
            pieChart.invalidate();
            return;
        }
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : expenseData.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }
        PieDataSet dataSet = new PieDataSet(entries, "Expense Distribution");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);
        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Expenses");
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void setupBarChart(double income, double expense) {
        if (getContext() == null) return;
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) income));
        entries.add(new BarEntry(1, (float) expense));
        BarDataSet dataSet = new BarDataSet(entries, "Monthly Overview");
        int incomeColor = ContextCompat.getColor(getContext(), R.color.teal_green);
        int expenseColor = ContextCompat.getColor(getContext(), R.color.vibrant_coral);
        dataSet.setColors(incomeColor, expenseColor);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);
        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Income", "Expense"}));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }
}

