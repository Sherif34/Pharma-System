package com.pharma.pricing;

public class AuditTrailEntry {
    private final String userId;
    private final String action;
    private final String details;

    public AuditTrailEntry(String userId, String action, String details) {
        this.userId = userId;
        this.action = action;
        this.details = details;
    }

    public String getUserId() { return userId; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
}
