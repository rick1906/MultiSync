/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.options;

import java.util.Map;

/**
 * Global options.
 *
 * @author Rick
 */
public class GlobalOptions extends SyncOptions
{
    public GlobalOptions(Map json)
    {
        super(json);
    }

    public String getLogFile()
    {
        return getStringOption("log");
    }

    public boolean appendLog()
    {
        return getBooleanOption("appendLog", true);
    }

    public boolean configDirAsRoot()
    {
        return getBooleanOption("configDirAsRoot", true);
    }
}
