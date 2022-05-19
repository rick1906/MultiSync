/*
 * Copyright (c) 2021 Rick.
 *
 * This file is a part of a project 'MultiSync'.
 * For license information see the project licence.
 */
package ru.com.rick.sync.json;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Alternative parser for json.simple, that outputs alternative JSON containers.
 *
 * @author Rick
 */
public class JsonParser extends JSONParser
{
    private static ContainerFactory containerFactory = new ContainerFactory()
    {
        @Override
        public Map createObjectContainer()
        {
            return new JsonObject();
        }

        @Override
        public List creatArrayContainer()
        {
            return new JsonArray();
        }
    };

    @Override
    public Object parse(Reader reader, ContainerFactory cf) throws IOException, ParseException
    {
        if (cf == null) {
            cf = containerFactory;
        }
        return super.parse(reader, cf);
    }

    @Override
    public Object parse(String string, ContainerFactory cf) throws ParseException
    {
        if (cf == null) {
            cf = containerFactory;
        }
        return super.parse(string, cf);
    }

}
