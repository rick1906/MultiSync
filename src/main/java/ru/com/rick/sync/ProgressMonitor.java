/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync;

import ru.com.rick.sync.list.FileList;

/**
 * Progress monitor for sync list.
 *
 * @author Rick
 */
public interface ProgressMonitor
{
    /**
     * 
     * @param list
     * @param targetIndex index in targets array
     * @param result root preparation result
     */
    public default void onStartSyncTarget(SyncList list, int targetIndex, SyncResult result)
    {
    }

    /**
     * 
     * @param list
     * @param element element added to SyncList
     */
    public default void onElementAdd(SyncList list, SyncElement element)
    {
    }

    /**
     * 
     * @param list
     * @param element analyzed element
     * @param count count of analyzed elements (element index in SyncList)
     */
    public default void onElementAnalyze(SyncList list, SyncElement element, int count)
    {
    }

    /**
     * 
     * @param list
     * @param element synced element
     * @param count count of done elements (current element index)
     * @param results sync results
     */
    public default void onElementSync(SyncList list, SyncElement element, int count, SyncResult[] results)
    {
    }

    /**
     * 
     * @param list
     * @param count
     * @param results 
     */
    public default void onFinishSync(SyncList list, int count, SyncResult[] results)
    {
    }

    /**
     * 
     * @param list
     * @param targetIndex 
     */
    public default void onFileListGenerateStart(SyncList list, int targetIndex)
    {
    }

    /**
     * 
     * @param list
     * @param targetIndex
     * @param fileList
     * @param count 
     */
    public default void onFileListGenerateEnd(SyncList list, int targetIndex, FileList fileList, int count)
    {
    }

    /**
     * 
     * @param list
     * @param targetIndex
     * @param fileList
     * @param element
     * @param count 
     */
    public default void onFileListGenerateEntry(SyncList list, int targetIndex, FileList fileList, SyncElement element, int count)
    {
    }
}
