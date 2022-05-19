/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.fs;

import java.nio.file.Path;

/**
 * Abstraction for storing options for a certain directory on a device.
 *
 * @author Rick
 */
public interface Segment
{
    /**
     *
     * @return the device
     */
    public Device getDevice();

    /**
     *
     * @return the root path from which to resolve relative paths
     */
    public Path getRootPath();

    /**
     *
     * @return path options
     */
    public PathOptions getOptions();

    /**
     *
     * @param rpath relative path
     * @return absolute path
     */
    public Path getAbsolutePath(Path rpath);

    /**
     *
     * @param path absolute path
     * @return relative path
     */
    public Path getRelativePath(Path path);

    /**
     *
     * @return segment can supply a file modified time
     */
    public boolean canUseModifiedTime();
    
    /**
     *
     * @return segment can supply a file created time
     */
    public boolean canUseCreatedTime();

    /**
     *
     * @return modified time is valid for file comparison
     */
    public boolean canCompareModifiedTime();

    /**
     *
     * @return allow calculate file hash
     */
    public boolean canUseHash();

    /**
     *
     * @return follow symlinks to directories
     */
    public boolean followSymLinks();

    /**
     *
     * @return write to symlink directories
     */
    public boolean writeToSymLinks();
}
