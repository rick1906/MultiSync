/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync;

import com.cedarsoftware.util.io.JsonWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONAware;
import org.json.simple.parser.ParseException;
import ru.com.rick.sync.fs.FileProperties;
import ru.com.rick.sync.fs.FileVersion;
import ru.com.rick.sync.fs.PathHolder;
import ru.com.rick.sync.fs.Root;
import ru.com.rick.sync.fs.Segment;
import ru.com.rick.sync.json.JsonObject;
import ru.com.rick.sync.json.JsonParser;
import ru.com.rick.sync.json.JsonUtils;
import ru.com.rick.sync.list.FileHistory;
import ru.com.rick.sync.list.FileList;
import ru.com.rick.sync.list.FileListTarget;
import ru.com.rick.sync.list.GlobalFileList;
import ru.com.rick.sync.list.LocalFileList;

/**
 * Class for managing file lists.
 *
 * @author Rick
 */
public class FileListContainer implements PathHolder
{
    /**
     * Root for list file
     */
    private final Root root;

    /**
     * List file relative path
     */
    private final Path relativePath;

    /**
     * List file absolute path
     */
    private final Path absolutePath;

    /**
     * List type
     */
    private boolean global = false;

    /**
     * List loaded
     */
    private boolean ready = false;

    /**
     * List loaded/saved successfully
     */
    private boolean success = false;

    /**
     * List saved
     */
    private boolean updated = false;

    /**
     * List
     */
    private FileList list = null;

    /**
     * Last error
     */
    private Exception error = null;

    /**
     * Max versions per file
     */
    private int maxVersions = 0;

    /**
     * Use compact list with no extra info
     */
    private boolean compact = false;

    /**
     * Ignore file list errors
     */
    private boolean ignoreErrors = false;

    /**
     * Create parent dirs for list file
     */
    private boolean createDirs = true;

    /**
     * Require hash calculation
     */
    private boolean requireHash = false;

    /**
     * Pretty print
     */
    private boolean prettyPrint = true;

    /**
     *
     * @param root root
     * @param path file list path
     * @param global if list is global
     */
    public FileListContainer(Root root, Path path, boolean global)
    {
        this.root = root;
        this.global = global;
        if (path == null) {
            absolutePath = null;
            relativePath = null;
        } else if (root.getDevice().isAbsolutePath(path)) {
            absolutePath = path.normalize();
            relativePath = root.getRelativePath(absolutePath);
        } else {
            relativePath = path.normalize();
            absolutePath = root.getAbsolutePath(relativePath);
        }
    }

    /**
     *
     * @param root
     * @param path
     * @param global
     */
    public FileListContainer(Root root, String path, boolean global)
    {
        this(root, path != null ? root.getDevice().path(path) : null, global);
    }

    /**
     *
     * @param root
     * @param json
     * @param global
     */
    public FileListContainer(Root root, Map json, boolean global)
    {
        this(root, JsonUtils.getString(json, "path"), global);
        this.maxVersions = JsonUtils.getInteger(json, "maxVersions", maxVersions);
        this.compact = JsonUtils.getBoolean(json, "compact", compact);
        this.requireHash = JsonUtils.getBoolean(json, "requireHash", requireHash);
        this.ignoreErrors = JsonUtils.getBoolean(json, "ignoreErrors", ignoreErrors);
        this.createDirs = JsonUtils.getBoolean(json, "createDirs", createDirs);
        this.prettyPrint = JsonUtils.getBoolean(json, "prettyPrint", prettyPrint);
        //TODO: globalCopy - copy list to global list
    }

    /**
     *
     * @return if this class is configured
     */
    public boolean isConfigured()
    {
        return absolutePath != null;
    }

    @Override
    public Segment getSegment()
    {
        return root;
    }

    @Override
    public Path getAbsolutePath()
    {
        return absolutePath;
    }

    @Override
    public Path getRelativePath()
    {
        return relativePath;
    }

