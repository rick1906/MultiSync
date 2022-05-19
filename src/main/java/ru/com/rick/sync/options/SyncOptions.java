/**
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.options;

import java.util.List;
import java.util.Map;
import ru.com.rick.sync.json.JsonOptions;
import ru.com.rick.sync.json.JsonUtils;

/**
 * Synchroniation options.
 *
 * @author Rick
 */
public class SyncOptions extends JsonOptions
{

    public SyncOptions(Map json)
    {
        super(json);
    }

    public SyncOptions(Map json, SyncOptions parent)
    {
        super(json, parent);
    }

    /**
     *
     * @return backup deleted and replaced files
     */
    public boolean isBackup()
    {
        Object value = getOption("backup");
        if (value instanceof Map) {
            return JsonUtils.getBoolean((Map)value, "enabled", true);
        }
        if (value instanceof Boolean) {
            return (Boolean)value;
        }
        return true;
    }

    /**
     *
     * @return local backuper configuration or null if not specified
     */
    public Map getBackuper()
    {
        Object value = json.get("backup");
        if (value instanceof Map) {
            return (Map)value;
        }
        if (value == null || value instanceof Boolean) {
            return null;
        }
        return JsonUtils.transformToMap(value, "path");
    }

    /**
     *
     * @return local file list configuration
     */
    public Map getFileList()
    {
        Object value = json.get("list");
        if (value instanceof Map) {
            return (Map)value;
        }
        if (value == null) {
            return null;
        }
        return JsonUtils.transformToMap(value, "path");
    }

    /**
     *
     * @return ignore errors not critical for file sync
     */
    public boolean ignoreMinorErrors()
    {
        return getBooleanOption("ignoreMinorErrors", true);
    }

    /**
     *
     * @return allow create root directory
     */
    public boolean createRoot()
    {
        return getBooleanOption("createRoot", true);
    }

    /**
     *
     * @return
     */
    public boolean allowCreate()
    {
        return getBooleanOption("create", true);
    }

    /**
     *
     * @return
     */
    public boolean allowCreateDir()
    {
        return allowCreate() && getBooleanOption("createDir", true);
    }

    /**
     *
     * @return
     */
    public boolean allowReplace()
    {
        return getBooleanOption("replace", true);
    }

    /**
     *
     * @return
     */
    public boolean allowReplaceDir()
    {
        return allowReplace() && getBooleanOption("replaceDir", true);
    }

    /**
     *
     * @return
     */
    public boolean allowDelete()
    {
        return getBooleanOption("delete", true);
    }

    /**
     *
     * @return
     */
    public boolean allowDeleteDir()
    {
        return allowDelete() && getBooleanOption("deleteDir", true);
    }

    /**
     *
     * @return prefer files with newer modified time
     */
    public boolean preferNewer()
    {
        return getBooleanOption("preferNewer", true);
    }

    /**
     *
     * @return prefer file creation over deletion
     */
    public boolean preferExisting()
    {
        return getBooleanOption("preferExisting", true);
    }

    /**
     *
     * @return relative paths to sync (other paths are ignored)
     */
    public String[] getIncludedPaths()
    {
        List array = getMergedArrayOption("include");
        String[] result = new String[array.size()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = JsonUtils.castToString(array.get(i));
        }
        return result;
    }

    /**
     *
     * @return relative paths to ignore
     */
    public String[] getIgnoredPaths()
    {
        List array = getMergedArrayOption("ignore");
        String[] result = new String[array.size()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = JsonUtils.castToString(array.get(i));
        }
        return result;
    }
}
