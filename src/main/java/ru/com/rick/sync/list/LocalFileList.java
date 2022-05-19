/**
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.list;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONAware;
import ru.com.rick.sync.fs.FileVersion;
import ru.com.rick.sync.json.JsonArray;
import ru.com.rick.sync.json.JsonObject;

/**
 * Representaion of the file list data used to monitor file changes.
 *
 * @author Rick
 */
public class LocalFileList implements FileList
{
    private final LinkedHashMap<String, FileHistory> map = new LinkedHashMap<>();
    private final long time;
    
    public LocalFileList(long defaultTime)
    {
        time = defaultTime;
    }

    /**
     *
     * @param json
     * @param defaultTime
     */
    public LocalFileList(Object json, long defaultTime)
    {
        if (json instanceof Map) {
            Map data = (Map)json;
            time = FileVersion.getTimeFromJson(data, "time", defaultTime);
            Object files = data.get("files");
            if (files instanceof List) {
                fillMap((List)files, time, false);
                Object delete = data.get("delete");
                if (delete instanceof List) {
                    fillMap((List)delete, time, true);
                }
            } else {
                fillMap((Map)files, time);
            }
        } else {
            time = defaultTime;
            fillMap((List)json, time, false);
        }
    }

    /**
     * From simple JSON array: ["path1", "path2", ...].
     *
     * @param jsonArray
     * @param defaultTime
     * @param delete
     */
    private void fillMap(List jsonArray, long defaultTime, boolean delete)
    {
        for (Object o : jsonArray) {
            if (o instanceof String) {
                FileHistory h = new FileHistory(!delete, defaultTime, delete ? FileHistory.STATE_DELETE : null);
                map.put((String)o, h);
            }
        }
    }

    /**
     * Create from JSON object: {"path1":{...params1...}, "path2":{...params2...}, ...}
     *
     * Can contain file info and versions.
     *
     * @param json
     * @param defaultTime generation time
     */
    private void fillMap(Map json, long defaultTime)
    {
        for (Object entry : json.entrySet()) {
            if (entry instanceof Map.Entry) {
                Object key = ((Map.Entry)entry).getKey();
                Object val = ((Map.Entry)entry).getValue();
                if (key instanceof String && val instanceof Map) {
                    map.put((String)key, new FileHistory((Map)val, defaultTime));
                }
            }
        }
    }

    /**
     *
     * @param maxVersions max file versions included (less 0 = unlimited)
     * @param withHash force hash calculation
     * @return JSON object with list of paths and file's information
     */
    @Override
    public JsonObject toJson(int maxVersions, boolean withHash)
    {
        JsonObject result = new JsonObject();
        JsonObject files = new JsonObject();
        for (Map.Entry<String, FileHistory> entry : map.entrySet()) {
            if (!entry.getValue().isEmpty(maxVersions != 0)) {
                files.put(entry.getKey(), entry.getValue().toJson(time, maxVersions, withHash));
            }
        }
        FileVersion.putTimeToJson(result, "time", time);
        result.put("files", files);
        return result;
    }
    
    @Override
    public JSONAware toCompactJson()
    {
        return toCompactJson(false);
    }

    /**
     *
     * @return simple JSON array of paths
     */
    public JsonArray toJsonArray()
    {
        JsonArray result = new JsonArray();
        for (Map.Entry<String, FileHistory> entry : map.entrySet()) {
            FileHistory h = entry.getValue();
            if (h.exists()) {
                result.add(entry.getKey());
            } else if (FileHistory.STATE_DELETE.equals(h.getSyncState())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     *
     * @param allowArray
     * @return simple JSON array of paths or {"files":[...],"delete":[...]}
     */
    public JSONAware toCompactJson(boolean allowArray)
    {
        JsonArray result = new JsonArray();
        JsonArray delete = new JsonArray();
        for (Map.Entry<String, FileHistory> entry : map.entrySet()) {
            FileHistory h = entry.getValue();
            if (h.exists()) {
                result.add(entry.getKey());
            } else if (FileHistory.STATE_DELETE.equals(h.getSyncState())) {
                delete.add(entry.getKey());
            }
        }
        if (allowArray && delete.isEmpty()) {
            return result;
        } else {
            JsonObject r = new JsonObject();
            FileVersion.putTimeToJson(r, "time", time);
            r.put("files", result);
            r.put("delete", delete);
            return r;
        }
    }
    
    @Override
    public void set(String path, FileHistory entry, FileListTarget target)
    {
        map.put(path, entry);
    }
    
    @Override
    public void add(String path, FileHistory entry, FileListTarget target)
    {
        FileHistory history = map.get(path);
        if (history != null) {
            history.addHistoryVersions(entry);
        } else {
            map.put(path, entry);
        }
    }
    
    @Override
    public FileHistory get(String path, FileListTarget target)
    {
        FileHistory entry = map.get(path);
        return entry != null ? entry : new FileHistory(time);
    }
    
    @Override
    public long getTime(FileListTarget target)
    {
        return time;
    }
    
    @Override
    public Collection<String> getPaths()
    {
        return map.keySet();
    }
    
}
