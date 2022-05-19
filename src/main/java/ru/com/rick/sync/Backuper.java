/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync;

import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import ru.com.rick.sync.fs.Device;
import ru.com.rick.sync.fs.FileEntry;
import ru.com.rick.sync.fs.FileProperties;
import ru.com.rick.sync.fs.FileVersion;
import ru.com.rick.sync.fs.FileReference;
import ru.com.rick.sync.fs.PathHolder;
import ru.com.rick.sync.fs.Segment;
import ru.com.rick.sync.json.JsonUtils;

/**
 * Class to manage file operations and backup files.
 *
 * @author Rick
 */
public class Backuper extends FileManager implements PathHolder
{
    /**
     * Root segment for backups
     */
    protected final Segment segment;

    /**
     * A device
     */
    protected final Device device;

    /**
     * Backup path relative to a root
     */
    protected final Path relativePath;

    /**
     * Absolute backup dir path
     */
    protected final Path absolutePath;

    /**
     * Format for backup by-date subdirs
     */
    protected String dateFolderFormat = "yyyyMMdd";
    /**
     * Move (not copy) files to backup if possible
     */
    protected boolean moveIfPossible = true;
    /**
     * Max versions of one file to store in backup
     */
    protected int maxVersions = 50;
    //
    private boolean initialized = false;
    private Path currentBackupPath = null;
    private long time = FileVersion.TIME_UNKNOWN;

    public Backuper(Segment segment, Path path)
    {
        this.segment = segment;
        this.device = segment.getDevice();
        if (path == null) {
            absolutePath = null;
            relativePath = null;
        } else if (segment.getDevice().isAbsolutePath(path)) {
            absolutePath = path.normalize();
            relativePath = segment.getRelativePath(absolutePath);
        } else {
            relativePath = path.normalize();
            absolutePath = segment.getAbsolutePath(relativePath);
        }
    }

    public Backuper(Segment segment, String backupPath)
    {
        this(segment, backupPath != null ? segment.getDevice().path(backupPath) : null);
    }

