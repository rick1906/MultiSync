/**
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync;

import ru.com.rick.sync.fs.PathFilter;
import ru.com.rick.sync.fs.Root;
import ru.com.rick.sync.list.FileList;
import ru.com.rick.sync.fs.FileEntry;
import ru.com.rick.sync.fs.Device;
import ru.com.rick.sync.options.SyncOptions;
import java.nio.file.Path;
import java.util.Map;
import ru.com.rick.sync.fs.FileProperties;
import ru.com.rick.sync.fs.Segment;
import ru.com.rick.sync.list.FileHistory;
import ru.com.rick.sync.list.FileListTarget;

/**
 * Class for a single sync-enabled root directory. Synchronization target (or source).
 *
 * @author Rick
 */
public class SyncTarget implements FileListTarget
{
    private final Controller controller;
    private final Device device;
    private final Root root;
    private final Options options;
    //
    private boolean enabled = true;
    private int availableState = 0;
    private FileListContainer fileList = null;
    private Backuper backuper = null;
    private PathFilter ignoredPaths = null;
    private PathFilter includedPaths = null;

    /**
     *
     * @param controller
     * @param device
     * @param root
     * @param options
     */
    public SyncTarget(Controller controller, Device device, String root, Options options)
    {
        this.controller = controller;
        this.device = device;
        this.options = options;
        this.root = new Root(device, device.path(root), options);
    }

    /**
     *
     * @return if path on the device is mounted and accessible and target is enabled
     */
    @Override
    public boolean isAvailable()
    {
        if (availableState == 0) {
            availableState = device.isAvailable(root.getRootPath()) ? 1 : -1;
        }
        return availableState > 0 && enabled;
    }

    /**
     *
     * @return if target is not manually disabled
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Enable or disable this target. Disabled targets are skipped in sync.
     *
     * @param flag true to enable
     */
    public void setEnabled(boolean flag)
    {
        enabled = flag;
    }

    /**
     *
     * @return device
     */
    public Device getDevice()
    {
        return device;
    }

    /**
     *
     * @return root
     */
    public Root getRoot()
    {
        return root;
    }

    /**
     *
     * @return sync coptions
     */
    public Options getOptions()
    {
        return options;
    }

    /**
     *
     * @return file list
     */
    public FileList getFileList()
    {
        return getFileListContainer().getList();
    }

    /**
     *
     * @return backkuper
     */
    public Backuper getBackuper()
    {
        if (backuper == null) {
            backuper = controller.createBackuper(this, options.getBackuper());
        }
        return backuper;
    }

    /**
     *
     * @return file list container
     */
    public FileListContainer getFileListContainer()
    {
        if (fileList == null) {
            fileList = controller.createFileListContainer(this, options.getFileList());
        }
        return fileList;
    }

    /**
     *
     * @return ignored paths
     */
    public PathFilter getIgnoredPaths()
    {
        if (ignoredPaths == null) {
            ignoredPaths = new PathFilter();
            ignoredPaths.addPaths(options.getIgnoredPaths(), device);
            Path listPath = getFileListContainer().getRelativePath(root);
            Path backupPath = getBackuper().getRelativePath(root);
            if (listPath != null) {
                ignoredPaths.addPath(listPath);
            }
            if (backupPath != null) {
                ignoredPaths.addPath(backupPath);
            }
        }
        return ignoredPaths;
    }

    /**
     *
     * @return enabled paths
     */
    public PathFilter getIncludedPaths()
    {
        if (includedPaths == null) {
            includedPaths = new PathFilter();
            includedPaths.addPaths(options.getIncludedPaths(), device);
        }
        return includedPaths;
    }

    /**
     *
     * @param source file source
     * @return cat create a file copy from source
     */
    public boolean canCreate(FileEntry source)
    {
        return source.isDir() ? options.allowCreateDir() : options.allowCreate();
    }

