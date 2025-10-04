package com.viranya.fintrack.model;

public class SavingGoal {
    private String goalName;
    private double targetAmount;
    private double savedAmount;

    // Required empty constructor for Firestore
    public SavingGoal() {}

    public SavingGoal(String goalName, double targetAmount, double savedAmount) {
        this.goalName = goalName;
        this.targetAmount = targetAmount;
        this.savedAmount = savedAmount;
    }

    // --- Getters and Setters ---
    public String getGoalName() { return goalName; }
    public void setGoalName(String goalName) { this.goalName = goalName; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public double getSavedAmount() { return savedAmount; }
    public void setSavedAmount(double savedAmount) { this.savedAmount = savedAmount; }
}