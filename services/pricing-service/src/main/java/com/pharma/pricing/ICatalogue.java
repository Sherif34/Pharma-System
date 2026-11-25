package com.pharma.pricing;

public interface ICatalogue {
    boolean skuExists(String sku);
    double getCurrentPrice(String sku);
}
