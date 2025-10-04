package com.viranya.fintrack.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Transaction implements java.io.Serializable {

    @Exclude // This prevents the document ID from being saved back into the document itself
    private String documentId;

    private String title;
    private String category;
    private double amount;
    private String type; // "Income" or "Expense"
    @ServerTimestamp
    private Date date;

    // --- Constructors ---

    /**
     * An empty constructor is required by Firestore for converting documents back into objects.
     */
    public Transaction() {}

    /**
     * This is the constructor we use in our code to create a new transaction object.
     * This was the missing piece that caused the error.
     */
    public Transaction(String title, String category, double amount, String type, Date date) {
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.type = type;
        this.date = date;
    }

    // --- Getters and Setters ---

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }



    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
}