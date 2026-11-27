package edu.cse454.pharma;

import java.util.Objects;
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

    public enum Approval { PENDING, APPROVED, REJECTED }
    public enum Reason { SPLIT_BLISTER, SAMPLE, RETURN, DAMAGE }
    public record Line(String sku, int quantity) {}
    public record Request(String id, String supplier, java.time.LocalDate delivery,
                          java.util.List<Line> lines, Approval approval) {
        public Request withDecision(Approval decision) {
            if (approval != Approval.PENDING || decision == null || decision == Approval.PENDING) {
                throw new IllegalStateException("Invalid approval transition");
            }
            return new Request(id, supplier, delivery, lines, decision);
        }
    }

    public interface Access {
        boolean authorize(String user, String action);
    }
    public interface RequestStore {
        Optional<Request> findById(String id);
        Request save(Request request);
    }
    public static final class PurchasingService {
        private final Access access;
        private final RequestStore store;
        public PurchasingService(Access access, RequestStore store) {
            this.access = access;
            this.store = store;
        }
        public Request recordApprovalState(String user, String id, Approval decision) {
            if (!access.authorize(user, "approve-request")) throw new SecurityException("Denied");
            Request request = store.findById(id).orElseThrow();
            return store.save(request.withDecision(decision));
        }
    }

    public record Snapshot(String sku, String batch, int quantity, int version) {}
    public record Movement(String id, String sku, String batch, int delta, Reason reason) {}
    public interface Catalogue {
        boolean skuExists(String sku);
    }
    public interface InventoryStore {
        Optional<Snapshot> getBatchSnapshot(String batch);
        Snapshot postMovementOnce(Movement movement, int expectedVersion);
    }
    public static final class InventoryService {
        private final Access access;
        private final Catalogue catalogue;
        private final InventoryStore store;
        public InventoryService(Access access, Catalogue catalogue, InventoryStore store) {
            this.access = access;
            this.catalogue = catalogue;
            this.store = store;
        }
        public Snapshot recordDamage(String user, String id, String sku, String batch, int quantity) {
            if (!access.authorize(user, "record-movement")) throw new SecurityException("Denied");
            if (quantity <= 0 || !catalogue.skuExists(sku)) throw new IllegalArgumentException("Invalid movement");
            Snapshot snapshot = store.getBatchSnapshot(batch).orElseThrow();
            if (!Objects.equals(snapshot.sku(), sku) || quantity > snapshot.quantity()) {
                throw new IllegalArgumentException("Invalid batch or quantity");
            }
            return store.postMovementOnce(new Movement(id, sku, batch, -quantity, Reason.DAMAGE),
                                          snapshot.version());
        }
    }
}
