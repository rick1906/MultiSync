/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result of a single sync operation. May contain subresults.
 *
 * @author Rick
 */
public class SyncResult extends SyncStatus
{
    public static final int BACKUP_FAILED = -1;
    public static final int BACKUP_SKIPPED = 0;
    public static final int BACKUP_EXISTS = 1; // identical file is backuped already
    public static final int BACKUP_MOVED = 2; // file moved (copied and deleted)
    public static final int BACKUP_COPIED = 3; // normally copied
    public static final int BACKUP_MIXED = 4; // mixed results for directory backup

    private int backup = BACKUP_SKIPPED;
    private Exception syncError = null;
    private Exception backupError = null;
    private boolean success = false;
    private LinkedHashMap<Path, SyncResult> subResults = null;

    /**
     * Create successful SyncResult with given status.
     *
     * @param syncStatus
     */
    public SyncResult(int syncStatus)
    {
        this.value = syncStatus;
        this.success = isGoodStatus();
    }

    /**
     * Create failed SyncResult.
     *
     * @param syncError
     */
    public SyncResult(Exception syncError)
    {
        this.value = SYNC_FAILURE;
        this.success = false;
        this.syncError = syncError;
    }

    /**
     * Create SyncResult and set if it's successful.
     *
     * @param syncStatus
     * @param success
     */
    public SyncResult(int syncStatus, boolean success)
    {
        this.value = syncStatus;
        this.success = success;
    }

    /**
     * Create SyncResult with backup status.
     *
     * @param syncStatus
     * @param success
     * @param backupStatus
     */
    public SyncResult(int syncStatus, boolean success, int backupStatus)
    {
        this.value = syncStatus;
        this.backup = backupStatus;
        this.success = success;
    }

    /**
     * Create SyncResult with failed backup operation.
     *
     * @param syncStatus
     * @param backupError
     */
    public SyncResult(int syncStatus, Exception backupError)
    {
        this.value = syncStatus;
        this.backup = BACKUP_FAILED;
        this.success = false;
        this.backupError = backupError;
    }

    /**
     * Create SyncResult with failed sync operation.
     *
     * @param syncStatus
     * @param syncError
     * @param backupStatus
     */
    public SyncResult(int syncStatus, Exception syncError, int backupStatus)
    {
        this.value = syncStatus;
        this.backup = backupStatus;
        this.success = false;
        this.syncError = syncError;
    }

    /**
     *
     * @return copy with no sub-results
     */
    public final SyncResult copyResult()
    {
        SyncResult copy = new SyncResult(value);
        copy.backup = backup;
        copy.success = success;
        copy.syncError = syncError;
        copy.backupError = backupError;
        return copy;
    }

    /**
     * Set different sync status if not skipped.
     *
     * @param syncStatus
     * @return this object
     */
    public final SyncResult transformValue(int syncStatus)
    {
        if (!isSkipStatus()) {
            this.value = syncStatus;
        }
        return this;
    }

    /**
     * Change success to false; or to true if no errors.
     *
     * @param success
     * @return this object
     */
    public final SyncResult transformSuccess(boolean success)
    {
        this.success = success ? syncError == null && backupError == null : false;
        return this;
    }

    /**
     * Change success to false and set an error.
     *
     * @param syncError
     * @return this object
     */
    public final SyncResult transformSuccess(Exception syncError)
    {
        this.success = false;
        this.syncError = syncError;
        return this;
    }

    /**
     *
     * @return init subresults collection
     */
    public SyncResult enableSubResults()
    {
        if (subResults == null) {
            subResults = new LinkedHashMap<>();
        }
        return this;
    }

    /**
     *
     * @return sync status
     */
    public final int getSyncStatus()
    {
        return value;
    }

    /**
     *
     * @return backup status
     */
    public final int getBackupStatus()
    {
        return backup;
    }

    /**
     *
     * @return exception on sync operation
     */
    public final Exception getSyncError()
    {
        return syncError;
    }

    /**
     *
     * @return exception on backup operation
     */
    public final Exception getBackupError()
    {
        return backupError;
    }

    /**
     *
     * @return true if backup is successful or skipped
     */
    public final boolean isBackupSuccess()
    {
        return backup >= BACKUP_SKIPPED;
    }

    /**
     *
     * @return sync operation is valid and performed successfully
     */
    @Override
    public final boolean isSuccess()
    {
        return success && isGoodStatus();
    }

