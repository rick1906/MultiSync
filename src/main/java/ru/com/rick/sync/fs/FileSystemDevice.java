/**
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import ru.com.rick.sync.json.JsonUtils;

/**
 * Device representing a normal file system.
 *
 * @author Rick
 */
public class FileSystemDevice extends Device
{

    /**
     * Default path (current path)
     */
    private String defaultPath = null;

    /**
     * The "root" path of the device
     */
    private String mountPath = null;

    /**
     * True to check if mountPath is mounted in isAvailable method
     */
    private boolean checkMounted = true;

    public FileSystemDevice()
    {
    }

    public FileSystemDevice(String defaultPath)
    {
        this.defaultPath = defaultPath;
    }

    public FileSystemDevice(Map json)
    {
        if (json.containsKey("defaultPath")) {
            defaultPath = JsonUtils.getString(json, "defaultPath");
        }
        if (json.containsKey("mount")) {
            mountPath = JsonUtils.getString(json, "mount");
            checkMounted = JsonUtils.getBoolean(json, "check", true);
        }

    }

    @Override
    public boolean isSameFileSystem(Device device)
    {
        return device != null && getId().equals(device.getId()) && getClass().equals(device.getClass());
    }

    @Override
    public Path path(String path)
    {
        return Paths.get(path);
    }

    @Override
    public String getId()
    {
        return "";
    }

    @Override
    public boolean isAvailable(Path path)
    {
        try {
            Path root = path.getRoot();
            if (root == null || !Files.exists(root)) {
                return false;
            }
            if (mountPath != null) {
                Path mount = getAbsolutePath(path(mountPath));
                if (!Files.exists(mount)) {
                    return false;
                }
                if (checkMounted) {
                    if (!Files.isDirectory(mount)) {
                        return false;
                    }
                    try (DirectoryStream<Path> s = openDir(mount)) {
                        if (!s.iterator().hasNext()) {
                            return false;
                        }
                    } catch (IOException ex) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    @Override
    public boolean isAbsolutePath(Path path)
    {
        return path.isAbsolute();
    }

    @Override
    public boolean exists(Path path)
    {
        return Files.exists(path);
    }

    @Override
    public boolean isDir(Path path)
    {
        return Files.isDirectory(path);
    }

    @Override
    public boolean isSymLink(Path path)
    {
        return Files.isSymbolicLink(path);
    }

    @Override
    public Path getAbsolutePath(Path path)
    {
        if (path.isAbsolute()) {
            return path;
        } else if (defaultPath != null) {
            return Paths.get(defaultPath, path.toString());
        } else {
            return path.toAbsolutePath();
        }
    }

    @Override
    public long getModifiedTime(Path path)
    {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            return FileVersion.TIME_UNKNOWN;
        }
    }

    @Override
    public long getFileSize(Path path)
    {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            return -1;
        }
    }

    @Override
    public BasicFileAttributes readFileAttrubutes(Path path)
    {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class);
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public boolean setModifiedTime(Path path, long time)
    {
        try {
            Files.setLastModifiedTime(path, FileTime.fromMillis(time));
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public boolean setFileAttributes(Path path, BasicFileAttributes attrs)
    {
        boolean success = true;
        if (!attrs.isDirectory()) {
            success = setModifiedTime(path, attrs.lastModifiedTime().toMillis()) && success;
        }
        return success;
    }

    @Override
    public DirectoryStream<Path> openDir(Path dir) throws IOException
    {
        return Files.newDirectoryStream(dir);
    }

    @Override
    public InputStream getInputStream(Path path) throws IOException
    {
        return Files.newInputStream(path);
    }

    @Override
    public OutputStream getOutputStream(Path path) throws IOException
    {
        return Files.newOutputStream(path);
    }

    @Override
    public void createDir(Path path) throws IOException
    {
        Files.createDirectory(path);
    }

    @Override
    public void createDirs(Path path) throws IOException
    {
        Files.createDirectories(path);
    }

    @Override
    public boolean deleteFile(Path path) throws IOException
    {
        return Files.deleteIfExists(path);
    }

    @Override
    public void moveFile(Path source, Path target, boolean replace) throws IOException
    {
        if (replace) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.move(source, target);
        }
    }

    @Override
    public void copyFile(Path source, Path target, boolean replace) throws IOException
    {
        if (replace) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        } else {
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

}
