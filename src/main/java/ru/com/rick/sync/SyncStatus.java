/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync;

/**
 * Status of a syncronization process for a single entry.
 *
 * @author Rick
 */
public class SyncStatus
{
    public static final int SYNC_FAILURE = -10;
    public static final int SYNC_IGNORE = -3; // ignore fo various reasons
    public static final int SYNC_SKIP = -2; // no need to sync
    public static final int SYNC_MATCHES = -1; // files match
    public static final int SYNC_UNKNOWN = 0;
    public static final int SYNC_CREATE = 1;
    public static final int SYNC_DELETE = 2;
    public static final int SYNC_REPLACE = 3;
    public static final int SYNC_MERGE = 4;

    /**
     * Sync status value.
     */
    protected int value = SYNC_UNKNOWN;

    public SyncStatus()
    {
    }

    public SyncStatus(int syncStatus)
    {
        value = syncStatus;
    }

    /**
     *
     * @return sync status value
     */
    public final int getValue()
    {
        return value;
    }

    /**
     *
     * @return true if sync operation is valid
     */
    public final boolean isGoodStatus()
    {
        return value != SYNC_FAILURE && value != SYNC_UNKNOWN;
    }

    /**
     *
     * @return true if no sync operation will be performed
     */
    public final boolean isSkipStatus()
    {
        return value <= SYNC_UNKNOWN;
    }

    /**
     *
     * @return not failure
     */
    public boolean isSuccess()
    {
        return isGoodStatus();
    }

    @Override
    public String toString()
    {
        switch (value) {
            case SYNC_FAILURE:
                return "failure";
            case SYNC_IGNORE:
                return "ignore";
            case SYNC_SKIP:
                return "skip";
            case SYNC_MATCHES:
                return "matches";
            case SYNC_UNKNOWN:
                return "unknown";
            case SYNC_CREATE:
                return "create";
            case SYNC_DELETE:
                return "delete";
            case SYNC_REPLACE:
                return "replace";
            case SYNC_MERGE:
                return "merge";
        }
        return "unknown";
    }

}
