/*
 * Copyright (c) 2022 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.synctests;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.com.rick.sync.Controller;
import ru.com.rick.sync.fs.FileEntry;
import ru.com.rick.sync.run.DefaultRunner;

/**
 *
 * @author Rick
 */
public class TestSyncWithList
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private void prepareFiles()
    {
        GenerateFiles gf = new GenerateFiles(folder);
        gf.generateDir(1);
        gf.generateDir(2);
        gf.generateDir(3);
        gf.generateFile(1, "test.txt", "source", gf.getTime(-100));
        gf.generateDir(1, "folder");
        gf.generateFile(1, "folder/in1.txt", "aaa", gf.getTime(-1000));
        gf.generateFile(1, "folder/in2.txt", "bbb", gf.getTime(-1000));

        GenerateConfig gc = new GenerateConfig();
        gc.addTarget(1);
        gc.addTarget(2);
        gc.addTarget(3);

        Controller controller = new Controller(gc.config(), folder.getRoot().toString());
        DefaultRunner runner = new DefaultRunner(controller);
        runner.run(true);

        sleep(2000);
    }

    private void sleep(long ms)
    {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testDeleteFolder()
    {
        GenerateFiles gf = new GenerateFiles(folder);
        prepareFiles();

        assert Files.exists(gf.path("filelist.json"));
        assert Files.isDirectory(gf.path(1, "folder"));
        assert Files.isDirectory(gf.path(2, "folder"));
        assert Files.isDirectory(gf.path(3, "folder"));

        GenerateConfig gc = new GenerateConfig();
        gc.addTarget(1);
        gc.addTarget(2);
        gc.addTarget(3);

        Controller controller = new Controller(gc.config(), folder.getRoot().toString());

        FileEntry entry = new FileEntry(controller.getDefaultRoot(), Paths.get("sync2/folder"));
        controller.getBackuper().delete(entry, false);

        assert gf.currentBackup("backup") == null; // no backup should be created

        sleep(2000);

        DefaultRunner runner = new DefaultRunner(controller);
        runner.run(true);

        assert !Files.exists(gf.path(1, "folder"));
        assert !Files.exists(gf.path(2, "folder"));
        assert !Files.exists(gf.path(3, "folder"));
        assert Files.isDirectory(gf.currentBackup("backup").resolve("folder"));
        assert gf.read(gf.currentBackup("backup").resolve("folder/in1.txt")).equals("aaa");
        assert gf.read(gf.currentBackup("backup").resolve("folder/in2.txt")).equals("bbb");
    }

    @Test
    public void testReplaceFile()
    {
        GenerateFiles gf = new GenerateFiles(folder);
        prepareFiles();

        assert Files.exists(gf.path("filelist.json"));
        assert Files.isDirectory(gf.path(1, "folder"));
        assert Files.isDirectory(gf.path(2, "folder"));
        assert Files.isDirectory(gf.path(3, "folder"));

        GenerateConfig gc = new GenerateConfig();
        gc.addTarget(1);
        gc.addTarget(2);
        gc.addTarget(3);

        Controller controller = new Controller(gc.config(), folder.getRoot().toString());

        gf.writeFile(3, "folder/in2.txt", "ccc", -1);
        sleep(2000);

        DefaultRunner runner = new DefaultRunner(controller);
        runner.run(true);

        assert gf.read(1, "folder/in2.txt").equals("ccc");
        assert gf.read(2, "folder/in2.txt").equals("ccc");
        assert gf.read(3, "folder/in2.txt").equals("ccc");
        assert gf.read(gf.currentBackup("backup").resolve("folder/in2.txt")).equals("bbb");
    }
}
