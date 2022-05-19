/*
 * Copyright (c) 2022 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.json.simple.JSONAware;
import ru.com.rick.sync.fs.FileUtils;
import ru.com.rick.sync.fs.FileVersion;
import ru.com.rick.sync.json.JsonArray;
import ru.com.rick.sync.json.JsonObject;
import ru.com.rick.sync.json.JsonUtils;

/**
 * One file list for multiple target directories.
 *
 * @author Rick
 */
public class GlobalFileList implements FileList
{
    private final LinkedHashMap<String, Entry> map = new LinkedHashMap<>();
    private final ArrayList<Target> targets = new ArrayList<>();
    private final HashMap<String, Integer> indexes = new HashMap<>();
    private final long time;

    /**
     * Create new list.
     *
     * @param defaultTime
     */
    public GlobalFileList(long defaultTime)
    {
        this.time = defaultTime;
    }

    /**
     * Create from JSON.
     *
     * @param json
     * @param defaultTime
     */
    public GlobalFileList(Map json, long defaultTime)
    {
        time = FileVersion.getTimeFromJson(json, "time", defaultTime);
        Object files = json.get("files");
        Object tlist = json.get("targets");
        if (tlist instanceof Map) {
            for (Object entry : ((Map)tlist).entrySet()) {
                if (entry instanceof Map.Entry) {
                    Object key = ((Map.Entry)entry).getKey();
                    Object val = ((Map.Entry)entry).getValue();
                    addTarget(new Target((String)key, val, time));
                }
            }
        }
        if (files instanceof Map) {
            for (Object entry : ((Map)files).entrySet()) {
                if (entry instanceof Map.Entry) {
                    Object key = ((Map.Entry)entry).getKey();
                    Object val = ((Map.Entry)entry).getValue();
                    map.put((String)key, new Entry((Map)val, time));
                }
            }
        }
    }

    @Override
    public JSONAware toJson(int maxVersions, boolean withHash)
    {
        JsonObject result = new JsonObject();
        JsonObject files = new JsonObject();
        JsonObject tlist = new JsonObject();
        for (Map.Entry<String, Entry> entry : map.entrySet()) {
            if (!entry.getValue().isEmpty(maxVersions != 0)) {
                files.put(entry.getKey(), entry.getValue().toJson(time, maxVersions, withHash));
            }
        }
        for (Target target : targets) {
            tlist.put(target.id, target.getJsonParams());
        }
        FileVersion.putTimeToJson(result, "time", time);
        result.put("targets", tlist);
        result.put("files", files);
        return result;
    }

    @Override
    public JSONAware toCompactJson()
    {
        return toJson(0, false);
    }

    /**
     * Add target to collections.
     *
     * @param target
     */
    private void addTarget(Target target)
    {
        int index = targets.size();
        targets.add(target);
        indexes.put(target.id, index);
        if (target.path != null && !indexes.containsKey(target.path)) {
            indexes.put(target.path, index);
        }
    }

