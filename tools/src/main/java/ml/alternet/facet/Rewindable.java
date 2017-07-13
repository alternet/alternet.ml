package ml.alternet.facet;

import java.io.IOException;
import java.io.Reader;

public interface Rewindable {

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
     * @throws IOException When an I/O error occur.
     *
     * @see Reader#mark(int)
     * @see #cancel()
     * @see #consume()
     */
    void mark() throws IOException;

    /**
     * Cancel the characters read since the last marked position
     * (the next read will start from the last marked position).
     *
     * @throws IOException When an I/O error occur.
     * @throws IllegalStateException When this method is called
     *                     whereas no position was marked so far.
     *
     * @see #mark()
     */
    void cancel() throws IOException, IllegalStateException;

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
     *                     whereas no position was marked so far.
     *
     * @see #mark()
     */
    void consume() throws IOException, IllegalStateException;

    /**
     * Consume or cancel the characters read since the last marked position.
     *
     * @param consume <code>true</code> to consume, <code>false</code> to cancel.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see #consume()
     * @see #cancel()
     */
    default void commit(boolean consume) throws IOException {
        if (consume) {
            consume();
        } else {
            cancel();
        }
    }

}
