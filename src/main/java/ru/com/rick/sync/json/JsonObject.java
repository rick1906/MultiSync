/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.json;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 * Alternative (generics-aware) container for JSON objects from json.simple.
 *
 * @author Rick
 */
public class JsonObject extends LinkedHashMap<String, Object> implements Map<String, Object>, JSONAware, JSONStreamAware
{
    public JsonObject()
    {
    }

    @SuppressWarnings("unchecked")
    public JsonObject(Map json)
    {
        super(json);
    }

    public static JsonObject from(Map map)
    {
        if (map instanceof JsonObject) {
            return (JsonObject)map;
        } else if (map != null) {
            return new JsonObject(map);
        } else {
            return new JsonObject();
        }
    }

    @Override
    public String toJSONString()
    {
        return JSONObject.toJSONString(this);
    }

    @Override
    public void writeJSONString(Writer writer) throws IOException
    {
        JSONObject.writeJSONString(this, writer);
    }

}
