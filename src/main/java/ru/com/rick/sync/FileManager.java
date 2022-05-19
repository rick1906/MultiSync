/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.HashSet;
import ru.com.rick.sync.fs.Device;
import ru.com.rick.sync.fs.FileEntry;
import ru.com.rick.sync.fs.Segment;

/**
 *
 * @author Rick
 */
public abstract class FileManager
{

    /**
     * Backup a file (not directory).
     *
     * @param entry file
     * @return backup status
     * @throws IOException
     */
    public abstract int backupFile(FileEntry entry) throws IOException;

    /**
     * Backup directory without contents or move it to backup as a whole if possible.
     *
     * @param entry file
     * @return backup status
     * @throws IOException
     */
    protected abstract int backupDirIfEmptyOrMove(FileEntry entry) throws IOException;

    /**
     * Create all non-existent parent directories for a file.
     *
     * @param entry file
     * @param createRoot allow create segment root
     * @return operation result
     */
    public SyncResult createDirs(FileEntry entry, boolean createRoot)
    {
        Device device = entry.getDevice();
        Path parent = entry.getAbsolutePath().getParent();
        Path root = entry.getSegment().getRootPath();
        SyncResult result = initializeRoot(entry.getSegment(), createRoot);
        if (!result.isSuccess()) {
            return result;
        }
        if (parent != null && !parent.equals(root) && !device.exists(parent)) {
            try {
                device.createDirs(parent);
                return new SyncResult(SyncResult.SYNC_MERGE);
            } catch (IOException ex) {
                return new SyncResult(ex);
            }
        }
        return new SyncResult(SyncResult.SYNC_MATCHES);
    }

    /**
     * Create non-existent root directories for a segment.
     *
     * @param segment segment
     * @param createRoot allow create segment root
     * @return operation result
     */
    public SyncResult initializeRoot(Segment segment, boolean createRoot)
    {
        Device device = segment.getDevice();
        Path root = segment.getRootPath();
        if (!device.exists(root)) {
            if (createRoot) {
                try {
                    device.createDirs(root);
                    return new SyncResult(SyncResult.SYNC_MERGE);
                } catch (IOException ex) {
                    return new SyncResult(ex);
                }
            } else {
                return new SyncResult(new NotDirectoryException("Root does not exists: '" + root + "'"));
            }
        } else if (!device.isDir(root)) {
            return new SyncResult(new NotDirectoryException("Root is not a directory: '" + root + "'"));
        }
        return new SyncResult(SyncResult.SYNC_MATCHES);
    }

    /**
     * Delete a file or dir.
     *
     * @param entry
     * @param backup create backup
     * @return operation result
     */
    public SyncResult delete(FileEntry entry, boolean backup)
    {
        if (!entry.exists()) {
            return new SyncResult(SyncResult.SYNC_SKIP, true);
        }
        if (entry.isDir()) {
            return deleteDir(entry, backup);
        } else {
            return deleteFile(entry, backup);
        }
    }

    /**
     * Replace a target file or dir by source file or dir.
     *
     * @param source
     * @param target
     * @param backup create backup of a target
     * @return operation result
     */
    public SyncResult replace(FileEntry source, FileEntry target, boolean backup)
    {
        if (source.isDir() && target.isDir()) {
            return replaceDirByDir(source, target, backup);
        } else if (source.isDir()) {
            SyncResult dr = deleteFile(target, backup);
            SyncResult r = dr.isSuccess() ? copy(source, target) : dr;
            return r.transformValue(SyncResult.SYNC_REPLACE);
        } else if (target.isDir()) {
            SyncResult dr = deleteDir(target, backup);
            SyncResult r = dr.isSuccess() ? copy(source, target) : dr;
            return r.transformValue(SyncResult.SYNC_REPLACE);
        } else {
            return replaceFileByFile(source, target, backup);
        }
    }

    /**
     * Copy a source file or dir to a non-existent target.
     *
     * @param source
     * @param target
     * @return operation result
     */
    public SyncResult copy(FileEntry source, FileEntry target)
    {
        if (source.isDir()) {
            return copyDir(source, target);
        } else {
            return copyFile(source, target);
        }
    }

