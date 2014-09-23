package ml.alternet.scan;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import ml.alternet.misc.ReaderAggregator;
import ml.alternet.util.IOUtil;

/**
 * A scanner for stream of characters.
 *
 * <p>Note: if you intend to scan strings, you are
 * not compelled to use <tt>StringReader</tt>, use
 * the specific scanner instead.</p>
 *
 * @see StringScanner
 *
 * @author Philippe Poulard
 */
public class ReaderScanner extends Scanner {

    /** The underlying reader. */
    private Reader reader;
    /** The next char to read to restore after canceling the first mark. */
    private char saveNext;
    /** The cumulative mark limit. */
    private int limit;

    /**
     * Create a new scanner.
     *
     * @param reader The input to read.
     * 		<span style="color:red">Must support marks.</span>
     *
     * @throws IOException When an I/O error occur.
     * @throws IllegalArgumentException When the reader doesn't support marks.
     */
    public ReaderScanner( Reader reader ) throws IOException {
        assert reader.markSupported();
        if ( reader.markSupported() ) {
            this.reader = reader;
            read();
        } else {
            throw new IllegalArgumentException( "The given reader doesn't support marks." );
        }
    }

    /**
     * Read the next character.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see #hasNextChar(char, boolean)
     * @see #nextChar()
     * @see #lookAhead()
     */
    public void read() throws IOException {
        int read = this.reader.read();
        this.next = (char) read;
        if ( read == -1 ) {
            this.end = true;
        } else {
            if ( this.pos > 0 ) {
                // cursors not empty
                this.cursor++;
            }
        }
    }

    /**
     * Mark the present position in the stream.
     *
     * <p>This method can be called safely several times.</p>
     *
     * <p>For each mark set, there should be sooner or later
     * a consume or cancel.</p>
     *
     * <p>Some convenient methods are available for getting a single
     * character without using a mark :
     * {@link #lookAhead()}, {@link #nextChar()}, {@link #hasNextChar(char, boolean)}
     * and {@link #hasNextChar(String, boolean)}.</p>
     *
     * @param limit The maximum number of characters
     * 		that can be read before loosing the mark.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see Reader#mark(int)
     * @see #cancel()
     * @see #consume()
     */
    public void mark( int limit ) throws IOException {
        if ( this.pos == 0 ) {
            // when the reader hasn't been marked yet, we need to save the current char
            this.saveNext = this.next;
            this.reader.mark( limit );
            push( 0 );
            this.limit = limit;
        } else {
            this.reader.reset();
            this.limit = this.cursor + limit;
            this.reader.mark( this.limit );
            this.reader.skip( this.cursor );
            push( this.cursor );
        }
    }

    /**
     * Cancel the characters read since the last marked position
     * (the next read will start from the last marked position).
     *
     * @throws IOException When an I/O error occur.
     * @throws IllegalStateException When this method is called
     * 					whereas no position was marked so far.
     *
     * @see #mark(int)
     */
    public void cancel() throws IOException {
        if ( this.pos == 0 ) {
            throw new IllegalStateException( "Can't cancel the reading since no position was marked." );
        } else {
            int prev = pop();
            this.cursor = prev;
            this.reader.reset();
            if ( prev > 0 ) {
                this.reader.mark( this.limit );
                this.reader.skip( this.cursor );
                read();
            } else {
                // everything was canceled : restore the current char
                this.next = this.saveNext;
            }
            this.end = false;
        }
    }

    /**
     * Consume the characters read so far.
     *
     * <p>This implies that the last marked position is removed
     * and the next read goes on from the current position.
     * If there wasn't other marker, it will be impossible
     * to go back. If there was at least another one marker,
     * it can be itself cancelled or consumed independently.</p>
     *
     * @throws IOException When an I/O error occur.
     * @throws IllegalStateException When this method is called
     * 					whereas no position was marked so far.
     *
     * @see #mark(int)
     */
    public void consume() throws IOException {
        if ( this.pos == 0 ) {
            throw new IllegalStateException( "Can't consume characters since no position was marked." );
        } else {
            int prev = pop();
            this.reader.reset();
            if ( prev > 0 ) {
                this.reader.mark( this.limit );
            }
            this.end = false;
            this.reader.skip( this.cursor - prev );
            if ( prev == 0 ) {
                this.cursor = 0;
            }
        }
    }

    /**
     * Return the remainder to read from the
     * current position.
     *
     * @return The remainder to read, or <code>null</code>
     * 		if the end was reached.
     */
    public Reader getRemainder() {
        // remember that the current character has been already read
        char c = lookAhead();
        if ( c == IOUtil.EOF ) {
            return null;
        } else {
            return new ReaderAggregator(
                // prepend the current char
                new StringReader( String.valueOf( c ) ),
                this.reader
            );
        }
    }

    /**
     * Return the remainder to read from the
     * current position.
     *
     * @return The remainder to read, or <code>null</code>
     * 		if the end was reached.
     *
     * @throws IOException When an I/O error occur.
     */
    public String getRemainderString() throws IOException {
        Reader reader = getRemainder();
        if ( reader == null ) {
            return null;
        } else {
            return readAll( reader );
        }
    }

    /**
     * Read all a character stream in a string.
     *
     * @param input The character stream to read.
     * @return The whole content of the input as a single string.
     *
     * @throws IOException When an I/O error occurs.
     */
    public static String readAll( Reader input ) throws IOException {
        StringBuilder out = new StringBuilder(IOUtil.BUFFER_SIZE);
        char[] c = new char[IOUtil.BUFFER_SIZE];
        for (int n; (n = input.read(c)) != -1;) {
            out.append( c, 0, n);
        }
        input.close();
        return out.toString();
    }


}
