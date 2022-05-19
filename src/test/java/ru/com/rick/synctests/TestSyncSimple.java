/*
 * Copyright (c) 2022 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.synctests;

import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.com.rick.sync.Controller;
import ru.com.rick.sync.FileListContainer;
import ru.com.rick.sync.fs.FileProperties;
import ru.com.rick.sync.json.JsonObject;
import ru.com.rick.sync.list.FileHistory;
import ru.com.rick.sync.list.FileList;
import ru.com.rick.sync.run.DefaultRunner;

/**
 *
 * @author Rick
 */
public class TestSyncSimple
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testReplaceByModifiedTime()
    {
        GenerateFiles gf = new GenerateFiles(folder);
        gf.generateDir(1);
        gf.generateDir(2);
        gf.generateDir(3);
        gf.generateFile(1, "test.txt", "source", gf.getTime(-100));
        gf.generateFile(2, "test.txt", "target1", gf.getTime(-200));
        gf.generateFile(3, "test.txt", "target2", gf.getTime(-300));

        GenerateConfig gc = new GenerateConfig();
        gc.addTarget(1);
        gc.addTarget(2);
        gc.addTarget(3);

        Controller controller = new Controller(gc.config(), folder.getRoot().toString());
        DefaultRunner runner = new DefaultRunner(controller);
        boolean result = runner.run(true);

        assert result;
        assert Files.exists(gf.path("filelist.json"));
        assert gf.read(1, "test.txt").equals("source");
        assert gf.read(2, "test.txt").equals("source");
        assert gf.read(3, "test.txt").equals("source");
        assert gf.read(gf.currentBackup("backup").resolve("test.txt")).equals("target1");
    }

    @Test
    public void testCopy()
    {
        GenerateFiles gf = new GenerateFiles(folder);
        gf.generateDir(1);
        gf.generateDir(2);
        gf.generateDir(3);
        gf.generateFile(1, "test.txt", "source", gf.getTime(-100));

        GenerateConfig gc = new GenerateConfig();
        gc.addTarget(1);
        gc.addTarget(2);
        gc.addTarget(3);

        Controller controller = new Controller(gc.config(), folder.getRoot().toString());
        DefaultRunner runner = new DefaultRunner(controller);
        boolean result = runner.run(true);

        assert result;
        assert Files.exists(gf.path("filelist.json"));
        assert gf.read(1, "test.txt").equals("source");
        assert gf.read(2, "test.txt").equals("source");
        assert gf.read(3, "test.txt").equals("source");
    }

    @Test
    public void testFileListAndHash()
    {
        GenerateFiles gf = new GenerateFiles(folder);
        gf.generateDir(1);
        gf.generateDir(2);
        gf.generateFile(1, "test.txt", "source", gf.getTime(-100));

        JsonObject listConfig = new JsonObject();
        listConfig.put("path", "filelist.json");
        listConfig.put("requireHash", true);

        GenerateConfig gc = new GenerateConfig();
        gc.addTarget(1);
        gc.addTarget(2);
        gc.config().put("list", listConfig);

        Controller controller = new Controller(gc.config(), folder.getRoot().toString());
        DefaultRunner runner = new DefaultRunner(controller);
        boolean result = runner.run(true);

        assert result;
        assert Files.exists(gf.path("filelist.json"));
        assert gf.read(1, "test.txt").equals("source");
        assert gf.read(2, "test.txt").equals("source");

        FileListContainer flc = new FileListContainer(controller.getDefaultRoot(), gf.path("filelist.json"), true);
        FileList fl = flc.getList();

        FileProperties props = new FileProperties(controller.getDefaultRoot().getDevice(), gf.path(1, "test.txt"));
        FileHistory history = fl.get("test.txt");
        assert history != null;
        assert history.getCurrentVersion() != null;
        assert history.getCurrentVersion().isSameVersion(props);
        assert history.getCurrentVersion().getHash().equals("36cd38f49b9afa08222c0dc9ebfe35eb");
    }
}
