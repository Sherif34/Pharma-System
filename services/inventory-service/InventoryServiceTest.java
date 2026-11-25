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

class InventoryServiceTest {
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

    @Test void damagePostsTheDiagramMovement() {
        when(access.authorize("CLERK-04", "record-movement")).thenReturn(true);
        when(catalogue.skuExists("SKU-042")).thenReturn(true);
        Snapshot before = new Snapshot("SKU-042", "B-210", 4, 7);
        Snapshot after = new Snapshot("SKU-042", "B-210", 0, 8);
        Movement movement = new Movement("MV-712", "SKU-042", "B-210", -4, Reason.DAMAGE);
        when(inventory.getBatchSnapshot("B-210")).thenReturn(Optional.of(before));
        when(inventory.postMovementOnce(movement, 7)).thenReturn(after);
        assertEquals(after, new InventoryService(access, catalogue, inventory)
                .recordDamage("CLERK-04", "MV-712", "SKU-042", "B-210", 4));
        InOrder order = inOrder(access, catalogue, inventory);
        order.verify(access).authorize("CLERK-04", "record-movement");
        order.verify(catalogue).skuExists("SKU-042");
        order.verify(inventory).getBatchSnapshot("B-210");
        order.verify(inventory).postMovementOnce(movement, 7);
    }
    @Test void negativeBalanceDoesNotPost() {
        when(access.authorize(anyString(), anyString())).thenReturn(true);
        when(catalogue.skuExists("SKU-042")).thenReturn(true);
        when(inventory.getBatchSnapshot("B-210")).thenReturn(Optional.of(new Snapshot("SKU-042", "B-210", 4, 7)));
        assertThrows(IllegalArgumentException.class, () -> new InventoryService(access, catalogue, inventory)
                .recordDamage("CLERK-04", "MV-712", "SKU-042", "B-210", 5));
        verify(inventory, never()).postMovementOnce(any(), anyInt());
    }
    @Test void deniedMovementDoesNotQueryDependencies() {
        assertThrows(SecurityException.class, () -> new InventoryService(access, catalogue, inventory)
                .recordDamage("CLERK-04", "MV-712", "SKU-042", "B-210", 4));
        verifyNoInteractions(catalogue, inventory);
    }
    @Test void wrongSkuOrMissingBatchDoesNotPost() {
        when(access.authorize(anyString(), anyString())).thenReturn(true);
        when(catalogue.skuExists("SKU-042")).thenReturn(true);
        when(inventory.getBatchSnapshot("B-210")).thenReturn(Optional.of(new Snapshot("OTHER", "B-210", 4, 7)), Optional.empty());
        InventoryService service = new InventoryService(access, catalogue, inventory);
        assertThrows(IllegalArgumentException.class, () -> service.recordDamage("CLERK-04", "MV-712", "SKU-042", "B-210", 1));
        assertThrows(java.util.NoSuchElementException.class, () -> service.recordDamage("CLERK-04", "MV-712", "SKU-042", "B-210", 1));
        verify(inventory, never()).postMovementOnce(any(), anyInt());
    }
}
