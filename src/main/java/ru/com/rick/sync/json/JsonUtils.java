/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.json;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONAware;
import org.json.simple.parser.ParseException;

/**
 * JSON utils for json.simple.
 *
 * @author Rick
 */
public abstract class JsonUtils
{
    /**
     *
     * @param <T>
     * @param file file
     * @param resultClass class for the result
     * @return JSON value of class T
     * @throws IOException
     * @throws ParseException
     */
    public static <T> T readJsonFile(File file, Class<T> resultClass) throws IOException, ParseException
    {
        JsonParser parser = new JsonParser();
        try (Reader reader = new FileReader(file)) {
            return resultClass.cast(parser.parse(reader));
        } catch (ClassCastException ex) {
            throw new ParseException(ParseException.ERROR_UNEXPECTED_EXCEPTION, ex);
        }
    }

    /**
     *
     * @param data
     * @param file
     * @throws IOException
     */
    public static void writeJsonFile(JSONAware data, File file) throws IOException
    {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(data.toJSONString());
        }
    }

    /**
     * Get a value from JSON as a JSON object.If a value is not an oobject, convert it to an object {objectKey:value}
     *
     * @param json
     * @param key key in JSON
     * @param objectKey key in a new object
     * @return
     */
    public static Map getMap(Map json, String key, String objectKey)
    {
        return getMap(json, key, objectKey, false);
    }

    /**
     * Get a value from JSON as a JSON object. If a value is not an oobject, convert it to an object {objectKey:value}
     *
     * @param json
     * @param key -//-
     * @param objectKey -//-
     * @param allowNull allow null value for {objectKey:value} (otherwise return null on null value)
     * @return
     */
    public static Map getMap(Map json, String key, String objectKey, boolean allowNull)
    {
        if (json.containsKey(key)) {
            Object value = json.get(key);
            if (value != null || allowNull) {
                return transformToMap(value, objectKey);
            }
        }
        return null;
    }

    /**
     *
     * @param value
     * @param objectKey
     * @return transform a value to Map
     */
    public static Map transformToMap(Object value, String objectKey)
    {
        if (value instanceof Map) {
            return (Map)value;
        } else {
            JsonObject obj = new JsonObject();
            obj.put(objectKey, value);
            return obj;
        }
    }

    /**
     *
     * @param val
     * @return
     */
    public static String castToString(Object val)
    {
        if (val instanceof Map || val instanceof List) {
            return (String)val; // throw default cast exception
        } else if (val != null) {
            return String.valueOf(val);
        } else {
            return null;
        }
    }

    /**
     *
     * @param val
     * @return
     */
    public static Integer castToInteger(Object val)
    {
        if (val instanceof Number) {
            return ((Number)val).intValue();
        } else if (val != null) {
            return Integer.valueOf(castToString(val));
        } else {
            return null;
        }
    }

    /**
     *
     * @param val
     * @return
     */
    public static Long castToLong(Object val)
    {
        if (val instanceof Number) {
            return ((Number)val).longValue();
        } else if (val != null) {
            return Long.valueOf(castToString(val));
        } else {
            return null;
        }
    }

    /**
     *
     * @param val
     * @return
     */
    public static Double castToDouble(Object val)
    {
        if (val instanceof Number) {
            return ((Number)val).doubleValue();
        } else if (val != null) {
            return Double.valueOf(castToString(val));
        } else {
            return null;
        }
    }

    /**
     *
     * @param val
     * @return
     */
    public static Boolean castToBoolean(Object val)
    {
        if (val instanceof Number) {
            return ((Number)val).doubleValue() != 0;
        } else if (val instanceof String) {
            return Boolean.valueOf((String)val);
        } else {
            return (Boolean)val;
        }
    }

    /**
     *
     * @param json
     * @param key
     * @return
     */
    public static String getString(Map json, String key)
    {
        return castToString(json.get(key));
    }

    /**
     *
     * @param json
     * @param key
     * @param defval
     * @return
     */
    public static String getString(Map json, String key, String defval)
    {
        return json.containsKey(key) ? castToString(json.get(key)) : defval;
    }

    /**
     *
     * @param json
     * @param key
     * @return
     */
    public static Integer getInteger(Map json, String key)
    {
        return castToInteger(json.get(key));
    }

    /**
     *
     * @param json
     * @param key
     * @param defval
     * @return
     */
    public static Integer getInteger(Map json, String key, Integer defval)
    {
        return json.containsKey(key) ? castToInteger(json.get(key)) : defval;
    }

    /**
     *
     * @param json
     * @param key
     * @return
     */
    public static Long getLong(Map json, String key)
    {
        return castToLong(json.get(key));
    }

    /**
     *
     * @param json
     * @param key
     * @param defval
     * @return
     */
    public static Long getLong(Map json, String key, Long defval)
    {
        return json.containsKey(key) ? castToLong(json.get(key)) : defval;
    }

    /**
     *
     * @param json
     * @param key
     * @return
     */
    public static Double getDouble(Map json, String key)
    {
        return castToDouble(json.get(key));
    }

    /**
     *
     * @param json
     * @param key
     * @param defval
     * @return
     */
    public static Double getDouble(Map json, String key, Double defval)
    {
        return json.containsKey(key) ? castToDouble(json.get(key)) : defval;
    }

    /**
     *
     * @param json
     * @param key
     * @return
     */
    public static Boolean getBoolean(Map json, String key)
    {
        return castToBoolean(json.get(key));
    }

    /**
     *
     * @param json
     * @param key
     * @param defval
     * @return
     */
    public static Boolean getBoolean(Map json, String key, Boolean defval)
    {
        return json.containsKey(key) ? castToBoolean(json.get(key)) : defval;
    }
}
