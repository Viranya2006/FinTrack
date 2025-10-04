package com.viranya.fintrack.model;

public class Account {
    private String name;
    private double balance;

    // Required empty constructor for Firestore
    public Account() {}

    public Account(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    // --- Getters and Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}