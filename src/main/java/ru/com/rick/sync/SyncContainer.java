/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync;

/**
 * Abstraction for resursive synchronization. Used as a parent sync element.
 *
 * @author Rick
 */
public interface SyncContainer
{
    /**
     *
     * @return sync targets
     */
    public SyncTarget[] getTargets();

    /**
     *
     * @return sync targets count
     */
    public default int countTargets()
    {
        return getTargets().length;
    }

    /**
     *
     * @param index
     * @return sync target by index
     */
    public default SyncTarget getTarget(int index)
    {
        return getTargets()[index];
    }

    /**
     *
     * @param index
     * @return sync entry for correspinding target, can be null
     */
    public FileSyncEntry getEntry(int index);

    /**
     *
     * @param index
     * @return sync result for correspinding target, can be null if not yet synced
     */
    public SyncResult getSyncResult(int index);

    /**
     *
     * @param index
     * @return pre-sync status for correspinding target
     */
    public SyncStatus getSyncStatus(int index);

    /**
     *
     * @return index of selected sync source or -1
     */
    public int getSourceIndex();

    /**
     *
     * @param index
     * @return true if directory is to be merged with other sources
     */
    public default boolean isMerging(int index)
    {
        SyncStatus s = getSyncResult(index);
        if (s == null) {
            s = getSyncStatus(index);
        }
        if (s.isSuccess()) {
            int v = s.getValue();
            return v == SyncResult.SYNC_MERGE || v == SyncResult.SYNC_MATCHES;
        }
        return false;
    }
}
