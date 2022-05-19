package ru.com.rick.sync.fs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Rick
 */
public final class LogOutputStream extends OutputStream
{
    private final OutputStream out;
    private final OutputStream tee;

    public LogOutputStream(File file, boolean append) throws IOException
    {
        this.out = System.out;
        boolean exists = file.exists();
        this.tee = new FileOutputStream(file, append);
        writeDate(exists && append);
    }

    private void writeDate(boolean exists) throws IOException
    {
        if (exists) {
            this.tee.write(System.lineSeparator().getBytes());
        }
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
        String line = "[" + date + "]" + System.lineSeparator();
        this.tee.write(line.getBytes());
    }

    @Override
    public void write(int b) throws IOException
    {
        out.write(b);
        tee.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException
    {
        out.write(b);
        tee.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        out.write(b, off, len);
        tee.write(b, off, len);
    }

    @Override
    public void flush() throws IOException
    {
        out.flush();
        tee.flush();
    }

    @Override
    public void close() throws IOException
    {
        try {
            out.close();
        } finally {
            tee.close();
        }
    }
}
