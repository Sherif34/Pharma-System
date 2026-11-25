package com.pharma.pricing;

public class PricingService {
    private final IAccessControl accessControl;
    private final ICatalogue catalogue;
    private final IPriceUpdateRepository priceUpdateRepository;
    private final IAuditTrailRepository auditTrailRepository;

    public PricingService(
            IAccessControl accessControl,
            ICatalogue catalogue,
            IPriceUpdateRepository priceUpdateRepository,
            IAuditTrailRepository auditTrailRepository) {
        this.accessControl = accessControl;
        this.catalogue = catalogue;
        this.priceUpdateRepository = priceUpdateRepository;
        this.auditTrailRepository = auditTrailRepository;
    }

    public PriceUpdate submitPriceUpdate(String updateId, String sku, double newPrice, String userId) {
        if (!accessControl.checkPermission(userId, "PRICE_SUBMIT")) {
            throw new SecurityException("User not authorized to submit price updates");
        }
        if (!catalogue.skuExists(sku)) {
            throw new IllegalArgumentException("Unknown SKU: " + sku);
        }
        double oldPrice = catalogue.getCurrentPrice(sku);
        PriceUpdate update = new PriceUpdate(updateId, sku, oldPrice, newPrice);
        update.submit();
        priceUpdateRepository.save(update);
        auditTrailRepository.save(new AuditTrailEntry(userId, "SUBMIT_PRICE_UPDATE", updateId));
        return update;
    }
}
