/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.fs;

import java.util.Map;
import ru.com.rick.sync.json.JsonOptions;
import ru.com.rick.sync.json.JsonUtils;

/**
 *
 * @author Rick
 */
public class PathOptions extends JsonOptions
{
    public PathOptions(Map json, JsonOptions parent)
    {
        super(json, parent);
    }

    public PathOptions(JsonOptions options)
    {
        super(options);
    }

    /**
     *
     * @return use last modified time of a file
     */
    public boolean useModifiedTime()
    {
        return getBooleanOption("useModifiedTime", true);
    }

    /**
     *
     * @return use created time of a file
     */
    public boolean useCreatedTime()
    {
        return getBooleanOption("useCreatedTime", true);
    }

    /**
     *
     * @return use file hash (md5) to identify a file
     */
    public boolean useHash()
    {
        return getBooleanOption("useHash", true);
    }

    /**
     *
     * @return compare level for comparing files
     */
    public int getCompareLevel()
    {
        Object value = getOption("compareLevel", FileEntry.COMPARE_LEVEL_MIXED);
        if (value instanceof Boolean) {
            return ((Boolean)value) ? FileEntry.COMPARE_LEVEL_MIXED : 0;
        }
        if (value instanceof String) {
            String s = ((String)value).toLowerCase();
            if (s.equals("size")) {
                return FileEntry.COMPARE_LEVEL_SIZE;
            }
            if (s.equals("hash")) {
                return FileEntry.COMPARE_LEVEL_HASH;
            }
            if (s.equals("mixed")) {
                return FileEntry.COMPARE_LEVEL_MIXED;
            }
            if (s.equals("content")) {
                return FileEntry.COMPARE_LEVEL_CONTENT;
            }
        }
        return JsonUtils.castToInteger(value);
    }

    /**
     *
     * @return size limit for comparing contents
     */
    public long getCompareSizeLimit()
    {
        Object value = getOption("compareSizeLimit", "1M");
        if (value instanceof String) {
            String v = (String)value;
            int len = v.length();
            if (!v.matches("^\\d+$") && len != 0) {
                String ch = v.substring(len - 1).toUpperCase();
                int ix = "KMGT".indexOf(ch);
                if (ix >= 0) {
                    long xv = Long.valueOf(v.substring(0, len - 1));
                    for (int i = 0; i <= ix; ++i) {
                        xv *= 1024;
                    }
                    return xv;
                }
            }
        }
        if (value == null || value.equals(false)) {
            return 0;
        }
        return JsonUtils.castToLong(value);
    }

    /**
     *
     * @return treat files with different modified times as unequal
     */
    public boolean compareModifiedTime()
    {
        return getBooleanOption("compareModifiedTime", true);
    }

    /**
     *
     * @return allow writing to symlink directories
     */
    public boolean writeToSymLinks()
    {
        return getBooleanOption("writeToSymLinks", true) && followSymLinks();
    }

    /**
     *
     * @return allow reading and syncing files from symlink directories
     */
    public boolean followSymLinks()
    {
        return getBooleanOption("followSymLinks", true);
    }
}
