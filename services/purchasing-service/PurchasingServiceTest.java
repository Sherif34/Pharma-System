package edu.cse454.pharma;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import edu.cse454.pharma.Scenarios.*;

class PurchasingServiceTest {
    private Access access;
    private RequestStore requests;
    private Catalogue catalogue;
    private InventoryStore inventory;
    private Request pending;

    @BeforeEach void setUp() {
        access = mock(Access.class);
        requests = mock(RequestStore.class);
        catalogue = mock(Catalogue.class);
        inventory = mock(InventoryStore.class);
        pending = new Request("SR-1042", "SUP-07", LocalDate.of(2026, 12, 3),
                List.of(new Line("SKU-042", 24), new Line("SKU-117", 12)), Approval.PENDING);
    }

    @Test void approvesAndSavesTheDiagramFixture() {
        when(access.authorize("OWNER-01", "approve-request")).thenReturn(true);
        when(requests.findById("SR-1042")).thenReturn(Optional.of(pending));
        Request approved = pending.withDecision(Approval.APPROVED);
        when(requests.save(approved)).thenReturn(approved);
        assertEquals(approved, new PurchasingService(access, requests)
                .recordApprovalState("OWNER-01", "SR-1042", Approval.APPROVED));
        InOrder order = inOrder(access, requests);
        order.verify(access).authorize("OWNER-01", "approve-request");
        order.verify(requests).findById("SR-1042");
        order.verify(requests).save(approved);
    }

    @Test void pendingCanBeRejected() {
        assertEquals(Approval.REJECTED, pending.withDecision(Approval.REJECTED).approval());
    }
    @Test void invalidAndRepeatedDecisionsAreRejected() {
        assertThrows(IllegalStateException.class, () -> pending.withDecision(Approval.PENDING));
        assertThrows(IllegalStateException.class, () -> pending.withDecision(null));
        assertThrows(IllegalStateException.class, () -> pending.withDecision(Approval.APPROVED).withDecision(Approval.REJECTED));
        assertThrows(IllegalStateException.class, () -> pending.withDecision(Approval.REJECTED).withDecision(Approval.APPROVED));
    }
    @Test void deniedApprovalDoesNotAccessStorage() {
        assertThrows(SecurityException.class, () -> new PurchasingService(access, requests)
                .recordApprovalState("OWNER-01", "SR-1042", Approval.APPROVED));
        verifyNoInteractions(requests);
    }
    @Test void unknownRequestIsNotSaved() {
        when(access.authorize(anyString(), anyString())).thenReturn(true);
        when(requests.findById("missing")).thenReturn(Optional.empty());
        assertThrows(java.util.NoSuchElementException.class, () -> new PurchasingService(access, requests)
                .recordApprovalState("OWNER-01", "missing", Approval.APPROVED));
        verify(requests, never()).save(any());
    }
}
