/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync;

import ru.com.rick.sync.fs.PathFilter;
import ru.com.rick.sync.fs.Root;
import ru.com.rick.sync.fs.Device;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import ru.com.rick.sync.options.GlobalOptions;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import ru.com.rick.sync.list.FileHistory;
import ru.com.rick.sync.list.FileList;
import ru.com.rick.sync.list.GlobalFileList;
import ru.com.rick.sync.list.LocalFileList;

/**
 * List of elements for synchronization. Main sync management class.
 *
 * @author Rick
 */
public class SyncList
{
    private final SyncTarget[] targets;
    private final RootElement rootElement;
    private final ProgressMonitor monitor;
    private final long time;
    private final LinkedHashMap<Path, SyncElement> map = new LinkedHashMap<>();
    private final ArrayList<Path> paths = new ArrayList<>();

    /**
     *
     * @param targets sync targets array
     * @param monitor object for processing callbacks while syncing
     */
    public SyncList(SyncTarget[] targets, ProgressMonitor monitor)
    {
        this.targets = targets;
        this.rootElement = new RootElement();
        this.monitor = monitor != null ? monitor : new NoMonitor();
        this.time = System.currentTimeMillis();
    }

    /**
     *
     * @param targets sync targets array
     */
    public SyncList(SyncTarget[] targets)
    {
        this(targets, null);
    }

    /**
     *
     * @return list items count
     */
    public int size()
    {
        return paths.size();
    }

    /**
     *
     * @return list generation time
     */
    public long getTime()
    {
        return time;
    }

    /**
     *
     * @return progress monitor
     */
    public ProgressMonitor getMonitor()
    {
        return monitor;
    }

    /**
     *
     * @return sync targets array
     */
    public SyncTarget[] getTargets()
    {
        return targets;
    }

    /**
     *
     * @return all relative paths in list
     */
    public Collection<Path> getPaths()
    {
        return map.keySet();
    }

    /**
     *
     * @return all sync elements in list
     */
    public Collection<SyncElement> getElements()
    {
        return map.values();
    }

    /**
     * Get element from list by path.
     *
     * @param path
     * @return sync element
     */
    public SyncElement get(Path path)
    {
        return map.get(path);
    }

    /**
     * Get element from list by index.
     *
     * @param index
     * @return sync element
     */
    public SyncElement get(int index)
    {
        return map.get(paths.get(index));
    }

    /**
     *
     * @param path
     * @return if list contains a path
     */
    public boolean contains(Path path)
    {
        return map.containsKey(path);
    }

    /**
     *
     * @param path
     * @return index of element with a supplied path
     */
    public int indexOf(Path path)
    {
        return paths.indexOf(path);
    }

    /**
     * Add an element to list (overwrite).
     *
     * @param rpath relative path
     * @param parent parent element/container
     * @return added element
     */
    public SyncElement set(Path rpath, SyncContainer parent)
    {
        if (parent == null) {
            parent = rootElement;
        }
        SyncElement element = new SyncElement(rpath, targets, parent);
        if (map.put(rpath, element) == null) {
            paths.add(rpath);
        }
        monitor.onElementAdd(this, element);
        return element;
    }

    /**
     * Add an element to list (not overwrite).
     *
     * @param rpath rpath relative path
     * @param parent parent element/container
     * @return added or existing element
     */
    public SyncElement add(Path rpath, SyncContainer parent)
    {
        if (map.containsKey(rpath)) {
            return map.get(rpath);
        } else {
            return set(rpath, parent);
        }
    }

    /**
     * Add an element and all subelements (scan all targets).
     *
     * @param rpath relative path
     * @param parent parent element/container
     * @return added or existing element
     */
    public SyncElement addRecursive(Path rpath, SyncContainer parent)
    {
        SyncElement element = add(rpath, parent);
        addChildren(element, true);
        return element;
    }

    /**
     * Add all child elements to the supplied (scan all targets).
     *
     * @param element sync element
     * @param recursive call this method for all children
     * @return results for each target
     */
    public SyncResult[] addChildren(SyncElement element, boolean recursive)
    {
        SyncResult[] results = new SyncResult[targets.length];
        for (int i = 0; i < targets.length; ++i) {
            if (element.getEntry(i).isContainer()) {
                results[i] = addChildrenForTarget(element, i, element.getPath(), recursive);
            } else {
                results[i] = new SyncResult(SyncResult.SYNC_SKIP);
            }
        }
        return results;
    }

    /**
     * Add all child elements from all targets' roots.
     *
     * @param recursive call this method for every added element
     * @return results array (one for each target)
     */
    public SyncResult[] addAllTargets(boolean recursive)
    {
        SyncResult[] results = new SyncResult[targets.length];
        for (int i = 0; i < targets.length; ++i) {
            results[i] = addSingleTarget(i, recursive);
        }
        return results;
    }