    /**
     * Copy a source file (not dir) to a non-existent target.
     *
     * @param source
     * @param target
     * @return
     */
    protected SyncResult copyFile(FileEntry source, FileEntry target)
    {
        if (target.isSymLink()) {
            return new SyncResult(SyncResult.SYNC_IGNORE, true);
        }
        try {
            source.copyFile(target, false);
            return new SyncResult(SyncResult.SYNC_CREATE, true);
        } catch (IOException ex) {
            return new SyncResult(SyncResult.SYNC_CREATE, ex, SyncResult.BACKUP_SKIPPED);
        }
    }

    /**
     * Copy a source dir to a non-existent target.
     *
     * @param source
     * @param target
     * @return
     */
    protected SyncResult copyDir(FileEntry source, FileEntry target)
    {
        if (!source.getSegment().followSymLinks() && source.isSymLink()) {
            return new SyncResult(SyncResult.SYNC_IGNORE, true);
        }
        if (!target.getSegment().writeToSymLinks() && target.isSymLink()) {
            return new SyncResult(SyncResult.SYNC_IGNORE, true); // should not happen
        }
        Device sdev = source.getDevice();
        Device tdev = target.getDevice();
        Path spath = source.getAbsolutePath();
        Path tpath = target.getAbsolutePath();
        Path srootp = source.getSegment().getRootPath();
        try {
            tdev.createDir(tpath);
            sdev.copyFileAttributes(spath, tdev, tpath);
        } catch (IOException ex) {
            return new SyncResult(SyncResult.SYNC_CREATE, ex, SyncResult.BACKUP_SKIPPED);
        }
        try (DirectoryStream<Path> stream = sdev.openDir(spath)) {
            SyncResult result = new SyncResult(SyncResult.SYNC_CREATE, true);
            for (Path sp : stream) {
                Path rel = srootp.relativize(sp);
                FileEntry se = new FileEntry(source.getSegment(), rel);
                FileEntry te = new FileEntry(target.getSegment(), rel);
                result.addSubResult(rel, copy(se, te));
            }
            return result.transformSuccess(!result.isTotalFailure());
        } catch (IOException ex) {
            return new SyncResult(SyncResult.SYNC_CREATE, ex, SyncResult.BACKUP_SKIPPED);
        }
    }

    /**
     * Replace a target file (not dir) by a source file (not dir).
     *
     * @param source
     * @param target
     * @param backup
     * @return
     */
    protected SyncResult replaceFileByFile(FileEntry source, FileEntry target, boolean backup)
    {
        int backupStatus = SyncResult.BACKUP_SKIPPED;
        if (target.isSymLink()) {
            return new SyncResult(SyncResult.SYNC_IGNORE);
        }
        if (backup) {
            try {
                backupStatus = backupFile(target);
            } catch (IOException ex) {
                return new SyncResult(SyncResult.SYNC_DELETE, ex);
            }
        }
        try {
            source.copyFile(target, true);
            return new SyncResult(SyncResult.SYNC_REPLACE, true, backupStatus);
        } catch (IOException ex) {
            return new SyncResult(SyncResult.SYNC_REPLACE, ex, backupStatus);
        }
    }

