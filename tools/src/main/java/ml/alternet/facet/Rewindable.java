package ml.alternet.facet;

/**
 * Allow to set marks and to rewind to the marked position.
 *
 * @author Philippe Poulard
 */
public interface Rewindable {

    /**
     * Mark the present position.
     *
     * <p>This method can be called safely several times.</p>
     *
     * <p>For each mark set, there should be sooner or later
     * one consume or cancel.</p>
     *
     * @see #cancel()
     * @see #consume()
     */
    void mark();

    /**
     * Cancel the operations since the last marked position
     * (the next operations will start from the last marked position).
     *
     * @throws IllegalStateException When this method is called
     *                     whereas no position was marked so far.
     *
     * @see #mark()
     */
    void cancel() throws IllegalStateException;

    /**
     * Consume the operations performed so far.
     *
     * <p>This implies that the last marked position is removed
     * and the next operation goes on from the current position.
     * If there wasn't other marker, it will be impossible
     * to go back. If there was at least another one marker,
     * it can be itself cancelled or consumed independently.</p>
     *
     * @throws IllegalStateException When this method is called
     *                     whereas no position was marked so far.
     *
     * @see #mark()
     */
    void consume() throws IllegalStateException;

    /**
     * Consume or cancel the operations since the last marked position.
     *
     * @param consume <code>true</code> to consume, <code>false</code> to cancel.
     * @return The same parameter.
     *
     * @see #consume()
     * @see #cancel()
     */
    default boolean commit(boolean consume) {
        if (consume) {
            consume();
        } else {
            cancel();
        }
        return consume;
    }

}
