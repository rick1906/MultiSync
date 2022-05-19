/**
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.fs;

import java.util.Map;
import ru.com.rick.sync.json.JsonObject;
import ru.com.rick.sync.json.JsonUtils;

/**
 * File properties to identify a file and it's status (exists or not).
 *
 * @author Rick
 */
public class FileVersion
{
    public static final long TIME_UNKNOWN = Long.MIN_VALUE;
    public static final long TIME_OLDEST = Long.MIN_VALUE + 1;
    //
    public static final FileVersion EMPTY = new FileVersion();
    //
    private static final String KEY_SYNCTIME = "sync";
    private static final String KEY_SINCE = "since";
    private static final String KEY_EXISTS = "exists";
    private static final String KEY_DIR = "dir";
    private static final String KEY_MTIME = "modified";
    private static final String KEY_SIZE = "size";
    private static final String KEY_HASH = "md5";

    /**
     * The last seen time (unix, ms) of file version
     */
    protected long time = TIME_UNKNOWN;

    /**
     * The first seen time (unix, ms) of file version
     */
    protected long since = TIME_UNKNOWN;

    // props
    protected boolean exists = false;
    protected boolean isDir = false;
    protected long mtime = TIME_UNKNOWN;
    protected long fsize = -1;
    protected String md5 = null;

    protected FileVersion()
    {

    }

    /**
     * Creates a copy of properties.
     *
     * @param base
     */
    public FileVersion(FileVersion base)
    {
        time = base.time;
        exists = base.exists;
        isDir = base.isDir;
        mtime = base.mtime;
        fsize = base.fsize;
        md5 = base.md5;
    }

    /**
     * Poperties from JSON.
     *
     * @param json
     * @param defaultTime time of JSON generation
     */
    public FileVersion(Map json, long defaultTime)
    {
        time = getTimeFromJson(json, KEY_SYNCTIME, defaultTime);
        since = getTimeFromJson(json, KEY_SINCE, time);
        exists = JsonUtils.getBoolean(json, KEY_EXISTS, true);
        mtime = getTimeFromJson(json, KEY_MTIME, TIME_UNKNOWN);
        fsize = JsonUtils.getLong(json, KEY_SIZE, -1L);
        md5 = JsonUtils.getString(json, KEY_HASH, null);
        isDir = JsonUtils.getBoolean(json, KEY_DIR, false);
    }

    /**
     * Exxport to JSON.
     *
     * @param defaultTime start time of JSON generation
     * @param withHash force hash calculation (if possible)
     * @return
     */
    public JsonObject toJson(long defaultTime, boolean withHash)
    {
        JsonObject result = new JsonObject();
        if (!exists()) {
            result.put(KEY_EXISTS, false);
            return result;
        }
        if (time != TIME_UNKNOWN && time != defaultTime) {
            putTimeToJson(result, KEY_SYNCTIME, time);
        }
        if (since != TIME_UNKNOWN && since != time) {
            putTimeToJson(result, KEY_SINCE, since);
        }
        if (isDir()) {
            result.put(KEY_DIR, true);
        } else {
            long vmtime = getModifiedTime();
            long vfsize = getFileSize();
            String vhash = withHash ? getHash() : md5;
            if (vmtime != TIME_UNKNOWN) {
                putTimeToJson(result, KEY_MTIME, vmtime);
            }
            if (vfsize >= 0) {
                result.put(KEY_SIZE, vfsize);
            }
            if (vhash != null) {
                result.put(KEY_HASH, vhash);
            }
        }
        return result;
    }

    /**
     *
     * @param json
     * @return if JSON has properties
     */
    public static boolean hasProperties(Map json)
    {
        return json.containsKey(KEY_SYNCTIME)
                || json.containsKey(KEY_SINCE)
                || json.containsKey(KEY_DIR)
                || json.containsKey(KEY_MTIME)
                || json.containsKey(KEY_SIZE);
    }

    /**
     * Get time in ms from JSON time in seconds.
     *
     * @param json JSON object
     * @param key JSON key
     * @param defval default value (unix time ms)
     * @return unix time in ms
     */
    public static long getTimeFromJson(Map json, String key, long defval)
    {
        if (json.containsKey(key)) {
            long time = JsonUtils.getLong(json, key);
            return time != TIME_UNKNOWN ? (time * 1000) : TIME_UNKNOWN;
        }
        return defval;
    }

    /**
     * Convert time in ms to time in seconds and put it to JSON.
     *
     * @param json JSON object
     * @param key JSON key
     * @param time unix time in ms
     */
    public static void putTimeToJson(Map<String, Object> json, String key, long time)
    {
        json.put(key, time != TIME_UNKNOWN ? (time / 1000) : null);
    }

