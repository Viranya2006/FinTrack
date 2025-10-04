package com.viranya.fintrack.model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;

public class Account implements Serializable {
    @Exclude
    private String documentId; // To hold the document ID from Firestore
    private String name;
    private double balance;

    public Account() {}

    public Account(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    // --- Getters and Setters ---
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}