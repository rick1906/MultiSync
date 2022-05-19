/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.fs;

/**
 * File utils.
 *
 * @author Rick
 */
public class FileUtils
{
    /**
     * Compares modified times with second or ms accuracy, depending on a given values.
     *
     * @param mtime1 first modified time in ms
     * @param mtime2 second modified time in ms
     * @param defval default result if both times are unknown
     * @return true if times are equal with provided accuracy
     */
    public static boolean compareModifiedTimes(long mtime1, long mtime2, boolean defval)
    {
        if (mtime1 != FileVersion.TIME_UNKNOWN && mtime2 != FileVersion.TIME_UNKNOWN) {
            if (mtime1 == mtime2) {
                return true;
            } else {
                long mtime1s = mtime1 / 1000;
                long mtime2s = mtime2 / 1000;
                if (mtime1s == mtime2s) {
                    return mtime1 == mtime1s * 1000 || mtime2 == mtime2s * 1000;
                }
            }
        }
        return defval;
    }

    /**
     *
     * @param t1 unix time in ms
     * @param t2 unix time in ms
     * @return times compared as seconds
     */
    public static int compareSeconds(long t1, long t2)
    {
        if (t1 != FileVersion.TIME_UNKNOWN && t2 != FileVersion.TIME_UNKNOWN) {
            long ts1 = t1 / 1000;
            long ts2 = t2 / 1000;
            return ts1 == ts2 ? 0 : (ts1 > ts2 ? 1 : -1);
        }
        return 0;
    }

}
