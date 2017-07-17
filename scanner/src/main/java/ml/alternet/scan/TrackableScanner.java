package ml.alternet.scan;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;

import ml.alternet.facet.Trackable;

/**
 * A scanner that tracks the current line, column, and offset.
 *
 * @see Scanner#asTrackable()
 *
 * @author Philippe Poulard
 */
public class TrackableScanner extends Scanner implements Trackable {

    static class Position extends Scanner.Cursor { // also hold a mark

        // track the current position
        private long line = 1;
        private long column = 0;
        private long offset = 0;
        // and the next position (read in advance)
        private long nextLine = 1;
        private long nextColumn = 0;
        // allow tracking \r\n
        private boolean previsousWasReturnChar = false;

        Position(int cursor, Position position) {
            super(cursor);
            this.line = position.line;
            this.column = position.column;
            this.offset = position.offset;
            this.nextLine = position.nextLine;
            this.nextColumn = position.nextColumn;
            this.previsousWasReturnChar = position.previsousWasReturnChar;
        }

        Position() {
            super(-1); // current position is not marked
        }

    }

    // the current position doesn't hold a mark
    private Position position = new Position();

    // the underlying concrete scanner
    private Scanner scanner;

    /**
     * Create a new trackable scanner.
     * The internal state of this trackable scanner is
     * the one which is wrapped.
     *
     * @param scanner The scanner to wrap.
     *
     * @throws IOException When an I/O error occur.
     */
    public TrackableScanner(Scanner scanner) throws IOException {
        this.scanner = scanner;
        this.state = scanner.state;
        this.state.source = this; // loop back to this allow trackable methods invokation
        setNextLocation();
    }

    private void setNextLocation() {
        if ( hasNext() ) {
            char c = (char) lookAhead();
            if ( c == '\r' ) {
                this.position.nextLine++;
                this.position.nextColumn = 0;
                this.position.previsousWasReturnChar = true;
            } else {
                if ( c == '\n' ) {
                    if ( ! this.position.previsousWasReturnChar) {
                        this.position.nextLine++;
                        this.position.nextColumn = 0;
                    }
                } else {
                    this.position.nextColumn++;
                }
                this.position.previsousWasReturnChar = false;
            }
            // next offset is always offset + 1
        }
    }

    /**
     * Create a new scanner with an initial position.
     *
     * @param scanner The scanner to wrap.
     * @param line The initial line number.
     * @param column The initial column number.
     * @param offset The initial offset number.
     *
     * @throws IOException When an I/O error occur.
     */
    public TrackableScanner(Scanner scanner, long line, long column, long offset) throws IOException {
        this(scanner);
        this.position.line = line;
        this.position.column = column;
        this.position.offset = offset;
    }

    @Override
    public Optional<ml.alternet.misc.Position> getPosition() {
        return Optional.of(ml.alternet.misc.Position
            .$(this.position.column, this.position.line, this.position.offset));
    }

    /**
     * Read a character.
     * The column and line are updated.
     *
     * @throws IOException When an I/O error occur.
     */
    @Override
    public void read() throws IOException {
        this.position.line = this.position.nextLine;
        this.position.column = this.position.nextColumn;
        this.scanner.read();
        if (hasNext()) {
            this.position.offset++;
        }
        setNextLocation();
    }

    /**
     * Mark the current position.
     * The current column and line are saved.
     *
     * @see Reader#mark(int)
     */
    @Override
    public void mark() {
        this.scanner.mark();
    }

    /**
     * Cancel the current mark.
     * The previous column and line are restored.
     *
     * @throws IllegalStateException When no mark have been previously saved.
     */
    @Override
    public void cancel() throws IllegalStateException {
        if (this.state.cursors.isEmpty()) {
            throw new IllegalStateException( "Can't cancel the reading since no position was marked." );
        } else {
            this.position = (Position) this.state.cursors.peek();
            // we should set this.position.mark = -1 but it is useless
        }
        this.scanner.cancel();
    }

    /**
     * Consume the characters read so far.
     * The previous column and line are removed.
     */
    @Override
    public void consume() {
        this.scanner.consume(); // just drop the last saved position
    }

    @Override
    protected void push( int cursor ) {
        // copy the current position and mark it with the cursor
        Position pos = new Position(cursor, this.position);
        this.state.cursors.push(pos);
    }

    @Override
    public Optional<Reader> getRemainder() throws IOException {
        return this.scanner.getRemainder();
    }

    @Override
    public Optional<String> getRemainderString() throws IOException {
        return this.scanner.getRemainderString();
    }

}
