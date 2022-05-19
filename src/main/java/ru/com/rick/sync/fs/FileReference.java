/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.fs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.apache.commons.io.IOUtils;

/**
 * Abstract reference to a file on device segment. Used to compare files and perform some operations, like copying or
 * deleting.
 *
 * @author Rick
 */
public abstract class FileReference
{
    public static final int COMPARE_LEVEL_SIZE = 0;
    public static final int COMPARE_LEVEL_HASH = 1;
    public static final int COMPARE_LEVEL_MIXED = 2;
    public static final int COMPARE_LEVEL_CONTENT = 3;

    /**
     * Device segment.
     */
    private final Segment segment;

    public FileReference(Segment segment)
    {
        this.segment = segment;
    }

    /**
     *
     * @return absolute path to a file
     */
    public abstract Path getAbsolutePath();

    /**
     *
     * @return device segment
     */
    public final Segment getSegment()
    {
        return segment;
    }

    /**
     *
     * @return device
     */
    public final Device getDevice()
    {
        return segment.getDevice();
    }

    /**
     *
     * @return file properties
     */
    public abstract FileProperties getProperties();

    /**
     *
     * @return if file is symlink
     */
    public boolean isSymLink()
    {
        return getProperties().isSymLink();
    }

    /**
     *
     * @return if file is dir
     */
    public boolean isDir()
    {
        return getProperties().isDir();
    }

    /**
     *
     * @return if file exists
     */
    public boolean exists()
    {
        return getProperties().exists();
    }

    /**
     * Refresh properties, etc.
     */
    public abstract void refresh();

    /**
     *
     * @return file input stream
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException
    {
        return getDevice().getInputStream(getAbsolutePath());
    }

    /**
     *
     * @param lim1
     * @param lim2
     * @return
     */
    private long getMinSizeLimit(long lim1, long lim2)
    {
        if (lim1 > 0 && lim2 > 0) {
            return Math.min(lim1, lim2);
        } else if (lim1 > 0) {
            return lim1;
        } else if (lim2 > 0) {
            return lim2;
        } else {
            return 0;
        }
    }

    /**
     *
     * @param other
     * @return if files are equal
     * @throws IOException
     */
    public boolean isEqualTo(FileReference other) throws IOException
    {
        PathOptions thisOpt = segment.getOptions();
        PathOptions otherOpt = other.segment.getOptions();
        boolean checkTime = segment.canCompareModifiedTime() && other.segment.canCompareModifiedTime();
        boolean checkHash = segment.canUseHash() && other.segment.canUseHash();
        int compareLevel = Math.min(thisOpt.getCompareLevel(), otherOpt.getCompareLevel());
        long sizeLimit = getMinSizeLimit(thisOpt.getCompareSizeLimit(), otherOpt.getCompareSizeLimit());
        return isEqualTo(other, checkTime, checkHash, compareLevel, sizeLimit);
    }

    /**
     *
     * @param other
     * @param checkTime trust modified time
     * @param checkHash check hash
     * @param compareLevel compare mode (see class constants)
     * @param sizeLimit max size to compare file contents
     * @return if files are equal
     * @throws IOException
     */
    public boolean isEqualTo(FileReference other, boolean checkTime, boolean checkHash, int compareLevel, long sizeLimit) throws IOException
    {
        FileVersion thisProp = getProperties();
        FileVersion otherProp = other.getProperties();
        if (compareLevel <= COMPARE_LEVEL_SIZE) {
            return thisProp.isEqualTo(otherProp, checkTime, false);
        }
        if (compareLevel <= COMPARE_LEVEL_HASH) {
            return thisProp.isEqualTo(otherProp, checkTime, checkHash);
        }
        if (!thisProp.isEqualTo(otherProp, checkTime, false)) {
            return false;
        }
        if (compareLevel <= COMPARE_LEVEL_MIXED && checkHash && thisProp.hasHash() && otherProp.hasHash()) {
            return thisProp.getHash().equals(otherProp.getHash());
        }

        long size = Math.max(thisProp.getFileSize(), otherProp.getFileSize()); // in case one file size is unknown (-1)
        if (sizeLimit > 0 && size > sizeLimit) {
            return thisProp.getHash().equals(otherProp.getHash());
        }

        try (InputStream s1 = getInputStream(); InputStream s2 = other.getInputStream()) {
            return IOUtils.contentEquals(s1, s2);
        }
    }

    /**
     * Delete a file or empty dir.
     *
     * @throws IOException
     */
    public void deleteFile() throws IOException
    {
        getDevice().deleteFile(getAbsolutePath());
        refresh();
    }

    /**
     * Copy a file.
     *
     * @param dest
     * @param replace
     * @throws IOException
     */
    public void copyFile(FileReference dest, boolean replace) throws IOException
    {
        getDevice().copyFile(getAbsolutePath(), dest.getDevice(), dest.getAbsolutePath(), replace);
        refresh();
    }
}