    /**
     *
     * @param file
     * @return success
     */
    private boolean writeFileList(Path file)
    {
        error = null;
        JSONAware data = generateJson();
        if (createDirs) {
            Path parent = file.getParent();
            if (parent != null) {
                try {
                    getDevice().createDirs(parent);
                } catch (IOException ex) {
                    error = ex;
                    return false;
                }
            }
        }
        try (OutputStream out = getDevice().getOutputStream(file)) {
            try (OutputStreamWriter sw = new OutputStreamWriter(out)) {
                String jsonString;
                if (prettyPrint) {
                    JsonObject params = new JsonObject();
                    params.put(JsonWriter.PRETTY_PRINT, true);
                    params.put(JsonWriter.TYPE, false);
                    jsonString = JsonWriter.objectToJson(data, params);
                } else {
                    jsonString = data.toJSONString();
                }
                sw.write(jsonString);
            }
        } catch (IOException ex) {
            error = ex;
            return false;
        }
        if (compact && list != null && list.getTime() != FileVersion.TIME_UNKNOWN) {
            getDevice().setModifiedTime(file, list.getTime());
        }
        return true;
    }

    /**
     *
     * @param file
     * @return success
     */
    private boolean readFileList(Path file)
    {
        list = null;
        error = null;
        Object json;
        if (!getDevice().exists(file)) {
            return false;
        }
        try (InputStream is = getDevice().getInputStream(file)) {
            try (InputStreamReader reader = new InputStreamReader(is)) {
                JsonParser parser = new JsonParser();
                json = parser.parse(reader);
            }
        } catch (IOException | ParseException ex) {
            error = ex;
            return false;
        }
        try {
            return processJson(json, file);
        } catch (Exception ex) {
            error = ex;
            return false;
        }
    }

    /**
     *
     * @return JSON array or object
     */
    private JSONAware generateJson()
    {
        if (list == null) {
            return new JsonObject();
        }
        if (compact) {
            return list.toCompactJson();
        } else {
            return list.toJson(maxVersions, requireHash);
        }
    }

    /**
     *
     * @param json
     * @param file
     * @return success
     */
    private boolean processJson(Object json, Path file)
    {
        if (json == null) {
            return false;
        }
        long time = getDevice().getModifiedTime(file);
        if (global) {
            list = new GlobalFileList((Map)json, time);
            return true;
        } else if (json instanceof List) {
            list = new LocalFileList((List)json, time);
            return true;
        } else {
            list = new LocalFileList((Map)json, time);
            return true;
        }
    }

    /**
     * Read list if not yet.
     */
    private void initialize()
    {
        ready = true;
        success = absolutePath == null || !exists() || readFileList(absolutePath);
    }

    /**
     * Reset list.
     */
    public void reset()
    {
        list = null;
        error = null;
        ready = false;
        success = false;
        updated = false;
    }

    /**
     * Set new list after sync.
     *
     * @param list
     * @return success
     */
    public boolean update(FileList list)
    {
        this.list = list;
        error = null;
        ready = true;
        success = absolutePath != null && writeFileList(absolutePath);
        updated = true;
        return success;
    }

    /**
     *
     * @return list errors can be ignored
     */
    public boolean ignoreErrors()
    {
        return ignoreErrors;
    }

    /**
     *
     * @return list root is available
     */
    public boolean isAvailable()
    {
        return root.isAvailable();
    }

    /**
     *
     * @return if list file exists
     */
    public boolean exists()
    {
        return absolutePath != null && root.getDevice().exists(absolutePath);
    }

    /**
     *
     * @return if list is global
     */
    public boolean isGlobal()
    {
        return global;
    }

    /**
     *
     * @return file list
     */
    public FileList getList()
    {
        if (!ready) {
            initialize();
        }
        return list;
    }

    /**
     *
     * @return error on reading/writing
     */
    public Exception getError()
    {
        if (!ready) {
            initialize();
        }
        return error;
    }

    /**
     *
     * @return list is ok
     */
    public boolean isValid()
    {
        if (!ready) {
            initialize();
        }
        return success;
    }

    /**
     *
     * @return list is updated
     */
    public boolean isUpdated()
    {
        return updated;
    }

    /**
     *
     * @param path
     * @param target
     * @return list entry or null if no list
     */
    public FileHistory getEntry(Path path, FileListTarget target)
    {
        if (!ready) {
            initialize();
        }
        if (!success && !ignoreErrors) {
            throw new IllegalStateException("File list '" + this + "' was not successfully loaded");
        }
        return list != null ? list.get(path, target) : null;
    }

    /**
     *
     * @param target
     * @return list updated time
     */
    public long getUpdatedTime(FileListTarget target)
    {
        if (!ready) {
            initialize();
        }
        return list != null ? list.getTime(target) : FileProperties.TIME_UNKNOWN;
    }

    @Override
    public String toString()
    {
        return getPathStringId();
    }

}
