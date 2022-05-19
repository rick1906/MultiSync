/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.com.rick.sync.fs.Device;
import ru.com.rick.sync.fs.FileSystemDevice;
import ru.com.rick.sync.fs.LogOutputStream;
import ru.com.rick.sync.fs.PathOptions;
import ru.com.rick.sync.fs.Root;
import ru.com.rick.sync.json.JsonObject;
import ru.com.rick.sync.json.JsonOptions;
import ru.com.rick.sync.json.JsonUtils;
import ru.com.rick.sync.options.GlobalOptions;

/**
 * Contains global objects used for a certain run of synchronization.
 *
 * @author Rick
 */
public class Controller
{
    /**
     * Global options
     */
    private final GlobalOptions options;

    /**
     * Sync targets
     */
    private final SyncTarget[] targets;

    /**
     * Default root (json file dir)
     */
    private final Root defaultRoot;

    /**
     * Global backuper object
     */
    private final Backuper backuper;

    /**
     * Global file list container
     */
    private final FileListContainer fileList;

    /**
     *
     * @param json
     * @param rootPath
     */
    public Controller(Map json, String rootPath)
    {
        this.options = new GlobalOptions(json);
        this.defaultRoot = initRoot(json, rootPath, options);
        this.backuper = initBackuper(options, defaultRoot);
        this.fileList = initFileListContainer(options, defaultRoot);
        Object targetsValue = json.get("targets");
        if (targetsValue instanceof List) {
            this.targets = initTargets((List)targetsValue, options);
        } else if (targetsValue instanceof Map) {
            this.targets = initTargets((Map)targetsValue, options);
        } else {
            throw new IllegalArgumentException("Invalid value for JSON configuration option 'targets'");
        }
    }

    private Root initRoot(Map json, String rootPath, GlobalOptions options)
    {
        Map deviceJson = JsonUtils.getMap(json, "device", "type");
        String root = JsonUtils.getString(json, "root");
        if (deviceJson != null || root != null) {
            Device device = createDevice(deviceJson);
            return new Root(device, device.path(root), options);
        }
        if (!options.configDirAsRoot()) {
            Device device = new FileSystemDevice();
            return new Root(device, device.path("."), options);
        } else {
            Path path = Paths.get(rootPath).normalize().toAbsolutePath();
            if (Files.exists(path) && !Files.isDirectory(path)) {
                path = path.getParent(); // handle json config submited as root
            }
            return new Root(new FileSystemDevice(path.toString()), path, options);
        }
    }

    private Backuper initBackuper(GlobalOptions options, Root root)
    {
        Map config = options.getBackuper();
        return new Backuper(root, JsonObject.from(config));
    }

    private FileListContainer initFileListContainer(GlobalOptions options, Root root)
    {
        Map config = options.getFileList();
        return new FileListContainer(root, JsonObject.from(config), true);
    }

    private SyncTarget[] initTargets(List targetsJson, GlobalOptions options)
    {
        ArrayList<SyncTarget> results = new ArrayList<>();
        for (Object item : targetsJson) {
            SyncTarget target;
            if (item instanceof Map) {
                target = createSyncTarget((Map)item);
            } else {
                String root = JsonUtils.castToString(item);
                target = createSyncTarget(root, new JsonObject());
            }
            results.add(target);
        }
        return results.toArray(new SyncTarget[0]);
    }

    private SyncTarget[] initTargets(Map targetsJson, GlobalOptions options)
    {
        ArrayList<SyncTarget> results = new ArrayList<>();
        for (Object entry : targetsJson.entrySet()) {
            if (entry instanceof Map.Entry) {
                Object key = ((Map.Entry)entry).getKey();
                Object val = ((Map.Entry)entry).getValue();
                String root = JsonUtils.castToString(key);
                Map config = (Map)val;
                SyncTarget target = createSyncTarget(root, config);
                results.add(target);
            }
        }
        return results.toArray(new SyncTarget[0]);
    }

    /**
     * Create a device by not-null type.
     *
     * @param type
     * @param deviceJson
     * @return
     */
    protected Device createDeviceByType(String type, Map deviceJson)
    {
        if (type.equalsIgnoreCase("fs")) {
            return new FileSystemDevice(deviceJson);
        }
        throw new IllegalArgumentException("Illegal device type: '" + type + "'");
    }

    /**
     * Create a device by type or throw on null type.
     *
     * @param type
     * @param deviceJson
     * @return
     */
    public final Device createDevice(String type, Map deviceJson)
    {
        if (type == null) {
            throw new IllegalArgumentException("Device type should not be null");
        }
        return createDeviceByType(type, deviceJson);
    }

    /**
     * Create a device by JSON or default fs device on null.
     *
     * @param deviceJson
     * @return
     */
    public Device createDevice(Map deviceJson)
    {
        if (deviceJson == null) {
            return defaultRoot.getDevice();
        } else {
            return createDevice(JsonUtils.getString(deviceJson, "type", "fs"), deviceJson);
        }
    }

