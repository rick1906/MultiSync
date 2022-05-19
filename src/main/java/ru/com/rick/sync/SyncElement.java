/**
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync;

import ru.com.rick.sync.fs.FileEntry;
import ru.com.rick.sync.options.GlobalOptions;
import java.io.IOException;
import java.nio.file.Path;
import ru.com.rick.sync.fs.BadDataException;
import ru.com.rick.sync.list.FileHistory;

/**
 * Representaion of a single synchronization element, one relative path for multiple sync targets.
 *
 * @author Rick
 */
public class SyncElement implements SyncContainer
{
    private final Path rpath;
    private final SyncTarget[] targets;
    private final SyncContainer parent;
    private final FileSyncEntry[] entries;
    private final Status[] statuses;
    private int sourceIndex = -1;

    /**
     *
     * @param rpath relative path
     * @param targets sync targets
     * @param parent parent element (with parent relative path)
     */
    public SyncElement(Path rpath, SyncTarget[] targets, SyncContainer parent)
    {
        this.rpath = rpath;
        this.targets = targets;
        this.parent = parent;
        this.entries = new FileSyncEntry[targets.length];
        this.statuses = new Status[targets.length];
    }

    /**
     *
     * @return parent element
     */
    public SyncContainer getParent()
    {
        return parent;
    }

    @Override
    public SyncResult getSyncResult(int index)
    {
        return getStatus(index).getResult();
    }

    @Override
    public SyncStatus getSyncStatus(int index)
    {
        return getStatus(index);
    }

    @Override
    public SyncTarget[] getTargets()
    {
        return targets;
    }

    @Override
    public FileSyncEntry getEntry(int index)
    {
        if (entries[index] == null) {
            return addEntry(index);
        }
        return entries[index];
    }

    /**
     *
     * @param index sync target index
     * @return single FileSyncEntry for parent element
     */
    public FileSyncEntry getParentEntry(int index)
    {
        return parent != null ? parent.getEntry(index) : null;
    }

    /**
     *
     * @param index sync target index
     * @param checked checked if file exists
     * @param exists file exists state (if checked)
     * @return new FileSyncEntry
     */
    protected FileSyncEntry newEntry(int index, boolean checked, boolean exists)
    {
        FileSyncEntry p = getParentEntry(index);
        FileEntry e;
        if (checked) {
            e = targets[index].getFileEntry(rpath, exists);
        } else if (p == null || p.isContainer()) {
            e = targets[index].getFileEntry(rpath);
        } else {
            e = targets[index].getFileEntry(rpath, false);
        }
        return new FileSyncEntry(targets[index], p, e, targets[index].getFileHistory(rpath));
    }

    /**
     * Add new sync entry to this element.
     *
     * @param index sync target index
     * @return sync entry for index
     */
    protected FileSyncEntry addEntry(int index)
    {
        return entries[index] = newEntry(index, false, false);
    }

    /**
     * Add new sync entry to this element.
     *
     * @param index sync target index
     * @param exists file exists state
     * @return sync entry for index
     */
    protected FileSyncEntry addEntry(int index, boolean exists)
    {
        return entries[index] = newEntry(index, true, exists);
    }

    /**
     *
     * @param index
     * @return internal sync status for index
     */
    public Status getStatus(int index)
    {
        if (statuses[index] == null) {
            statuses[index] = new Status(index);
        }
        return statuses[index];
    }

    /**
     *
     * @return sync element relative path
     */
    public Path getPath()
    {
        return rpath;
    }

    /**
     *
     * @return selected sync source index or -1
     */
    @Override
    public int getSourceIndex()
    {
        return sourceIndex;
    }

    /**
     *
     * @param defaultTime sync time
     * @return file list history entry for selected source
     */
    public FileHistory generateResultHistory(long defaultTime)
    {
        FileSyncEntry selected = getSelectedEntry();
        if (selected != null) {
            FileHistory history = new FileHistory(defaultTime);
            history.setCurrentVersion(selected.getProperties(), selected.getHistory(), true);
            history.addHistoryVersions(selected.getHistory());
            if (!isFullySynced()) {
                history.setSyncState(selected.getHistorySyncState());
            }
            return history;
        }
        return null;
    }

