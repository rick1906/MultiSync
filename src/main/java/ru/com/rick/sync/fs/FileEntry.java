/**
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.fs;

import java.nio.file.Path;

/**
 * Reference to a file with a relative path.
 *
 * @author Rick
 */
public class FileEntry extends FileReference implements PathHolder
{
    private final Path rpath;
    private FileProperties props = null;

    public FileEntry(Segment segment, Path rpath)
    {
        super(segment);
        this.rpath = rpath;
    }

    public FileEntry(Segment segment, Path rpath, boolean exists)
    {
        this(segment, rpath);
        this.props = new FileProperties(getDevice(), getAbsolutePath(), exists);
    }

    @Override
    public final Path getRelativePath()
    {
        return rpath;
    }

    @Override
    public final Path getAbsolutePath()
    {
        return getSegment().getAbsolutePath(rpath);
    }

    @Override
    public FileProperties getProperties()
    {
        if (props == null) {
            props = new FileProperties(getDevice(), getAbsolutePath());
        }
        return props;
    }

    @Override
    public void refresh()
    {
        if (props != null) {
            props.refresh();
        }
    }

    @Override
    public String toString()
    {
        return getPathStringId();
    }

}