    /**
     *
     * @param target target file
     * @return can delete a file
     */
    public boolean canDelete(FileEntry target)
    {
        return target.isDir() ? options.allowDeleteDir() : options.allowDelete();
    }

    /**
     *
     * @param target target file
     * @param source file source
     * @return can replace target with source file
     */
    public boolean canReplace(FileEntry target, FileEntry source)
    {
        return target.isDir() ? options.allowReplaceDir() : options.allowReplace();
    }

    /**
     *
     * @param rpath
     * @return file history entry from file list
     */
    public FileHistory getFileHistory(Path rpath)
    {
        return getFileListContainer().getEntry(rpath, this);
    }

    /**
     *
     * @param rpath
     * @return file entry for path
     */
    public FileEntry getFileEntry(Path rpath)
    {
        if (isAvailable()) {
            return new FileEntry(root, rpath);
        } else {
            return new UnavailableFileEntry(root, rpath);
        }
    }

    /**
     *
     * @param rpath
     * @param exists file exists
     * @return file entry for path with known exists state
     */
    public FileEntry getFileEntry(Path rpath, boolean exists)
    {
        return new FileEntry(root, rpath, exists);
    }

    /**
     *
     * @param rpath
     * @return relative path is ignored
     */
    public boolean pathIsIgnored(Path rpath)
    {
        PathFilter paths = getIncludedPaths();
        if (paths.size() > 0 && !paths.containsPath(rpath)) {
            return true;
        }
        
        PathFilter ignored = getIgnoredPaths();
        return ignored.containsPath(rpath);
    }

    /**
     *
     * @return backup enabled
     */
    public boolean isBackup()
    {
        return options.isBackup();
    }

    /**
     *
     * @return sync target can be source
     */
    public boolean isSource()
    {
        return options.isSource();
    }

    /**
     *
     * @return stop sync if not available
     */
    public boolean isRequired()
    {
        return options.isRequired();
    }

    /**
     *
     * @return do not allow changes to this target
     */
    public boolean isReadOnly()
    {
        return options.isReadOnly();
    }

    /**
     *
     * @return compare files by modified or updated time
     */
    public boolean preferNewer()
    {
        return options.preferNewer();
    }

    /**
     *
     * @return prefer existing file as a sync source
     */
    public boolean preferExisting()
    {
        return options.preferExisting();
    }

    /**
     *
     * @return source priority (higher is better)
     */
    public int getPriority()
    {
        return options.getPriority();
    }
    
    @Override
    public String getTargetId()
    {
        return device.getPathStringId(root.getRootPath());
    }
    
    @Override
    public String getRelativeTargetId()
    {
        Root r = controller.getDefaultRoot();
        if (r != null && r.getDevice().isSameFileSystem(device)) {
            Path path = r.getRelativePath(root.getRootPath());
            if (path != null) {
                return device.getPathStringId(path);
            }
        }
        return null;
    }
    
    @Override
    public long getUpdatedTime()
    {
        return fileList.getUpdatedTime(this);
    }
    
    @Override
    public String toString()
    {
        return root.toString();
    }

    /**
     * Class to prevent reading unavailable files.
     */
    private static class UnavailableFileEntry extends FileEntry
    {
        public UnavailableFileEntry(Segment segment, Path rpath)
        {
            super(segment, rpath);
        }
        
        @Override
        public FileProperties getProperties()
        {
            throw new IllegalStateException("Entry '" + toString() + "' is not available");
        }
        
    }

    /**
     * Options for sync target.
     */
    public static class Options extends SyncOptions
    {
        public Options(Map json, SyncOptions parent)
        {
            super(json, parent);
        }
        
        public int getPriority()
        {
            return getIntegerOption("priority", 0);
        }
        
        public boolean isReadOnly()
        {
            return getBooleanOption("readOnly", false);
        }
        
        public boolean isSource()
        {
            return getBooleanOption("source", true);
        }
        
        public boolean isRequired()
        {
            return getBooleanOption("required", !isReadOnly());
        }
    }
}
