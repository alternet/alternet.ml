package ml.alternet.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * While reading an input, the data flow is forwarded to
 * an output sink.
 *
 * @author Philippe Poulard
 */
public class TeeInputStream extends FilterInputStream {

    OutputStream out;

    /**
     * Create a Tee.
     *
     * @param in The data source.
     * @param out The output sink.
     */
    public TeeInputStream(InputStream in, OutputStream out) {
        super(in);
        this.out = out;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            out.flush();
            out.close();
        }
    }

    @Override
    public int read() throws IOException {
        int n = super.read();
        if (n != -1) {
            out.write(n);
        }
        return n;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n != -1) {
            out.write(b, off, n);
        }
        return n;
    }

}
