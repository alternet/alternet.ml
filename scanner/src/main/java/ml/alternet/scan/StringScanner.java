package ml.alternet.scan;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Optional;

import ml.alternet.io.IOUtil;
import ml.alternet.misc.Thrower;

/**
 * A scanner for sequences of chars (i.e. strings).
 *
 * @see ReaderScanner
 *
 * @see Scanner#of(Reader)
 * @see Scanner#of(CharSequence)
 *
 * @author Philippe Poulard
 */
public class StringScanner extends Scanner {

    /** The underlying sequence of chars. */
    private final String sequence;

    /**
     * Create a new scanner.
     *
     * @param sequence The input to read, can be {@code null}.
     *
     * @throws IOException When an I/O error occur.
     */
    public StringScanner( CharSequence sequence ) throws IOException {
        this.sequence = sequence.toString();
        if (this.sequence == null) {
            this.state.end = true;
            this.state.next = IOUtil.EOF;
        } else {
            this.state.source.read();
        }
    }

    /**
     * Read the next Unicode character.
     *
     * @see #hasNextChar(int, boolean)
     * @see #nextChar()
     * @see #lookAhead()
     */
    @Override
    public void read() {
        if ( this.state.cursor == this.sequence.length() ) {
            this.state.end = true;
            this.state.next = IOUtil.EOF;
        } else {
            this.state.next = this.sequence.codePointAt( this.state.cursor++ );
            this.state.end = false; // need this on cancel after mark and read til the end
            if (Character.isSupplementaryCodePoint(this.state.next)) {
                this.state.cursor++; // because codePointAt() takes the codepoint at the char index
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
     * @see Reader#mark(int)
     * @see #cancel()
     * @see #consume()
     */
    @Override
    public void mark() {
        // the current cursor has read a char in advance, except on EOF
        this.state.source.push( this.state.cursor - (this.state.end?0:1) );
    }

    /**
     * Cancel the characters read since the last marked position
     * (the next read will start from the last marked position).
     *
     * @throws IllegalStateException When this method is called
     *          whereas no position was marked so far.
     *
     * @see #mark()
     */
    @Override
    public void cancel() throws IllegalStateException {
        if (this.state.cursors.isEmpty()) {
            throw new IllegalStateException( "Can't cancel the reading since no position was marked." );
        } else {
            this.state.cursor = this.state.source.pop();
            try {
                this.state.source.read();
            } catch (IOException e) {
                Thrower.doThrow(e);
            }
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
     * @throws IllegalStateException When this method is called
     *              whereas no position was marked so far.
     *
     * @see #mark()
     */
    @Override
    public void consume() throws IllegalStateException {
        if (this.state.cursors.isEmpty()) {
            throw new IllegalStateException( "Can't consume characters since no position was marked." );
        } else {
            this.state.source.pop(); // just discard the mark
        }
    }

    /**
     * Return the remainder to read from the
     * current position.
     *
     * @return The remainder to read, or <code>null</code>
     *          if the end was reached.
     */
    @Override
    public Optional<Reader> getRemainder() {
        Optional<String> remainder = getRemainderString();
        if ( remainder.isPresent() ) {
            return Optional.of(new StringReader( remainder.get() ));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Return the remainder to read from the
     * current position.
     *
     * @return The remainder to read, or <code>null</code>
     *          if the end was reached.
     */
    @Override
    public Optional<String> getRemainderString() {
        // remember that the current character has been already read
        if ( this.state.end ) {
            return Optional.empty();
        } else {
            int[] codepoints = this.sequence.codePoints()
                .skip(this.state.cursor - Character.charCount(this.state.next))
                .toArray();
            String remainder = new String(codepoints, 0, codepoints.length);
            this.state.cursor = this.sequence.length();
            read(); // just set internal states
            return Optional.of(remainder);
        }
    }

    @Override
    public String toString() {
        return this.sequence + '\n' + this.state;
    }

}
