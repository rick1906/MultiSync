/**
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.fs;

import java.nio.file.Path;
import ru.com.rick.sync.json.JsonOptions;

/**
 * Segment of a device. Root for sync target.
 *
 * @author Rick
 */
public class Root implements Segment
{
    private final Device device;
    private final Path path;
    private final PathOptions options;

    public Root(Device device, Path path, PathOptions options)
    {
        this.device = device;
        this.options = options;
        if (path != null) {
            this.path = device.getAbsolutePath(path.normalize());
        } else {
            throw new IllegalArgumentException("Root path should not be null");
        }
    }

    public Root(Device device, Path path, JsonOptions options)
    {
        this(device, path, new PathOptions(options));
    }

    @Override
    public final Device getDevice()
    {
        return device;
    }

    @Override
    public final Path getRootPath()
    {
        return path;
    }

    @Override
    public final PathOptions getOptions()
    {
        return options;
    }

    @Override
    public Path getAbsolutePath(Path rpath)
    {
        return rpath != null ? path.resolve(rpath) : path;
    }

    @Override
    public Path getRelativePath(Path path)
    {
        Path p = device.getAbsolutePath(path.normalize());
        if (p.startsWith(this.path)) {
            return this.path.relativize(p);
        }
        return null;
    }

    /**
     *
     * @param path
     * @return true if path is a subpath of this root
     */
    public boolean containsPath(Path path)
    {
        return device.getAbsolutePath(path.normalize()).startsWith(this.path);
    }

    /**
     *
     * @return true if this root is mounted and available on the device
     */
    public boolean isAvailable()
    {
        return device.isAvailable(path);
    }

    @Override
    public boolean canUseModifiedTime()
    {
        return options.useModifiedTime();
    }

    @Override
    public boolean canUseCreatedTime()
    {
        return options.useCreatedTime();
    }

    @Override
    public boolean canCompareModifiedTime()
    {
        return options.useModifiedTime() && options.compareModifiedTime();
    }

    @Override
    public boolean canUseHash()
    {
        return options.useHash();
    }

    @Override
    public boolean followSymLinks()
    {
        return options.followSymLinks();
    }

    @Override
    public boolean writeToSymLinks()
    {
        return options.writeToSymLinks();
    }

    @Override
    public int hashCode()
    {
        return path.hashCode() ^ device.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Root other = (Root)obj;
        return path.equals(other.path) && device.equals(other.device);
    }

    @Override
    public String toString()
    {
        return device.getPathStringId(path);
    }

}
