/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.fs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * Class for matching subpaths against a set of parent paths.
 *
 * @author Rick
 */
public class PathFilter
{
    private final LinkedHashSet<Path> paths = new LinkedHashSet<>();

    /**
     *
     * @return
     */
    public int size()
    {
        return paths.size();
    }

    /**
     *
     * @param array
     * @param device
     * @return
     */
    public int addPaths(String[] array, Device device)
    {
        int c = 0;
        for (String s : array) {
            if (addPath(device.path(s))) {
                c++;
            }
        }
        return c;
    }

    /**
     *
     * @param path
     * @return
     */
    public boolean addPath(Path path)
    {
        Path np = path.normalize();
        return paths.add(np);
    }

    /**
     *
     * @param path
     * @return if contains an exact path
     */
    public boolean containsExactPath(Path path)
    {
        return paths.contains(path.normalize());
    }

    /**
     *
     * @param path
     * @return if contains a path or a subpath
     */
    public boolean containsPath(Path path)
    {
        Path np = path.normalize();
        for (Path p : paths) {
            if (np.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @return minimal paths collection to represent this set (extra subpaths removed).
     */
    public Collection<Path> getReducedPathCollection()
    {
        ArrayList<Path> list = new ArrayList<>(paths);
        ArrayList<Path> result = new ArrayList<>();
        Collections.sort(list);
        for (Path np : list) {
            boolean found = false;
            for (Path p : result) {
                if (np.startsWith(p)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                result.add(np);
            }
        }
        return result;
    }

}
