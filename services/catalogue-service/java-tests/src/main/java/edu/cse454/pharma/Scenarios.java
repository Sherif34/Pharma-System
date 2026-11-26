package edu.cse454.pharma;

import java.util.Optional;

public final class Scenarios {
    private Scenarios() {}

    public record Product(String sku, String barcode) {}
    public interface CatalogueStore {
        void save(Product product);
        Optional<Product> findByBarcode(String barcode);
        boolean skuExists(String sku);
    }
    public static final class CatalogueService {
        private final CatalogueStore store;
        public CatalogueService(CatalogueStore store) { this.store = store; }
        public void importProduct(Product product) {
            if (store.skuExists(product.sku())) throw new IllegalArgumentException("Duplicate SKU");
            store.save(product);
        }
        public Optional<Product> findByBarcode(String barcode) {
            return store.findByBarcode(barcode);
        }
    }
}
