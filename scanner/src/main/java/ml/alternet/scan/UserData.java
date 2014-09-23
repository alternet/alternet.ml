package ml.alternet.scan;

import ml.alternet.facet.Unwrappable;

/**
 * A basic container for wrapping a user data.
 *
 * @author Philippe Poulard
 */
public class UserData<T> implements Unwrappable<T> {

    /** The user data. */
    private T userData;
    /** The length, if relevant. */
    private int length = -1;

    /**
     * Set the user data.
     *
     * @param userData The user data, can be <code>null</code>.
     */
    public void setUserData( T userData ) {
        this.userData = userData;
    }

    /**
     * Return the user data.
     *
     * @return The user data, can be <code>null</code>.
     */
    public T unwrap() {
        return this.userData;
    }

    /**
     * The length of the user data if relevant.
     *
     * <p>By default it is not relevant.</p>
     *
     * @return -1 if the length of the user data
     * 		is not relevant, its length otherwise.
     */
    public int getLength() {
        return length;
    }

    /**
     * Set the length of the user data.
     *
     * <p>To make the length irrelevant, set -1.</p>
     *
     * @param length The length of the user data.
     */
    public void setLength(int length) {
        this.length = length;
    }

}
