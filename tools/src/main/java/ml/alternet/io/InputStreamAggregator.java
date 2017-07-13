package ml.alternet.misc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <tt>InputStreamAggregator</tt> aggregates several InputStreams into a single
 * one.
 *
 * @see java.io.InputStream
 *
 * @author Philippe Poulard
 */
public class InputStreamAggregator extends InputStream {

    private final static Logger LOGGER = Logger.getLogger(InputStreamAggregator.class.getName());

    // An iterator on the input streams.
    private Iterator<InputStream> it = null;
    // The current InputStream.
    private InputStream current = null;
    // True if this close() method has been called.
    private boolean closed = false;

    /**
     * Create a new InputStream aggregator.
     *
     * @param input
     *            A non-{@code null} list (possibly empty) of InputStreams.
     */
    public InputStreamAggregator(List<InputStream> input) {
        this.it = input.iterator();
        next();
    }

    /**
     * Create a new InputStream aggregator.
     *
     * @param input
     *            A non-{@code null} iterator (possibly empty) on InputStreams.
     */
    public InputStreamAggregator(Iterator<InputStream> input) {
        this.it = input;
        next();
    }

    /**
     * Create a new InputStream aggregator.
     *
     * @param input
     *            The InputStream to read.
     */
    public InputStreamAggregator(InputStream... input) {
        this.it = Arrays.asList(input).iterator();
        next();
    }

    /**
     * Advance to the next input.
     */
    private void next() {
        if (this.current != null) {
            try {
                this.current.close();
            } catch (IOException e) {
                LOGGER.log(Level.INFO, e.getMessage(), e);
            }
        }
        if (this.it.hasNext()) {
            this.current = this.it.next();
        } else {
            this.current = null;
        }
    }

    /**
     * Read a single byte. This method will block until a byte is available, an
     * I/O error occurs, or the end of all underlying streams are reached.
     *
     * @return The byte read, as an integer in the range 0 to 255, or -1 if the
     *         end of the stream has been reached.
     *
     * @throws IOException
     *             If an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        if (this.closed)
            throw new IOException("InputStream closed");
        int r = -1;
        if (this.current != null) {
            r = this.current.read();
            if (r == -1) {
                next();
                r = read();
            }
        }
        return r;
    }

    /**
     * Read bytes into an array. This method will block until some input is
     * available, an I/O error occurs, or the end of all underlying streams are
     * reached.
     *
     * @param bytes
     *            The destination buffer.
     * @return The number of bytes read, or -1 if the end of the stream has been
     *         reached.
     *
     * @throws IOException
     *             If an I/O error occurs
     * @throws NullPointerException
     *             If cbuf is null.
     */
    @Override
    public int read(byte[] bytes) throws IOException {
        return read(bytes, 0, bytes.length);
    }

    /**
     * Read bytes into a portion of an array. This method will block until some
     * input is available, an I/O error occurs, or the end of all underlying
     * streams are reached.
     *
     * @param bytes
     *            The destination buffer.
     * @param off
     *            The offset at which to start storing bytes.
     * @param len
     *            The maximum number of bytes to read.
     * @return The number of bytes read, or -1 if the end of the stream has been
     *         reached.
     *
     * @throws IOException
     *             If an I/O error occurs
     * @throws NullPointerException
     *             If "bytes" is null.
     * @throws IndexOutOfBoundsException
     *             If len or offset are out of the boundaries of the buffer.
     */
    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > bytes.length)
            throw new IndexOutOfBoundsException();
        if (this.closed)
            throw new IOException("InputStream closed");
        int r = -1;
        if (this.current != null) {
            r = this.current.read(bytes, off, len);
            if (r == -1) {
                next();
                r = read(bytes, off, len);
            } else if (r < len) {
                next();
                int next = read(bytes, off + r, len - r);
                if (next != -1) {
                    r += next;
                }
            }
        }
        return r;
    }

    /**
     * Skip bytes. This method will block until some bytes are available, an I/O
     * error occurs, or the end of the stream is reached.
     *
     * @param n
     *            The number of bytes to skip.
     * @return The number of bytes actually skipped.
     *
     * @throws IllegalArgumentException
     *             If n is negative.
     * @throws IOException
     *             If an I/O error occurs
     */
    @Override
    public long skip(long n) throws IOException {
        if (this.closed)
            throw new IOException("InputStream closed");
        if (n < 0)
            throw new IllegalArgumentException("Can't skip " + n + " bytes");
        if (n == 0 || this.current == null)
            return 0;
        long s = this.current.skip(n);
        if (s < n) {
            next();
            s += skip(n - s);
        }
        return s;
    }

    /**
     * Returns the number of bytes that can be read from this input stream
     * without blocking.
     * <p>
     * The <code>available</code> method of <code>InputStreamAggregator</code>
     * returns the sum of the the number of bytes remaining to be read in the
     * current buffer (<code>count&nbsp;- pos</code>) and the result of calling
     * the <code>available</code> method of the underlying input stream.
     *
     * @return The number of bytes that can be read from this input stream
     *         without blocking.
     * @exception IOException
     *                if an I/O error occurs.
     * @see java.io.FilterInputStream
     */
    @Override
    public synchronized int available() throws IOException {
        if (this.closed)
            throw new IOException("InputStream closed");
        if (this.current == null) {
            return 0;
        }
        return this.current.available();
    }

    /**
     * Close the stream and any underlying streams. Once a stream has been
     * closed, further read(), ready(), mark(), or reset() invocations will
     * throw an IOException. Closing a previously-closed stream, however, has no
     * effect.
     *
     * @throws IOException
     *             If an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (closed)
            return;
        while (this.current != null) {
            this.current.close();
            next();
        }
        closed = true;
    }

    /**
     * Mark not supported.
     *
     * @param readlimit
     *            Not used.
     */
    @Override
    public void mark(int readlimit) {
    }

    /**
     * Reset not supported.
     *
     * @throws IOException
     *             Always thrown.
     */
    @Override
    public void reset() throws IOException {
        throw new IOException("Reset not supported");
    }

    /**
     * Mark not supported.
     *
     * @return <code>false</code>.
     */
    @Override
    public boolean markSupported() {
        return false;
    }

}