    public Backuper(Segment segment, Map json)
    {
        this(segment, JsonUtils.getString(json, "path"));
        this.maxVersions = JsonUtils.getInteger(json, "maxVersions", maxVersions);
        this.moveIfPossible = JsonUtils.getBoolean(json, "move", moveIfPossible);
        this.dateFolderFormat = JsonUtils.getString(json, "dateFolderFormat", dateFolderFormat);
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
        return segment;
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
     * Initialize current backup path. On failure set it to null.
     *
     * @return success
     */
    private boolean initializePath()
    {
        currentBackupPath = null;
        if (absolutePath == null) {
            return false;
        }
        if (!device.isAvailable(absolutePath)) {
            return false;
        }
        if (device.exists(absolutePath) && !device.isDir(absolutePath)) {
            return false;
        }
        currentBackupPath = absolutePath.resolve(getDateString());
        return true;
    }

    /**
     * Initialize current backup path.
     *
     * @return success
     */
    public boolean initialize()
    {
        return initialize(false);
    }

    /**
     * Initialize (reinitialize) current backup path.
     *
     * @param reset true to reinit
     * @return success
     */
    public boolean initialize(boolean reset)
    {
        if (!initialized || reset) {
            initialized = true;
            time = System.currentTimeMillis();
            initializePath();
        }
        return currentBackupPath != null;
    }

    /**
     *
     * @return backup start time
     */
    protected long getCurrentBackupTime()
    {
        initialize(false);
        return time;
    }

    /**
     *
     * @return current backup path
     * @throws IOException when path initialization failed
     */
    protected Path getCurrentBackupPath() throws IOException
    {
        initialize(false);
        if (currentBackupPath == null) {
            throw new NotDirectoryException("Invalid backup directory: '" + String.valueOf(absolutePath) + "'");
        }
        return currentBackupPath;
    }

    /**
     *
     * @return by-date subpath
     */
    protected String getDateString()
    {
        return new SimpleDateFormat("yyyyMMdd").format(time);
    }

    /**
     *
     * @param entry
     * @return target path to store a backup of a file
     * @throws IOException
     */
    protected Path getBackupTargetPath(FileEntry entry) throws IOException
    {
        return getCurrentBackupPath().resolve(entry.getRelativePath());
    }

    /**
     *
     * @param entry
     * @param path
     * @return alternative path to store a backup or null if backup path already contains an equal file
     * @throws IOException
     */
    protected Path getBackupAlternativePath(FileEntry entry, Path path) throws IOException
    {
        if (compareFiles(entry, path)) {
            return null;
        }

        Path parent = path.getParent();
        String name = path.getFileName().toString();
        String base = FilenameUtils.removeExtension(name);
        String ext = FilenameUtils.getExtension(name);
        String end = ext.length() > 0 ? ("." + ext) : "";
        long ts = entry.getProperties().getModifiedTime() / 1000;

        String altName = base + (".ts" + ts) + end;
        Path altPath = parent.resolve(altName);
        if (!device.exists(altPath)) {
            return altPath;
        }
        if (compareFiles(entry, altPath)) {
            return null;
        }

        for (int i = 1; i < maxVersions; ++i) {
            String n = base + (".ts" + ts + "." + i) + end;
            Path ap = parent.resolve(n);
            if (!device.exists(ap)) {
                return ap;
            }
            if (compareFiles(entry, ap)) {
                return null;
            }
        }

        throw new IOException("File '" + entry.getRelativePath() + "' already has maximum backup versions");
    }

    /**
     *
     * @param entry
     * @param path
     * @return true if files are equal
     * @throws IOException
     */
    protected boolean compareFiles(FileEntry entry, Path path) throws IOException
    {
        BackupFile existing = new BackupFile(segment, path);
        return entry.isEqualTo(existing);
    }

    @Override
    public int backupFile(FileEntry entry) throws IOException
    {
        initialize(false);
        Path path = getBackupTargetPath(entry);
        if (device.exists(path)) {
            path = getBackupAlternativePath(entry, path);
            if (path == null) {
                return SyncResult.BACKUP_EXISTS;
            }
        }

        device.createDirs(path.getParent());
        if (moveIfPossible && device.isSameFileSystem(entry.getDevice())) {
            device.moveFile(entry.getAbsolutePath(), path, false);
            return SyncResult.BACKUP_MOVED;
        } else {
            device.moveFile(entry.getAbsolutePath(), path, false);
            return SyncResult.BACKUP_COPIED;
        }
    }

    @Override
    protected int backupDirIfEmptyOrMove(FileEntry entry) throws IOException
    {
        initialize(false);
        Path path = getBackupTargetPath(entry);
        if (device.exists(path)) {
            return SyncResult.BACKUP_EXISTS;
        }
        if (moveIfPossible && device.isSameFileSystem(entry.getDevice())) {
            device.createDirs(path.getParent());
            device.moveFile(entry.getAbsolutePath(), path, false);
            return SyncResult.BACKUP_MOVED;
        } else {
            device.createDirs(path);
            entry.getDevice().copyFileAttributes(entry.getAbsolutePath(), device, path);
            return SyncResult.BACKUP_COPIED;
        }
    }

    @Override
    public String toString()
    {
        return getPathStringId();
    }

    /**
     * Reference to a backup file.
     */
    private static class BackupFile extends FileReference
    {
        private final Path path;
        private final FileProperties properties;

        public BackupFile(Segment segment, Path path)
        {
            super(segment);
            this.path = path;
            this.properties = new FileProperties(segment.getDevice(), path, true);
        }

        @Override
        public Path getAbsolutePath()
        {
            return path;
        }

        @Override
        public FileProperties getProperties()
        {
            return properties;
        }

        @Override
        public void refresh()
        {
            properties.refresh();
        }

    }

}
