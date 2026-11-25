package com.pharma.pricing;

public class PriceUpdate {
    private final String id;
    private final String sku;
    private final double oldPrice;
    private final double newPrice;
    private PriceUpdateStatus status;

    public PriceUpdate(String id, String sku, double oldPrice, double newPrice) {
        this.id = id;
        this.sku = sku;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.status = PriceUpdateStatus.DRAFT;
    }

    public void submit() {
        if (status != PriceUpdateStatus.DRAFT) {
            throw new IllegalStateException("Cannot submit from status " + status);
        }
        this.status = PriceUpdateStatus.PENDING_APPROVAL;
    }

    public void approve() {
        if (status != PriceUpdateStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Cannot approve from status " + status);
        }
        this.status = PriceUpdateStatus.APPROVED;
    }

    public void reject() {
        if (status != PriceUpdateStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Cannot reject from status " + status);
        }
        this.status = PriceUpdateStatus.REJECTED;
    }

    public void activateDuePrices() {
        if (status != PriceUpdateStatus.APPROVED) {
            throw new IllegalStateException("Cannot activate from status " + status);
        }
        this.status = PriceUpdateStatus.ACTIVE;
    }

    public void expire() {
        if (status != PriceUpdateStatus.ACTIVE) {
            throw new IllegalStateException("Cannot expire from status " + status);
        }
        this.status = PriceUpdateStatus.EXPIRED;
    }

    public String getId() { return id; }
    public String getSku() { return sku; }
    public double getOldPrice() { return oldPrice; }
    public double getNewPrice() { return newPrice; }
    public PriceUpdateStatus getStatus() { return status; }
}
