/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.run;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import ru.com.rick.sync.Controller;
import ru.com.rick.sync.FileListContainer;
import ru.com.rick.sync.FileSyncEntry;
import ru.com.rick.sync.ProgressMonitor;
import ru.com.rick.sync.SyncElement;
import ru.com.rick.sync.SyncList;
import ru.com.rick.sync.SyncResult;
import ru.com.rick.sync.SyncTarget;

/**
 *
 * @author Rick
 */
public class DefaultRunner extends Runner
{
    public DefaultRunner(Controller controller)
    {
        super(controller);
        //this.monitor = new DebugMonitor();
    }

    public DefaultRunner(Controller controller, ProgressMonitor monitor)
    {
        super(controller, monitor);
    }

    @Override
    protected void onWarning(String message, int index, Exception error)
    {
        if (error != null) {
            out.print("Warning: " + message);
            out.println(" (" + error.toString() + ")");
        } else {
            out.println("Warning: " + message);
        }
    }

    @Override
    protected void onGlobalError(Exception error, String category)
    {
        out.println("Fatal error on '" + category + "':");
        error.printStackTrace(out);
    }

    @Override
    protected void onStart(SyncTarget[] targets)
    {
        out.println("Starting synchronization of " + targets.length + " targets...");
        for (int i = 0; i < targets.length; ++i) {
            out.println("#" + i + " " + targets[i]);
        }
    }

    @Override
    protected void onAfterScan(SyncList list, SyncResult[] results)
    {
        if (results.length > 0) {
            out.println("Done scanning targets:");
        }
        for (int i = 0; i < results.length; ++i) {
            out.println("#" + i + " " + results[i].getResultString("added"));
        }
    }

    @Override
    protected void onAfterAnalyze(SyncList list)
    {
        int c = list.size() + 1;
        out.println("Done analyzing, " + c + " elements to synchronize.");
    }

    @Override
    protected void onAfterSync(SyncList list, SyncResult[] results)
    {
        if (results.length > 0) {
            out.println("Done synchronization:");
        } else {
            out.println("No synchronization targets.");
        }
        for (int i = 0; i < results.length; ++i) {
            out.println("#" + i + " " + results[i].toString());
            for (Map.Entry<Path, SyncResult> entry : results[i].getSubResults().entrySet()) {
                SyncResult r = entry.getValue();
                if (!r.isSuccess()) {
                    out.println(entry.getKey() + ": " + r.toString());
                }
            }
        }
    }

    @Override
    protected void onAfterSave(SyncList list, Controller controller)
    {
        Set<FileListContainer> lists = controller.getFileListContainers();
        long done = lists.stream().filter(x -> x.isUpdated()).count();
        long fail = lists.stream().filter(x -> x.isUpdated() && !x.isValid()).count();
        out.println("Done saving " + (done - fail) + " of " + done + " file lists.");
    }

    private class DebugMonitor implements ProgressMonitor
    {
        @Override
        public void onElementAnalyze(SyncList list, SyncElement element, int index)
        {
            if (element.getSourceIndex() >= 0) {
                int k = element.getSourceIndex();
                FileSyncEntry source = element.getEntry(k);
                out.println();
                out.print(">> " + source.getUpdateTime(true) + " " + source.getFileEntry().getAbsolutePath());
                out.print(" " + element.getStatus(k).toString());
                out.print(" " + source.getSyncChangeStatus());
                out.println(" ->");
                for (int i = 0; i < element.getTargets().length; ++i) {
                    FileSyncEntry entry = element.getEntry(i);
                    if (i != k && entry.isAvailable()) {
                        out.print("++ " + entry.getUpdateTime(true) + " " + entry.getFileEntry().getAbsolutePath());
                        out.print(" " + element.getStatus(i).toString());
                        out.print(" " + entry.getSyncChangeStatus());
                        out.println();
                    }
                }
            }
        }

        @Override
        public void onElementSync(SyncList list, SyncElement element, int index, SyncResult[] results)
        {
            if (element.getSourceIndex() >= 0) {
                int k = element.getSourceIndex();
                FileSyncEntry source = element.getEntry(k);
                out.println();
                out.print("## " + source.getFileEntry().getAbsolutePath());
                out.print(" " + element.getStatus(k));
                out.print(" " + (element.getStatus(k).isDone() ? element.getStatus(k).getResult().toString() : "-"));
                out.println(" ->");
                for (int i = 0; i < element.getTargets().length; ++i) {
                    FileSyncEntry entry = element.getEntry(i);
                    if (i != k && entry.isAvailable()) {
                        out.print("** " + entry.getFileEntry().getAbsolutePath());
                        out.print(" " + element.getStatus(i).toString());
                        out.print(" " + (element.getStatus(i).isDone() ? element.getStatus(i).getResult().toString() : "-"));
                        out.println();
                    }
                }
            }
        }
    }

}
