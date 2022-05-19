/*
 * Copyright (c) 2022 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.list;

/**
 * Abstraction for sync target in file lists.
 *
 * @author Rick
 */
public interface FileListTarget
{
    /**
     *
     * @return target global id
     */
    public String getTargetId();

    /**
     *
     * @return target relative id or null
     */
    public String getRelativeTargetId();

    /**
     *
     * @return time of target's file list generation
     */
    public long getUpdatedTime();

    /**
     *
     * @return target is available
     */
    public boolean isAvailable();

}
