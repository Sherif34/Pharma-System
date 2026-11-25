package com.pharma.continuity;

import java.io.IOException;

public interface ILocalStorage {
    long writeBackupFile(String path, String payload) throws IOException;
}
