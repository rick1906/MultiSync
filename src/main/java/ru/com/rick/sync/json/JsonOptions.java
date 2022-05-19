/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.json;

import java.util.List;
import java.util.Map;

/**
 * A base class to extract a configuration form JSON and fallback to parent options if it's not present.
 *
 * To be extended with custom public methods for each option.
 *
 * @author Rick
 */
public class JsonOptions
{
    protected final Map json;
    protected final JsonOptions parent;

    public JsonOptions(Map json, JsonOptions parent)
    {
        this.json = json;
        this.parent = parent;
    }

    public JsonOptions(Map json)
    {
        this.json = json;
        this.parent = null;
    }

    public JsonOptions(JsonOptions options)
    {
        this.json = options.json;
        this.parent = options.parent;
    }

    /**
     *
     * @param name
     * @param recursive check inherited options
     * @return contains an local or inherited (if recursive) option
     */
    protected boolean hasOption(String name, boolean recursive)
    {
        if (json.containsKey(name)) {
            return true;
        }
        if (recursive && parent != null) {
            return parent.hasOption(name, recursive);
        }
        return false;
    }

    /**
     * Merge array inherited array-options with local.
     *
     * @param name
     * @return
     */
    protected JsonArray getMergedArrayOption(String name)
    {
        JsonArray result = null;
        if (json.containsKey(name)) {
            Object array = json.get(name);
            if (array == null || array instanceof Boolean && !((Boolean)array)) {
                return new JsonArray();
            } else {
                result = JsonArray.from((List)array);
            }
        }
        if (parent != null) {
            JsonArray extra = parent.getMergedArrayOption(name);
            if (result == null) {
                result = extra;
            } else if (extra != null && extra.size() > 0) {
                result = (JsonArray)result.clone();
                result.addAll(extra);
            }
        }
        return result != null ? result : new JsonArray();
    }

    /**
     *
     * @param name
     * @param defval
     * @return local or inherited option, or defval if none present
     */
    protected Object getOption(String name, Object defval)
    {
        if (json.containsKey(name)) {
            return json.get(name);
        }
        if (parent != null) {
            return parent.getOption(name, defval);
        }
        return defval;
    }

    /**
     *
     * @param name
     * @param defval
     * @return
     */
    protected String getStringOption(String name, String defval)
    {
        return JsonUtils.castToString(getOption(name, defval));
    }

    /**
     *
     * @param name
     * @param defval
     * @return
     */
    protected Integer getIntegerOption(String name, Integer defval)
    {
        return JsonUtils.castToInteger(getOption(name, defval));
    }

    /**
     *
     * @param name
     * @param defval
     * @return
     */
    protected Long getLongOption(String name, Long defval)
    {
        return JsonUtils.castToLong(getOption(name, defval));
    }

    /**
     *
     * @param name
     * @param defval
     * @return
     */
    protected Double getDoubleOption(String name, Double defval)
    {
        return JsonUtils.castToDouble(getOption(name, defval));
    }

    /**
     *
     * @param name
     * @param defval
     * @return
     */
    protected Boolean getBooleanOption(String name, Boolean defval)
    {
        return JsonUtils.castToBoolean(getOption(name, defval));
    }

    /**
     *
     * @param name
     * @return
     */
    protected Object getOption(String name)
    {
        return getOption(name, null);
    }

    /**
     *
     * @param name
     * @return
     */
    protected String getStringOption(String name)
    {
        return JsonUtils.castToString(getOption(name));
    }

    /**
     *
     * @param name
     * @return
     */
    protected Integer getIntegerOption(String name)
    {
        return JsonUtils.castToInteger(getOption(name));
    }

    /**
     *
     * @param name
     * @return
     */
    protected Long getLongOption(String name)
    {
        return JsonUtils.castToLong(getOption(name));
    }

    /**
     *
     * @param name
     * @return
     */
    protected Double getDoubleOption(String name)
    {
        return JsonUtils.castToDouble(getOption(name));
    }

    /**
     *
     * @param name
     * @return
     */
    protected Boolean getBooleanOption(String name)
    {
        return JsonUtils.castToBoolean(getOption(name));
    }

}
