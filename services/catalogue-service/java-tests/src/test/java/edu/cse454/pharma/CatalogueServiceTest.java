package edu.cse454.pharma;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import edu.cse454.pharma.Scenarios.*;

class CatalogueServiceTest {
    @Test void findsProductByBarcode() {
        CatalogueStore store = mock(CatalogueStore.class);
        Product product = new Product("SKU-042", "6221234567890");
        when(store.findByBarcode(product.barcode())).thenReturn(Optional.of(product));
        assertEquals(Optional.of(product), new CatalogueService(store).findByBarcode(product.barcode()));
        verify(store).findByBarcode(product.barcode());
    }

    @Test void rejectsDuplicateSkuWithoutSaving() {
        CatalogueStore store = mock(CatalogueStore.class);
        Product product = new Product("SKU-042", "6221234567890");
        when(store.skuExists(product.sku())).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> new CatalogueService(store).importProduct(product));
        verify(store, never()).save(any());
    }

    @Test void importsNewProduct() {
        CatalogueStore store = mock(CatalogueStore.class);
        Product product = new Product("SKU-042", "6221234567890");
        new CatalogueService(store).importProduct(product);
        verify(store).skuExists("SKU-042");
        verify(store).save(product);
    }
}
