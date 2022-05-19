package ru.com.rick.sync.run;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.parser.ParseException;
import ru.com.rick.sync.Controller;
import ru.com.rick.sync.json.JsonUtils;

public final class MultiSync
{
    public static void main(final String[] args)
    {
        Options options = new Options();
        if (args.length == 0) {
            printUsage(null);
            return;
        }

        int index = 0;
        do {
            try {
                int c = setOption(options, args[index], args, index);
                index += c + 1;
            } catch (ArgException ex) {
                printUsage(ex.getMessage());
                return;
            }
        } while (index < args.length);

        if (!options.containsKey("")) {
            printUsage("No path to JSON config supplied");
            return;
        }

        String jsonPath = options.get("")[0];
        runSync(jsonPath);
    }

    private static void runSync(String jsonPath)
    {
        Map config;
        try {
            config = JsonUtils.readJsonFile(new File(jsonPath), Map.class);
        } catch (IOException ex) {
            System.err.println("Unable to read JSON config '" + jsonPath + "'");
            System.err.println(ex.toString());
            return;
        } catch (ParseException ex) {
            System.err.println("Unable to parse JSON config in '" + jsonPath + "'");
            System.err.println(ex.toString());
            return;
        }

        Controller controller;
        try {
            controller = new Controller(config, jsonPath);
        } catch (Exception ex) {
            System.err.println("Interpretaion of JSON config failed");
            ex.printStackTrace(System.err);
            return;
        }

        DefaultRunner runner = new DefaultRunner(controller);
        runner.run(true);
    }

    private static int setOption(Options options, String arg, String[] args, int index) throws ArgException
    {
        if (arg.startsWith("--")) {
            if (arg.equals("--test")) {
                return setOption(options, arg, args, index, 0);
            }
            throw new ArgException("Unknown option '" + arg + "'");
        }
        if (options.containsKey("")) {
            throw new ArgException("Path to JSON config already set");
        }
        String[] value = new String[]{arg};
        options.put("", value);
        return 0;
    }

    private static int setOption(Options options, String opt, String[] args, int index, int count) throws ArgException
    {
        if (options.containsKey(opt)) {
            throw new ArgException("Option '" + opt + "' is already set");
        }
        String[] value = Arrays.copyOfRange(args, index + 1, index + 1 + count);
        options.put(opt, value);
        return count;
    }

    private static void printUsage(String message)
    {
        if (message != null) {
            System.err.println(message);
        }
        System.out.println("Usage: <main class> JSON_CONFIG");
        //-----------------#    **op                        # text here
        System.out.println("      JSON_CONFIG               Synchronization config file in JSON format.");
        System.out.println("      --test                    Test option, does nothing.");
    }

    private static class Options extends HashMap<String, String[]>
    {
    }

    private static class ArgException extends Exception
    {
        public ArgException(String message)
        {
            super(message);
        }

    }

}
