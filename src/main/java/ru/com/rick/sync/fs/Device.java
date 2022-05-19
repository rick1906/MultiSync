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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

/**
 * Abstraction for a file system or other file-system-like location. Used to manage files.
 *
 * Currentry only normal file system is supported.
 *
 * @author Rick
 */
public abstract class Device
{

    /**
     * Create Path object.
     *
     * @param path path string
     * @return path as a Path object
     */
    public abstract Path path(String path);

    /**
     *
     * @return unique device id
     */
    public abstract String getId();

    /**
     *
     * @param path path
     * @return true if corresponding drive or another device is available
     */
    public abstract boolean isAvailable(Path path);

    /**
     *
     * @param path path
     * @return file exists
     */
    public abstract boolean exists(Path path);

    /**
     *
     * @param path path
     * @return file is dir
     */
    public abstract boolean isDir(Path path);

    /**
     *
     * @param path path
     * @return file is symlink
     */
    public abstract boolean isSymLink(Path path);

    /**
     *
     * @param path path
     * @return path is absolute
     */
    public abstract boolean isAbsolutePath(Path path);

    /**
     *
     * @param path path
     * @return absolute path
     */
    public abstract Path getAbsolutePath(Path path);

    /**
     *
     * @param path path
     * @return last modified unix time in ms
     */
    public abstract long getModifiedTime(Path path);

    /**
     *
     * @param path path
     * @return file size in bytes
     */
    public abstract long getFileSize(Path path);

    /**
     *
     * @param path path
     * @return file attributes
     */
    public abstract BasicFileAttributes readFileAttrubutes(Path path);

    /**
     * Set last modified time of a file.
     *
     * @param path path
     * @param time unix time in ms
     * @return success or not
     */
    public abstract boolean setModifiedTime(Path path, long time);

    /**
     * Set last modified time and other attrs of a file or directory.
     *
     * @param path path
     * @param attrs attributes
     * @return success or not
     */
    public abstract boolean setFileAttributes(Path path, BasicFileAttributes attrs);

    /**
     *
     * @param dir directory
     * @return stream of files
     * @throws IOException
     */
    public abstract DirectoryStream<Path> openDir(Path dir) throws IOException;

    /**
     * Open file for reading.
     *
     * @param path path
     * @return input stream
     * @throws IOException
     */
    public abstract InputStream getInputStream(Path path) throws IOException;

    /**
     * Open file for writing. Create or replace.
     *
     * @param path path
     * @return output stream
     * @throws IOException
     */
    public abstract OutputStream getOutputStream(Path path) throws IOException;

    /**
     * Create directory.
     *
     * @param path path
     * @throws IOException
     */
    public abstract void createDir(Path path) throws IOException;

    /**
     * Create directory & all parent directories if some do not exist.
     *
     * @param path path
     * @throws IOException
     */
    public abstract void createDirs(Path path) throws IOException;

    /**
     * Delete file or empty directory is exists.
     *
     * @param path path
     * @return true if file existed and was deleted
     * @throws IOException
     */
    public abstract boolean deleteFile(Path path) throws IOException;

    /**
     * Move file within device.
     *
     * @param source source path
     * @param target target path
     * @param replace replace or not
     * @throws IOException
     */
    public abstract void moveFile(Path source, Path target, boolean replace) throws IOException;

    /**
     * Copy file within device.
     *
     * @param source source path
     * @param target target path
     * @param replace replace or not
     * @throws IOException
     */
    public abstract void copyFile(Path source, Path target, boolean replace) throws IOException;

    /**
     * Copy file to different device.
     *
     * @param source source path
     * @param targetDevice target device
     * @param targetPath target path
     * @param replace replace or not
     * @throws IOException
     */
    public void copyFile(Path source, Device targetDevice, Path targetPath, boolean replace) throws IOException
    {
        if (isSameFileSystem(targetDevice)) {
            copyFile(source, targetPath, replace);
        } else {
            if (!replace && targetDevice.exists(targetPath)) {
                throw new FileAlreadyExistsException(targetPath.toString());
            } else {
                try (InputStream inp = getInputStream(source)) {
                    try (OutputStream out = targetDevice.getOutputStream(targetPath)) {
                        IOUtils.copy(inp, out);
                    }
                }
                copyFileAttributes(source, targetDevice, targetPath);
            }
        }
    }

    /**
     *
     * @param dir directory
     * @return list of files
     * @throws IOException
     */
    public List<Path> listFiles(Path dir) throws IOException
    {
        try (DirectoryStream<Path> stream = openDir(dir)) {
            ArrayList<Path> result = new ArrayList<>();
            for (Path path : stream) {
                result.add(path);
            }
            return result;
        }
    }

    /**
     * Copy file attributes to another file.
     *
     * @param source source path
     * @param targetDevice target device
     * @param targetPath target path
     */
    public void copyFileAttributes(Path source, Device targetDevice, Path targetPath)
    {
        BasicFileAttributes attrs = readFileAttrubutes(source);
        if (attrs != null) {
            targetDevice.setFileAttributes(targetPath, attrs);
        }
    }

    /**
     *
     * @param device other device
     * @return true if this device represents the same file system as the other
     */
    public boolean isSameFileSystem(Device device)
    {
        return equals(device);
    }

    /**
     *
     * @param path path or null
     * @return string representaion of device and a path
     */
    public String getPathStringId(Path path)
    {
        String id = getId();
        String ps = FilenameUtils.separatorsToUnix(String.valueOf(path));
        return id.length() > 0 ? (id + ": " + ps) : ps;
    }
}
