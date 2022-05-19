/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync;

import ru.com.rick.sync.fs.FileVersion;
import ru.com.rick.sync.fs.FileEntry;
import ru.com.rick.sync.fs.Segment;
import ru.com.rick.sync.options.GlobalOptions;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import ru.com.rick.sync.fs.FileProperties;
import ru.com.rick.sync.fs.FileUtils;
import ru.com.rick.sync.list.FileHistory;
import ru.com.rick.sync.options.SyncOptions;

/**
 * Class representing a file entry ready for synchronization, with attached file list entry, etc.
 *
 * @author Rick
 */
public class FileSyncEntry
{
    public static final int CHANGE_UNCHANGED = -1;
    public static final int CHANGE_UNKNOWN = 0;
    public static final int CHANGE_CREATED = 1;
    public static final int CHANGE_DELETED = 2;
    public static final int CHANGE_MODIFIED = 3;

    /**
     * Device segment
     */
    private final Segment segment;

    /**
     * Synchronization target object
     */
    private final SyncTarget target;

    /**
     * Entry for parent dir or null for near-root entries
     */
    private final FileSyncEntry parent;

    /**
     * File entry
     */
    private final FileEntry entry;

    /**
     * List entry
     */
    private final FileHistory history;

    /**
     * File properties before syncing
     */
    private FileVersion oldProperties = null;

    /**
     * Status of file changes in relation to file list
     */
    private int changeStatus = CHANGE_UNKNOWN;

    /**
     * Ignore code, 0 for not yet calculated, >0 to ignore
     */
    private int ignoreCode = 0;

    /**
     *
     * @param target
     * @param parent
     * @param entry
     * @param history
     */
    public FileSyncEntry(SyncTarget target, FileSyncEntry parent, FileEntry entry, FileHistory history)
    {
        this.target = target;
        this.parent = parent;
        this.entry = entry;
        this.segment = entry.getSegment();
        this.history = history;
    }

    /**
     *
     * @return parent sync entry or null
     */
    public FileSyncEntry getParent()
    {
        return parent;
    }

    /**
     *
     * @return sync target
     */
    public SyncTarget getSyncTarget()
    {
        return target;
    }

    /**
     *
     * @return file entry
     */
    public FileEntry getFileEntry()
    {
        return entry;
    }

    /**
     *
     * @return list entry
     */
    public FileHistory getHistory()
    {
        return history;
    }

    /**
     *
     * @return sync options
     */
    public SyncOptions getSyncOptions()
    {
        return target.getOptions();
    }

    /**
     *
     * @return file properties
     */
    public FileProperties getProperties()
    {
        return entry.getProperties();
    }

    /**
     *
     * @return saved properties before sync operation
     */
    public FileVersion getOldProperties()
    {
        return oldProperties != null ? oldProperties : getProperties();
    }

    /**
     *
     * @return is valid directory
     */
    public boolean isContainer()
    {
        return isAvailable() && !isIgnored() && entry.isDir() && (segment.getOptions().followSymLinks() || !entry.isSymLink());
    }

    /**
     *
     * @return sync target is available and enabled
     */
    public boolean isAvailable()
    {
        return target.isAvailable();
    }

    /**
     *
     * @return sync entry is ignored
     */
    public boolean isIgnored()
    {
        if (ignoreCode != 0) {
            return ignoreCode > 0;
        }
        if (parent != null && (parent.isIgnored() || !parent.isContainer())) {
            ignoreCode = 2;
        } else if (target.pathIsIgnored(entry.getRelativePath())) {
            ignoreCode = 1;
        } else {
            ignoreCode = -1;
        }
        return ignoreCode > 0;
    }

    /**
     * Refresh file entry and statuses.
     */
    public void refresh()
    {
        entry.refresh();
        changeStatus = CHANGE_UNKNOWN;
        ignoreCode = 0;
    }

    /**
     *
     * @param other
     * @param checkTime
     * @return int value for comparing this with other
     */
    public int compareUpdateTime(FileSyncEntry other, boolean checkTime)
    {
        long t0 = getUpdateTime(checkTime);
        long t1 = other.getUpdateTime(checkTime);
        return FileUtils.compareSeconds(t0, t1);
    }

    /**
     *
     * @param other
     * @return int value to compare change statuses
     */
    public int compareChangeStatus(FileSyncEntry other)
    {
        int s0 = getChangeStatus();
        int s1 = other.getChangeStatus();
        if (s0 > CHANGE_UNKNOWN && s1 <= CHANGE_UNKNOWN) {
            return 1;
        }
        if (s0 <= CHANGE_UNKNOWN && s1 > CHANGE_UNKNOWN) {
            return -1;
        }
        return 0;
    }

