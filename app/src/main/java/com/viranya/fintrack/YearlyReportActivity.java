package com.viranya.fintrack;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.viranya.fintrack.model.Transaction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * This activity displays a yearly financial report using a line chart,
 * showing total income and expenses for each month of the current year.
 */
public class YearlyReportActivity extends AppCompatActivity {

    // --- UI Elements ---
    private LineChart lineChart;
    private TextView tvReportTitle, tvReportYear;

    // --- Firebase Services ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yearly_report);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Bind UI elements
        lineChart = findViewById(R.id.line_chart);
        tvReportTitle = findViewById(R.id.tv_report_title);
        tvReportYear = findViewById(R.id.tv_report_year);

        // Fetch and display the yearly data
        fetchYearlyData();
    }

    /**
     * Fetches all transactions for the current year, processes them month by month,
     * and then calls the method to set up the line chart.
     */
    private void fetchYearlyData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        // --- Determine the start and end of the current year ---
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        tvReportYear.setText("For the Year " + currentYear); // Update the title

        calendar.set(currentYear, Calendar.JANUARY, 1, 0, 0, 0);
        Date startOfYear = calendar.getTime();

        calendar.set(currentYear, Calendar.DECEMBER, 31, 23, 59, 59);
        Date endOfYear = calendar.getTime();

        // --- Query Firestore for all transactions within the current year ---
        db.collection("users").document(userId).collection("transactions")
                .whereGreaterThanOrEqualTo("date", startOfYear)
                .whereLessThanOrEqualTo("date", endOfYear)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No data found for the current year.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // --- Process the Data Month by Month ---
                    // Create arrays to hold the totals for each of the 12 months.
                    float[] monthlyIncome = new float[12];
                    float[] monthlyExpenses = new float[12];

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction transaction = doc.toObject(Transaction.class);
                        calendar.setTime(transaction.getDate());
                        int month = calendar.get(Calendar.MONTH); // 0 = January, 1 = February, etc.

                        if ("Income".equals(transaction.getType())) {
                            monthlyIncome[month] += transaction.getAmount();
                        } else {
                            monthlyExpenses[month] += transaction.getAmount();
                        }
                    }

                    // Now that the data is processed, set up the chart.
                    setupLineChart(monthlyIncome, monthlyExpenses);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch yearly data.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Configures and displays the line chart with the processed monthly data.
     * @param monthlyIncome An array of 12 floats representing total income for each month.
     * @param monthlyExpenses An array of 12 floats representing total expense for each month.
     */
    private void setupLineChart(float[] monthlyIncome, float[] monthlyExpenses) {
        // --- Create Data Entries for the Chart ---
        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            incomeEntries.add(new Entry(i, monthlyIncome[i]));
            expenseEntries.add(new Entry(i, monthlyExpenses[i]));
        }

        // --- Create and Style the Income Data Set ---
        LineDataSet incomeDataSet = new LineDataSet(incomeEntries, "Income");
        incomeDataSet.setColor(ContextCompat.getColor(this, R.color.teal_green));
        incomeDataSet.setCircleColor(ContextCompat.getColor(this, R.color.teal_green));
        incomeDataSet.setLineWidth(2.5f);
        incomeDataSet.setCircleRadius(4f);
        incomeDataSet.setValueTextSize(15f);

        // --- Create and Style the Expense Data Set ---
        LineDataSet expenseDataSet = new LineDataSet(expenseEntries, "Expenses");
        expenseDataSet.setColor(ContextCompat.getColor(this, R.color.vibrant_coral));
        expenseDataSet.setCircleColor(ContextCompat.getColor(this, R.color.vibrant_coral));
        expenseDataSet.setLineWidth(2.5f);
        expenseDataSet.setCircleRadius(4f);
        expenseDataSet.setValueTextSize(15f);

        // --- Combine the data sets and set them to the chart ---
        LineData lineData = new LineData(incomeDataSet, expenseDataSet);
        lineChart.setData(lineData);

        // --- Style the Chart's Axes and General Appearance ---
        // X-Axis (Bottom Axis with Month Names)
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        final String[] months = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        // Y-Axis (Left and Right)
        lineChart.getAxisRight().setEnabled(false); // Disable the right-side axis
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setAxisMinimum(0f); // Start the Y-axis at 0

        // General Chart Styling
        lineChart.getDescription().setEnabled(false); // Hide the description text
        lineChart.animateX(1500); // Animate the chart drawing
        lineChart.invalidate(); // Refresh the chart
    }
}