package com.viranya.fintrack.model;

public class Budget {
    private String category;
    private double limitAmount;
    private double spentAmount;

    // Required empty constructor for Firestore
    public Budget() {}

    public Budget(String category, double limitAmount, double spentAmount) {
        this.category = category;
        this.limitAmount = limitAmount;
        this.spentAmount = spentAmount;
    }

    // --- Getters and Setters ---
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getLimitAmount() { return limitAmount; }
    public void setLimitAmount(double limitAmount) { this.limitAmount = limitAmount; }

    public double getSpentAmount() { return spentAmount; }
    public void setSpentAmount(double spentAmount) { this.spentAmount = spentAmount; }
}