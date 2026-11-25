package com.pharma.continuity;

public class BackupJob {
    private final String jobId;
    private final String startTime;
    private BackupJobStatus status;
    private long sizeBytes;

    public BackupJob(String jobId, String startTime) {
        this.jobId = jobId;
        this.startTime = startTime;
        this.status = BackupJobStatus.INITIATED;
        this.sizeBytes = 0L;
    }

    public void start() {
        if (status != BackupJobStatus.INITIATED) {
            throw new IllegalStateException("Cannot start from status " + status);
        }
        this.status = BackupJobStatus.RUNNING;
    }

    public void exportSuccess(long sizeBytes) {
        if (status != BackupJobStatus.RUNNING) {
            throw new IllegalStateException("Cannot complete export from status " + status);
        }
        this.sizeBytes = sizeBytes;
        this.status = BackupJobStatus.COMPLETED;
    }

    public void error() {
        if (status != BackupJobStatus.RUNNING) {
            throw new IllegalStateException("Cannot fail from status " + status);
        }
        this.sizeBytes = 0L;
        this.status = BackupJobStatus.FAILED;
    }

    public void archive() {
        if (status != BackupJobStatus.COMPLETED) {
            throw new IllegalStateException("Cannot archive from status " + status);
        }
        this.status = BackupJobStatus.ARCHIVED;
    }

    public String getJobId() { return jobId; }
    public String getStartTime() { return startTime; }
    public BackupJobStatus getStatus() { return status; }
    public long getSizeBytes() { return sizeBytes; }
}
