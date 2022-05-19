/*
 * Copyright (c) 2022 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.list;

import java.nio.file.Path;
import java.util.Collection;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONAware;

/**
 * Abstraction for file lists.
 *
 * @author Rick
 */
public interface FileList
{
    /**
     * Add history for the file (replace if exists).
     *
     * @param path
     * @param entry
     * @param target
     */
    public void set(String path, FileHistory entry, FileListTarget target);

    /**
     * Add history for the file (replace if exists).
     *
     * @param path
     * @param entry
     * @param target
     */
    public default void set(Path path, FileHistory entry, FileListTarget target)
    {
        set(pathToString(path), entry, target);
    }

    /**
     * Add history for the file (replace if exists).
     *
     * @param path
     * @param entry
     */
    public default void set(Path path, FileHistory entry)
    {
        set(path, entry, null);
    }

    /**
     * Add history for the file (replace if exists).
     *
     * @param path
     * @param entry
     */
    public default void set(String path, FileHistory entry)
    {
        set(path, entry, null);
    }

    /**
     * Add history for the file (no not replace).
     *
     * @param path
     * @param entry
     * @param target
     */
    public void add(String path, FileHistory entry, FileListTarget target);

    /**
     * Add history for the file (no not replace).
     *
     * @param path
     * @param entry
     * @param target
     */
    public default void add(Path path, FileHistory entry, FileListTarget target)
    {
        add(pathToString(path), entry, target);
    }

    /**
     * Add history for the file (no not replace).
     *
     * @param path
     * @param entry
     */
    public default void add(Path path, FileHistory entry)
    {
        add(path, entry, null);
    }

    /**
     * Add history for the file (no not replace).
     *
     * @param path
     * @param entry
     */
    public default void add(String path, FileHistory entry)
    {
        add(path, entry, null);
    }

    /**
     * Get history for the file or empty history, if not in list.
     *
     * @param path
     * @param target
     * @return
     */
    public FileHistory get(String path, FileListTarget target);

    /**
     * Get history for the file or empty history, if not in list.
     *
     * @param path
     * @param target
     * @return
     */
    public default FileHistory get(Path path, FileListTarget target)
    {
        return get(pathToString(path), target);
    }

    /**
     * Get history for the file or empty history, if not in list.
     *
     * @param path
     * @return
     */
    public default FileHistory get(Path path)
    {
        return get(path, null);
    }

    /**
     * Get history for the file or empty history, if not in list.
     *
     * @param path
     * @return
     */
    public default FileHistory get(String path)
    {
        return get(path, null);
    }

    /**
     *
     * @param target
     * @return list generation time for target
     */
    public long getTime(FileListTarget target);

    /**
     *
     * @return list generation time
     */
    public default long getTime()
    {
        return getTime(null);
    }

    /**
     *
     * @param path
     * @return string representaion of relative path
     */
    public default String pathToString(Path path)
    {
        return FilenameUtils.separatorsToUnix(path.toString());
    }

    /**
     *
     * @return all paths in file list
     */
    public Collection<String> getPaths();

    /**
     * Generate JSON.
     *
     * @param maxVersions
     * @param withHash
     * @return json
     */
    public JSONAware toJson(int maxVersions, boolean withHash);

    /**
     * Generate compact JSON.
     *
     * @return json
     */
    public JSONAware toCompactJson();

}