    /**
     * Add child elements for a single target's root.
     *
     * @param index target index
     * @param recursive call this method for every added element
     * @return result
     */
    public SyncResult addSingleTarget(int index, boolean recursive)
    {
        if (targets[index].isAvailable()) {
            PathFilter pf = targets[index].getIncludedPaths();
            if (pf.size() > 0) {
                return addRootPaths(index, pf, recursive);
            } else {
                return addRootContents(index, recursive);
            }
        } else {
            return new SyncResult(SyncResult.SYNC_SKIP);
        }
    }

    /**
     * Add all paths from a file list. Used to keep track of elements from unavailable targets.
     */
    public void addAllFileLists()
    {
        HashSet<FileListContainer> done = new HashSet<>();
        for (int i = 0; i < targets.length; ++i) {
            FileListContainer flc = targets[i].getFileListContainer();
            if (!done.contains(flc)) {
                done.add(flc);
                FileList list = flc.getList();
                if (list != null) {
                    addSingleFileList(i, list);
                }
            }
        }
    }

    /**
     * Add all paths form single file list related to target.
     *
     * @param index target index
     * @param list file list
     */
    public void addSingleFileList(int index, FileList list)
    {
        for (String p : list.getPaths()) {
            Path path = targets[index].getDevice().path(p);
            add(path, null);
        }
    }

    /**
     * Add multiple sync-enabled paths from target's root.
     *
     * @param index
     * @param pf
     * @param recursive
     * @return
     */
    private SyncResult addRootPaths(int index, PathFilter pf, boolean recursive)
    {
        if (!targets[index].isAvailable()) {
            return new SyncResult(SyncResult.SYNC_SKIP);
        }
        SyncResult result = new SyncResult(SyncResult.SYNC_MERGE).enableSubResults();
        for (Path rel : pf.getReducedPathCollection()) {
            SyncElement element = map.containsKey(rel) ? map.get(rel) : add(rel, null);
            if (element.getEntry(index).isContainer()) {
                result.addSubResult(rel, addChildrenForTarget(element, index, rel, recursive));
            }
        }
        return result;
    }

    /**
     * Add all child paths from target's root.
     *
     * @param index
     * @param recursive
     * @return
     */
    private SyncResult addRootContents(int index, boolean recursive)
    {
        SyncTarget target = targets[index];
        Root root = target.getRoot();
        Device device = target.getDevice();
        if (device.isDir(root.getRootPath())) {
            return addChildrenForTarget(null, index, null, recursive).enableSubResults();
        } else {
            return new SyncResult(SyncResult.SYNC_UNKNOWN);
        }
    }

    /**
     * Add child paths for single sync target.
     *
     * @param element
     * @param index
     * @param rpath
     * @param recursive
     * @return
     */
    private SyncResult addChildrenForTarget(SyncElement element, int index, Path rpath, boolean recursive)
    {
        if (!targets[index].isAvailable()) {
            return new SyncResult(SyncResult.SYNC_SKIP);
        }
        SyncTarget target = targets[index];
        SyncResult result = new SyncResult(SyncResult.SYNC_MERGE);
        Root root = target.getRoot();
        Path p = root.getAbsolutePath(rpath);
        Device device = target.getDevice();
        try (DirectoryStream<Path> stream = device.openDir(p)) {
            for (Path sp : stream) {
                Path rel = root.getRelativePath(sp);
                SyncElement e = add(rel, element);
                if (recursive && e.getEntry(index).isContainer()) {
                    result.addSubResult(rel, addChildrenForTarget(e, index, rel, recursive));
                } else {
                    result.addSubResult(rel, new SyncResult(SyncResult.SYNC_CREATE));
                }
            }
            return result;
        } catch (IOException ex) {
            if (element != null) {
                return element.getStatus(index).setResult(ex);
            } else {
                return new SyncResult(ex);
            }
        }
    }

    /**
     * Analyze all elements.
     *
     * @param options
     * @return count alanyzed
     */
    public int analyze(GlobalOptions options)
    {
        int c = 0;
        for (SyncElement el : map.values()) {
            el.analyze(options, true);
            monitor.onElementAnalyze(this, el, c++);
        }
        return c;
    }

    /**
     *
     * @param index sync target index
     * @return sync result for root element (root dir creation)
     */
    public SyncResult getRootSyncResult(int index)
    {
        return rootElement.getSyncResult(index);
    }

    /**
     * Start sync for single target. Create root (if configured).
     *
     * @param index sync target index
     * @param options
     * @return sync result
     */
    public SyncResult startSync(int index, GlobalOptions options)
    {
        SyncResult r = rootElement.startSync(index);
        monitor.onStartSyncTarget(this, index, r);
        return r;
    }

    /**
     * Sync all elements.
     *
     * @param options
     * @return sync results (with subresults)
     */
    public SyncResult[] sync(GlobalOptions options)
    {
        int c = 0;
        SyncResult[] results = new SyncResult[targets.length];
        for (int i = 0; i < targets.length; ++i) {
            results[i] = startSync(i, options).enableSubResults();
        }
        for (Map.Entry<Path, SyncElement> entry : map.entrySet()) {
            SyncElement element = entry.getValue();
            SyncResult[] rs = element.sync(options);
            monitor.onElementSync(this, element, c++, rs);
            if (rs != null) {
                for (int i = 0; i < results.length; ++i) {
                    results[i].addSubResult(entry.getKey(), rs[i]);
                }
            }
        }
        for (int i = 0; i < results.length; ++i) {
            results[i].transformSuccess(!results[i].isTotalFailure());
        }
        monitor.onFinishSync(this, c, results);
        return results;
    }

