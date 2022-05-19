/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.json;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONStreamAware;

/**
 * Alternative (generics-aware) container for JSON arrays from json.simple.
 *
 * @author Rick
 */
public class JsonArray extends ArrayList<Object> implements List<Object>, JSONAware, JSONStreamAware
{
    public JsonArray()
    {
    }

    @SuppressWarnings("unchecked")
    public JsonArray(List list)
    {
        super(list);
    }

    public static JsonArray from(List list)
    {
        if (list instanceof JsonArray) {
            return (JsonArray)list;
        } else if (list != null) {
            return new JsonArray(list);
        } else {
            return new JsonArray();
        }
    }

    @Override
    public String toJSONString()
    {
        return JSONArray.toJSONString(this);
    }

    @Override
    public void writeJSONString(Writer writer) throws IOException
    {
        JSONArray.writeJSONString(this, writer);
    }

}
