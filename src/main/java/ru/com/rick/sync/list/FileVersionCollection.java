/*
 * Copyright (c) 2022 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import ru.com.rick.sync.fs.FileVersion;
import ru.com.rick.sync.json.JsonArray;
import ru.com.rick.sync.json.JsonObject;

/**
 * Abstraction for storing file versions.
 *
 * @author Rick
 * @param <T> extra options
 */
public class FileVersionCollection<T extends FileVersionCollection.Options>
{
    /**
     * versions
     */
    protected final ArrayList<Record> versions = new ArrayList<>();

    /**
     * Export to JSON array.
     *
     * @param defaultTime sync time
     * @param maxVersions
     * @param withHash
     * @return JSON array
     */
    public JsonArray toJson(long defaultTime, int maxVersions, boolean withHash)
    {
        JsonArray result = new JsonArray();
        if (versions.size() > 0 && maxVersions != 0) {
            Iterable<Record> items = () -> getStream(null).sorted().iterator();
            int count = 0;
            for (Record item : items) {
                result.add(item.toJson(defaultTime, withHash));
                count++;
                if (maxVersions > 0 && count >= maxVersions) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Fill the list form JSON array.
     *
     * @param versions
     * @param defaultTime
     * @param optionsContructor
     */
    public void fill(List versions, long defaultTime, Function<Map, T> optionsContructor)
    {
        for (int i = versions.size() - 1; i >= 0; --i) {
            Object item = versions.get(i);
            addRecord(createRecord((Map)item, defaultTime, optionsContructor));
        }
    }

    /**
     * Fill the list form JSON array.
     *
     * @param versions
     * @param defaultTime
     */
    public void fill(List versions, long defaultTime)
    {
        fill(versions, defaultTime, null);
    }

    /**
     * Fill the list.
     *
     * @param versions
     * @param optionsContructor
     */
    public void fill(Iterable<FileVersion> versions, Supplier<T> optionsContructor)
    {
        for (FileVersion ver : versions) {
            addRecord(createRecord(ver, optionsContructor));
        }
    }

    /**
     * Fill the list.
     *
     * @param versions
     */
    public void fill(Iterable<FileVersion> versions)
    {
        for (FileVersion ver : versions) {
            addRecord(createRecord(ver, null));
        }
    }

    /**
     * Create version from JSON.
     *
     * @param item
     * @param defaultTime
     * @param optionsContructor
     * @return
     */
    protected Record createRecord(Map item, long defaultTime, Function<Map, T> optionsContructor)
    {
        if (optionsContructor != null) {
            T options = optionsContructor.apply(item);
            return new Record(item, defaultTime, options);
        } else {
            return new Record(item, defaultTime, null);
        }
    }

    /**
     * Create version.
     *
     * @param version
     * @param optionsContructor
     * @return
     */
    protected Record createRecord(FileVersion version, Supplier<T> optionsContructor)
    {
        if (optionsContructor != null) {
            T options = optionsContructor.get();
            return new Record(version, options);
        } else {
            return new Record(version, null);
        }
    }

    /**
     * Add record.
     *
     * @param item
     */
    protected void addRecord(Record item)
    {
        versions.add(item);
    }

    /**
     *
     * @return list is empty
     */
    public boolean isEmpty()
    {
        return versions.isEmpty();
    }

    /**
     *
     * @param predicate
     * @return records stream
     */
    protected Stream<Record> getStream(Predicate<? super T> predicate)
    {
        if (predicate != null) {
            return versions.stream().filter(x -> x.test(predicate));
        } else {
            return versions.stream();
        }
    }

    /**
     *
     * @param predicate
     * @return versions stream
     */
    public Stream<FileVersion> getVersionsStream(Predicate<? super T> predicate)
    {
        return getStream(predicate).map(x -> x.getVersion());
    }

    /**
     *
     * @return versions stream
     */
    public Stream<FileVersion> getVersionsStream()
    {
        return getVersionsStream(null);
    }

    /**
     *
     * @param predicate
     * @return versions stream
     */
    public Stream<FileVersion> getSortedVersionsStream(Predicate<? super T> predicate)
    {
        return getStream(predicate).sorted().map(x -> x.getVersion());
    }

    /**
     *
     * @return versions stream
     */
    public Stream<FileVersion> getSortedVersionsStream()
    {
        return getSortedVersionsStream(null);
    }

    /**
     *
     * @param predicate
     * @return versions array
     */
    public FileVersion[] getVersions(Predicate<? super T> predicate)
    {
        return getVersionsStream(predicate).toArray(c -> new FileVersion[c]);
    }

    /**
     *
     * @return versions array
     */
    public FileVersion[] getVersions()
    {
        return getVersions(null);
    }

    /**
     *
     * @param predicate
     * @return versions sorted DESC by sync time
     */
    public FileVersion[] getSortedVersions(Predicate<? super T> predicate)
    {
        return getSortedVersionsStream(predicate).toArray(c -> new FileVersion[c]);
    }

    /**
     *
     * @return versions sorted DESC by sync time
     */
    public FileVersion[] getSortedVersions()
    {
        return getSortedVersions(null);
    }

    /**
     * Add a version (if not yet added).
     *
     * @param ver
     * @param options
     * @return success
     */
    public boolean addVersion(FileVersion ver, T options)
    {
        for (int i = 0; i < versions.size(); ++i) {
            Record item = versions.get(i);
            FileVersion v = item.getVersion();
            if (ver.isSameVersion(v)) {
                if (ver.compareSyncTime(v) > 0) {
                    options = mergeOptions(options, item.getOptions());
                    versions.remove(i);
                    break;
                } else {
                    versions.set(i, copyRecordMergeOptions(item, options));
                    return false;
                }
            }
        }
        versions.add(new Record(ver, options));
        return true;
    }

    /**
     * Add a version (if not yet added).
     *
     * @param ver
     * @return success
     */
    public boolean addVersion(FileVersion ver)
    {
        return addVersion(ver, null);
    }

    /**
     *
     * @param primary
     * @param secondary
     * @return
     */
    protected T mergeOptions(T primary, T secondary)
    {
        if (primary != null && secondary != null) {
            primary.addOptions(secondary);
            return primary;
        } else if (primary != null) {
            return primary;
        } else if (secondary != null) {
            return secondary;
        } else {
            return null;
        }
    }

    /**
     *
     * @param item
     * @param options
     * @return
     */
    protected Record copyRecordMergeOptions(Record item, T options)
    {
        options = mergeOptions(item.getOptions(), options);
        return new Record(item.version, options);
    }

    /**
     * Get version equal to the supplied.
     *
     * @param ver
     * @param predicate
     * @return version from history
     */
    public FileVersion getVersion(FileVersion ver, Predicate<? super T> predicate)
    {
        for (int i = 0; i < versions.size(); ++i) {
            Record item = versions.get(i);
            if (predicate != null && !item.test(predicate)) {
                continue;
            }
            if (ver.isSameVersion(item.getVersion())) {
                return item.getVersion();
            }
        }
        return null;
    }

    /**
     * Get version equal to the supplied.
     *
     * @param ver
     * @param checkTime
     * @param checkHash
     * @param predicate
     * @return version from history
     */
    public FileVersion getVersion(FileVersion ver, boolean checkTime, boolean checkHash, Predicate<? super T> predicate)
    {
        for (int i = 0; i < versions.size(); ++i) {
            Record item = versions.get(i);
            if (predicate != null && !item.test(predicate)) {
                continue;
            }
            if (ver.isEqualTo(item.getVersion(), checkTime, checkHash)) {
                return item.getVersion();
            }
        }
        return null;
    }

    /**
     * Get version equal to the supplied.
     *
     * @param ver
     * @param checkTime
     * @param checkHash
     * @return
     */
    public FileVersion getVersion(FileVersion ver, boolean checkTime, boolean checkHash)
    {
        return getVersion(ver, checkTime, checkHash, null);
    }

    /**
     * Get version equal to the supplied.
     *
     * @param ver
     * @return
     */
    public FileVersion getVersion(FileVersion ver)
    {
        return getVersion(ver, null);
    }

    /**
     * Interface for extra version info.
     */
    public interface Options
    {
        /**
         *
         * @param other
         */
        public void addOptions(Options other);

        /**
         *
         * @param versionJson
         */
        public default void export(JsonObject versionJson)
        {
        }
    }

    /**
     * Class for the entry in collection.
     */
    public class Record implements Comparable<Record>
    {
        private final FileVersion version;
        private final T options;

        public Record(Map json, long defaultTime, T options)
        {
            this.version = new FileVersion(json, defaultTime);
            this.options = options;
        }

        public Record(FileVersion version, T options)
        {
            this.version = version;
            this.options = options;
        }

        public FileVersion getVersion()
        {
            return version;
        }

        public boolean test(Predicate<? super T> predicate)
        {
            return predicate != null && options != null ? predicate.test(options) : true;
        }

        public T getOptions()
        {
            return options;
        }

        @Override
        public int compareTo(Record o)
        {
            return -version.compareSyncTime(o.version);
        }

        public JsonObject toJson(long defaultTime, boolean withHash)
        {
            JsonObject result = version.toJson(defaultTime, withHash);
            if (options != null) {
                options.export(result);
            }
            return result;
        }
    }

}