    /**
     * Create a root from an object configuration or return null.
     *
     * @param configJson object configuration json
     * @param configVar object configuration that represents a path
     * @param parentOptions inherited options
     * @return
     */
    public Root tryCreateRoot(Map configJson, String configVar, JsonOptions parentOptions)
    {
        Object r = configJson.get("root");
        Map deviceJson = JsonUtils.getMap(configJson, "device", "type");
        if (r instanceof Boolean && (Boolean)r && deviceJson == null) {
            return getDefaultRoot();
        } else {
            Map rootJson = JsonUtils.getMap(configJson, "root", "path");
            Device device = createDevice(deviceJson);
            if (rootJson != null) {
                String path = JsonUtils.getString(rootJson, "path");
                return new Root(device, device.path(path), new PathOptions(configJson, parentOptions));
            } else if (deviceJson != null && configVar != null) {
                Path componentPath = device.path(JsonUtils.getString(configJson, configVar));
                Path parentPath = componentPath.getParent();
                Path path = parentPath != null ? parentPath : componentPath;
                return new Root(device, path, new PathOptions(configJson, parentOptions));
            } else {
                return null;
            }
        }
    }

    /**
     * Create backuper for a sync target.
     *
     * @param target
     * @param configJson
     * @return
     */
    public Backuper createBackuper(SyncTarget target, Map configJson)
    {
        if (configJson == null) {
            return getBackuper();
        } else {
            Root root = tryCreateRoot(configJson, "path", target.getOptions());
            if (root != null) {
                return new Backuper(root, configJson);
            } else {
                return new Backuper(target.getRoot(), configJson);
            }
        }
    }

    /**
     * Create file list container for a sync target.
     *
     * @param target
     * @param configJson
     * @return
     */
    public FileListContainer createFileListContainer(SyncTarget target, Map configJson)
    {
        if (configJson == null) {
            return getFileListContainer();
        } else {
            Root root = tryCreateRoot(configJson, "path", target.getOptions());
            if (root != null) {
                return new FileListContainer(root, configJson, false);
            } else {
                return new FileListContainer(target.getRoot(), configJson, false);
            }
        }
    }

    /**
     * Create a sync target.
     *
     * @param root
     * @param configJson
     * @return
     */
    public SyncTarget createSyncTarget(String root, Map configJson)
    {
        Map deviceJson = JsonUtils.getMap(configJson, "device", "type");
        Device device = createDevice(deviceJson);
        String realRoot = JsonUtils.getString(configJson, "root", root);
        return new SyncTarget(this, device, realRoot, new SyncTarget.Options(configJson, getGlobalOptions()));
    }

    /**
     * Create a sync target.
     *
     * @param configJson
     * @return
     */
    public SyncTarget createSyncTarget(Map configJson)
    {
        Map deviceJson = JsonUtils.getMap(configJson, "device", "type");
        Device device = createDevice(deviceJson);
        String root = JsonUtils.getString(configJson, "root");
        return new SyncTarget(this, device, root, new SyncTarget.Options(configJson, getGlobalOptions()));
    }

    /**
     *
     * @return all file list containers
     */
    public Set<FileListContainer> getFileListContainers()
    {
        HashSet<FileListContainer> result = new HashSet<>();
        result.add(getFileListContainer());
        for (SyncTarget target : targets) {
            FileListContainer flc = target.getFileListContainer();
            if (!result.contains(flc)) {
                result.add(flc);
            }
        }
        return result;
    }

    /**
     *
     * @return all backupers
     */
    public Set<Backuper> getBackupers()
    {
        HashSet<Backuper> result = new HashSet<>();
        result.add(getBackuper());
        for (SyncTarget target : targets) {
            Backuper bkp = target.getBackuper();
            if (!result.contains(bkp)) {
                result.add(bkp);
            }
        }
        return result;
    }

    /**
     *
     * @return
     */
    public final GlobalOptions getGlobalOptions()
    {
        return options;
    }

    /**
     *
     * @return
     */
    public final Root getDefaultRoot()
    {
        return defaultRoot;
    }

    /**
     *
     * @return
     */
    public final Backuper getBackuper()
    {
        return backuper;
    }

    /**
     *
     * @return
     */
    public final FileListContainer getFileListContainer()
    {
        return fileList;
    }

    /**
     *
     * @return
     */
    public final SyncTarget[] getTargets()
    {
        return targets;
    }

    /**
     *
     * @return log file output stream
     * @throws IOException
     */
    public OutputStream openLogFile() throws IOException
    {
        String logFile = options.getLogFile();
        if (logFile != null) {
            Path path = defaultRoot.getDevice().path(logFile);
            if (!defaultRoot.getDevice().isAbsolutePath(path)) {
                path = defaultRoot.getAbsolutePath(path);
            }
            return new LogOutputStream(path.toFile(), options.appendLog());
        }
        return null;
    }
}
