/*
 * Copyright (c) 2022 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.io.IOUtils;

/**
 *
 * Properties linked to a file. Lazy-loaded.
 *
 * @author Rick
 */
public class FileProperties extends FileVersion
{
    /**
     * Full path to file
     */
    private Path path;

    /**
     * Device for file
     */
    private Device device;

    /**
     * File attrs
     */
    private BasicFileAttributes attrs = null;
    private boolean checkedAttrs = false;

    // file props check statuses
    protected boolean checkedExists = false;
    protected boolean checkedIsDir = false;
    protected boolean checkedMtime = false;
    protected boolean checkedFsize = false;
    protected boolean checkedMd5 = false;

    public FileProperties(Device device, Path path)
    {
        this.device = device;
        this.path = path;
    }

    public FileProperties(Device device, Path path, boolean exists)
    {
        this(device, path);
        this.checkedExists = true;
        this.exists = exists;
    }

    @Override
    public boolean exists()
    {
        if (!checkedExists) {
            exists = device.exists(path);
            checkedExists = true;
        }
        return exists;
    }

    @Override
    public boolean isDir()
    {
        if (!checkedIsDir) {
            isDir = device.isDir(path);
            checkedIsDir = true;
        }
        return isDir;
    }

    @Override
    public long getModifiedTime()
    {
        if (!checkedMtime) {
            mtime = device.getModifiedTime(path);
            checkedMtime = true;
        }
        return mtime;
    }

    @Override
    public long getFileSize()
    {
        if (!checkedFsize) {
            fsize = device.getFileSize(path);
            checkedFsize = true;
        }
        return fsize;
    }

    @Override
    public String getHash()
    {
        if (!checkedMd5) {
            checkedMd5 = true;
            try (InputStream is = device.getInputStream(path)) {
                md5 = md5hash(is);
            } catch (IOException ex) {
                md5 = null;
            }
        }
        return md5;
    }

    /**
     *
     * @return file attributes
     */
    public BasicFileAttributes getAttributes()
    {
        if (!checkedAttrs) {
            checkedAttrs = true;
            attrs = device.readFileAttrubutes(path);
        }
        return attrs;
    }

    /**
     *
     * @return file created time
     */
    public long getCreatedTime()
    {
        BasicFileAttributes a = getAttributes();
        return a != null ? a.creationTime().toMillis() : TIME_UNKNOWN;
    }

    /**
     *
     * @return file is symlink
     */
    public boolean isSymLink()
    {
        BasicFileAttributes a = getAttributes();
        return a != null ? a.isSymbolicLink() : false;
    }

    /**
     * Refresh if possible.
     *
     * @return true if refreshed
     */
    public boolean refresh()
    {
        checkedExists = false;
        checkedIsDir = false;
        checkedMtime = false;
        checkedFsize = false;
        checkedMd5 = false;
        return true;
    }

    /**
     * Validate values or throw an exception.
     *
     * @throws BadDataException
     */
    public void validateOrThrow() throws BadDataException
    {
        if (isDir()) {
            return;
        }
        if (checkedMtime && mtime == TIME_UNKNOWN) {
            throw new BadDataException("Unable to get modified time of '" + path + "'");
        }
        if (checkedFsize && fsize < 0) {
            throw new BadDataException("Unable to get file size of '" + path + "'");
        }
        if (checkedMd5 && md5 == null) {
            throw new BadDataException("Unable to get hash of '" + path + "'");
        }
    }

    @Override
    public boolean isValidVersion()
    {
        return exists();
    }

    @Override
    protected long getSyncTimeCompareValue()
    {
        return time == TIME_UNKNOWN ? Long.MAX_VALUE : time;
    }

    private static String md5hash(InputStream is)
    {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(is, md);
            IOUtils.copy(dis, new OutputStream()
            {
                @Override
                public void write(int b) throws IOException
                {

                }
            });
            return String.format("%032x", new BigInteger(1, md.digest()));
        } catch (IOException | NoSuchAlgorithmException ex) {
            return null;
        }
    }

}
