package rf.mizuka.io;

import java.io.IOException;
import java.io.InputStream;

public class AudioInputStream
        extends InputStream
{
    private final InputStream source;
    private final long maxBytesToRead;
    private long bytesRead = 0;

    public AudioInputStream(InputStream source, long startByte, long rangeLength)
            throws IOException
    {
        this.source = source;
        this.maxBytesToRead = rangeLength;

        long skipped = source.skip(startByte);
        if (skipped < startByte)
        {
            throw new IOException("Could not skipped to needed position");
        }
    }

    @Override
    public int read()
            throws IOException
    {
        if (bytesRead >= maxBytesToRead)
        {
            return -1;
        }

        int b = source.read();
        if (b != -1)
        {
            bytesRead++;
        }

        return b;
    }

    @Override
    public int read(byte[] b, int off, int len)
            throws IOException
    {
        if (bytesRead >= maxBytesToRead)
        {
            return -1;
        }

        long remaining = maxBytesToRead - bytesRead;
        int bytesToReadNow = (int) Math.min(len, remaining);

        int result = source.read(b, off, bytesToReadNow);
        if (result != -1)
        {
            bytesRead += result;
        }

        return result;
    }

    @Override
    public void close()
            throws IOException
    {
        source.close();
    }
}