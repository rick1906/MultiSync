/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.run;

/**
 *
 * @author Rick
 */
public class CheckException extends RuntimeException
{
    public CheckException(String message)
    {
        super(message);
    }

    public CheckException(String message, Throwable previous)
    {
        super(message, previous);
    }
}
