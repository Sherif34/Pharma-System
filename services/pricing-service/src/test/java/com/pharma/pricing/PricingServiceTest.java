package com.pharma.pricing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock private IAccessControl accessControl;
    @Mock private ICatalogue catalogue;
    @Mock private IPriceUpdateRepository priceUpdateRepository;
    @Mock private IAuditTrailRepository auditTrailRepository;

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService(
            accessControl, catalogue, priceUpdateRepository, auditTrailRepository
        );
    }

    // --- Collaboration Diagram Scenario: Submit Price Update (1.1 -> 1.4) ---
    @Test
    void submitPriceUpdate_validScenario_followsCollaborationSequenceAndCapturesFixtures() {
        String updateId = "PU-104";
        String sku = "ASP-500";
        String userId = "USR-202";
        double oldPrice = 10.5;
        double newPrice = 12.0;

        when(accessControl.checkPermission(userId, "PRICE_SUBMIT")).thenReturn(true);
        when(catalogue.skuExists(sku)).thenReturn(true);
        when(catalogue.getCurrentPrice(sku)).thenReturn(oldPrice);

        PriceUpdate result = pricingService.submitPriceUpdate(updateId, sku, newPrice, userId);

        InOrder inOrder = inOrder(accessControl, catalogue, priceUpdateRepository, auditTrailRepository);
        inOrder.verify(accessControl).checkPermission(userId, "PRICE_SUBMIT");
        inOrder.verify(catalogue).skuExists(sku);

        ArgumentCaptor<PriceUpdate> updateCaptor = ArgumentCaptor.forClass(PriceUpdate.class);
        inOrder.verify(priceUpdateRepository).save(updateCaptor.capture());

        ArgumentCaptor<AuditTrailEntry> auditCaptor = ArgumentCaptor.forClass(AuditTrailEntry.class);
        inOrder.verify(auditTrailRepository).save(auditCaptor.capture());

        PriceUpdate savedUpdate = updateCaptor.getValue();
        assertEquals("PU-104", savedUpdate.getId());
        assertEquals("ASP-500", savedUpdate.getSku());
        assertEquals(10.5, savedUpdate.getOldPrice(), 0.001);
        assertEquals(12.0, savedUpdate.getNewPrice(), 0.001);
        assertEquals(PriceUpdateStatus.PENDING_APPROVAL, savedUpdate.getStatus());
        assertEquals(PriceUpdateStatus.PENDING_APPROVAL, result.getStatus());

        AuditTrailEntry savedAudit = auditCaptor.getValue();
        assertEquals("USR-202", savedAudit.getUserId());
        assertEquals("SUBMIT_PRICE_UPDATE", savedAudit.getAction());
    }

    @Test
    void submitPriceUpdate_unauthorizedUser_abortsBeforeCatalogueCheck() {
        when(accessControl.checkPermission("USR-999", "PRICE_SUBMIT")).thenReturn(false);

        assertThrows(SecurityException.class, () ->
            pricingService.submitPriceUpdate("PU-105", "ASP-500", 12.0, "USR-999")
        );

        verify(accessControl).checkPermission("USR-999", "PRICE_SUBMIT");
        verifyNoInteractions(catalogue, priceUpdateRepository, auditTrailRepository);
    }

    @Test
    void submitPriceUpdate_unknownSku_throwsAndSkipsPersistence() {
        when(accessControl.checkPermission("USR-202", "PRICE_SUBMIT")).thenReturn(true);
        when(catalogue.skuExists("INVALID-SKU")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
            pricingService.submitPriceUpdate("PU-106", "INVALID-SKU", 12.0, "USR-202")
        );

        verify(catalogue).skuExists("INVALID-SKU");
        verifyNoInteractions(priceUpdateRepository, auditTrailRepository);
    }

    // --- State Diagram Tests: PriceUpdate Lifecycle ---
    @Test
    void priceUpdateState_happyPathLifecycle_succeeds() {
        PriceUpdate pu = new PriceUpdate("PU-104", "ASP-500", 10.5, 12.0);
        assertEquals(PriceUpdateStatus.DRAFT, pu.getStatus());

        pu.submit();
        assertEquals(PriceUpdateStatus.PENDING_APPROVAL, pu.getStatus());

        pu.approve();
        assertEquals(PriceUpdateStatus.APPROVED, pu.getStatus());

        pu.activateDuePrices();
        assertEquals(PriceUpdateStatus.ACTIVE, pu.getStatus());

        pu.expire();
        assertEquals(PriceUpdateStatus.EXPIRED, pu.getStatus());
    }

    @Test
    void priceUpdateState_pendingToRejected_succeeds() {
        PriceUpdate pu = new PriceUpdate("PU-104", "ASP-500", 10.5, 12.0);
        pu.submit();
        pu.reject();
        assertEquals(PriceUpdateStatus.REJECTED, pu.getStatus());
    }

    @Test
    void priceUpdateState_invalidTransitions_throwIllegalStateException() {
        PriceUpdate draft = new PriceUpdate("PU-104", "ASP-500", 10.5, 12.0);
        assertThrows(IllegalStateException.class, draft::approve);
        assertThrows(IllegalStateException.class, draft::activateDuePrices);

        PriceUpdate rejected = new PriceUpdate("PU-104", "ASP-500", 10.5, 12.0);
        rejected.submit();
        rejected.reject();
        assertThrows(IllegalStateException.class, rejected::approve);
        assertThrows(IllegalStateException.class, rejected::activateDuePrices);

        PriceUpdate expired = new PriceUpdate("PU-104", "ASP-500", 10.5, 12.0);
        expired.submit();
        expired.approve();
        expired.activateDuePrices();
        expired.expire();
        assertThrows(IllegalStateException.class, expired::activateDuePrices);
    }
}
