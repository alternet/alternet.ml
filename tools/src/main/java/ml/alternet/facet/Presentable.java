package ml.alternet.facet;

/**
 * A <tt>Presentable</tt> object can provide a pretty string representation of
 * itself.
 *
 * <p>
 * Usefull for console and logs outputs.
 * </p>
 *
 * @author Philippe Poulard
 */
public interface Presentable {

    /**
     * Return a pretty string representation of this.
     *
     * @return A new string buffer.
     */
    default StringBuilder toPrettyString() {
        return toPrettyString(new StringBuilder());
    }

    /**
     * Append a pretty string representation of this to the given buffer.
     *
     * @param buf
     *            The buffer to append to.
     *
     * @return The buffer given.
     */
    StringBuilder toPrettyString(StringBuilder buf);

}
