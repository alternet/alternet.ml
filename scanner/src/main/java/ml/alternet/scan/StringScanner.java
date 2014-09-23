package ml.alternet.scan;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import ml.alternet.util.IOUtil;

/**
 * A scanner for sequences of chars (i.e. strings).
 *
 * @see ReaderScanner
 *
 * @author Philippe Poulard
 */
public class StringScanner extends Scanner {

    /** The underlying sequence of chars. */
    private final CharSequence sequence;

    /**
     * Create a new scanner.
     *
     * @param sequence The input to read, can be {@code null}.
     */
    public StringScanner( CharSequence sequence ) {
        this.sequence = sequence;
        if (this.sequence == null) {
            this.end = true;
            this.next = IOUtil.EOF;
        } else {
            read();
        }
    }

    /**
     * Read the next character.
     *
     * @see #hasNextChar(char, boolean)
     * @see #nextChar()
     * @see #lookAhead()
     */
    @Override
    public void read() {
        if ( this.cursor == this.sequence.length() ) {
            this.end = true;
            this.next = IOUtil.EOF;
        } else {
            this.next = this.sequence.charAt( this.cursor++ );
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
     * @see Reader#mark(int)
     * @see #cancel()
     * @see #consume()
     */
    @Override
    public void mark( int limit ) {
        // the current cursor has read a char in advance
        push( this.cursor - 1 );
    }

    /**
     * Cancel the characters read since the last marked position
     * (the next read will start from the last marked position).
     *
     * @throws IllegalStateException When this method is called
     * 					whereas no position was marked so far.
     *
     * @see #mark(int)
     */
    @Override
    public void cancel() {
        if ( this.pos == 0 ) {
            throw new IllegalStateException( "Can't cancel the reading since no position was marked." );
        } else {
            int prev = pop();
            this.cursor = prev;
            read();
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
    @Override
    public void consume() throws IOException {
        if ( this.pos == 0 ) {
            throw new IllegalStateException( "Can't consume characters since no position was marked." );
        } else {
            pop();
        }
    }

    /**
     * Return the remainder to read from the
     * current position.
     *
     * @return The remainder to read, or <code>null</code>
     * 		if the end was reached.
     */
    @Override
    public Reader getRemainder() {
        String remainder = getRemainderString();
        if ( remainder == null ) {
            return null;
        } else {
            return new StringReader( remainder );
        }
    }

    /**
     * Return the remainder to read from the
     * current position.
     *
     * @return The remainder to read, or <code>null</code>
     * 		if the end was reached.
     */
    @Override
    public String getRemainderString() {
        // remember that the current character has been already read
        if ( this.end ) {
            return null;
        } else {
            return this.sequence.toString().substring( this.cursor - 1 );
        }
    }

}
