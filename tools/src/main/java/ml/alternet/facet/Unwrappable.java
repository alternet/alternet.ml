package ml.alternet.facet;

/**
 * An unwrappable object is an object that wraps another object for
 * compatibility purpose, and that can be safely unwrapped.
 *
 * @author Philippe Poulard
 *
 * @param <T>
 *            The type of the class to unwrap.
 */
public interface Unwrappable<T> {

    /**
     * The singleton unwrapper.
     */
    Unwrapper UNWRAPPER = new Unwrapper();

    /**
     * Unwrap recursively an unwrappable if necessary.
     *
     * @see Unwrappable#UNWRAPPER
     *
     * @author Philippe Poulard
     */
    class Unwrapper {
        /**
         * Unusable constructor.
         */
        private Unwrapper() { }

        /**
         * Unwrap an object.
         *
         * @param o
         *            The object to unwrap
         * @param <T>
         *            The type of the class to unwrap.
         *
         * @return The object itself if it not unwrappable, or the unwrapped
         *         object (recursively).
         */
        @SuppressWarnings("unchecked")
        <T> T unwrap(Object o) {
            while (o instanceof Unwrappable) {
                @SuppressWarnings("rawtypes")
                Object unwrapped = ((Unwrappable) o).unwrap();
                if (unwrapped == o) {
                    o = unwrapped;
                    break;
                } else {
                    o = unwrapped;
                }
            }
            return (T) o;
        }
    }

    /**
     * Return the object wrapped. The object wrapped must be of the same type of
     * the wrapper.
     *
     * @return The wrapped object.
     */
    T unwrap();

}
