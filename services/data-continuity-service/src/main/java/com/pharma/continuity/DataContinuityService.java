package com.pharma.continuity;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DataContinuityService {
    private final IAccessControl accessControl;
    private final ILocalStorage localStorage;
    private final IRecordStore recordStore;
    private final String backupDir;

    public DataContinuityService(
            IAccessControl accessControl,
            ILocalStorage localStorage,
            IRecordStore recordStore,
            String backupDir) {
        this.accessControl = accessControl;
        this.localStorage = localStorage;
        this.recordStore = recordStore;
        this.backupDir = backupDir;
    }

    public BackupJob runDailyBackup(String actorId, String jobId, String startTime) {
        if (!accessControl.authorize(actorId, "DAILY_BACKUP")) {
            throw new SecurityException("Actor not authorized for daily backup");
        }
        BackupJob job = new BackupJob(jobId, startTime);
        job.start();

        String dateStamp = LocalDate.parse(startTime.substring(0, 10))
            .format(DateTimeFormatter.BASIC_ISO_DATE);
        String filePath = backupDir + "/daily_" + dateStamp + ".dat";

        try {
            List<String> records = recordStore.readAllRecords();
            String payload = String.join("\n", records);
            long bytesWritten = localStorage.writeBackupFile(filePath, payload);
            job.exportSuccess(bytesWritten);
        } catch (IOException ex) {
            job.error();
        }
        return job;
    }
}
