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
import ru.com.rick.sync.SyncElement;
import ru.com.rick.sync.SyncList;

/**
 *
 * @author Rick
 */
public class TestSyncConsequent
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    public SyncList consequentSync(Controller controller)
    {
        SyncList list = new SyncList(controller.getTargets());
        list.addAllTargets(false); // add roots non-recursively
        for (int i = 0; i < controller.getTargets().length; ++i) {
            list.startSync(i, controller.getGlobalOptions()); // initialize roots
        }
        int index = 0;
        while (index < list.size()) {
            SyncElement current = list.get(index);
            if (current.analyze(controller.getGlobalOptions(), true)) {
                current.sync(controller.getGlobalOptions()); // sync one element
                list.addChildren(current, false); // for directories add child files non-recursively
            }
            index++;
        }
        list.saveFileLists(); // save local filelists
        list.saveGlobalFileList(controller.getFileListContainer()); // save global filelist
        return list;
    }

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
        consequentSync(controller);

        assert Files.exists(gf.path("filelist.json"));
        assert gf.read(1, "test.txt").equals("source");
        assert gf.read(2, "test.txt").equals("source");
        assert gf.read(3, "test.txt").equals("source");
        assert gf.read(gf.currentBackup("backup").resolve("test.txt")).equals("target1");
    }

}
