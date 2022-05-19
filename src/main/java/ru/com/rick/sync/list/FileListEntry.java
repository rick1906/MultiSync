/*
 * Copyright (c) 2022 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.list;

import java.util.Map;
import ru.com.rick.sync.fs.FileVersion;
import ru.com.rick.sync.json.JsonObject;
import ru.com.rick.sync.json.JsonUtils;

/**
 * Base class for file list entries (history/global).
 *
 * @author Rick
 */
public abstract class FileListEntry
{
    public static final String STATE_NONE = null;
    public static final String STATE_DELETE = "delete";
    public static final String STATE_UPDATE = "update";

    /**
     * Current version data
     */
    protected FileVersion current = null;

    /**
     * True if file existed
     */
    protected boolean exists = false;

    /**
     * Sync state (null if fully sync, otherwise last sync action)
     */
    protected String state = STATE_NONE;

    /**
     * List or list entry generation time
     */
    protected long time = FileVersion.TIME_UNKNOWN;

    /**
     * New record without specific info.
     *
     * @param exists
     * @param defaultTime
     * @param state
     */
    public FileListEntry(boolean exists, long defaultTime, String state)
    {
        this.time = defaultTime;
        this.exists = exists;
        this.state = state;
    }

    /**
     * New record from existing stored file version.
     *
     * @param current
     * @param defaultTime
     * @param state
     */
    public FileListEntry(FileVersion current, long defaultTime, String state)
    {
        this.current = current;
        this.state = state;
        if (current != null) {
            this.time = current.getSyncTime();
            this.exists = current.exists();
        } else {
            this.time = defaultTime;
            this.exists = false;
        }
    }

    /**
     * New entry from JSON.
     *
     * @param json
     * @param defaultTime
     */
    public FileListEntry(Map json, long defaultTime)
    {
        state = JsonUtils.getString(json, "state", null);
        if (FileVersion.hasProperties(json)) {
            current = new FileVersion(json, defaultTime);
            time = current.getSyncTime();
            exists = current.exists();
        } else {
            time = defaultTime;
            exists = JsonUtils.getBoolean(json, "exists", false);
        }
    }

    /**
     * Export. See FileList::toJson.
     *
     * @param defaultTime
     * @param maxVersions
     * @param withHash
     * @return
     */
    public JsonObject toJson(long defaultTime, int maxVersions, boolean withHash)
    {
        JsonObject result;
        if (time != FileVersion.TIME_UNKNOWN) {
            defaultTime = time;
        }
        if (current != null) {
            result = current.exists() ? current.toJson(defaultTime, withHash) : new JsonObject();
        } else {
            result = new JsonObject();
            if (exists) {
                result.put("exists", true);
            }
        }
        if (state != null && !state.isEmpty()) {
            result.put("state", state);
        }
        return result;
    }

    /**
     *
     * @return if file existed
     */
    public boolean exists()
    {
        return current != null ? current.exists() : exists;
    }

    /**
     *
     * @param withHistory
     * @return has no versions and no current state
     */
    public boolean isEmpty(boolean withHistory)
    {
        return !exists() && (state == null || state.isEmpty()) && (!withHistory || hasNoHistory());
    }

    /**
     *
     * @return history is empty
     */
    public abstract boolean hasNoHistory();

    /**
     *
     * @return generation time (sync time)
     */
    public long getSyncTime()
    {
        if (current != null) {
            long t = current.getSyncTime();
            return t != FileVersion.TIME_UNKNOWN ? t : time;
        }
        return time;
    }

    /**
     *
     * @return sync state
     */
    public String getSyncState()
    {
        return state;
    }

    /**
     *
     * @param state
     */
    public void setSyncState(String state)
    {
        this.state = state;
    }

    /**
     *
     * @return current version
     */
    public FileVersion getCurrentVersion()
    {
        return current;
    }

    /**
     * Supplement this current version with data from other record current.
     *
     * @param record other file history record
     * @param synced if current version is synced with source
     * @return true if current history in both records represent the same file
     */
    protected boolean supplement(FileListEntry record, boolean synced)
    {
        if (current != null && record.current != null && record.current.isValidVersion() && record.current.isSameVersion(current)) {
            current.supplement(record.current, synced);
            return true;
        }
        return false;
    }

    /**
     * Set the current version.
     *
     * @param version current file version from file system (sync source file / skipped target file)
     * @param record history for that file version (sync source history / skipped target history)
     * @param synced if current version is synced with source
     */
    public void setCurrentVersion(FileVersion version, FileListEntry record, boolean synced)
    {
        if (version == null || !version.exists()) { // file was deleted
            current = null;
            exists = false;
        } else {
            current = version;
            exists = true;
            if (record != null) {
                supplement(record, synced);
            }
        }
    }

    /**
     *
     * @return history versions sorted DESC by sync time
     */
    public abstract FileVersion[] getSortedHistoryVersions();

    /**
     *
     * @return versions in stored history (properties' versions) of a file
     */
    public abstract FileVersion[] getHistoryVersions();

}