    /**
     *
     * @param index sync target index
     * @param defaultTime sync time
     * @return file list history entry for target with index
     */
    public FileHistory generateResultHistory(int index, long defaultTime)
    {
        if (!targets[index].isAvailable()) {
            return getEntry(index).getHistory();
        }
        if (index != sourceIndex) {
            FileHistory history = new FileHistory(defaultTime);
            FileSyncEntry selected = getSelectedEntry(index);
            FileSyncEntry entry = getEntry(index);
            if (entry.isIgnored()) {
                return null;
            } else if (selected != null) {
                history.setCurrentVersion(selected.getProperties(), selected.getHistory(), true);
                history.addHistoryVersion(entry.getOldProperties());
                history.addHistoryVersions(entry.getHistory());
            } else {
                history.setCurrentVersion(entry.getProperties(), entry.getHistory(), false);
                history.addHistoryVersions(entry.getHistory());
            }
            return history;
        } else {
            FileHistory history = generateResultHistory(defaultTime);
            if (history != null) {
                return history;
            } else {
                FileSyncEntry entry = getEntry(index);
                return entry.isIgnored() ? null : entry.getHistory();
            }
        }
    }

    /**
     *
     * @return selected source (selected sync target)
     */
    public SyncTarget getSelectedTarget()
    {
        if (sourceIndex >= 0) {
            return targets[sourceIndex];
        }
        return null;
    }

    /**
     *
     * @return selected FileSyncEntry
     */
    public FileSyncEntry getSelectedEntry()
    {
        if (sourceIndex >= 0) {
            return getEntry(sourceIndex);
        }
        return null;
    }

    /**
     *
     * @param index sync target index
     * @return selected FileSyncEntry as source for target with index
     */
    public FileSyncEntry getSelectedEntry(int index)
    {
        Status status = getStatus(index);
        if (status.isSuccess() || index == sourceIndex) {
            return getSelectedEntry();
        }
        return null;
    }