    /**
     * Targets set to int indexes.
     *
     * @param set
     * @return
     */
    private ArrayList<Integer> getIndexes(HashSet<Target> set)
    {
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < targets.size(); ++i) {
            if (set.contains(targets.get(i))) {
                result.add(i);
            }
        }
        if (result.size() == targets.size()) {
            return null; // all targets
        }
        return result;
    }

    /**
     * Get internal target by index.
     *
     * @param index
     * @return
     */
    private Target getTarget(int index)
    {
        return index >= 0 && index < targets.size() ? targets.get(index) : null;
    }

    /**
     * Get internal target by FileListTarget interface.
     *
     * @param target
     * @return
     */
    private Target getTarget(FileListTarget target)
    {
        String id = target.getTargetId();
        String path = target.getRelativeTargetId();
        if (indexes.containsKey(id)) {
            return targets.get(indexes.get(id));
        }
        if (indexes.containsKey(path)) {
            return targets.get(indexes.get(path));
        }
        return null;
    }

    @Override
    public void set(String path, FileHistory entry, FileListTarget target)
    {
        Target t = null;
        if (target != null) {
            t = getTarget(target);
            if (t == null) {
                t = new Target(target, time);
                addTarget(t);
            }
        }
        Entry current = new Entry(t != null ? t.time : time);
        current.setHistory(entry, t);
        map.put(path, current);
    }

    @Override
    public void add(String path, FileHistory entry, FileListTarget target)
    {
        Target t = null;
        if (target != null) {
            t = getTarget(target);
            if (t == null) {
                t = new Target(target, time);
                addTarget(t);
            }
        }
        Entry current = map.get(path);
        if (current == null) {
            current = new Entry(t != null ? t.time : time);
            current.addHistory(entry, t);
            map.put(path, current);
        } else {
            current.addHistory(entry, t);
        }
    }

    @Override
    public FileHistory get(String path, FileListTarget target)
    {
        if (target == null) {
            Entry current = map.get(path);
            return current != null ? current.extractHistory(null) : new FileHistory(time);
        } else {
            Target t = getTarget(target);
            if (t != null) {
                Entry current = map.get(path);
                if (current != null) {
                    return current.extractHistory(t);
                } else if (target.isAvailable()) {
                    return new FileHistory(t.time);
                }
            }
            return null; // if no target present or target is unavailable and no list record
        }
    }

    @Override
    public long getTime(FileListTarget target)
    {
        if (target != null) {
            Target t = getTarget(target);
            if (t == null) {
                return FileVersion.TIME_UNKNOWN;
            } else if (t.time != FileVersion.TIME_UNKNOWN) {
                return t.time;
            }
        }
        return time;
    }

    @Override
    public Collection<String> getPaths()
    {
        return map.keySet();
    }

    /**
     * Class for identification of SyncTarget.
     */
    protected static class Target
    {
        private final String id;
        private final String path;
        private final long time;

        public Target(String id, Object json, long defaultTime)
        {
            Map map = JsonUtils.transformToMap(json, "time");
            this.id = id;
            this.path = JsonUtils.getString(map, "path", null);
            this.time = FileVersion.getTimeFromJson(map, "time", defaultTime);
        }

        public Target(FileListTarget target, long defaultTime)
        {
            this.id = target.getTargetId();
            this.path = target.getRelativeTargetId();
            this.time = target.isAvailable() ? defaultTime : target.getUpdatedTime();
        }

        public JsonObject getJsonParams()
        {
            JsonObject result = new JsonObject();
            FileVersion.putTimeToJson(result, "time", time);
            if (path != null) {
                result.put("path", path);
            }
            return result;
        }

        public long getTime()
        {
            return time;
        }
    }

    /**
     * Version options (targets).
     */
    protected class Options implements FileVersionCollection.Options
    {
        private final HashSet<Target> targets = new HashSet<>();
        private boolean global = false;

        public Options(int[] targets)
        {
            if (targets == null) {
                this.global = true;
            } else {
                for (int i : targets) {
                    Target t = getTarget(i);
                    if (t != null) {
                        this.targets.add(t);
                    }
                }
            }
        }

        public Options(Target target)
        {
            if (target == null) {
                this.global = true;
            } else {
                this.targets.add(target);
            }
        }

        @Override
        public void addOptions(FileVersionCollection.Options other)
        {
            if (other instanceof Options) {
                Options options = (Options)other;
                targets.addAll(options.targets);
                global = global || options.global;
            }
        }

        /**
         *
         * @param target
         * @return if version is valid for target
         */
        public boolean isForTarget(Target target)
        {
            return global || target == null || targets.contains(target);
        }

        @Override
        public void export(JsonObject versionJson)
        {
            if (!global) {
                ArrayList<Integer> indexes = getIndexes(targets);
                if (indexes != null) {
                    String v = indexes.stream().map(String::valueOf).collect(Collectors.joining(","));
                    versionJson.put("targets", v);
                }
            }
        }

    }

    /**
     * Global list entry.
     */
    public class Entry extends FileListEntry
    {
        /**
         * Variants of versions for different targets
         */
        private FileVersionCollection<Options> variants = null;

        /**
         * Versions history
         */
        private FileVersionCollection<Options> history = null;

        /**
         *
         * @param json
         * @param defaultTime
         */
        public Entry(Map json, long defaultTime)
        {
            super(json, defaultTime);
            Object vobj = json.get("variants");
            Object hobj = json.get("history");
            if (vobj instanceof List) {
                List varray = (List)vobj;
                for (int i = varray.size() - 1; i >= 0; --i) {
                    addVariantVersion((Map)varray.get(i), defaultTime);
                }
            }
            if (hobj instanceof List) {
                List harray = (List)hobj;
                for (int i = harray.size() - 1; i >= 0; --i) {
                    addHistoryVersion((Map)harray.get(i), defaultTime);
                }
            }
        }

        /**
         *
         * @param defaultTime
         */
        public Entry(long defaultTime)
        {
            super(false, defaultTime, null);
        }

        @Override
        public JsonObject toJson(long defaultTime, int maxVersions, boolean withHash)
        {
            JsonObject result = super.toJson(defaultTime, maxVersions, withHash);
            if (variants != null) {
                JsonArray varr = variants.toJson(defaultTime, -1, withHash);
                if (varr.size() > 0) {
                    result.put("variants", varr);
                }
            }
            if (history != null) {
                JsonArray harr = history.toJson(defaultTime, maxVersions, withHash);
                if (harr.size() > 0) {
                    result.put("history", harr);
                }
            }
            return result;
        }

        @Override
        public boolean isEmpty(boolean withHistory)
        {
            return super.isEmpty(withHistory) && (variants == null || variants.isEmpty());
        }

        @Override
        public boolean hasNoHistory()
        {
            return (history == null || history.isEmpty());
        }

        @Override
        public FileVersion[] getSortedHistoryVersions()
        {
            return history != null ? history.getSortedVersions() : new FileVersion[0];
        }

        @Override
        public FileVersion[] getHistoryVersions()
        {
            return history != null ? history.getVersions() : new FileVersion[0];
        }

        /**
         * Set primary (synced) history entry
         *
         * @param entry
         * @param target
         */
        private void setHistory(FileHistory entry, Target target)
        {
            Options o = target != null ? new Options(target) : null;
            this.current = entry.getCurrentVersion();
            this.exists = entry.exists();
            this.state = entry.getSyncState();
            this.time = entry.getSyncTime();
            for (FileVersion ver : entry.getHistoryVersions()) {
                addHistoryVersion(ver, o);
            }
        }

        /**
         * Add a history entry for a target.
         *
         * @param entry
         * @param target
         */
        private void addHistory(FileHistory entry, Target target)
        {
            Options o = null;
            FileVersion v = entry.getCurrentVersion();
            if (target == null) {
                this.current = v;
                this.exists = entry.exists();
                this.state = entry.getSyncState();
                this.time = entry.getSyncTime();
            } else {
                o = new Options(target);
                addVariantVersion(v != null ? v : FileVersion.EMPTY, o);
            }
            for (FileVersion ver : entry.getHistoryVersions()) {
                addHistoryVersion(ver, o);
            }
        }

        /**
         * Get history entry for a target.
         *
         * @param target
         * @return history entry
         */
        private FileHistory extractHistory(Target target)
        {
            FileVersion c;
            ArrayList<FileVersion> vs = new ArrayList<>();
            if (variants != null) {
                Optional<FileVersion> v = variants.getVersionsStream(x -> x.isForTarget(target)).findFirst();
                c = v.isPresent() ? v.get() : current;
            } else {
                c = current;
            }
            if (c != null && target != null && FileUtils.compareSeconds(c.getFirstSeenTime(), target.getTime()) > 0) {
                return null;
            }
            if (history != null) {
                history.getVersionsStream(x -> x.isForTarget(target)).forEachOrdered(vs::add);
            } else {
                vs = null;
            }
            return new FileHistory(c, time, state, vs);
        }

        /**
         *
         * @param map
         * @return array of targets' indexes from json
         */
        private int[] getTargets(Map map)
        {
            Object tobj = map.get("targets");
            if (tobj instanceof Number) {
                return new int[]{JsonUtils.castToInteger(tobj)};
            }
            if (tobj instanceof String) {
                String[] parts = ((String)tobj).split(",");
                int size = parts.length;
                int[] result = new int[size];
                for (int i = 0; i < size; ++i) {
                    result[i] = Integer.parseInt(parts[i]);
                }
                return result;
            }
            if (tobj instanceof List) {
                List list = (List)tobj;
                int size = list.size();
                int[] result = new int[size];
                for (int i = 0; i < size; ++i) {
                    result[i] = JsonUtils.castToInteger(list.get(i));
                }
                return result;
            }
            return null;
        }

        private void addVariantVersion(Map map, long defaultTime)
        {
            if (variants == null) {
                variants = new FileVersionCollection<>();
            }
            Options opt = new Options(getTargets(map));
            FileVersion ver = new FileVersion(map, defaultTime);
            variants.addVersion(ver, opt);
        }

        private void addHistoryVersion(Map map, long defaultTime)
        {
            if (history == null) {
                history = new FileVersionCollection<>();
            }
            Options opt = new Options(getTargets(map));
            FileVersion ver = new FileVersion(map, defaultTime);
            history.addVersion(ver, opt);
        }

        private boolean addVariantVersion(FileVersion ver, Options opt)
        {
            if (ver.isSameVersion(current)) {
                return false;
            }
            if (variants == null) {
                variants = new FileVersionCollection<>();
            }
            return variants.addVersion(ver, opt);
        }

        private boolean addHistoryVersion(FileVersion ver, Options opt)
        {
            if (!ver.isValidVersion()) {
                return false;
            }
            if (ver.isSameVersion(current)) {
                return false;
            }
            if (history == null) {
                history = new FileVersionCollection<>();
            }
            return history.addVersion(ver, opt);
        }

    }

}
