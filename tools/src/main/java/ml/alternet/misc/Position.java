package ml.alternet.misc;

/**
 * Informations about the position.
 *
 * @see #$(long, long, long)
 *
 * @author Philippe Poulard
 */
public interface Position {

    /**
     * Return the current column number.
     *
     * @return The current column number, or <code>-1</code>.
     */
    long getColumnNumber();

    /**
     * Return the current line number.
     *
     * @return The current line number, or <code>-1</code>.
     */
    long getLineNumber();

    /**
     * Return the current offset.
     *
     * @return The current offset, or <code>-1</code>.
     */
    long getOffset();

    /**
     * Create a position with the given values.
     *
     * @param column The column number.
     * @param line The line number.
     * @param offset The offset.
     *
     * @return The actual position.
     */
    static Position $(long column, long line, long offset) {
        return new Position() {

            @Override
            public long getColumnNumber() {
                return column;
            }

            @Override
            public long getLineNumber() {
                return line;
            }

            @Override
            public long getOffset() {
                return offset;
            }
        };
    }

}