    /**
     * Generate resulting file list for single target.
     *
     * @param index target index
     * @return file list
     */
    public LocalFileList generateFileList(int index)
    {
        int c = 0;
        LocalFileList list = new LocalFileList(time);
        monitor.onFileListGenerateStart(this, index);
        for (SyncElement element : map.values()) {
            FileHistory entry = element.generateResultHistory(index, list.getTime());
            if (entry != null) {
                list.set(element.getPath(), entry, targets[index]);
            }
            monitor.onFileListGenerateEntry(this, index, list, element, c++);
        }
        monitor.onFileListGenerateEnd(this, index, list, c);
        return list;
    }

    /**
     * Generate global file list for targets with no local lists.
     *
     * @return file list
     */
    public GlobalFileList generateGlobalFileList()
    {
        HashSet<Integer> related = new HashSet<>();
        for (int i = 0; i < targets.length; ++i) {
            FileListContainer flc = targets[i].getFileListContainer();
            if (flc.isGlobal()) {
                related.add(i);
            }
        }
        return generateGlobalFileList(related.stream().mapToInt(x -> x).toArray());
    }

    /**
     * Generate global file list for targets with given indexes.
     *
     * @param indexes targets' indexes
     * @return file list
     */
    public GlobalFileList generateGlobalFileList(int... indexes)
    {
        int c = 0;
        GlobalFileList list = new GlobalFileList(time);
        monitor.onFileListGenerateStart(this, -1);
        for (SyncElement element : map.values()) {
            FileHistory selected = element.generateResultHistory(list.getTime());
            if (selected != null) {
                list.set(element.getPath(), selected, element.getSelectedTarget());
            }
            for (int index : indexes) {
                FileHistory entry = element.generateResultHistory(index, list.getTime());
                if (entry != null) {
                    list.add(element.getPath(), entry, targets[index]);
                }
            }
            monitor.onFileListGenerateEntry(this, -1, list, element, c++);
        }
        monitor.onFileListGenerateEnd(this, -1, list, c);
        return list;
    }

    /**
     * Generate and save global file list (if not yet updated).
     *
     * @param container list container with old list
     * @return success
     */
    public boolean saveGlobalFileList(FileListContainer container)
    {
        if (container.isConfigured() && container.isGlobal()) {
            if (container.isUpdated()) {
                return container.isValid();
            } else {
                return container.update(generateGlobalFileList());
            }
        } else {
            return false;
        }
    }

    /**
     * Generate and save local file list for target with given index (if not yet updated).
     *
     * @param index target index.
     * @return file list
     */
    public boolean saveFileList(int index)
    {
        FileListContainer flc = targets[index].getFileListContainer();
        if (flc.isConfigured() && !flc.isGlobal() && targets[index].isAvailable()) {
            if (flc.isUpdated()) {
                return flc.isValid();
            } else {
                return flc.update(generateFileList(index));
            }
        } else {
            return false;
        }
    }

    /**
     * Generate and save all local file lists.
     *
     * @return results array
     */
    public boolean[] saveFileLists()
    {
        boolean[] results = new boolean[targets.length];
        for (int i = 0; i < results.length; ++i) {
            results[i] = saveFileList(i);
        }
        return results;
    }

    /**
     * Default ProgressMonitor stub.
     */
    private static class NoMonitor implements ProgressMonitor
    {
    }

    /**
     * Root SyncContainer.
     */
    private class RootElement implements SyncContainer
    {
        private final SyncResult[] results;
        private final SyncStatus mergeStatus = new SyncStatus(SyncStatus.SYNC_MERGE);

        public RootElement()
        {
            results = new SyncResult[targets.length];
        }

        /**
         * Create root if needed and configured.
         *
         * @param index
         * @return result
         */
        public SyncResult startSync(int index)
        {
            SyncTarget target = targets[index];
            if (results[index] == null) {
                if (!target.isAvailable()) {
                    results[index] = new SyncResult(SyncResult.SYNC_SKIP);
                } else {
                    results[index] = target.getBackuper().initializeRoot(target.getRoot(), target.getOptions().createRoot());
                }
            }
            return results[index];
        }

        @Override
        public SyncTarget[] getTargets()
        {
            return targets;
        }

        @Override
        public FileSyncEntry getEntry(int index)
        {
            return null;
        }

        @Override
        public SyncResult getSyncResult(int index)
        {
            return results[index];
        }

        @Override
        public SyncStatus getSyncStatus(int index)
        {
            return mergeStatus;
        }

        @Override
        public int getSourceIndex()
        {
            return -1;
        }

    }
}
