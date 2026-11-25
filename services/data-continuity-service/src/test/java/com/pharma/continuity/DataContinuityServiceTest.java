package com.pharma.continuity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataContinuityServiceTest {

    @Mock private IAccessControl accessControl;
    @Mock private ILocalStorage localStorage;
    @Mock private IRecordStore recordStore;

    private DataContinuityService continuityService;

    @BeforeEach
    void setUp() {
        continuityService = new DataContinuityService(
            accessControl, localStorage, recordStore, "/var/backups/pharma"
        );
    }

    // --- Collaboration Diagram Scenario: Run Daily Backup (1.1 -> 1.5) ---
    @Test
    void runDailyBackup_authorizedAdmin_executesSequenceAndCompletesJob() throws IOException {
        String actorId = "USR-ADMIN-1";
        String expectedPath = "/var/backups/pharma/daily_20261125.dat";
        List<String> mockRecords = List.of("SKU:ASP-500|PRICE:12.0", "SKU:IBU-200|PRICE:8.5");

        when(accessControl.authorize(actorId, "DAILY_BACKUP")).thenReturn(true);
        when(recordStore.readAllRecords()).thenReturn(mockRecords);
        when(localStorage.writeBackupFile(eq(expectedPath), anyString())).thenReturn(248010L);

        BackupJob job = continuityService.runDailyBackup(actorId, "BAK-609", "2026-11-25T19:53:00");

        InOrder inOrder = inOrder(accessControl, recordStore, localStorage);
        inOrder.verify(accessControl).authorize(actorId, "DAILY_BACKUP");
        inOrder.verify(recordStore).readAllRecords();
        inOrder.verify(localStorage).writeBackupFile(eq(expectedPath), anyString());

        assertEquals("BAK-609", job.getJobId());
        assertEquals("2026-11-25T19:53:00", job.getStartTime());
        assertEquals(BackupJobStatus.COMPLETED, job.getStatus());
        assertEquals(248010L, job.getSizeBytes());
    }

    @Test
    void runDailyBackup_unauthorizedActor_throwsSecurityExceptionAndSkipsDiskIO() {
        when(accessControl.authorize("USR-GUEST", "DAILY_BACKUP")).thenReturn(false);

        assertThrows(SecurityException.class, () ->
            continuityService.runDailyBackup("USR-GUEST", "BAK-610", "2026-11-25T20:00:00")
        );

        verify(accessControl).authorize("USR-GUEST", "DAILY_BACKUP");
        verifyNoInteractions(recordStore, localStorage);
    }

    @Test
    void runDailyBackup_storageWriteFailure_transitionsJobToFailed() throws IOException {
        when(accessControl.authorize("USR-ADMIN-1", "DAILY_BACKUP")).thenReturn(true);
        when(recordStore.readAllRecords()).thenReturn(List.of("RECORD_1"));
        when(localStorage.writeBackupFile(anyString(), anyString()))
            .thenThrow(new IOException("Disk quota exceeded"));

        BackupJob job = continuityService.runDailyBackup("USR-ADMIN-1", "BAK-611", "2026-11-25T20:05:00");

        assertEquals(BackupJobStatus.FAILED, job.getStatus());
        assertEquals(0L, job.getSizeBytes());
    }

    // --- State Diagram Tests: BackupJob Lifecycle ---
    @Test
    void backupJobState_validTransitions_succeed() {
        BackupJob job = new BackupJob("BAK-609", "2026-11-25T19:53:00");
        assertEquals(BackupJobStatus.INITIATED, job.getStatus());

        job.start();
        assertEquals(BackupJobStatus.RUNNING, job.getStatus());

        job.exportSuccess(248010L);
        assertEquals(BackupJobStatus.COMPLETED, job.getStatus());
        assertEquals(248010L, job.getSizeBytes());

        job.archive();
        assertEquals(BackupJobStatus.ARCHIVED, job.getStatus());
    }

    @Test
    void backupJobState_invalidTransitions_throwIllegalStateException() {
        BackupJob initiated = new BackupJob("BAK-609", "2026-11-25T19:53:00");
        assertThrows(IllegalStateException.class, () -> initiated.exportSuccess(248010L));
        assertThrows(IllegalStateException.class, initiated::archive);

        BackupJob failed = new BackupJob("BAK-609", "2026-11-25T19:53:00");
        failed.start();
        failed.error();
        assertEquals(BackupJobStatus.FAILED, failed.getStatus());
        assertThrows(IllegalStateException.class, () -> failed.exportSuccess(248010L));
        assertThrows(IllegalStateException.class, failed::archive);

        BackupJob archived = new BackupJob("BAK-609", "2026-11-25T19:53:00");
        archived.start();
        archived.exportSuccess(248010L);
        archived.archive();
        assertThrows(IllegalStateException.class, archived::start);
    }
}