    /**
     *
     * @param checkTime
     * @return last modified time or last sync time in list entry
     */
    public long getUpdateTime(boolean checkTime)
    {
        int change = getSyncChangeStatus();
        if (change == CHANGE_CREATED) {
            long t1 = getModifiedTime(checkTime);
            long t2 = getLastSyncTime(); // last time there was no file
            long t3 = getCreatedTime();
            return Math.max(Math.max(t1, t2), t3);
        }
        if (change == CHANGE_MODIFIED || entry.exists()) {
            long t1 = getModifiedTime(checkTime);
            long t2 = getLastSyncTime(); // last time old file was seen
            return Math.max(t1, t2);
        }
        if (change == CHANGE_DELETED) {
            return getLastSyncTime(); // last time there was a file
        }
        return FileVersion.TIME_OLDEST;
    }

    /**
     *
     * @param checkTime
     * @return file modified time
     */
    public long getModifiedTime(boolean checkTime)
    {
        FileProperties p = getProperties();
        if (p.exists() && !p.isDir() && checkTime) {
            return p.getModifiedTime();
        }
        return FileVersion.TIME_UNKNOWN;
    }

    /**
     *
     * @return file created time
     */
    public long getCreatedTime()
    {
        FileProperties p = getProperties();
        if (p.exists() && !p.isDir() && segment.canUseCreatedTime()) {
            return p.getCreatedTime();
        }
        return FileVersion.TIME_UNKNOWN;
    }

    /**
     *
     * @return previous sync time
     */
    public long getLastSyncTime()
    {
        return history != null ? history.getSyncTime() : FileVersion.TIME_UNKNOWN;
    }

    /**
     *
     * @param checkTime
     * @param checkHash
     * @return change status
     */
    public int getChangeStatus(boolean checkTime, boolean checkHash)
    {
        if (history == null) {
            return CHANGE_UNKNOWN;
        }

        FileVersion pNew = getProperties();
        FileVersion pOld = history.getCurrentVersion();
        if (!history.exists()) {
            return pNew.exists() ? CHANGE_CREATED : CHANGE_UNCHANGED;
        } else if (!pNew.exists()) {
            return CHANGE_DELETED;
        }
        return pOld != null && pNew.isEqualTo(pOld, checkTime, checkHash) ? CHANGE_UNCHANGED : CHANGE_MODIFIED;
    }

    /**
     *
     * @return change status
     */
    public int getChangeStatus()
    {
        if (changeStatus == CHANGE_UNKNOWN) {
            changeStatus = getChangeStatus(segment.canUseModifiedTime(), segment.canUseHash());
        }
        return changeStatus;
    }

    /**
     *
     * @return tweaked change status
     */
    public int getSyncChangeStatus()
    {
        int value = getChangeStatus();
        if (history != null) {
            String state = history.getSyncState();
            if (FileHistory.STATE_DELETE.equals(state) && !entry.exists()) {
                return (value == CHANGE_UNCHANGED) ? CHANGE_DELETED : value;
            }
            if (FileHistory.STATE_UPDATE.equals(state) && entry.exists()) {
                return (value == CHANGE_UNCHANGED) ? CHANGE_MODIFIED : value;
            }
        }
        return value;
    }

    /**
     *
     * @return sync state to keep in history if not synced
     */
    public String getHistorySyncState()
    {
        int value = getSyncChangeStatus();
        if (value == CHANGE_CREATED || value == CHANGE_MODIFIED) {
            return FileHistory.STATE_UPDATE;
        }
        if (value == CHANGE_DELETED) {
            return FileHistory.STATE_DELETE;
        }
        return FileHistory.STATE_NONE;
    }

