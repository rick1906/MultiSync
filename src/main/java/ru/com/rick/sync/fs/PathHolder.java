/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.fs;

import java.nio.file.Path;

/**
 * Interface for objects with a path on a device's segment.
 *
 * @author Rick
 */
public interface PathHolder
{

    public Segment getSegment();

    /**
     *
     * @return object path relative to segment's root
     */
    public Path getRelativePath();

    /**
     *
     * @return object absolute path
     */
    public Path getAbsolutePath();

    /**
     *
     * @return device
     */
    public default Device getDevice()
    {
        return getSegment().getDevice();
    }

    /**
     *
     * @param root
     * @return object path relative to another root or null
     */
    public default Path getRelativePath(Segment root)
    {
        Path path = getAbsolutePath();
        if (path != null && getDevice().isSameFileSystem(root.getDevice())) {
            return root.getRelativePath(path);
        }
        return null;
    }

    /**
     *
     * @return global string id of the absolute path and the device
     */
    public default String getPathStringId()
    {
        return getDevice().getPathStringId(getAbsolutePath());
    }

    /**
     *
     * @param root global root
     * @return global string id of the relative path to root and the device
     */
    public default String getPathStringId(Segment root)
    {
        if (root != null) {
            Path path = getRelativePath(root);
            if (path != null) {
                return getDevice().getPathStringId(path);
            }
        }
        return getDevice().getPathStringId(getAbsolutePath());
    }
}