    /**
     * For equal versions (doesn't check), supplements current version with missing properties.
     *
     * @param version
     * @param synced
     * @return success
     */
    public boolean supplement(FileVersion version, boolean synced)
    {
        if (!exists()) {
            return false;
        }
        if (!synced && time == TIME_UNKNOWN && version.time != TIME_UNKNOWN) {
            time = version.time;
        }
        if (since == TIME_UNKNOWN && version.since != TIME_UNKNOWN) {
            since = version.since;
        }
        if (!hasModifiedTime() && version.hasModifiedTime()) {
            mtime = version.getModifiedTime();
        }
        if (!hasFileSize() && version.hasFileSize()) {
            fsize = version.getFileSize();
        }
        if (!hasHash() && version.hasHash()) {
            md5 = version.getHash();
        }
        return true;
    }

    /**
     *
     * @return last time the data was synced (TIME_UNKNOWN for new)
     */
    public final long getSyncTime()
    {
        return time;
    }

    /**
     *
     * @return first time the file version was found
     */
    public final long getFirstSeenTime()
    {
        return since;
    }

    /**
     *
     * @return file exists
     */
    public boolean exists()
    {
        return exists;
    }

    /**
     *
     * @return file is directiry
     */
    public boolean isDir()
    {
        return isDir;
    }

    /**
     *
     * @return last modified time
     */
    public long getModifiedTime()
    {
        return mtime;
    }

    /**
     *
     * @return file size
     */
    public long getFileSize()
    {
        return fsize;
    }

    /**
     *
     * @return md5 hash
     */
    public String getHash()
    {
        return md5;
    }

    /**
     *
     * @return
     */
    public boolean hasModifiedTime()
    {
        return mtime != TIME_UNKNOWN;
    }

    /**
     *
     * @return
     */
    public boolean hasFileSize()
    {
        return fsize >= 0;
    }

    /**
     *
     * @return
     */
    public boolean hasHash()
    {
        return md5 != null;
    }

    /**
     * Check if properties possibly represent equal files.
     *
     * @param other other properties
     * @param checkTime trust modified time
     * @param checkHash check hash
     * @return
     */
    public boolean isEqualTo(FileVersion other, boolean checkTime, boolean checkHash)
    {
        if (isDir() != other.isDir() || exists() != other.exists()) {
            return false;
        }
        if (isDir() || !exists()) {
            return true;
        }
        if (checkTime) {
            long tNew = getModifiedTime();
            long tOld = other.getModifiedTime();
            if (tOld != TIME_UNKNOWN && !FileUtils.compareModifiedTimes(tOld, tNew, true)) {
                return false;
            }
        }
        if (true) {
            long sNew = getFileSize();
            long sOld = other.getFileSize();
            if (sOld >= 0 && sOld != sNew) {
                return false;
            }
        }
        if (checkHash) {
            String hNew = getHash();
            String hOld = other.getHash();
            if (hOld != null && !hOld.equals(hNew)) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @return long value for comparing sync time
     */
    protected long getSyncTimeCompareValue()
    {
        return time == TIME_UNKNOWN ? TIME_OLDEST : time;
    }

    /**
     *
     * @param other
     * @return comparison result (0 for equal)
     */
    public int compareSyncTime(FileVersion other)
    {
        long t0 = getSyncTimeCompareValue();
        long t1 = other.getSyncTimeCompareValue();
        return t0 == t1 ? 0 : (t0 > t1 ? 1 : -1);
    }

    /**
     *
     * @return if properties can represent a file version
     */
    public boolean isValidVersion()
    {
        return time != TIME_UNKNOWN && (isDir || hasModifiedTime() || hasFileSize() || hasHash());
    }

    /**
     *
     * @param other
     * @return if properrties represent the same version
     */
    public boolean isSameVersion(FileVersion other)
    {
        if (other == null) {
            return !exists();
        }
        if (this == other) {
            return true;
        }
        if (isDir() != other.isDir() || exists() != other.exists()) {
            return false;
        }
        if (isDir() || !exists()) {
            return true;
        }
        if (!FileUtils.compareModifiedTimes(getModifiedTime(), other.getModifiedTime(), true)) {
            return false;
        }
        if (getFileSize() != other.getFileSize()) {
            return false;
        }
        if (hasHash() && other.hasHash() && !getHash().equals(other.getHash())) {
            return false;
        }
        return true;
    }

}
