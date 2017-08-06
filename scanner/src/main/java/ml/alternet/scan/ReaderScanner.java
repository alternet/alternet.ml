package ml.alternet.scan;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Optional;

import ml.alternet.io.IOUtil;
import ml.alternet.io.NoCloseReader;
import ml.alternet.io.ReaderAggregator;
import ml.alternet.misc.Thrower;

/**
 * A scanner for stream of characters.
 *
 * <p>Note: if you intend to scan strings, you are
 * not compelled to use <tt>StringReader</tt>, use
 * the specific scanner instead.</p>
 *
 * @see StringScanner
 *
 * @see Scanner#of(Reader)
 * @see Scanner#of(CharSequence)
 *
 * @author Philippe Poulard
 */
public class ReaderScanner extends Scanner {

    // things to be aware of :
    // -this.state.cursor = 0 unless there is a mark
    //        in that case the cursor is the position from the mark
    // -there is a single phycical mark (set on the underlying reader)
    //        but several logical marks
    //        Only setting/unsetting THE FIRST logical mark cause
    //             setting/unsetting the physical mark

    /** The underlying reader. */
    private Reader reader;
    /** The next char to read to restore after canceling the first mark. */
    private int saveNext;

    /** The maximum number of characters
    *      that can be read before loosing the mark. */
    public int limit = IOUtil.BUFFER_SIZE;

    /**
     * Create a new scanner.
     *
     * @param reader The input to read.
     *      <span style="color:red">Must support marks.</span>
     *
     * @throws IOException When an I/O error occur.
     * @throws IllegalArgumentException When the reader doesn't support marks.
     */
    public ReaderScanner( Reader reader ) throws IOException, IllegalArgumentException {
        // TODO : monitor this reader ???
        assert reader.markSupported();
        if ( reader.markSupported() ) {
            this.reader = reader;
            this.state.source.read();
        } else {
            throw new IllegalArgumentException( "The given reader doesn't support marks." );
        }
    }

    /**
     * Read the next Unicode character.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see #hasNextChar(int, boolean)
     * @see #nextChar()
     * @see #lookAhead()
     */
    @Override
    public void read() throws IOException {
        if (this.state.end) {
            this.state.next = IOUtil.EOF;
        } else {
            this.state.next = this.reader.read();
            this.state.cursor++; // count from the mark
            if (this.state.next == IOUtil.EOF) {
                this.state.end = true;
            } else {
                if (Character.isHighSurrogate((char) this.state.next)) {
                    char c = (char) this.reader.read();
                    this.state.next = Character.toCodePoint((char) this.state.next, c);
                    this.state.cursor++; // count from the mark
                }
                this.state.end = false; // need this on cancel after mark and read til the end
                if (this.state.cursors.isEmpty()) {
                    this.state.cursor = 0; // this is a physical mark
                }
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
     * {@link #lookAhead()}, {@link #nextChar()}, {@link #hasNextChar(int, boolean)}
     * and {@link #hasNextChar(String, boolean)}.</p>
     *
     * @see Reader#mark(int)
     * @see #cancel()
     * @see #consume()
     */
    @Override
    public void mark() {
        if (this.state.cursors.isEmpty()) {
            // when the reader hasn't been marked yet
            // we need to save the current char
            this.saveNext = this.state.next;
            try {
                this.reader.mark( this.limit );
            } catch (IOException e) {
                Thrower.doThrow(e);
            }
            this.state.source.push( 0 );
        } else {
            // the physical mark has been already set
            this.state.source.push( this.state.cursor );
        }
    }

    /**
     * Cancel the characters read since the last marked position
     * (the next read will start from the last marked position).
     *
     * @throws IllegalStateException When this method is called
     *              whereas no position was marked so far.
     *
     * @see #mark()
     */
    @Override
    public void cancel() throws IllegalStateException {
        if (this.state.cursors.isEmpty()) {
            throw new IllegalStateException( "Can't cancel the reading since no position was marked." );
        } else {
            try {
                this.state.cursor = this.state.source.pop();
                this.reader.reset();
                if ( this.state.cursor == 0 ) { // not necessary empty when several
                                                // marks were set at the position 0
                    // restore the char that was saved
                    this.state.next = saveNext;
                }
                if ( ! this.state.cursors.isEmpty() ) {
                    this.reader.mark( this.limit );
                    if (this.state.cursor > 0) {
                        // skip until the previous position...
                        this.reader.skip( this.state.cursor - Character.charCount(this.state.next));
                        // ...that allow to read it
                        this.state.next = (char) this.reader.read();
                        if (Character.isHighSurrogate((char) this.state.next)) {
                            char c = (char) this.reader.read();
                            this.state.next = Character.toCodePoint((char) this.state.next, c);
                            this.state.cursor++; // count from the mark
                        }
                    } // the char that was saved has been restore
                }
                this.state.end = this.state.next == IOUtil.EOF;
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
            this.state.source.pop();
            if (this.state.cursors.isEmpty()) {
                try {
                    // remove the physical mark
                    this.reader.reset();
                    // move to the current place
                    this.reader.skip( this.state.cursor );
                    this.state.cursor = 0; // this is a physical mark
                } catch (IOException e) {
                    Thrower.doThrow(e);
                }
            }
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
        // remember that the current character has been already read
        if ( this.state.end ) {
            return Optional.empty();
        } else {
            int c = this.state.next;
            this.state.next = IOUtil.EOF;
            this.state.end = true;
            return Optional.of(new ReaderAggregator(
                // prepend the current char
                new StringReader( new String(Character.toChars(c) ) ),
                this.state.cursors.isEmpty()
                    ? this.reader
                    : new NoCloseReader( this.reader ) // allow to reset mark
            ));
        }
    }

    /**
     * Return the remainder to read from the
     * current position.
     *
     * @return The remainder to read, or <code>null</code>
     *          if the end was reached.
     *
     * @throws IOException When an I/O error occur.
     */
    @Override
    public Optional<String> getRemainderString() throws IOException {
        Optional<Reader> reader = getRemainder();
        if ( reader.isPresent() ) {
            return Optional.of(readAll( reader.get() ));
        } else {
            return Optional.empty();
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
        for (int n ; (n = input.read(c)) != -1 ; ) {
            out.append( c, 0, n);
        }
        input.close();
        return out.toString();
    }

    @Override
    public String toString() {
        return this.state.toString();
    }

}