    /**
     *
     * @return true if every target is synced successfully
     */
    public boolean isFullySynced()
    {
        for (int i = 0; i < statuses.length; ++i) {
            if (i != sourceIndex && (!statuses[i].isDone() || !statuses[i].isSuccess())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Choose one sync target as source for other for this path.
     *
     * @param options
     * @return selected source index
     */
    public int detectSource(GlobalOptions options)
    {
        sourceIndex = -1;
        FileSyncEntry source = null;
        if (parent != null) {
            int ps = parent.getSourceIndex();
            if (ps >= 0 && !parent.isMerging(ps)) {
                sourceIndex = ps;
                return sourceIndex;
            }
        }
        for (int i = 0; i < targets.length; ++i) {
            SyncTarget target = targets[i];
            if (target.isSource() && target.isAvailable()) {
                FileSyncEntry entry = getEntry(i);
                if (entry.comparePreferredSource(source, options) > 0) {
                    sourceIndex = i;
                    source = entry;
                }
            }
        }
        return sourceIndex;
    }

    /**
     * Compare files in all targets, determine sync actions.
     *
     * @param options
     * @param detectSource detect source before analyze
     * @return success
     */
    public boolean analyze(GlobalOptions options, boolean detectSource)
    {
        if (detectSource) {
            detectSource(options);
        }
        if (sourceIndex >= 0) {
            FileSyncEntry source = getEntry(sourceIndex);
            for (int i = 0; i < targets.length; ++i) {
                analyze(source, i, options);
            }
            return true;
        }
        return false;
    }

    /**
     * Analyze single sync target.
     *
     * @param index
     * @param options
     * @return success
     */
    public boolean analyze(int index, GlobalOptions options)
    {
        if (sourceIndex >= 0) {
            analyze(getEntry(index), index, options);
            return true;
        }
        return false;
    }

    /**
     * Internal analyze one target with supplied source.
     *
     * @param source sync source
     * @param index sync target index
     * @param options
     */
    protected void analyze(FileSyncEntry source, int index, GlobalOptions options)
    {
        FileSyncEntry target = getEntry(index);
        if (!target.isAvailable()) {
            getStatus(index).init(Status.SYNC_SKIP, target);
        } else {
            try {
                getStatus(index).init(source.getSyncStatus(target, options), target);
            } catch (IOException ex) {
                getStatus(index).init(ex);
            }
        }
    }

    /**
     * Synchronize this element.
     *
     * @param options
     * @return sync results for each target
     */
    public SyncResult[] sync(GlobalOptions options)
    {
        if (sourceIndex >= 0) {
            SyncResult[] results = new SyncResult[entries.length];
            FileSyncEntry source = getEntry(sourceIndex);
            for (int i = 0; i < entries.length; ++i) {
                results[i] = sync(source, i, options);
            }
            return results;
        }
        return null;
    }

    /**
     * Synchronize one entry in this element.
     *
     * @param index sync target index
     * @param options
     * @return sync result
     */
    public SyncResult sync(int index, GlobalOptions options)
    {
        if (sourceIndex >= 0) {
            FileSyncEntry source = getEntry(index);
            return sync(source, index, options);
        }
        return null;
    }

    /**
     * Internal sync one entry with supplied source.
     *
     * @param source sync source
     * @param index sync target index
     * @param options
     * @return
     */
    protected SyncResult sync(FileSyncEntry source, int index, GlobalOptions options)
    {
        Status status = getStatus(index);
        FileSyncEntry target = getEntry(index);
        if (target.isAvailable()) {
            return syncFile(source, target, status, options);
        } else {
            return status.setResult(Status.SYNC_SKIP);
        }
    }

    /**
     * Internal sync one entry with the other.
     *
     * @param source sync source entry
     * @param target sync target entry
     * @param status target status
     * @param options
     * @return sync result
     */
    protected SyncResult syncFile(FileSyncEntry source, FileSyncEntry target, Status status, GlobalOptions options)
    {
        if (status.isDone()) {
            return status.getResult();
        }
        if (source.equals(target) || !status.parentIsMerging()) {
            return status.setResult(status.isGoodStatus());
        }
        if (!status.isGoodStatus()) {
            return status.setResult(false);
        }
        if (status.isSkipStatus()) {
            return status.setResult(true);
        }
        if (status.getValidationError() != null && !target.getSyncOptions().ignoreMinorErrors()) {
            return status.setResult(status.getValidationError());
        }

        SyncResult result;
        int value = status.getValue();
        if (value == Status.SYNC_DELETE) {
            result = target.delete(options);
        } else if (value == Status.SYNC_CREATE) {
            result = target.copyFrom(source, false, options);
        } else if (value == Status.SYNC_REPLACE) {
            result = target.copyFrom(source, true, options);
        } else if (value == Status.SYNC_MERGE) {
            result = target.mergeWith(source, options);
        } else {
            return status.setResult(Status.SYNC_UNKNOWN);
        }
        return status.setResult(result);
    }

    /**
     * Internal sync status, storing analyze and sync results.
     */
    public class Status extends SyncStatus
    {
        private final int index;
        private boolean conflict = false;
        private Exception error = null;
        private SyncResult result = null;

        /**
         *
         * @param index
         */
        public Status(int index)
        {
            this.index = index;
        }

        /**
         * Init with status value.
         *
         * @param syncStatus
         * @param entry
         * @return
         */
        private boolean init(int syncStatus, FileSyncEntry entry)
        {
            value = syncStatus;
            validate(entry);
            return true;
        }

        /**
         * Init with error.
         *
         * @param ex
         * @return
         */
        private boolean init(Exception ex)
        {
            value = SYNC_FAILURE;
            error = ex;
            return false;
        }

        /**
         * Validate entry, check it for supressed errors.
         *
         * @param entry
         */
        private void validate(FileSyncEntry entry)
        {
            error = null;
            conflict = false;
            if (entry.isAvailable()) {
                try {
                    entry.getFileEntry().getProperties().validateOrThrow();
                } catch (BadDataException ex) {
                    error = ex;
                }
            }
            if (value > SYNC_UNKNOWN) {
                int changeStatus = entry.getChangeStatus();
                conflict = changeStatus > FileSyncEntry.CHANGE_UNKNOWN;
            }
        }

        /**
         * Set sync result to this status object.
         *
         * @param result
         * @return sync result
         */
        public SyncResult setResult(SyncResult result)
        {
            this.result = result;
            if (this.value == SYNC_UNKNOWN) {
                this.value = result.getValue();
            }
            return result;
        }

        /**
         * Set sync result by result code.
         *
         * @param syncStatus
         * @return sync result
         */
        public SyncResult setResult(int syncStatus)
        {
            return setResult(new SyncResult(syncStatus));
        }

        /**
         * Set sync result with error.
         *
         * @param error
         * @return sync result
         */
        public SyncResult setResult(Exception error)
        {
            return setResult(new SyncResult(error));
        }

        /**
         * Set sync result with current status code and supplied success flag.
         *
         * @param success success flag
         * @return sync result
         */
        public SyncResult setResult(boolean success)
        {
            result = new SyncResult(value, success);
            return result;
        }

        /**
         *
         * @return validation error
         */
        public Exception getValidationError()
        {
            return error;
        }

        /**
         *
         * @return if the entry is changed, but not choosen as sync source
         */
        public boolean isConflict()
        {
            return conflict && index != sourceIndex;
        }

        /**
         *
         * @return has sync result
         */
        public boolean isDone()
        {
            return result != null;
        }

        /**
         *
         * @return sync result
         */
        public SyncResult getResult()
        {
            return result;
        }

        /**
         *
         * @return if done, sync result success, otherwise analyze success
         */
        @Override
        public boolean isSuccess()
        {
            return result != null ? result.isSuccess() : isGoodStatus();
        }

        /**
         *
         * @return parent is a directory ready to merge with others
         */
        public boolean parentIsMerging()
        {
            return parent == null || parent.isMerging(index);
        }

    }
}
