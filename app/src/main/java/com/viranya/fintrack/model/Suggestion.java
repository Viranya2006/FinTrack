package com.viranya.fintrack.model;

public class Suggestion {
    private String title;
    private String description;
    private double amountRequired;

    public Suggestion(String title, String description, double amountRequired) {
        this.title = title;
        this.description = description;
        this.amountRequired = amountRequired;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getAmountRequired() { return amountRequired; }
}