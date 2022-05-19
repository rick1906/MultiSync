/*
 * Copyright (c) 2022 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.synctests;

import ru.com.rick.sync.json.JsonObject;

/**
 *
 * @author Rick
 */
public class GenerateConfig
{
    private JsonObject config = new JsonObject();
    private JsonObject targets = new JsonObject();

    public GenerateConfig()
    {
        config.put("backup", "backup");
        config.put("list", "filelist.json");
        config.put("log", "sync.log");
        config.put("targets", targets);
    }

    public JsonObject config()
    {
        return config;
    }

    public JsonObject targets()
    {
        return targets;
    }

    public JsonObject target(int index)
    {
        return (JsonObject)targets.get("sync" + index);
    }

    public void addTarget(int index)
    {
        addTarget(index, new JsonObject());
    }

    public void addTarget(int index, JsonObject config)
    {
        String name = "sync" + index;
        targets.put(name, config);
    }
}
