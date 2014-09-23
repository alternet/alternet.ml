package ml.alternet.scan;

import java.io.IOException;
import java.io.Reader;
import java.util.Stack;

import ml.alternet.facet.Trackable;

/**
 * A scanner that tracks the current line and column.
 *
 * @author Philippe Poulard
 */
public class TrackableReaderScanner extends ReaderScanner implements Trackable {

    private int line = 1;
    private int column = 0;
    private Stack<Integer> positions;

    /**
     * Create a new scanner.
     *
     * @param reader The input to read.
     *
     * @throws IOException When an I/O error occur.
     */
    public TrackableReaderScanner(Reader reader) throws IOException {
        super(reader);
    }

    /**
     * Create a new scanner with an initial position.
     *
     * @param reader The input to read.
     * @param line The initial line number.
     * @param column The initial column number.
     *
     * @throws IOException When an I/O error occur.
     */
    public TrackableReaderScanner(Reader reader, int line, int column) throws IOException {
        super(reader);
        this.line = line;
        this.column = column;
    }

    /**
     * Return the current column number.
     *
     * @return The current column number
     *
     * @see Trackable#getColumnNumber()
     */
    public int getColumnNumber() {
        return this.column;
    }

    /**
     * Return the current line number.
     *
     * @return The current line number.
     *
     * @see Trackable#getLineNumber()
     */
    public int getLineNumber() {
        return this.line;
    }

    /**
     * Read a character.
     * The column and line are updated.
     *
     * @throws IOException When an I/O error occur.
     */
    public void read() throws IOException {
        super.read();
        if ( hasNext() ) {
            char c = lookAhead();
            if ( c == '\n' || c == '\r' ) {
                this.line++;
                this.column = 0;
            } else {
                this.column++;
            }
        }
    }

    /**
     * Mark the current position.
     * The current column and line are saved.
     *
     * @param limit The max number of characters than can be buffered
     *
     * @throws IOException When an I/O error occur.
     *
     * @see Reader#mark(int)
     */
    public void mark(int limit) throws IOException {
        super.mark(limit);
        if ( this.positions == null ) {
            this.positions = new Stack<Integer>();
        }
        this.positions.push( this.column );
        this.positions.push( this.line );
    }

    /**
     * Cancel the current mark.
     * The previous column and line are restored.
     *
     * @throws IOException When an I/O error occur.
     */
    public void cancel() throws IOException {
        super.cancel();
        this.line = this.positions.pop();
        this.column = this.positions.pop();
    }

    /**
     * Consume the characters read so far.
     * The previous column and line are removed.
     *
     * @throws IOException When an I/O error occur.
     */
    public void consume() throws IOException {
        super.consume();
        this.positions.pop();
        this.positions.pop();
    }

}