    /**
     *
     * @param other
     * @param options
     * @return sync status in relation to other entry
     * @throws IOException
     */
    public int getSyncStatus(FileSyncEntry other, GlobalOptions options) throws IOException
    {
        if (equals(other)) {
            return isContainer() ? SyncStatus.SYNC_MERGE : SyncStatus.SYNC_SKIP;
        }
        if (isIgnored() || other.isIgnored()) {
            return SyncStatus.SYNC_IGNORE;
        }
        if (!isAvailable() || !other.isAvailable()) {
            return SyncStatus.SYNC_SKIP;
        }
        boolean thisExists = entry.exists();
        boolean otherExists = other.entry.exists();
        if (!thisExists && !otherExists) {
            return SyncStatus.SYNC_MATCHES;
        }
        if (thisExists && !otherExists) {
            return SyncStatus.SYNC_CREATE;
        }
        if (!thisExists && otherExists) {
            return SyncStatus.SYNC_DELETE;
        }
        if (entry.isDir() && other.entry.isDir()) {
            return SyncStatus.SYNC_MERGE;
        }
        if (entry.isEqualTo(other.entry)) {
            return SyncStatus.SYNC_MATCHES;
        } else {
            return SyncStatus.SYNC_REPLACE;
        }
    }

    /**
     *
     * @param other
     * @param options
     * @return int value for preference of this agains other as a source (>0 - more preferred)
     */
    public int comparePreferredSource(FileSyncEntry other, GlobalOptions options)
    {
        if (isIgnored() || !isAvailable()) {
            return -1;
        }
        if (other == null || other.isIgnored() || !other.isAvailable()) {
            return 1;
        }

        boolean thisExists = entry.exists();
        boolean otherExists = other.entry.exists();
        if (thisExists && !otherExists && target.isReadOnly() && other.target.preferExisting()) {
            return 1;
        }
        if (otherExists && !thisExists && other.target.isReadOnly() && target.preferExisting()) {
            return -1;
        }

        int thisPriority = target.getPriority();
        int otherPriority = other.target.getPriority();
        if (thisPriority != otherPriority) {
            return thisPriority - otherPriority;
        }

        if (target.preferNewer() && other.target.preferNewer()) {
            boolean checkTime = segment.canUseModifiedTime() && other.segment.canUseModifiedTime();
            int r = compareUpdateTime(other, checkTime);
            if (r != 0) {
                return r;
            }
        }

        int cr = compareChangeStatus(other);
        if (cr != 0) {
            return cr;
        }

        if (thisExists && !otherExists && other.target.preferExisting()) {
            return 1;
        }
        if (otherExists && !thisExists && target.preferExisting()) {
            return -1;
        }
        return 0;
    }

    /**
     * Save file properties before making changes.
     */
    public void saveOldProperties()
    {
        oldProperties = new FileVersion(getProperties());
    }

    /**
     * Delete a file.
     *
     * @param options
     * @return operation result
     */
    public SyncResult delete(GlobalOptions options)
    {
        if (target.isReadOnly() || !target.canDelete(entry)) {
            return new SyncResult(SyncResult.SYNC_IGNORE, true);
        }
        saveOldProperties();
        return target.getBackuper().delete(entry, target.isBackup());
    }

    /**
     * Copy source file to current file entry.
     *
     * @param source
     * @param replace
     * @param options
     * @return operation result
     */
    public SyncResult copyFrom(FileSyncEntry source, boolean replace, GlobalOptions options)
    {
        if (target.isReadOnly()) {
            return new SyncResult(SyncResult.SYNC_IGNORE, true);
        }
        if (replace && entry.exists()) {
            if (!target.canReplace(entry, source.entry)) {
                return new SyncResult(SyncResult.SYNC_IGNORE, true);
            }
            saveOldProperties();
            return target.getBackuper().replace(source.entry, entry, target.isBackup());
        }
        if (!target.canCreate(source.entry)) {
            return new SyncResult(SyncResult.SYNC_IGNORE, true);
        }
        if (parent == null) {
            SyncResult result = target.getBackuper().createDirs(entry, target.getOptions().createRoot());
            if (!result.isSuccess()) {
                return result;
            }
        }
        saveOldProperties();
        return target.getBackuper().copy(source.entry, entry);

    }

    /**
     * Merge directories.
     *
     * @param source
     * @param options
     * @return operation result
     */
    public SyncResult mergeWith(FileSyncEntry source, GlobalOptions options)
    {
        if (!entry.isDir() || !source.entry.isDir()) {
            return new SyncResult(new NotDirectoryException("Not a directory: '" + entry.getRelativePath() + "'"));
        }
        if (target.isReadOnly()) {
            return new SyncResult(SyncResult.SYNC_IGNORE, true);
        }
        if (!segment.getOptions().writeToSymLinks() && entry.isSymLink()) {
            return new SyncResult(SyncResult.SYNC_IGNORE, true);
        }
        return new SyncResult(SyncResult.SYNC_MERGE, true);
    }

}
