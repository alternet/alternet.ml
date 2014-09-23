package ml.alternet.facet;

/**
 * A trackable object can supply informations about its position (within an
 * input stream).
 *
 * @author Philippe Poulard
 */
public interface Trackable {

    /**
     * Return the current column number.
     *
     * @return The current column number
     */
    int getColumnNumber();

    /**
     * Return the current line number.
     *
     * @return The current line number.
     */
    int getLineNumber();

}
