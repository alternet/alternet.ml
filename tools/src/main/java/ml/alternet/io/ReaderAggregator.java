package ml.alternet.io;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <tt>ReaderAggregator</tt> aggregates several Readers into a single one.
 *
 * @see java.io.Reader
 *
 * @author Philippe Poulard
 */
public class ReaderAggregator extends Reader {

    private final static Logger LOGGER = Logger.getLogger(ReaderAggregator.class.getName());

    /** An iterator on the readers. */
    private Iterator<Reader> it = null;
    /** The current reader. */
    private Reader current = null;
    /** True if this close() method has been called. */
    private boolean closed = false;

    /**
     * Create a new reader aggregator.
     *
     * @param readers
     *            A non-{@code null} list (possibly empty) of readers.
     *
     * @see Reader
     */
    public ReaderAggregator(List<Reader> readers) {
        this.it = readers.iterator();
        next();
    }

    /**
     * Create a new reader aggregator.
     *
     * @param readers
     *            A non-{@code null} iterator (possibly empty) on readers.
     *
     * @see Reader
     */
    public ReaderAggregator(Iterator<Reader> readers) {
        this.it = readers;
        next();
    }

    /**
     * Create a new reader aggregator.
     *
     * @param readers
     *            A non-{@code null} list (possibly empty) of readers.
     *
     * @see Reader
     */
    public ReaderAggregator(Reader... readers) {
        this.it = Arrays.asList(readers).iterator();
        next();
    }

    /**
     * Advance to the next reader.
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
     * Read a single character. This method will block until a character is
     * available, an I/O error occurs, or the end of all underlying streams are
     * reached.
     *
     * @return The character read, as an integer in the range 0 to 65535
     *         (0x00-0xffff), or -1 if the end of the stream has been reached.
     *
     * @throws IOException
     *             If an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        if (closed)
            throw new IOException("Reader closed");
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
     * Read characters into an array. This method will block until some input is
     * available, an I/O error occurs, or the end of all underlying streams are
     * reached.
     *
     * @param cbuf
     *            The destination buffer.
     * @return The number of characters read, or -1 if the end of the stream has
     *         been reached.
     *
     * @throws IOException
     *             If an I/O error occurs
     * @throws NullPointerException
     *             If cbuf is null.
     */
    @Override
    public int read(char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    /**
     * Read characters into a portion of an array. This method will block until
     * some input is available, an I/O error occurs, or the end of all
     * underlying streams are reached.
     *
     * @param cbuf
     *            The destination buffer.
     * @param off
     *            The offset at which to start storing characters.
     * @param len
     *            The maximum number of characters to read.
     * @return The number of characters read, or -1 if the end of the stream has
     *         been reached.
     *
     * @throws IOException
     *             If an I/O error occurs
     * @throws NullPointerException
     *             If cbuf is null.
     * @throws IndexOutOfBoundsException
     *             If len or offset are out of the boundaries of the buffer.
     */
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > cbuf.length)
            throw new IndexOutOfBoundsException();
        if (closed)
            throw new IOException("Reader closed");
        int r = -1;
        if (this.current != null) {
            r = this.current.read(cbuf, off, len);
            if (r == -1) {
                next();
                r = read(cbuf, off, len);
            } else if (r < len) {
                next();
                int next = read(cbuf, off + r, len - r);
                if (next != -1) {
                    r += next;
                }
            }
        }
        return r;
    }

    /**
     * Skip characters. This method will block until some characters are
     * available, an I/O error occurs, or the end of the stream is reached.
     *
     * @param n
     *            The number of characters to skip.
     * @return The number of characters actually skipped.
     *
     * @throws IllegalArgumentException
     *             If n is negative.
     * @throws IOException
     *             If an I/O error occurs
     */
    @Override
    public long skip(long n) throws IOException {
        if (closed)
            throw new IOException("Reader closed");
        if (n < 0)
            throw new IllegalArgumentException("Can't skip " + n + " characters");
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
     * Tell whether this stream is ready to be read.
     *
     * @return <code>true</code> if the next read() is guaranteed not to block
     *         for input, <code>false</code> otherwise. Note that returning
     *         false does not guarantee that the next read will block.
     *
     * @throws IOException
     *             If an I/O error occurs.
     */
    @Override
    public boolean ready() throws IOException {
        if (closed)
            throw new IOException("Reader closed");
        if (this.current == null) {
            return false;
        } else {
            return this.current.ready();
        }
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
     *
     * @throws IOException
     *             Always thrown.
     */
    @Override
    public void mark(int readlimit) throws IOException {
        throw new IOException("Mark not supported");
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
