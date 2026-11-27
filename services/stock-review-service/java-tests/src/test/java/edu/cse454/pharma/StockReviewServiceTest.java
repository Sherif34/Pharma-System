package edu.cse454.pharma;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import edu.cse454.pharma.Scenarios.*;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * FR12 Approve Stock Adjustments.
 * Fixtures  = objects of the Part A object diagram (ADJ-001, SKU-0412, B-2407, U-01, U-02).
 * Mocks     = the «mock» objects (access, inventory) and the repository.
 * Verify    = the numbered messages of the collaboration diagram (1.1 ... 2.5).
 * Tests of AdjustmentRequest cover every transition of the state diagram.
 */
class StockReviewServiceTest {
    private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 11, 24, 18, 30);
    private static final LocalDateTime DECIDED_AT = LocalDateTime.of(2026, 11, 25, 9, 0);

    private IAccessControl access;
    private IInventory inventory;
    private StockReviewRepository repo;
    private AdjustmentApprovalManager aam;
    private AdjustmentRequest req;

    @BeforeEach
    void setUp() {
        access = mock(IAccessControl.class);
        inventory = mock(IInventory.class);
        repo = mock(StockReviewRepository.class);
        aam = new AdjustmentApprovalManager(inventory, access, repo, () -> DECIDED_AT);
        req = pendingRequest();
    }

    private static AdjustmentRequest pendingRequest() {
        return new AdjustmentRequest("ADJ-001", "REV-001", "SKU-0412", "B-2407", -4,
                "Counted 38, recorded 42", "U-02", REQUESTED_AT);
    }

    // ---------- Collaboration diagram: messages 1, 1.1, 1.2, 1.3 ----------

    @Test
    void requestAdjustmentChecksPermissionCreatesPendingAndSaves() {
        when(access.checkPermission("U-02", "requestAdjustment")).thenReturn(true);
        when(repo.saveRequest(any())).thenAnswer(i -> i.getArgument(0));

        AdjustmentRequest created = aam.requestAdjustment("ADJ-001", "REV-001", "U-02",
                "SKU-0412", "B-2407", -4, "Counted 38, recorded 42");

        InOrder order = inOrder(access, repo);
        order.verify(access).checkPermission("U-02", "requestAdjustment");   // 1.1
        ArgumentCaptor<AdjustmentRequest> saved = ArgumentCaptor.forClass(AdjustmentRequest.class);
        order.verify(repo).saveRequest(saved.capture());                      // 1.3
        assertSame(created, saved.getValue());
        assertEquals(AdjustmentStatus.PENDING, created.status());             // 1.2
        assertEquals("ADJ-001", created.requestId());
        assertEquals(-4, created.correctionQty());
        assertEquals("U-02", created.requestedBy());
        assertNull(created.approvedBy());
        assertNull(created.decidedAt());
        verifyNoInteractions(inventory);
    }

    @Test
    void requestAdjustmentDeniedDoesNotSave() {
        when(access.checkPermission("U-99", "requestAdjustment")).thenReturn(false);

        assertThrows(SecurityException.class, () -> aam.requestAdjustment("ADJ-002", "REV-001",
                "U-99", "SKU-0412", "B-2407", -4, "x"));

        verify(repo, never()).saveRequest(any());
        verifyNoInteractions(inventory);
    }

    // ---------- Collaboration diagram: messages 2, 2.1 ... 2.5 ----------

    @Test
    void ownerApprovalAppliesCorrectionInDiagramOrder() {
        when(access.checkPermission("U-01", "approveAdjustment")).thenReturn(true);
        when(repo.findRequest("ADJ-001")).thenReturn(Optional.of(req));
        when(inventory.applyCorrection("B-2407", -4, "Counted 38, recorded 42"))
                .thenReturn(Result.success());

        ApprovalResult result = aam.approveAdjustment("ADJ-001", "U-01", true);

        InOrder order = inOrder(access, repo, inventory);
        order.verify(access).checkPermission("U-01", "approveAdjustment");   // 2.1
        order.verify(repo).findRequest("ADJ-001");                            // 2.2
        order.verify(inventory).applyCorrection("B-2407", -4, "Counted 38, recorded 42"); // 2.4
        order.verify(repo).saveRequest(req);                                  // 2.5
        verify(inventory, times(1)).applyCorrection(anyString(), anyInt(), anyString());
        assertEquals(AdjustmentStatus.APPLIED, req.status());
        assertEquals("U-01", req.approvedBy());
        assertEquals(DECIDED_AT, req.decidedAt());
        assertTrue(result.applied());
        assertEquals(AdjustmentStatus.APPLIED, result.status());
    }

    @Test
    void counterStaffCannotApprove() {
        when(access.checkPermission("U-02", "approveAdjustment")).thenReturn(false);

        assertThrows(SecurityException.class, () -> aam.approveAdjustment("ADJ-001", "U-02", true));

        verifyNoInteractions(inventory, repo);
    }

    @Test
    void rejectionEndsRejectedAndStockIsUntouched() {
        when(access.checkPermission("U-01", "approveAdjustment")).thenReturn(true);
        when(repo.findRequest("ADJ-001")).thenReturn(Optional.of(req));

        ApprovalResult result = aam.approveAdjustment("ADJ-001", "U-01", false);

        assertEquals(AdjustmentStatus.REJECTED, req.status());
        assertFalse(result.applied());
        verify(repo).saveRequest(req);
        verifyNoInteractions(inventory);
    }

    @Test
    void failedInventoryCorrectionLeavesRequestApproved() {
        when(access.checkPermission("U-01", "approveAdjustment")).thenReturn(true);
        when(repo.findRequest("ADJ-001")).thenReturn(Optional.of(req));
        when(inventory.applyCorrection(anyString(), anyInt(), anyString()))
                .thenReturn(Result.failure("batch locked"));

        ApprovalResult result = aam.approveAdjustment("ADJ-001", "U-01", true);

        assertEquals(AdjustmentStatus.APPROVED, req.status());
        assertFalse(result.applied());
        verify(repo).saveRequest(req);
    }

    @Test
    void nullInventoryOutcomeIsTreatedAsFailure() {
        when(access.checkPermission("U-01", "approveAdjustment")).thenReturn(true);
        when(repo.findRequest("ADJ-001")).thenReturn(Optional.of(req));
        when(inventory.applyCorrection(anyString(), anyInt(), anyString())).thenReturn(null);

        aam.approveAdjustment("ADJ-001", "U-01", true);

        assertEquals(AdjustmentStatus.APPROVED, req.status());
    }

    @Test
    void alreadyDecidedRequestCannotBeDecidedAgain() {
        req.approve("U-01", DECIDED_AT);
        req.markApplied();
        when(access.checkPermission("U-01", "approveAdjustment")).thenReturn(true);
        when(repo.findRequest("ADJ-001")).thenReturn(Optional.of(req));

        assertThrows(IllegalStateException.class, () -> aam.approveAdjustment("ADJ-001", "U-01", true));

        verifyNoInteractions(inventory);
        verify(repo, never()).saveRequest(any());
    }

    @Test
    void unknownRequestIsNotFound() {
        when(access.checkPermission("U-01", "approveAdjustment")).thenReturn(true);
        when(repo.findRequest("ADJ-404")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> aam.approveAdjustment("ADJ-404", "U-01", true));

        verifyNoInteractions(inventory);
    }

    // ---------- State diagram: valid transitions ----------

    @Test
    void newRequestStartsPending() {
        assertEquals(AdjustmentStatus.PENDING, req.status());
        assertTrue(req.isPending());
        assertTrue(req.canApply());
    }

    @Test
    void pendingToApproved() {
        req.approve("U-01", DECIDED_AT);
        assertEquals(AdjustmentStatus.APPROVED, req.status());
        assertFalse(req.canApply());
    }

    @Test
    void approvedToApplied() {
        req.approve("U-01", DECIDED_AT);
        req.markApplied();
        assertEquals(AdjustmentStatus.APPLIED, req.status());
    }

    @Test
    void pendingToRejected() {
        req.reject("U-01", DECIDED_AT);
        assertEquals(AdjustmentStatus.REJECTED, req.status());
        assertEquals("U-01", req.approvedBy());
        assertEquals(DECIDED_AT, req.decidedAt());
    }

    // ---------- State diagram: invalid transitions must be refused ----------

    @Test
    void pendingCannotJumpStraightToApplied() {
        assertThrows(IllegalStateException.class, () -> req.markApplied());
        assertEquals(AdjustmentStatus.PENDING, req.status());
    }

    @Test
    void approvedCannotBeApprovedAgain() {
        req.approve("U-01", DECIDED_AT);
        assertThrows(IllegalStateException.class, () -> req.approve("U-01", DECIDED_AT));
    }

    @Test
    void approvedCannotBeRejected() {
        req.approve("U-01", DECIDED_AT);
        assertThrows(IllegalStateException.class, () -> req.reject("U-01", DECIDED_AT));
        assertEquals(AdjustmentStatus.APPROVED, req.status());
    }

    @Test
    void appliedCannotGoBackOrChange() {
        req.approve("U-01", DECIDED_AT);
        req.markApplied();
        assertThrows(IllegalStateException.class, () -> req.approve("U-01", DECIDED_AT));
        assertThrows(IllegalStateException.class, () -> req.reject("U-01", DECIDED_AT));
        assertThrows(IllegalStateException.class, () -> req.markApplied());
        assertEquals(AdjustmentStatus.APPLIED, req.status());
    }

    @Test
    void rejectedIsFinal() {
        req.reject("U-01", DECIDED_AT);
        assertThrows(IllegalStateException.class, () -> req.approve("U-01", DECIDED_AT));
        assertThrows(IllegalStateException.class, () -> req.reject("U-01", DECIDED_AT));
        assertThrows(IllegalStateException.class, () -> req.markApplied());
        assertEquals(AdjustmentStatus.REJECTED, req.status());
    }

    // ---------- Object diagram: the fixture really holds the diagram values ----------

    @Test
    void fixtureMatchesObjectDiagram() {
        assertEquals("ADJ-001", req.requestId());
        assertEquals("REV-001", req.reviewId());
        assertEquals("SKU-0412", req.sku());
        assertEquals("B-2407", req.batchId());
        assertEquals(-4, req.correctionQty());
        assertEquals("Counted 38, recorded 42", req.reason());
        assertEquals("U-02", req.requestedBy());
        assertEquals(REQUESTED_AT, req.requestedAt());
    }
}
