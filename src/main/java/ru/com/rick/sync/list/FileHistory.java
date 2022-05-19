/*
 * Copyright (c) 2022 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.list;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import ru.com.rick.sync.fs.FileVersion;
import ru.com.rick.sync.json.JsonArray;
import ru.com.rick.sync.json.JsonObject;

/**
 * Represents an history entry in file list. Contains information about a file at the moment of list generation.
 *
 * @author Rick
 */
public class FileHistory extends FileListEntry
{

    /**
     * Previous file history
     */
    protected FileVersionCollection<?> history = null;

    /**
     * New record without specific info.
     *
     * @param exists
     * @param defaultTime
     * @param state
     */
    public FileHistory(boolean exists, long defaultTime, String state)
    {
        super(exists, defaultTime, state);
    }

    /**
     * New record without specific info.
     *
     * @param defaultTime
     */
    public FileHistory(long defaultTime)
    {
        super(false, defaultTime, null);
    }

    /**
     * New record from existing stored file version.
     *
     * @param current
     * @param defaultTime
     * @param state
     * @param versions
     */
    public FileHistory(FileVersion current, long defaultTime, String state, Collection<FileVersion> versions)
    {
        super(current, defaultTime, state);
        if (versions != null && versions.size() > 0) {
            this.history = new FileVersionCollection();
            this.history.fill(versions);
        }
    }

    /**
     * New entry from JSON.
     *
     * @param json
     * @param defaultTime
     */
    public FileHistory(Map json, long defaultTime)
    {
        super(json, defaultTime);
        Object hobj = json.get("history");
        if (hobj instanceof List) {
            List array = (List)hobj;
            int n = array.size();
            if (n > 0) {
                history = new FileVersionCollection();
                history.fill(array, defaultTime);
            }
        }
    }

    @Override
    public JsonObject toJson(long defaultTime, int maxVersions, boolean withHash)
    {
        JsonObject result = super.toJson(defaultTime, maxVersions, withHash);
        if (history != null) {
            JsonArray varr = history.toJson(defaultTime, maxVersions, withHash);
            if (varr.size() > 0) {
                result.put("history", varr);
            }
        }
        return result;
    }

    @Override
    public boolean hasNoHistory()
    {
        return (history == null || history.isEmpty());
    }

    @Override
    public FileVersion[] getHistoryVersions()
    {
        return history != null ? history.getVersions() : new FileVersion[0];
    }

    @Override
    public FileVersion[] getSortedHistoryVersions()
    {
        return history != null ? history.getSortedVersions() : new FileVersion[0];
    }

    /**
     * Add history from another entry.
     *
     * @param record
     * @return count added
     */
    public int addHistoryVersions(FileHistory record)
    {
        int c = 0;
        if (record == null) {
            return c;
        }
        if (record.current != null && addHistoryVersion(record.current)) {
            c++;
        }
        if (record.history != null) {
            c += addHistoryVersions(() -> record.history.getVersionsStream().iterator());
        }
        return c;
    }

    /**
     * Add versions.
     *
     * @param versions
     * @return count added
     */
    public int addHistoryVersions(Iterable<FileVersion> versions)
    {
        int c = 0;
        for (FileVersion ver : versions) {
            if (addHistoryVersion(ver)) {
                c++;
            }
        }
        return c;
    }

    /**
     * Add a version (if it is valid and not yet added).
     *
     * @param ver
     * @return success
     */
    public boolean addHistoryVersion(FileVersion ver)
    {
        if (!ver.isValidVersion()) {
            return false;
        }
        if (ver.isSameVersion(current)) {
            return false;
        }
        if (history == null) {
            history = new FileVersionCollection();
        }
        return history.addVersion(ver);
    }

    /**
     * Get version from history equal to the supplied.
     *
     * @param ver
     * @param checkTime
     * @param checkHash
     * @return version from history
     */
    public FileVersion getHistoryVersion(FileVersion ver, boolean checkTime, boolean checkHash)
    {
        if (history == null) {
            return null;
        } else {
            return history.getVersion(ver, checkTime, checkHash);
        }
    }
}