    /**
     * Replace a target dir by a source dir. Replaces full file tree.
     *
     * @param source
     * @param target
     * @param backup
     * @return
     */
    protected SyncResult replaceDirByDir(FileEntry source, FileEntry target, boolean backup)
    {
        if (!target.getSegment().writeToSymLinks() && target.isSymLink()) {
            return new SyncResult(SyncResult.SYNC_IGNORE, true);
        }
        if (!source.getSegment().followSymLinks() && source.isSymLink()) {
            return new SyncResult(SyncResult.SYNC_IGNORE, true);
        }
        Device sdev = source.getDevice();
        Device tdev = target.getDevice();
        Path spath = source.getAbsolutePath();
        Path tpath = target.getAbsolutePath();
        Path srootp = source.getSegment().getRootPath();
        Path trootp = target.getSegment().getRootPath();
        HashSet<Path> visited = new HashSet<>();
        SyncResult result = new SyncResult(SyncResult.SYNC_REPLACE, true);
        if (!target.isSymLink()) {
            sdev.copyFileAttributes(spath, tdev, tpath);
        }
        try (DirectoryStream<Path> stream = sdev.openDir(spath)) {
            for (Path sp : stream) {
                Path rel = srootp.relativize(sp);
                FileEntry se = new FileEntry(source.getSegment(), rel);
                FileEntry te = new FileEntry(target.getSegment(), rel);
                result.addSubResult(rel, replace(se, te, backup));
                visited.add(rel);
            }
        } catch (IOException ex) {
            return new SyncResult(SyncResult.SYNC_CREATE, ex, SyncResult.BACKUP_SKIPPED);
        }
        try {
            for (Path tp : tdev.listFiles(tpath)) {
                Path rel = trootp.relativize(tp);
                if (!visited.contains(rel)) {
                    FileEntry te = new FileEntry(target.getSegment(), rel);
                    result.addSubResult(rel, delete(te, backup));
                }
            }
        } catch (IOException ex) {
            return new SyncResult(SyncResult.SYNC_CREATE, ex, SyncResult.BACKUP_SKIPPED);
        }
        return result.transformSuccess(!result.isTotalFailure());
    }

    /**
     * Delete a file (not dir).
     *
     * @param entry
     * @param backup
     * @return
     */
    protected SyncResult deleteFile(FileEntry entry, boolean backup)
    {
        int backupStatus = SyncResult.BACKUP_SKIPPED;
        if (entry.isSymLink()) {
            return new SyncResult(SyncResult.SYNC_IGNORE);
        }
        if (backup) {
            try {
                backupStatus = backupFile(entry);
            } catch (IOException ex) {
                return new SyncResult(SyncResult.SYNC_DELETE, ex);
            }
            if (backupStatus == SyncResult.BACKUP_MOVED) {
                return new SyncResult(SyncResult.SYNC_DELETE, true, backupStatus);
            }
        }
        try {
            entry.deleteFile();
            return new SyncResult(SyncResult.SYNC_DELETE, true, backupStatus);
        } catch (IOException ex) {
            return new SyncResult(SyncResult.SYNC_DELETE, ex, backupStatus);
        }
    }

    /**
     * Delete a dir (recursively).
     *
     * @param entry
     * @param backup
     * @return
     */
    protected SyncResult deleteDir(FileEntry entry, boolean backup)
    {
        if (!entry.getSegment().writeToSymLinks() && entry.isSymLink()) {
            return new SyncResult(SyncResult.SYNC_IGNORE, true);
        }
        int backupStatus = SyncResult.BACKUP_SKIPPED;
        Device device = entry.getDevice();
        Path path = entry.getAbsolutePath();
        Path rootp = entry.getSegment().getRootPath();
        if (backup && !entry.isSymLink()) {
            try {
                backupStatus = backupDirIfEmptyOrMove(entry);
            } catch (IOException ex) {
                ex.printStackTrace();
                return new SyncResult(SyncResult.SYNC_DELETE, ex);
            }
        }
        if (backupStatus == SyncResult.BACKUP_MOVED) {
            return new SyncResult(SyncResult.SYNC_DELETE, true, backupStatus);
        }
        SyncResult result = new SyncResult(SyncResult.SYNC_DELETE, true);
        try {
            for (Path p : device.listFiles(path)) {
                Path rel = rootp.relativize(p);
                FileEntry e = new FileEntry(entry.getSegment(), rel);
                result.addSubResult(rel, delete(e, backup));
            }
        } catch (IOException ex) {
            return new SyncResult(SyncResult.SYNC_DELETE, ex, SyncResult.BACKUP_FAILED);
        }
        if (result.isTotalSuccess()) {
            if (entry.isSymLink()) {
                result.transformSuccess(result.countAll() > 0);
                return result;
            }
            try {
                device.deleteFile(path);
                return result;
            } catch (IOException ex) {
                return result.transformSuccess(ex);
            }
        } else {
            return result.transformSuccess(false);
        }
    }

}
