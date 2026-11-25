package com.pharma.pricing;

public interface IAuditTrailRepository {
    void save(AuditTrailEntry entry);
}
