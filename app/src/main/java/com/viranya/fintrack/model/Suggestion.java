package com.viranya.fintrack.model;

// Suggestion model class represents a suggestion item with title, description, and required amount
public class Suggestion {
    // Title of the suggestion
    private String title;
    // Description of the suggestion
    private String description;
    // Amount required for the suggestion
    private double amountRequired;

    // Constructor to initialize all fields of Suggestion
    public Suggestion(String title, String description, double amountRequired) {
        this.title = title;
        this.description = description;
        this.amountRequired = amountRequired;
    }

    // Getter for title
    public String getTitle() { return title; }
    // Getter for description
    public String getDescription() { return description; }
    // Getter for amountRequired
    public double getAmountRequired() { return amountRequired; }
}