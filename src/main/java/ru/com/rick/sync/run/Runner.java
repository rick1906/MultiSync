/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.run;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import ru.com.rick.sync.Backuper;
import ru.com.rick.sync.Controller;
import ru.com.rick.sync.FileListContainer;
import ru.com.rick.sync.ProgressMonitor;
import ru.com.rick.sync.SyncList;
import ru.com.rick.sync.SyncResult;
import ru.com.rick.sync.SyncTarget;

/**
 *
 * @author Rick
 */
public abstract class Runner
{
    protected final Controller controller;
    protected ProgressMonitor monitor;
    protected PrintStream out;

    public Runner(Controller controller)
    {
        this.controller = controller;
        this.out = System.out;
        this.monitor = null;
    }
    
    public Runner(Controller controller, ProgressMonitor monitor)
    {
        this.controller = controller;
        this.out = System.out;
        this.monitor = monitor;
    }

    public boolean run(boolean redirectOutput)
    {
        try {
            out = openLoggingStream();
            if (redirectOutput) {
                System.setOut(out);
                System.setErr(out);
            }
        } catch (IOException | RuntimeException ex) {
            onGlobalError(ex, "startup");
            return false;
        }

        try {
            runCheck();
        } catch (Exception ex) {
            onGlobalError(ex, "initialization");
            return false;
        }

        SyncList list;
        try {
            list = createSyncList();
            runScan(list);
        } catch (Exception ex) {
            onGlobalError(ex, "scanning");
            return false;
        }

        try {
            runAnalyze(list);
        } catch (Exception ex) {
            onGlobalError(ex, "analyzing");
            return false;
        }

        try {
            runSync(list);
        } catch (Exception ex) {
            onGlobalError(ex, "synchronization");
            return false;
        }

        try {
            runSave(list);
        } catch (Exception ex) {
            onGlobalError(ex, "saving");
            return false;
        }

        return true;
    }

    protected PrintStream openLoggingStream() throws IOException
    {
        OutputStream stream = controller.openLogFile();
        if (stream != null) {
            return new PrintStream(stream);
        } else {
            return System.out;
        }
    }

    protected SyncList createSyncList()
    {
        return new SyncList(controller.getTargets(), monitor);
    }

    protected void runCheck()
    {
        SyncTarget[] targets = controller.getTargets();
        onStart(targets);
        for (int i = 0; i < targets.length; ++i) {
            checkTarget(targets[i], i);
            checkFileList(targets[i], i);
            checkBackuper(targets[i], i);
        }
    }

    protected void disableTarget(SyncTarget target, int index)
    {
        target.setEnabled(false);
        if (target.isRequired()) {
            throw new CheckException("Required target " + index + " is not available");
        } else {
            onWarning("Target #" + index + " is not available, excluded from synchronization", index, null);
        }
    }

    protected void checkTarget(SyncTarget target, int index)
    {
        if (!target.isAvailable()) {
            disableTarget(target, index);
        }
    }

    protected void checkFileList(SyncTarget target, int index)
    {
        FileListContainer flc = target.getFileListContainer();
        if (flc.isConfigured()) {
            if (!flc.isAvailable()) {
                onWarning("Path of file list '" + flc + "' for target #" + index + " is not available, target disabled", index, null);
                disableTarget(target, index);
            } else if (!flc.isValid()) {
                onWarning("Invalid file list '" + flc + "' for target #" + index, index, flc.getError());
                if (!flc.ignoreErrors()) {
                    throw new CheckException("Invalid file list '" + flc + "' for target #" + index, flc.getError());
                }
            }
        } else if (!target.isReadOnly()) {
            onWarning("File list for target #" + index + " is not configured", index, null);
        }
    }

    protected void checkBackuper(SyncTarget target, int index)
    {
        Backuper bkp = target.getBackuper();
        if (bkp.isConfigured()) {
            if (!bkp.initialize()) {
                onWarning("Invalid backup path '" + bkp + "' for target #" + index + ", this will cause backup errors", index, null);
            }
        } else if (target.isBackup() && !target.isReadOnly()) {
            throw new CheckException("No backup configuration for target #" + index);
        }
    }

    protected void runScan(SyncList list)
    {
        SyncResult[] results = list.addAllTargets(true);
        list.addAllFileLists();
        onAfterScan(list, results);
    }

    protected void runSync(SyncList list)
    {
        SyncResult[] results = list.sync(controller.getGlobalOptions());
        onAfterSync(list, results);
    }

    protected void runAnalyze(SyncList list)
    {
        list.analyze(controller.getGlobalOptions());
        onAfterAnalyze(list);
    }

    protected void runSave(SyncList list)
    {
        list.saveFileLists();
        list.saveGlobalFileList(controller.getFileListContainer());
        for (FileListContainer flc : controller.getFileListContainers()) {
            if (flc.isUpdated() && !flc.isValid()) {
                onWarning("Error writing file list '" + flc + "'", -1, flc.getError());
            }
        }
        onAfterSave(list, controller);
    }

    protected abstract void onWarning(String message, int index, Exception error);

    protected abstract void onGlobalError(Exception error, String category);

    protected abstract void onStart(SyncTarget[] targets);

    protected abstract void onAfterScan(SyncList list, SyncResult[] results);

    protected abstract void onAfterAnalyze(SyncList list);

    protected abstract void onAfterSync(SyncList list, SyncResult[] results);

    protected abstract void onAfterSave(SyncList list, Controller controller);

}
