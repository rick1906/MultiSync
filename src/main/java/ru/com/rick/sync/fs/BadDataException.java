/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.fs;

import java.io.IOException;

/**
 * Thrown when the acquired data is found to be invalid.
 *
 * @author Rick
 */
public class BadDataException extends IOException
{
    public BadDataException()
    {
    }

    public BadDataException(String message)
    {
        super(message);
    }

}
