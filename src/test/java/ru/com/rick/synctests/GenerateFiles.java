/*
 * Copyright (c) 2022 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.synctests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import org.junit.rules.TemporaryFolder;
import ru.com.rick.sync.json.JsonObject;

/**
 *
 * @author Rick
 */
public class GenerateFiles
{
    private TemporaryFolder folder;
    private long time;

    public GenerateFiles(TemporaryFolder folder)
    {
        this.folder = folder;
        this.time = System.currentTimeMillis();
    }

    public long getTime()
    {
        return time;
    }

    public long getTime(long diff)
    {
        return time + diff * 1000;
    }

    public Path path(String rpath)
    {
        return folder.getRoot().toPath().resolve(rpath);
    }

    public Path path(int index, String rpath)
    {
        return folder.getRoot().toPath().resolve(Paths.get("sync" + index, rpath));
    }

    public Path currentBackup(String name)
    {
        if (!Files.isDirectory(path(name))) {
            return null;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path(name))) {
            Iterator<Path> it = stream.iterator();
            return it.hasNext() ? it.next() : null;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String read(int index, String path)
    {
        return read(path(index, path));
    }

    public String read(String path)
    {
        return read(path(path));
    }

    public String read(Path path)
    {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            return String.join(System.lineSeparator(), lines);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public File generateDir(int index)
    {
        try {
            return folder.newFolder("sync" + index);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public File generateDir(int index, String rpath)
    {
        try {
            return folder.newFolder(Paths.get("sync" + index, rpath).toString());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public File generateFile(int index, String rpath, String content, long mtime)
    {
        try {
            File file = folder.newFile(Paths.get("sync" + index, rpath).toString());
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"))) {
                writer.write(content);
            }
            file.setLastModified(mtime);
            return file;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public File writeFile(int index, String rpath, String content, long mtime)
    {
        try {
            File file = path(index, rpath).toFile();
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"))) {
                writer.write(content);
            }
            if (mtime > 0) {
                file.setLastModified(mtime);
            }
            return file;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public File generateConfigFile(JsonObject json)
    {
        try {
            File file = folder.newFile("multisync.json");
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"))) {
                writer.write(json.toJSONString());
            }
            return file;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
