package edu.cse454.pharma;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * stock-review-service (FR12 Approve Stock Adjustments).
 * Mirrors the Part A class diagram: AdjustmentApprovalManager with its required
 * interfaces IAccessControl and IInventory and the StockReviewRepository.
 */
public final class Scenarios {
    private Scenarios() {}

    /** State diagram: PENDING -> APPROVED -> APPLIED, or PENDING -> REJECTED. */
    public enum AdjustmentStatus { PENDING, APPROVED, APPLIED, REJECTED }

    /** Class AdjustmentRequest. Every state change goes through a guarded method. */
    public static final class AdjustmentRequest {
        private final String requestId;
        private final String reviewId;
        private final String sku;
        private final String batchId;
        private final int correctionQty;
        private final String reason;
        private final String requestedBy;
        private final LocalDateTime requestedAt;
        private AdjustmentStatus status = AdjustmentStatus.PENDING;
        private String approvedBy;
        private LocalDateTime decidedAt;

        public AdjustmentRequest(String requestId, String reviewId, String sku, String batchId,
                                 int correctionQty, String reason, String requestedBy,
                                 LocalDateTime requestedAt) {
            this.requestId = requestId;
            this.reviewId = reviewId;
            this.sku = sku;
            this.batchId = batchId;
            this.correctionQty = correctionQty;
            this.reason = reason;
            this.requestedBy = requestedBy;
            this.requestedAt = requestedAt;
        }

        public boolean isPending() { return status == AdjustmentStatus.PENDING; }
        public boolean canApply() { return isPending(); }

        public void approve(String approver, LocalDateTime when) {
            require(AdjustmentStatus.PENDING, AdjustmentStatus.APPROVED);
            status = AdjustmentStatus.APPROVED;
            approvedBy = approver;
            decidedAt = when;
        }
        public void reject(String approver, LocalDateTime when) {
            require(AdjustmentStatus.PENDING, AdjustmentStatus.REJECTED);
            status = AdjustmentStatus.REJECTED;
            approvedBy = approver;
            decidedAt = when;
        }
        public void markApplied() {
            require(AdjustmentStatus.APPROVED, AdjustmentStatus.APPLIED);
            status = AdjustmentStatus.APPLIED;
        }
        private void require(AdjustmentStatus from, AdjustmentStatus to) {
            if (status != from) {
                throw new IllegalStateException("Invalid transition " + status + " -> " + to);
            }
        }

        public String requestId() { return requestId; }
        public String reviewId() { return reviewId; }
        public String sku() { return sku; }
        public String batchId() { return batchId; }
        public int correctionQty() { return correctionQty; }
        public String reason() { return reason; }
        public String requestedBy() { return requestedBy; }
        public LocalDateTime requestedAt() { return requestedAt; }
        public AdjustmentStatus status() { return status; }
        public String approvedBy() { return approvedBy; }
        public LocalDateTime decidedAt() { return decidedAt; }
    }

    /** Required interface IAccessControl (a mock in the object diagram). */
    public interface IAccessControl {
        boolean checkPermission(String staff, String action);
    }

    /** Required interface IInventory (a mock in the object diagram). */
    public interface IInventory {
        Result applyCorrection(String batch, int qty, String reason);
    }

    public record Result(boolean ok, String error) {
        public static Result success() { return new Result(true, null); }
        public static Result failure(String error) { return new Result(false, error); }
    }

    public record ApprovalResult(String requestId, AdjustmentStatus status, boolean applied) {}

    /** Class StockReviewRepository (only the two methods FR12 uses). */
    public interface StockReviewRepository {
        AdjustmentRequest saveRequest(AdjustmentRequest request);
        Optional<AdjustmentRequest> findRequest(String requestId);
    }

    /** Class AdjustmentApprovalManager. */
    public static final class AdjustmentApprovalManager {
        private final IInventory inventory;
        private final IAccessControl access;
        private final StockReviewRepository repo;
        private final Supplier<LocalDateTime> clock;

        public AdjustmentApprovalManager(IInventory inventory, IAccessControl access,
                                         StockReviewRepository repo, Supplier<LocalDateTime> clock) {
            this.inventory = inventory;
            this.access = access;
            this.repo = repo;
            this.clock = clock;
        }

        /** Messages 1.1 checkPermission, 1.2 create PENDING, 1.3 saveRequest. */
        public AdjustmentRequest requestAdjustment(String requestId, String reviewId, String staff,
                                                   String sku, String batch, int correctionQty,
                                                   String reason) {
            requirePermission(staff, "requestAdjustment");
            AdjustmentRequest request = new AdjustmentRequest(requestId, reviewId, sku, batch,
                    correctionQty, reason, staff, clock.get());
            return repo.saveRequest(request);
        }

        /** Messages 2.1 checkPermission, 2.2 findRequest, 2.3 canApply, 2.4 applyCorrection, 2.5 saveRequest. */
        public ApprovalResult approveAdjustment(String requestId, String approver, boolean approve) {
            requirePermission(approver, "approveAdjustment");
            AdjustmentRequest request = repo.findRequest(requestId)
                    .orElseThrow(() -> new NoSuchElementException("request " + requestId + " not found"));
            if (!request.canApply()) {
                throw new IllegalStateException("request " + requestId + " is " + request.status());
            }
            if (!approve) {
                request.reject(approver, clock.get());
                repo.saveRequest(request);
                return new ApprovalResult(requestId, request.status(), false);
            }
            request.approve(approver, clock.get());
            Result outcome = inventory.applyCorrection(request.batchId(), request.correctionQty(),
                    request.reason());
            if (outcome != null && outcome.ok()) {
                request.markApplied();
            }
            repo.saveRequest(request);
            return new ApprovalResult(requestId, request.status(),
                    request.status() == AdjustmentStatus.APPLIED);
        }

        private void requirePermission(String staff, String action) {
            if (!access.checkPermission(staff, action)) {
                throw new SecurityException(staff + " may not " + action);
            }
        }
    }
}