    /**
     *
     * @return sync operation is valid and skipped
     */
    public final boolean isSkipped()
    {
        return isSuccess() && (isSkipStatus() || value == SYNC_MERGE);
    }

    /**
     *
     * @return sync operation is successful and not skipped
     */
    public final boolean isUpdated()
    {
        return isSuccess() && !isSkipStatus() && value != SYNC_MERGE;
    }

    /**
     * Add child SyncResult for files in directory.
     *
     * @param path relative path to a file
     * @param result operation result
     */
    public void addSubResult(Path path, SyncResult result)
    {
        if (subResults == null) {
            subResults = new LinkedHashMap<>();
        }
        if (result.subResults != null) {
            subResults.put(path, result.copyResult());
            subResults.putAll(result.subResults);
        } else {
            subResults.put(path, result);
        }
        if (value == SYNC_MATCHES) {
            value = SYNC_MERGE;
        }
        if (!result.isBackupSuccess() || !isBackupSuccess()) {
            backup = BACKUP_FAILED;
        } else if (backup == BACKUP_SKIPPED) {
            backup = result.backup;
        } else if (result.backup != BACKUP_SKIPPED && backup != result.backup) {
            backup = BACKUP_MIXED;
        }
    }

    /**
     *
     * @return sub-results
     */
    public Map<Path, SyncResult> getSubResults()
    {
        return subResults != null ? subResults : Collections.emptyMap();
    }

    /**
     *
     * @return true if no failures, even for sub-results
     */
    public boolean isTotalSuccess()
    {
        return countFailed() == 0;
    }

    /**
     *
     * @return true if there are failures and no updates
     */
    public boolean isTotalFailure()
    {
        return countFailed() > 0 && countUpdated() == 0;
    }

    /**
     *
     * @return sync operation count
     */
    public int countAll()
    {
        return 1 + (subResults != null ? subResults.size() : 0);
    }

    /**
     *
     * @return count of successful and not skipped sync operations
     */
    public int countUpdated()
    {
        return (isUpdated() ? 1 : 0) + countUpdatedSubResults();
    }

    private int countUpdatedSubResults()
    {
        return subResults != null ? (int)subResults.values().stream().filter(r -> r.isUpdated()).count() : 0;
    }

    /**
     *
     * @return count of valid sync operations skipped
     */
    public int countSkipped()
    {
        return (isSkipped() ? 1 : 0) + countSkippedSubResults();
    }

    private int countSkippedSubResults()
    {
        return subResults != null ? (int)subResults.values().stream().filter(r -> r.isSkipped()).count() : 0;
    }

    /**
     *
     * @return count of sync operation failures
     */
    public int countFailed()
    {
        return (isSuccess() ? 0 : 1) + countFailedSubResults();
    }

    private int countFailedSubResults()
    {
        return subResults != null ? (int)subResults.values().stream().filter(r -> !r.isSuccess()).count() : 0;
    }

    /**
     *
     * @param updatesName string to name 'updated' sub-results
     * @return string representaion of the result
     */
    public String getResultString(String updatesName)
    {
        String status = super.toString();
        String result;
        String note;
        String extra = "";
        if (isSuccess()) {
            result = "success";
            note = status;
            extra = backup > BACKUP_SKIPPED ? "backup done" : "";
        } else if (!isBackupSuccess()) {
            result = "failure";
            note = status;
            extra = "backup failed";
            if (backupError != null) {
                extra += " (" + backupError.toString() + ")";
            }
        } else {
            result = "error";
            note = status;
            if (syncError != null) {
                note += "; " + syncError.toString();
            }
        }
        if (subResults != null) {
            if (isTotalSuccess()) {
                result = "success";
            } else if (isTotalFailure() && !isSuccess()) {
                result = "failure";
            } else if (isSuccess()) {
                result = "partial success";
            } else {
                result = "partial failure";
            }
            if (note.length() > 0) {
                note += "; ";
            }
            note += countUpdated() + " " + updatesName + ", ";
            note += countSkipped() + " skipped, ";
            note += countFailed() + " failed, ";
            note += countAll() + " total";
        }
        if (note.length() > 0) {
            result += " (" + note + ")";
        }
        if (extra.length() > 0) {
            result += ", " + extra;
        }
        return result;
    }

    @Override
    public String toString()
    {
        return getResultString("updated");
    }

}
